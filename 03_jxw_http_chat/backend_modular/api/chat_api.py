import uuid
from datetime import datetime
from flask import Blueprint, request, jsonify
from logic.chat_logic import jxw_bot, save_message_service
from utils.helpers import save_user_data, UserData

chat_bp = Blueprint('chat', __name__)

@chat_bp.route('/chat', methods=['POST'])
def chat():
    """處理聊天請求"""
    data = request.json
    user_message = data.get('message', '')
    user_name = data.get('user_name')
    
    # 驗證必要參數
    if not user_message:
        return jsonify({"error": "訊息不能為空"}), 400
    if not user_name:
        return jsonify({"error": "用戶名稱不能為空"}), 400
        
    timestamp_1 = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    print(f"{timestamp_1}| 使用者 {user_name} 發送訊息: {user_message}")
    
    # 儲存用戶訊息
    save_message_service(user_name, "user", user_message)
    
    # 獲取機器人回覆
    response = jxw_bot(user_name, user_message)
    
    timestamp_2 = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    print(f"{timestamp_2}| 機器人回覆: {response['question']}")
    
    # 儲存機器人回覆
    save_message_service(user_name, "bot", response["question"])
    
    return jsonify(response)

@chat_bp.route('/reset', methods=['POST'])
def reset_chat():
    """重置聊天"""
    data = request.json
    user_name = data.get('user_name')
    
    if not user_name:
        return jsonify({"error": "用戶名稱不能為空"}), 400
    
    # 重置用戶資料
    user_data = UserData()
    save_user_data(user_name, user_data)
    
    return jsonify({"status": "success"})

@chat_bp.route('/create_user', methods=['POST'])
def create_user():
    """創建新用戶"""
    new_user_name = str(uuid.uuid4())
    
    # 初始化用戶資料
    user_data = UserData()
    save_user_data(new_user_name, user_data)
    
    return jsonify({"user_name": new_user_name})

@chat_bp.route('/health', methods=['GET'])
def health():
    return jsonify({"status": "healthy"})
