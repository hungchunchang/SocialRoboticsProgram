from pydantic import BaseModel, Field


class MessageRecord(BaseModel):
    timestamp: str
    speaker: str
    message: str


class UserData(BaseModel):
    turns: int = 0
    conversation: list[MessageRecord] = Field(default_factory=list)
    is_ended: bool = False
    nickname: str = ""


class ChatRequest(BaseModel):
    message: str = Field(..., min_length=1)
    user_name: str = Field(..., min_length=1)


class ResetRequest(BaseModel):
    user_name: str = Field(..., min_length=1)


class CreateUserResponse(BaseModel):
    user_name: str


VALID_EMOTIONS = {"neutral", "angry", "joy", "sad", "surprise", "scared", "disgusted"}


class ChatResponse(BaseModel):
    reply: str
    question: str
    emotion: str = "neutral"
    turn_index: int
    current_stage: str
    is_ended: bool = False


class HealthResponse(BaseModel):
    status: str
