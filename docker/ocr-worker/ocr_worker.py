import hashlib
import json
import os
import tempfile
import threading
import time
import traceback
import urllib.request
import urllib.error
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path

os.environ.setdefault("FLAGS_enable_pir_api", "0")
os.environ.setdefault("PADDLE_PDX_ENABLE_MKLDNN_BYDEFAULT", "False")
os.environ.setdefault("PADDLE_PDX_CPU_NUM_THREADS", "4")

import numpy as np
from paddleocr import PaddleOCR


PORT = int(os.getenv("OCR_WORKER_PORT", "9000"))
STORAGE_ROOT = Path(os.getenv("OCR_STORAGE_ROOT", "/documents")).resolve()
DEFAULT_CALLBACK_TOKEN = os.getenv("OCR_CALLBACK_TOKEN", "")
BACKEND_BASE_URL = os.getenv("OCR_BACKEND_BASE_URL", "http://host.docker.internal:8080")
MAX_PAGES = int(os.getenv("OCR_MAX_PAGES", "20"))
ENGINE_NAME = "PADDLE_OCR_V5_KOREAN"
TEXT_DETECTION_MODEL = os.getenv("OCR_TEXT_DETECTION_MODEL", "PP-OCRv5_mobile_det")
TEXT_RECOGNITION_MODEL = os.getenv("OCR_TEXT_RECOGNITION_MODEL", "korean_PP-OCRv5_mobile_rec")
TEXT_DET_LIMIT_SIDE_LEN = int(os.getenv("OCR_TEXT_DET_LIMIT_SIDE_LEN", "1280"))
TEXT_RECOGNITION_BATCH_SIZE = int(os.getenv("OCR_TEXT_RECOGNITION_BATCH_SIZE", "8"))

ENGINE = None
ENGINE_LOCK = threading.Lock()
ENGINE_READY = threading.Event()
ENGINE_ERROR = None


def get_engine():
    global ENGINE, ENGINE_ERROR
    if ENGINE_READY.is_set():
        return ENGINE
    with ENGINE_LOCK:
        if ENGINE is None:
            try:
                started_at = time.time()
                print(f"initializing OCR engine={ENGINE_NAME}", flush=True)
                ENGINE = PaddleOCR(
                    text_detection_model_name=TEXT_DETECTION_MODEL,
                    text_recognition_model_name=TEXT_RECOGNITION_MODEL,
                    use_doc_orientation_classify=False,
                    use_doc_unwarping=False,
                    use_textline_orientation=False,
                    text_det_limit_side_len=TEXT_DET_LIMIT_SIDE_LEN,
                    text_recognition_batch_size=TEXT_RECOGNITION_BATCH_SIZE,
                    device=os.getenv("OCR_DEVICE", "cpu"),
                )
                ENGINE_READY.set()
                ENGINE_ERROR = None
                print(f"OCR engine ready engine={ENGINE_NAME} elapsedMs={round((time.time() - started_at) * 1000)}", flush=True)
            except Exception as exc:
                ENGINE_ERROR = {
                    "code": exc.__class__.__name__,
                    "message": str(exc),
                }
                print(traceback.format_exc(), flush=True)
                raise
    return ENGINE


def warmup_engine():
    try:
        get_engine()
    except Exception:
        pass


def callback(callback_url, token, payload):
    started_at = time.time()
    data = json.dumps(payload, ensure_ascii=False).encode("utf-8")
    request = urllib.request.Request(
        callback_url,
        data=data,
        method="POST",
        headers={
            "Content-Type": "application/json",
            "X-OCR-Callback-Token": token or DEFAULT_CALLBACK_TOKEN,
        },
    )
    with urllib.request.urlopen(request, timeout=30) as response:
        response.read()
        print(f"callback sent: {callback_url} status={response.status} elapsedMs={elapsed_ms(started_at)} bytes={len(data)}", flush=True)


def elapsed_ms(started_at):
    return round((time.time() - started_at) * 1000)


def secure_document_path(storage_key):
    if not storage_key:
        raise ValueError("storageKey is required")
    normalized_storage_key = storage_key.replace("\\", "/")
    path = (STORAGE_ROOT / normalized_storage_key).resolve()
    if not path.is_relative_to(STORAGE_ROOT):
        raise ValueError("storageKey escapes storage root")
    if not path.is_file():
        raise FileNotFoundError(f"stored document not found: {normalized_storage_key}")
    return path


def sha256_hex(path):
    started_at = time.time()
    digest = hashlib.sha256()
    with path.open("rb") as source:
        for chunk in iter(lambda: source.read(1024 * 1024), b""):
            digest.update(chunk)
    value = digest.hexdigest()
    print(f"document hash completed path={path.name} elapsedMs={elapsed_ms(started_at)}", flush=True)
    return value


