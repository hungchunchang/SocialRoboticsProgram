from dataclasses import dataclass, field
from typing import List, Optional

@dataclass
class Message:
    timestamp: str
    speaker: str
    message: str

    def to_dict(self):
        return {
            "timestamp": self.timestamp,
            "speaker": self.speaker,
            "message": self.message
        }

@dataclass
class UserData:
    turns: int = 0
    conversation: List[Message] = field(default_factory=list)
    is_ended: bool = False

    def to_dict(self):
        return {
            "turns": self.turns,
            "conversation": [m.to_dict() for m in self.conversation],
            "is_ended": self.is_ended
        }

    @classmethod
    def from_dict(cls, data: dict):
        messages = [
            Message(
                timestamp=m["timestamp"],
                speaker=m["speaker"],
                message=m["message"]
            )
            for m in data.get("conversation", [])
        ]
        return cls(
            turns=data.get("turns", 0),
            conversation=messages,
            is_ended=data.get("is_ended", False)
        )
