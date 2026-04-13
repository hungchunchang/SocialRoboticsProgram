"""
簡易聊天客戶端
用途：在另一個終端機運行，對本機的 FastAPI Server 發送訊息

使用方式：
1. 確認 server.py 已在另一個終端機運行
2. python client.py
3. 輸入訊息即可開始對話，輸入 'quit' 結束
"""

import requests
import json

# ===== 修改這裡來連到不同的伺服器 =====
SERVER_URL = "http://localhost:8000"
# =======================================

USER_NAME = "terminal_user"


def chat(message: str) -> dict:
    """發送訊息到伺服器"""
    response = requests.post(
        f"{SERVER_URL}/api/chat",
        json={"message": message, "user_name": USER_NAME},
    )
    return response.json()


def reset():
    """重置對話"""
    requests.post(
        f"{SERVER_URL}/api/reset",
        json={"user_name": USER_NAME},
    )
    print("對話已重置。\n")


def main():
    print("=" * 50)
    print("社交機器人聊天客戶端")
    print(f"連線到：{SERVER_URL}")
    print("輸入 'quit' 結束，輸入 'reset' 重置對話")
    print("=" * 50)
    print()

    while True:
        user_input = input("你：").strip()

        if not user_input:
            continue
        if user_input.lower() == "quit":
            print("再見！")
            break
        if user_input.lower() == "reset":
            reset()
            continue

        try:
            result = chat(user_input)
            print(f"機器人：{result.get('reply', '（無回覆）')}")

            if result.get("is_ended", False):
                print("\n（對話已結束）")
                break

        except requests.ConnectionError:
            print(f"無法連線到 {SERVER_URL}，請確認 server 是否已啟動。")
        except Exception as e:
            print(f"發生錯誤：{e}")

        print()


if __name__ == "__main__":
    main()
