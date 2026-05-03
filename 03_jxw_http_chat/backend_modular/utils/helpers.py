import json
from config import CONVERSATION_DIR, PROMPT_DIR, INTRO_FILE
from models.data_structures import UserData

def ensure_dirs():
    CONVERSATION_DIR.mkdir(parents=True, exist_ok=True)
    PROMPT_DIR.mkdir(parents=True, exist_ok=True)

def load_user_data(user_name: str) -> UserData:
    file_path = CONVERSATION_DIR / f"{user_name}.json"
    try:
        with file_path.open('r', encoding='utf-8') as f:
            data = json.load(f)
            return UserData.model_validate(data)
    except (FileNotFoundError, json.JSONDecodeError):
        return UserData()

def save_user_data(user_name: str, user_data: UserData):
    file_path = CONVERSATION_DIR / f"{user_name}.json"
    with file_path.open('w', encoding='utf-8') as f:
        json.dump(user_data.model_dump(), f, ensure_ascii=False, indent=2)

def read_prompt(filename: str) -> str:
    file_path = PROMPT_DIR / filename
    with file_path.open('r', encoding='utf-8') as f:
        return f.read()

def get_intro_text() -> str:
    try:
        with INTRO_FILE.open('r', encoding='utf-8') as f:
            return f.read()
    except FileNotFoundError:
        return "歡迎來到磯永吉小屋！這裡是關於磯永吉和末永仁的展覽，讓我們一起探索他們的故事和對台灣農業的貢獻。"
