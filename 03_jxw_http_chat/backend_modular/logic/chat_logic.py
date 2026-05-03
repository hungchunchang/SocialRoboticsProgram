import json
import re
from datetime import datetime
from config import OPENAI_MODEL, get_openai_client
from utils.helpers import load_user_data, save_user_data, read_prompt, get_intro_text
from models.data_structures import ChatResponse, MessageRecord, UserData, VALID_EMOTIONS


def get_stage_name(turns: int) -> str:
    if turns <= 2:
        return "knowledge_quiz"
    if turns <= 7:
        return "experience_interview"
    if turns == 8:
        return "museum_day_question"
    if turns == 9:
        return "museum_day_answer"
    return "completed"


def build_system_prompt(user_data: UserData) -> str | None:
    history = "".join(
        f"{msg.timestamp} | {msg.speaker}: {msg.message}\n"
        for msg in user_data.conversation
    )
    intro_text = get_intro_text()
    turns = user_data.turns

    if turns <= 2:
        template = read_prompt('system_prompt_template.txt')
        return template.format(intro=intro_text, context=history)
    if turns <= 7:
        template = read_prompt('interview_prompt.txt')
        return template.format(context=history)
    if turns == 8:
        template = read_prompt('international_musemu_day.txt')
        return template.format(context=history)
    if turns == 9:
        template = read_prompt('international_musemu_day_ans.txt')
        return template.format(context=history)
    return None


def parse_init_message(message: str) -> tuple[bool, str]:
    """Check if message is an init command. Returns (is_init, nickname)."""
    msg = message.strip()
    if msg == "init":
        return True, ""
    match = re.match(r"^init[_\s](.+)$", msg)
    if match:
        return True, match.group(1).strip()
    return False, ""


def parse_model_reply(raw_text: str) -> tuple[str, str, bool]:
    """Parse model reply. Returns (reply_text, emotion, is_ended)."""
    try:
        json_text = raw_text.replace('```json', '').replace('```', '').strip()
        data = json.loads(json_text)
        reply_text = data.get("reply") or data.get("question") or raw_text
        emotion = data.get("emotion", "neutral")
        if emotion not in VALID_EMOTIONS:
            emotion = "neutral"
        return reply_text, emotion, bool(data.get("is_ended", False))
    except json.JSONDecodeError:
        return raw_text, "neutral", False


def handle_init(user_name: str, nickname: str) -> ChatResponse:
    """Reset user data and return a greeting."""
    user_data = UserData(nickname=nickname)
    save_user_data(user_name, user_data)
    greeting = f"你好{nickname}！" if nickname else "你好！"
    greeting += "歡迎來到磯永吉小屋，我是機器人阿蓬。準備好開始了嗎？"
    return ChatResponse(
        reply=greeting,
        question=greeting,
        emotion="joy",
        turn_index=0,
        current_stage="init",
        is_ended=False,
    )


def generate_bot_reply(user_name: str, question: str) -> ChatResponse:
    # Check for init command
    is_init, nickname = parse_init_message(question)
    if is_init:
        return handle_init(user_name, nickname)

    user_data = load_user_data(user_name)
    current_stage = get_stage_name(user_data.turns)

    if user_data.is_ended or current_stage == "completed":
        return ChatResponse(
            reply="謝謝你們今天來參觀！期待下次再見！",
            question="謝謝你們今天來參觀！期待下次再見！",
            emotion="joy",
            turn_index=user_data.turns,
            current_stage="completed",
            is_ended=True,
        )

    system_prompt = build_system_prompt(user_data)
    client = get_openai_client()
    emotion = "neutral"

    try:
        completion = client.chat.completions.create(
            model=OPENAI_MODEL,
            messages=[
                {"role": "system", "content": system_prompt},
                {"role": "user", "content": f"參與者回應道：{question}"},
            ],
            response_format={"type": "json_object"},
        )
        raw_text = completion.choices[0].message.content or ""
        reply_text, emotion, model_ended = parse_model_reply(raw_text)
    except Exception as e:
        reply_text = f"抱歉，我遇到了一些問題，請稍後再試。({e})"
        model_ended = False

    user_data.turns += 1
    user_data.is_ended = model_ended or user_data.turns >= 10
    save_user_data(user_name, user_data)

    return ChatResponse(
        reply=reply_text,
        question=reply_text,
        emotion=emotion,
        turn_index=user_data.turns,
        current_stage=current_stage,
        is_ended=user_data.is_ended,
    )

def save_message_service(user_name: str, speaker: str, message: str) -> UserData:
    user_data = load_user_data(user_name)
    timestamp = datetime.now().strftime("%Y-%m-%d %H:%M:%S")

    new_message = MessageRecord(
        timestamp=timestamp,
        speaker=speaker,
        message=message
    )
    user_data.conversation.append(new_message)
    save_user_data(user_name, user_data)
    return user_data
