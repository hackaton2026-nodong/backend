import json
import os
import threading
import time
import urllib.request
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer


PORT = int(os.getenv("OCR_MOCK_PORT", "9000"))
DEFAULT_CALLBACK_TOKEN = os.getenv("OCR_CALLBACK_TOKEN", "local-ocr-token")
BACKEND_BASE_URL = os.getenv("OCR_BACKEND_BASE_URL", "http://host.docker.internal:8080")

MOCK_OCR_TEXT = (
    "## 표준근로계약서 Standard Labor Contract\n"
    "한국제조 031-555-1290 김민수 Identification number 214-86-73951 "
    "MARIA LUZ SANTOS 1998-07-21 "
    "26년 06월 01일 ~ 27년 05월 31일 "
    "- 수습기간: [√] 활용(입국일부터 [√] 1개월) "
    "- 업종: 제조업 - 사업내용: 자동차 금속부품 생산 "
    "- 직무내용: 금속부품 조립, 품질검사, 포장작업 "
    "08시 30분 ~ 17시 30분 "
    "-1일 평균 시간외 근로시간: 1시간 "
    "(사업장 사정에 따라 변동 가능: 2시간 이내) "
    "5. 휴게시간 1일 60분 "
    "6. 휴일 [√]일요일 [√]공휴일([√]유급 [ ]무급) [√]매주 토요일 "
    "7. 임금 1) 월 통상임금 ( 2,300,000 )원"
    "- 기본급[ 월급 ] ( 2,150,000 )원"
    "- 고정적 수당: ( 생산 수당: 100,000 )원), ( 식대 수당: 50,000 )원)"
    "- 상여금 ( 0 )원) "
    "8) 임금지급일 매월 ( 10 )일 "
    "9) 지급방법 [ ]직접 지급, [ √ ]통장 임금 "
    "1) 숙박시설 제공- 숙박시설 제공 여부: [ √ ]제공 [ ]미제공 "
    "기타주택형태 시설( 기숙사 ))"
    "10) 숙박시설 제공 시 근로자 부담금액: 매월 150,000 원"
    "2) 식사 제공- 식사 제공 여부: 제공([ ]조식, [ √ ]중식, [ ]석식), [ ]미제공"
    "- 식사 제공시 근로자 부담금액:매월 0 )원 "
    "2026.06.01. 사용자:김민수 근로자 : MARIA LUZ SANTOS"
)


def callback_later(callback_url, token):
    time.sleep(0.5)
    payload = {
        "ocrResult": {
            "layoutParsingResults": [
                {
                    "markdown": {
                        "text": MOCK_OCR_TEXT
                    }
                }
            ]
        }
    }
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
    try:
        with urllib.request.urlopen(request, timeout=10) as response:
            response.read()
            print(f"callback sent: {callback_url} status={response.status}", flush=True)
    except Exception as exc:
        print(f"callback failed: {callback_url} error={exc}", flush=True)


class Handler(BaseHTTPRequestHandler):
    def do_GET(self):
        if self.path == "/health":
            self.send_json(200, {"status": "ok"})
            return
        self.send_json(404, {"error": "not_found"})

    def do_POST(self):
        if self.path != "/ocr/jobs":
            self.send_json(404, {"error": "not_found"})
            return

        content_length = int(self.headers.get("Content-Length", "0"))
        body = self.rfile.read(content_length)
        print(
            f"request content-length={content_length} content-type={self.headers.get('Content-Type')} body={body.decode('utf-8', errors='replace')}",
            flush=True,
        )
        try:
            job = json.loads(body.decode("utf-8")) if body else {}
        except json.JSONDecodeError:
            self.send_json(400, {"error": "invalid_json"})
            return

        print(f"job received: {json.dumps(job, ensure_ascii=False)}", flush=True)

        callback_url = job.get("callbackUrl") or job.get("callback_url")
        if not callback_url:
            document_id = job.get("documentId") or job.get("document_id")
            if not document_id:
                self.send_json(400, {"error": "callbackUrl_or_documentId_required"})
                return
            callback_url = (
                BACKEND_BASE_URL.rstrip("/")
                + f"/api/internal/documents/{document_id}/ocr-result"
            )

        callback_token = self.headers.get("X-OCR-Callback-Token", DEFAULT_CALLBACK_TOKEN)
        threading.Thread(
            target=callback_later,
            args=(callback_url, callback_token),
            daemon=True,
        ).start()
        self.send_json(202, {
            "accepted": True,
            "documentId": job.get("documentId"),
            "callbackUrl": callback_url,
        })

    def send_json(self, status, payload):
        data = json.dumps(payload, ensure_ascii=False).encode("utf-8")
        self.send_response(status)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(data)))
        self.end_headers()
        self.wfile.write(data)

    def log_message(self, fmt, *args):
        print(fmt % args, flush=True)


if __name__ == "__main__":
    server = ThreadingHTTPServer(("0.0.0.0", PORT), Handler)
    print(f"mock OCR worker listening on :{PORT}", flush=True)
    server.serve_forever()
