import os
import json
from config import CONVERSATION_DIR, PROMPT_DIR
from models.data_structures import UserData

def ensure_dirs():
    os.makedirs(CONVERSATION_DIR, exist_ok=True)
    os.makedirs(PROMPT_DIR, exist_ok=True)

def load_user_data(user_name: str) -> UserData:
    file_path = f"{CONVERSATION_DIR}/{user_name}.json"
    try:
        with open(file_path, 'r', encoding='utf-8') as f:
            data = json.load(f)
            return UserData.from_dict(data)
    except (FileNotFoundError, json.JSONDecodeError):
        return UserData()

def save_user_data(user_name: str, user_data: UserData):
    file_path = f"{CONVERSATION_DIR}/{user_name}.json"
    with open(file_path, 'w', encoding='utf-8') as f:
        json.dump(user_data.to_dict(), f, ensure_ascii=False, indent=2)

def read_prompt(filename: str) -> str:
    file_path = os.path.join(PROMPT_DIR, filename)
    with open(file_path, 'r', encoding='utf-8') as f:
        return f.read()

def get_intro_text() -> str:
    # intro.txt is in the root of backend_modular
    try:
        with open('intro.txt', 'r', encoding='utf-8') as f:
            return f.read()
    except FileNotFoundError:
        return "歡迎來到磯永吉小屋！這裡是關於磯永吉和末永仁的展覽，讓我們一起探索他們的故事和對台灣農業的貢獻。"
