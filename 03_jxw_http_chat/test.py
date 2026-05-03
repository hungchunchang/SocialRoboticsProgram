import requests
import json
import time
import uuid

url = "http://127.0.0.1:8080/api/chat"

# 請求標頭
headers = {
    "Content-Type": "application/json"
}

uid = str(uuid.uuid4())

# 請求內容：第一句用 init 啟動，之後正常對話
messages = [
    "init_小明",
    "你好",
    "我記得有提到農業研究",
    "是磯永吉",
    "我印象最深的是育種故事",
    "會，我想推薦朋友來",
    "目前沒有其他問題",
]

for i in range(12):
    data = {
        "user_name": uid,
        "message": messages[min(i, len(messages) - 1)]
    }
    try:
        response = requests.post(url, headers=headers, json=data)
        if response.status_code == 200:
            result = response.json()
            print(f"請求 #{i+1} 成功:")
            print(json.dumps(result, ensure_ascii=False, indent=2))
        else:
            print(f"請求 #{i+1} 失敗，狀態碼: {response.status_code}")
            print(response.text)
    
    except Exception as e:
        print(f"請求 #{i+1} 發生錯誤: {str(e)}")

    time.sleep(1)
    print("-" * 50)
