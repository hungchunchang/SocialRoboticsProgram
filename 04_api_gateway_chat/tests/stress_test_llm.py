import asyncio
import time
import aiohttp
import os
from dotenv import load_dotenv

# 直接讀取 api-gateway 下的 .env
load_dotenv("../../../api-gateway/.env")

API_KEY = os.getenv("API_KEY")
# 修正路徑：通常 OpenAI 相容的 API 需要完整的 /v1/chat/completions
LLM_BASE_URL = os.getenv("LLM_ENDPOINT", "https://sociallab.duckdns.org/llm")
LLM_ENDPOINT = f"{LLM_BASE_URL}/v1/chat/completions"

async def test_llm(session, idx):
    data = {
        "model": "Qwen/Qwen2.5-7B-Instruct-AWQ",
        "messages": [
            {"role": "user", "content": f"這是第 {idx} 個壓力測試請求，請簡短回覆。"}
        ]
    }
    headers = {"Authorization": f"Bearer {API_KEY}"}
    
    start = time.time()
    try:
        async with session.post(LLM_ENDPOINT, json=data, headers=headers) as resp:
            if resp.status == 200:
                result = await resp.json()
                print(f"[LLM {idx}] 成功: {result['choices'][0]['message']['content']}")
            else:
                print(f"[LLM {idx}] 錯誤 {resp.status}: {await resp.text()}")
    except Exception as e:
        print(f"[LLM {idx}] 異常: {e}")
    
    return time.time() - start

async def main():
    if not LLM_ENDPOINT:
        print("錯誤: 找不到 LLM_ENDPOINT")
        return

    num_requests = 10
    print(f"啟動 {num_requests} 個併發 LLM 請求...")
    
    async with aiohttp.ClientSession() as session:
        tasks = [test_llm(session, i) for i in range(num_requests)]
        start_all = time.time()
        durations = await asyncio.gather(*tasks)
        end_all = time.time()

    print("\n" + "="*30)
    print(f"LLM 壓力測試結果")
    print(f"總耗時: {end_all - start_all:.2f}s")
    print(f"平均響應時間: {sum(durations)/len(durations):.2f}s")
    print("="*30)

if __name__ == "__main__":
    asyncio.run(main())
