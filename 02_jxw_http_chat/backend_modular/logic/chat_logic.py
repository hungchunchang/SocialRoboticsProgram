import json
from datetime import datetime
from config import client
from utils.helpers import load_user_data, save_user_data, read_prompt, get_intro_text
from models.data_structures import Message

def jxw_bot(user_name, question):
    """與AI互動，生成回覆"""
    # 獲取用戶資料
    user_data = load_user_data(user_name)
    
    # 生成對話歷史文本
    history = ""
    for msg in user_data.conversation:
        history += f"{msg.timestamp} | {msg.speaker}: {msg.message}\n"
    
    turns = user_data.turns
    intro_text = get_intro_text()
    
    if turns <= 2:
        # 知識問答環節
        template = read_prompt('system_prompt_template.txt')
        system_prompt = template.format(
            intro=intro_text, 
            context=history
        )
    elif turns > 2 and turns <= 7:
        # 使用訪談提示
        template = read_prompt('interview_prompt.txt')
        system_prompt = template.format(
            context=history
        )
    elif turns == 8:
        template = read_prompt('international_musemu_day.txt')
        system_prompt = template.format(
            context=history
        )
    elif turns == 9:
        template = read_prompt('international_musemu_day_ans.txt')
        system_prompt = template.format(
            context=history
        )
    elif turns >= 10:
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
                user_data.is_ended = True
            user_data.turns += 1
        
            save_user_data(user_name, user_data)
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

def save_message_service(user_name, speaker, message):
    """儲存訊息並更新用戶資料"""
    user_data = load_user_data(user_name)
    timestamp = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    
    new_message = Message(
        timestamp=timestamp,
        speaker=speaker,
        message=message
    )
    user_data.conversation.append(new_message)
    save_user_data(user_name, user_data)