def normalize_result(index, result):
    data = to_jsonable(getattr(result, "json", {}))
    if isinstance(data, dict) and isinstance(data.get("res"), dict):
        data = data["res"]
    markdown = to_jsonable(getattr(result, "markdown", {}))
    page_number = page_number_from(data, index)
    blocks = extract_blocks(data, page_number)
    markdown_text = markdown_text_from(markdown)
    return {
        "page": page_number,
        "markdown": {"text": markdown_text or "\n".join(block["block_content"] for block in blocks if block["block_content"])},
        "prunedResult": {"parsing_res_list": blocks},
        "sourceResult": data,
    }


def page_number_from(data, index):
    page_index = data.get("page_index")
    if isinstance(page_index, int):
        return page_index + 1
    page_number = data.get("page") or data.get("page_num")
    return int(page_number) if isinstance(page_number, (int, float)) else index + 1


def markdown_text_from(markdown):
    if not isinstance(markdown, dict):
        return ""
    value = markdown.get("markdown_texts") or markdown.get("text") or markdown.get("markdown")
    if isinstance(value, list):
        return "\n".join(str(item) for item in value if item)
    return str(value) if value else ""


def extract_blocks(data, page_number):
    blocks = []
    append_ocr_blocks(blocks, data, page_number)
    append_ocr_blocks(blocks, data.get("overall_ocr_res"), page_number)
    append_ocr_blocks(blocks, data.get("text_paragraphs_ocr_res"), page_number)
    append_table_blocks(blocks, data.get("table_res_list"), page_number)
    append_parsing_blocks(blocks, data.get("parsing_res_list"), page_number)
    append_parsing_blocks(blocks, data.get("layout_parsing_result"), page_number)
    return blocks


def append_ocr_blocks(blocks, ocr_result, page_number):
    if not isinstance(ocr_result, dict):
        return
    texts = ocr_result.get("rec_texts") or []
    scores = ocr_result.get("rec_scores") or []
    boxes = ocr_result.get("rec_boxes") or ocr_result.get("rec_polys") or []
    for index, text in enumerate(texts):
        if not text:
            continue
        blocks.append({
            "page": page_number,
            "block_label": "text",
            "block_bbox": box_to_xyxy(boxes[index]) if index < len(boxes) else None,
            "block_content": str(text),
            "confidence": numeric(scores[index]) if index < len(scores) else None,
        })


def append_table_blocks(blocks, table_results, page_number):
    if not isinstance(table_results, list):
        return
    for table in table_results:
        if not isinstance(table, dict):
            continue
        html = table.get("pred_html")
        if html:
            blocks.append({
                "page": page_number,
                "block_label": "table",
                "block_bbox": box_to_xyxy(table.get("rec_boxes") or table.get("rec_polys")),
                "block_content": str(html),
                "confidence": None,
            })
        append_ocr_blocks(blocks, table.get("table_ocr_pred"), page_number)


def append_parsing_blocks(blocks, parsing_results, page_number):
    if not isinstance(parsing_results, list):
        return
    for item in parsing_results:
        if not isinstance(item, dict):
            continue
        content = item.get("block_content") or item.get("text") or item.get("content")
        if not content:
            continue
        blocks.append({
            "page": int(item.get("page") or page_number),
            "block_label": item.get("block_label") or item.get("label") or item.get("type") or "unknown",
            "block_bbox": box_to_xyxy(item.get("block_bbox") or item.get("bbox") or item.get("box")),
            "block_content": str(content),
            "confidence": numeric(item.get("confidence") or item.get("score")),
        })


def box_to_xyxy(value):
    if value is None:
        return None
    if isinstance(value, np.ndarray):
        value = value.tolist()
    if not isinstance(value, list) or not value:
        return None
    if all(isinstance(item, (int, float)) for item in value[:4]):
        return [float(item) for item in value[:4]]
    points = value[0] if len(value) == 1 and isinstance(value[0], list) else value
    xs = []
    ys = []
    for point in points:
        if isinstance(point, list) and len(point) >= 2:
            xs.append(float(point[0]))
            ys.append(float(point[1]))
    if not xs or not ys:
        return None
    return [min(xs), min(ys), max(xs), max(ys)]


def numeric(value):
    if isinstance(value, (int, float)):
        return float(value)
    return None


def to_jsonable(value):
    if isinstance(value, np.ndarray):
        return value.tolist()
    if isinstance(value, np.generic):
        return value.item()
    if isinstance(value, dict):
        return {str(key): to_jsonable(item) for key, item in value.items() if key != "img"}
    if isinstance(value, (list, tuple)):
        return [to_jsonable(item) for item in value]
    return value


