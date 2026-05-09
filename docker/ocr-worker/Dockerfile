FROM python:3.10-bullseye

ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 \
    FLAGS_enable_pir_api=0 \
    PADDLE_PDX_ENABLE_MKLDNN_BYDEFAULT=False

RUN apt-get update \
    && apt-get install -y --no-install-recommends \
        libglib2.0-0 \
        libgl1 \
        libgomp1 \
    && rm -rf /var/lib/apt/lists/*

RUN python -m pip install --no-cache-dir --upgrade pip \
    && python -m pip install --no-cache-dir \
        "paddlepaddle>=3.3,<3.4" -i https://www.paddlepaddle.org.cn/packages/stable/cpu/ \
    && python -m pip install --no-cache-dir \
        "paddleocr[doc-parser]>=3.3,<4.0"

WORKDIR /app
COPY ocr_worker.py /app/ocr_worker.py

CMD ["python", "ocr_worker.py"]
