# ===== 构建阶段 =====
FROM python:3.12-alpine AS build

WORKDIR /build
COPY pyproject.toml ./
COPY src/ ./src/
RUN pip install build && python -m build

# ===== 运行阶段 =====
FROM python:3.12-alpine

RUN addgroup -g 1001 appgroup && \
    adduser -u 1001 -G appgroup -s /sbin/nologin -D appuser

WORKDIR /app
COPY --from=build /build/dist/*.whl ./
RUN pip install --no-cache-dir *.whl && rm *.whl

USER appuser

EXPOSE 8089

HEALTHCHECK --interval=15s --timeout=5s --retries=3 --start-period=10s \
    CMD python -c "import http.client as h; c=h.HTTPConnection('localhost',8089,timeout=5); c.request('GET','/health'); exit(0 if c.getresponse().status==200 else 1)"

ENV JAVA_BASE_URL=http://backend:8000
ENTRYPOINT ["life-assistant-agent"]
