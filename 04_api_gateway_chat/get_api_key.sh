#!/bin/bash

# Default values
GATEWAY_URL="https://sociallab.duckdns.org"

echo "=== API Gateway 登入與配置工具 ==="
echo ""

read -p "請輸入帳號: " USERNAME
read -p "請輸入密碼: " PASSWORD
echo ""

echo "正在連線至 $GATEWAY_URL ..."

# 1. 發送請求獲取 API Key
RESPONSE=$(curl -s -k -X POST "$GATEWAY_URL/auth/generate_key" \
     -H "Content-Type: application/json" \
     -d "{\"username\": \"$USERNAME\", \"password\": \"$PASSWORD\"}")

# 檢查是否為空
if [ -z "$RESPONSE" ]; then
    echo "錯誤: 伺服器沒有回應。請確認伺服器是否已啟動於 $GATEWAY_URL"
    exit 1
fi

# 解析 API Key
if command -v jq >/dev/null 2>&1; then
    API_KEY=$(echo "$RESPONSE" | jq -r '.api_key // empty')
    ERROR=$(echo "$RESPONSE" | jq -r '.detail // empty')
else
    # 簡易解析
    API_KEY=$(echo "$RESPONSE" | grep -o '"api_key":"[^"]*' | cut -d'"' -f4)
    ERROR=$(echo "$RESPONSE" | grep -o '"detail":"[^"]*' | cut -d'"' -f4)
fi

if [ -n "$API_KEY" ] && [ "$API_KEY" != "null" ]; then
    echo "-----------------------------------------------"
    echo "登入成功！已取得 API Key。"
    
    # 2. 詢問是否修改密碼
    echo ""
    read -p "為了安全起見，您想現在修改您的登入密碼嗎？(y/N): " CHANGE_PWD
    if [[ "$CHANGE_PWD" =~ ^[Yy]$ ]]; then
        read -s -p "請輸入新密碼: " NEW_PASSWORD
        echo ""
        PWD_RESP=$(curl -s -X POST "$GATEWAY_URL/auth/change_password" \
             -H "Content-Type: application/json" \
             -d "{\"username\": \"$USERNAME\", \"old_password\": \"$PASSWORD\", \"new_password\": \"$NEW_PASSWORD\"}")
        
        MSG=$(echo "$PWD_RESP" | grep -o '"message":"[^"]*' | cut -d'"' -f4)
        if [ -n "$MSG" ]; then
            echo "密碼修改成功！下次請使用新密碼登入。"
        else
            echo "密碼修改失敗：$PWD_RESP"
        fi
    fi

    # 3. 寫入 .env
    echo "-----------------------------------------------"
    echo "正在更新當前目錄下的 .env 檔案..."
    echo "GATEWAY_URL=$GATEWAY_URL" > .env
    echo "API_KEY=$API_KEY" >> .env
    echo "ASR_ENDPOINT=$GATEWAY_URL/asr" >> .env
    echo "TTS_ENDPOINT=$GATEWAY_URL/tts" >> .env
    echo "LLM_ENDPOINT=$GATEWAY_URL/llm" >> .env
    
    echo "完成！配置已寫入 .env。"
else
    echo "-----------------------------------------------"
    echo "登入失敗！"
    echo "錯誤訊息: ${ERROR:-未知錯誤}"
    echo "-----------------------------------------------"
fi
