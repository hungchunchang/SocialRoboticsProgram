 FROM python:3.13-slim

WORKDIR /app

# 複製 requirements 文件並安裝依賴
COPY requirement.txt .
RUN pip install --no-cache-dir -r requirement.txt

# 複製當前目錄的所有文件到容器中
COPY . .

# 確保數據目錄存在
RUN mkdir -p data/conversation data/prompt
RUN chmod -R 755 data

# 創建非 root 使用者運行應用程式 (安全性考量)
RUN useradd -m appuser
RUN chown -R appuser:appuser /app
USER appuser

# 設定環境變數
ENV PYTHONUNBUFFERED=1

# 暴露應用程式端口
EXPOSE 8080

# 使用 Gunicorn 運行應用程式
CMD ["gunicorn", "main:app", "--bind", "0.0.0.0:8080", "--workers=3", "--timeout", "120", "--access-logfile", "-", "--error-logfile", "-"]
