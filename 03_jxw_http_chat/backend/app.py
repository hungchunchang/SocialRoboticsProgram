import os
import json
import uuid
from datetime import datetime
from flask import Flask, request, jsonify
from flask_cors import CORS
from dotenv import load_dotenv
from openai import OpenAI

# 載入環境變數
load_dotenv()
OPENAI_API_KEY = os.getenv('OPENAI_API_KEY')

app = Flask(__name__)
CORS(app)  # 啟用CORS支援，允許前端呼叫

# 確保資料目錄存在
os.makedirs('data/conversations', exist_ok=True)

# 讀取介紹文本
try:
    with open('intro.txt', 'r', encoding='utf-8') as f:
        intro_text = f.read()
except FileNotFoundError:
    intro_text = "歡迎來到磯永吉小屋！這裡是關於磯永吉和末永仁的展覽，讓我們一起探索他們的故事和對台灣農業的貢獻。"
    with open('intro.txt', 'w', encoding='utf-8') as f:
        f.write(intro_text)

# OpenAI客戶端
client = OpenAI(api_key=OPENAI_API_KEY)

'''
***可以改地方***
'''
# 系統提示模板
system_prompt_template = """你是一名在台大磯永吉小屋工作的機器人阿蓬，負責與參觀完畢的參與者互動，你要從文本 {intro} 中，提出三個簡單的問題、與參與者進行問答互動，幫助參與者回顧當天參展的經驗，你們目前已經聊了這些：{context}。

你需要遵循以下問答流程：
1. 首先自我介紹，並且說明接下來的互動，然後提出一個簡單問題
2. 等待參與者回答
3. 根據參與者的回答進行評估：
 - 如果答對了，確認他們的答案並給予鼓勵
 - 如果答錯了或不知道，提供正確答案並簡短解釋
 - 如果答案部分正確，肯定正確部分並補充完整答案
4. 然後再提出下一個問題

你要用繁體中文回答問題，保持禮貌並使用簡單的語言，讓參與者感到輕鬆愉快。一句回應只應包含一個問題。

回答的格式為 json 格式，並包含：
1. question(string) - 你對參與者的回應或問題
2. is_ended(bool) - 對話是否結束
"""

interview_prompt = """你是一名在台大磯永吉小屋工作的機器人阿蓬，負責與參觀完畢的參與者互動，你們目前已經聊了這些：{context}，按照時間順序，回答使用者最新的一個回答。

依序詢問下列三個問題，根據使用者的回應{context}給予適切的回答：
1. 詢問您今天參觀中印象最深刻的事情
2. 詢問您會想要再來參觀？或推薦家人朋友來參觀嗎？
3. 詢問您是否有其他問題，或是對於展覽的建議嗎？
如果使用者回答了問題，請根據他們的回答給予適當的回應，並且告訴他們你會記錄下來，問過的問題就不要再問一次了。

你要用繁體中文回答問題，保持禮貌並使用簡單的語言，讓參與者感到輕鬆愉快。一句回應只應包含一個問題。

回答的格式為 json 格式，並包含：
1. question(string) - 你對參與者的回應或問題
2. is_ended(bool) - 對話是否結束
"""

international_musemu_day = """你是一名在台大磯永吉小屋工作的機器人阿蓬，負責與參觀完畢的參與者互動，你們目前已經聊了這些：{context}，按照時間順序，回答使用者最新的一個回答，作為收尾，不准以問題形式進行。
並且問使用者：你們知道5/18是什麼日子嗎？
你們知道五月十八日是什麼日子嗎？

回答的格式為 json 格式，並包含：
1. question(string) - 你對參與者的回應以及問題
2. is_ended(bool) - 對話是否結束
"""

international_musemu_day_ans = """你是一名在台大磯永吉小屋工作的機器人阿蓬，負責與參觀完畢的參與者互動，你們目前已經聊了這些：{context}。
根據使用者的回答，告訴使用者的答案是否正確，並且告訴他們五月十八日是國際博物館日，並且告訴他們今年的主題是「博物館、社會轉型與可持續發展」，並且告訴他們這個主題的意義，然後不要提出問題。

回答的格式為 json 格式，並包含：
1. question(string) - 你對參與者的回應
2. is_ended(bool) - 對話是否結束
"""

def get_user_data(user_name):
    """獲取用戶資料，如果不存在則初始化"""
    file_path = f"data/conversations/{user_name}.json"
    try:
        with open(file_path, 'r', encoding='utf-8') as f:
            return json.load(f)
    except (FileNotFoundError, json.JSONDecodeError):
        # 如果檔案不存在或JSON無效，初始化新資料
        user_data = {
            "turns": 0,
            "conversation": [],
            "is_ended": False
        }
        with open(file_path, 'w', encoding='utf-8') as f:
            json.dump(user_data, f, ensure_ascii=False, indent=2)
        return user_data

