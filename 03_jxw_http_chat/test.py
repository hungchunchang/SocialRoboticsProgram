import requests
import json
import time
import uuid

# API 端點
url = "http://140.112.92.133:8080/api/chat"

# 請求標頭
headers = {
    "Content-Type": "application/json"
}

uid = str(uuid.uuid4())

# 請求內容
data = {
    "user_name": uid,
    "message": "沒有"
}

# 發送 10 次請求
for i in range(12):
    try:
        # 發送 POST 請求
        response = requests.post(url, headers=headers, json=data)
        
        # 檢查請求是否成功
        if response.status_code == 200:
            # 解析 JSON 回應
            result = response.json()
            
            # 格式化輸出回應
            print(f"請求 #{i+1} 成功:")
            print(json.dumps(result, ensure_ascii=False, indent=2))
        else:
            print(f"請求 #{i+1} 失敗，狀態碼: {response.status_code}")
            print(response.text)
    
    except Exception as e:
        print(f"請求 #{i+1} 發生錯誤: {str(e)}")
    
    # 暫停一秒，避免過快發送請求
    time.sleep(1)
    
    # 分隔線
    print("-" * 50)
