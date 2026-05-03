from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from api.chat_api import router
from utils.helpers import ensure_dirs


def create_app() -> FastAPI:
    ensure_dirs()

    app = FastAPI(
        title="03 FastAPI Turn-Based Chatbot",
        description="Extend 02_fastapi_chat with turn-based flow control and persistent conversation storage.",
        version="1.0.0",
    )
    app.add_middleware(
        CORSMiddleware,
        allow_origins=["*"],
        allow_methods=["*"],
        allow_headers=["*"],
    )
    app.include_router(router)
    return app