def save_message(user_name, speaker, message):
    """儲存訊息並更新用戶資料"""
    user_data = get_user_data(user_name)
    timestamp = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    
    user_data["conversation"].append({
        "timestamp": timestamp,
        "speaker": speaker,
        "message": message
    })
    
    file_path = f"data/conversations/{user_name}.json"
    with open(file_path, 'w', encoding='utf-8') as f:
        json.dump(user_data, f, ensure_ascii=False, indent=2)

'''
***可以改地方***
'''
def jxw_bot(user_name, question):
    """與AI互動，生成回覆"""
    # 獲取用戶資料
    user_data = get_user_data(user_name)
            
    # 生成對話歷史文本
    history = ""
    for msg in user_data["conversation"]:
        history += f"{msg['timestamp']} | {msg['speaker']}: {msg['message']}\n"
    
    turns=user_data.get("turns", 0)
    if(turns<=2):
        # 知識問答環節
        system_prompt = system_prompt_template.format(
            intro=intro_text, 
            context=history,
            turns=user_data.get("turns", 0)
        )
    elif(turns>2 and turns<=7):
        # 使用訪談提示
        system_prompt = interview_prompt.format(
            context=history, 
            turns=user_data.get("turns", 0)
        )
    elif(turns ==8):
        system_prompt = international_musemu_day.format(
            context=history, 
            turns=user_data.get("turns", 0)
        )
    elif(turns ==9):
        system_prompt = international_musemu_day_ans.format(
            context=history, 
            turns=user_data.get("turns", 0)
        )
    elif(turns>=10):
        # 如果對話結束，返回結束訊息
        return {
            "question": "謝謝你們今天來參觀！期待下次再見！",
            "is_ended": True
        }
    
    # 生成AI回覆
    try:
        completion = client.chat.completions.create(
            model="gpt-4o",
            messages=[
                {"role": "system", "content": system_prompt},
                {"role": "user", "content": "參與者回應道：" + question},
            ],
            max_tokens=500,
        )
        
        raw = completion.choices[0].message.content
        # 嘗試解析JSON
        try:
            # 移除可能的Markdown格式
            json_text = raw.replace('```json', '').replace('```', '').strip()
            data = json.loads(json_text)
            
            # 更新用戶資料
            if data.get("is_ended", False):
                user_data["is_ended"] = True
            user_data["turns"] += 1
        
            with open(f"data/conversations/{user_name}.json", 'w', encoding='utf-8') as f:
                json.dump(user_data, f, ensure_ascii=False, indent=2)
                
            return data
        except json.JSONDecodeError:
            # 如果無法解析JSON，返回原始文本作為問題
            return {
                "question": raw,
                "is_ended": False
            }
    except Exception as e:
        print(f"錯誤: {e}")
        return {
            "question": "抱歉，我遇到了一些問題，請稍後再試。",
            "is_ended": False
        }

@app.route('/api/chat', methods=['POST'])
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
    save_message(user_name, "user", user_message)
    
    # 獲取機器人回覆
    response = jxw_bot(user_name, user_message)
    timestamp_2 = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    print(f"{timestamp_2}| 機器人回覆: {response['question']}")
    # 儲存機器人回覆
    save_message(user_name, "bot", response["question"])
    
    return jsonify(response)

@app.route('/api/reset', methods=['POST'])
def reset_chat():
    """重置聊天"""
    data = request.json
    user_name = data.get('user_name')
    
    if not user_name:
        return jsonify({"error": "用戶名稱不能為空"}), 400
    
    # 重置用戶資料
    user_data = {
        "turns": 0,
        "conversation": [],
        "is_ended": False
    }
    
    with open(f"data/conversations/{user_name}.json", 'w', encoding='utf-8') as f:
        json.dump(user_data, f, ensure_ascii=False, indent=2)
    
    return jsonify({"status": "success"})

@app.route('/api/create_user', methods=['POST'])
def create_user():
    """創建新用戶"""
    new_user_name = str(uuid.uuid4())
    
    # 初始化用戶資料
    user_data = {
        "turns": 0,
        "conversation": [],
        "is_ended": False
    }
    
    with open(f"data/conversations/{new_user_name}.json", 'w', encoding='utf-8') as f:
        json.dump(user_data, f, ensure_ascii=False, indent=2)
    
    return jsonify({"user_name": new_user_name})

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=8080, debug=False)
