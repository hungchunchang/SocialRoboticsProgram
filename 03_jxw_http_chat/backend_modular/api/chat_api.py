import uuid
from fastapi import APIRouter

from logic.chat_logic import generate_bot_reply, save_message_service, parse_init_message
from models.data_structures import (
    ChatRequest,
    ChatResponse,
    CreateUserResponse,
    HealthResponse,
    ResetRequest,
    UserData,
)
from utils.helpers import save_user_data

router = APIRouter(prefix="/api", tags=["chat"])


@router.post("/chat", response_model=ChatResponse)
def chat(req: ChatRequest) -> ChatResponse:
    is_init, _ = parse_init_message(req.message)
    if is_init:
        # init resets the session; don't log "init" as a conversation message
        return generate_bot_reply(req.user_name, req.message)

    save_message_service(req.user_name, "user", req.message)
    response = generate_bot_reply(req.user_name, req.message)
    save_message_service(req.user_name, "bot", response.reply)
    return response


@router.post("/reset")
def reset_chat(req: ResetRequest) -> dict[str, str]:
    save_user_data(req.user_name, UserData())
    return {"status": "success"}


@router.post("/create_user", response_model=CreateUserResponse)
def create_user() -> CreateUserResponse:
    new_user_name = str(uuid.uuid4())
    save_user_data(new_user_name, UserData())
    return CreateUserResponse(user_name=new_user_name)


@router.get("/health", response_model=HealthResponse)
def health() -> HealthResponse:
    return HealthResponse(status="healthy")