def run_job(job, callback_token):
    job_started_at = time.time()
    callback_url = job.get("callbackUrl") or job.get("callback_url")
    document_id = job.get("documentId") or job.get("document_id")
    print(f"ocr job received documentId={document_id} storageKey={job.get('storageKey') or job.get('storage_key')}", flush=True)
    if not callback_url:
        if not document_id:
            raise ValueError("callbackUrl or documentId is required")
        callback_url = BACKEND_BASE_URL.rstrip("/") + f"/api/internal/documents/{document_id}/ocr-result"

    try:
        document_path = secure_document_path(job.get("storageKey") or job.get("storage_key"))
        expected_hash = (job.get("sha256Hash") or job.get("sha256_hash") or "").lower()
        actual_hash = sha256_hex(document_path)
        if expected_hash and expected_hash != actual_hash:
            raise ValueError("stored document hash mismatch")

        engine = get_engine()
        predict_started_at = time.time()
        print(
            f"ocr predict started documentId={document_id} detModel={TEXT_DETECTION_MODEL} recModel={TEXT_RECOGNITION_MODEL} "
            f"detLimitSideLen={TEXT_DET_LIMIT_SIDE_LEN} recBatch={TEXT_RECOGNITION_BATCH_SIZE}",
            flush=True,
        )
        output = list(engine.predict(
            input=str(document_path),
            text_det_limit_side_len=TEXT_DET_LIMIT_SIDE_LEN,
        ))
        predict_elapsed = elapsed_ms(predict_started_at)
        print(f"ocr predict completed documentId={document_id} pages={len(output)} elapsedMs={predict_elapsed}", flush=True)
        normalize_started_at = time.time()
        layout_results = [normalize_result(index, result) for index, result in enumerate(output[:MAX_PAGES])]
        normalize_elapsed = elapsed_ms(normalize_started_at)
        print(f"ocr normalize completed documentId={document_id} pages={len(layout_results)} elapsedMs={normalize_elapsed}", flush=True)

        payload = {
            "ocrResult": {
                "engine": ENGINE_NAME,
                "documentId": document_id,
                "sha256Hash": actual_hash,
                "layoutParsingResults": layout_results,
                "metadata": {
                    "pageCount": len(layout_results),
                    "elapsedMs": elapsed_ms(job_started_at),
                    "predictElapsedMs": predict_elapsed,
                    "normalizeElapsedMs": normalize_elapsed,
                    "detectionModel": TEXT_DETECTION_MODEL,
                    "recognitionModel": TEXT_RECOGNITION_MODEL,
                    "detLimitSideLen": TEXT_DET_LIMIT_SIDE_LEN,
                    "recognitionBatchSize": TEXT_RECOGNITION_BATCH_SIZE,
                },
            }
        }
    except Exception as exc:
        payload = {
            "ocrResult": {
                "engine": ENGINE_NAME,
                "documentId": document_id,
                "error": {
                    "code": exc.__class__.__name__,
                    "message": str(exc),
                },
                "layoutParsingResults": [],
            }
        }
        print(traceback.format_exc(), flush=True)

    try:
        callback(callback_url, callback_token, payload)
    except urllib.error.HTTPError as exc:
        error_body = exc.read().decode("utf-8", errors="replace")
        print(
            f"callback failed: {callback_url} status={exc.code} elapsedMs={elapsed_ms(job_started_at)} body={error_body[:2000]}",
            flush=True,
        )
    except Exception as exc:
        print(
            f"callback failed: {callback_url} error={exc.__class__.__name__}: {exc} elapsedMs={elapsed_ms(job_started_at)}",
            flush=True,
        )


class Handler(BaseHTTPRequestHandler):
    def do_GET(self):
        if self.path == "/health":
            if ENGINE_READY.is_set():
                self.send_json(200, {"status": "ready", "engine": ENGINE_NAME})
            elif ENGINE_ERROR:
                self.send_json(500, {"status": "failed", "engine": ENGINE_NAME, "error": ENGINE_ERROR})
            else:
                self.send_json(503, {"status": "warming", "engine": ENGINE_NAME})
            return
        self.send_json(404, {"error": "not_found"})

    def do_POST(self):
        if self.path != "/ocr/jobs":
            self.send_json(404, {"error": "not_found"})
            return

        content_length = int(self.headers.get("Content-Length", "0"))
        body = self.rfile.read(content_length)
        try:
            job = json.loads(body.decode("utf-8")) if body else {}
        except json.JSONDecodeError:
            self.send_json(400, {"error": "invalid_json"})
            return

        threading.Thread(
            target=run_job,
            args=(job, self.headers.get("X-OCR-Callback-Token", DEFAULT_CALLBACK_TOKEN)),
            daemon=True,
        ).start()
        self.send_json(202, {"accepted": True, "documentId": job.get("documentId")})

    def send_json(self, status, payload):
        data = json.dumps(payload, ensure_ascii=False).encode("utf-8")
        try:
            self.send_response(status)
            self.send_header("Content-Type", "application/json; charset=utf-8")
            self.send_header("Content-Length", str(len(data)))
            self.end_headers()
            self.wfile.write(data)
        except BrokenPipeError:
            print(f"client disconnected before response status={status}", flush=True)

    def log_message(self, fmt, *args):
        print(fmt % args, flush=True)


if __name__ == "__main__":
    with tempfile.TemporaryDirectory():
        if os.getenv("OCR_WARMUP_ON_START", "true").lower() != "false":
            threading.Thread(target=warmup_engine, daemon=True).start()
        server = ThreadingHTTPServer(("0.0.0.0", PORT), Handler)
        print(f"PaddleOCR worker listening on :{PORT} engine={ENGINE_NAME} storage={STORAGE_ROOT}", flush=True)
        server.serve_forever()
