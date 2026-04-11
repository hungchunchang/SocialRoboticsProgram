from flask import Flask
from flask_cors import CORS
from api.chat_api import chat_bp
from utils.helpers import ensure_dirs

def create_app():
    app = Flask(__name__)
    CORS(app)
    
    # 確保資料目錄存在
    ensure_dirs()
    
    # 註冊 API 路由
    app.register_blueprint(chat_bp, url_prefix='/api')
    
    return app
