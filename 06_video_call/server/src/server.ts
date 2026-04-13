// src/server.ts
import WebSocket from 'ws';
import { createServer } from 'http';

// Types
interface UserData {
    ws: WebSocket;
    info: {
        registeredAt: Date;
        lastSeen: Date;
    };
}

interface SignalingMessage {
    type: string;
    id?: string;
    from?: string;
    to?: string;
    sdp?: string;
    candidate?: string;
    sdpMid?: string;
    sdpMLineIndex?: number;
    [key: string]: any;
}

console.log('🚀 WebRTC Signaling Server 啟動中...');

// 創建 HTTP 服務器
const server = createServer();
const wss = new WebSocket.Server({
    server,
    perMessageDeflate: false
});

// 儲存用戶連接
const users = new Map<string, UserData>(); // userId -> UserData
const connections = new Map<WebSocket, string>(); // ws -> userId

// 工具函數：安全發送訊息
function safeSend(ws: WebSocket, data: object): boolean {
    if (ws.readyState === WebSocket.OPEN) {
        ws.send(JSON.stringify(data));
        return true;
    }
    return false;
}

// 廣播線上用戶列表
function broadcastUserList(): void {
    const userList = Array.from(users.keys());
    const message = {
        type: 'user_list',
        users: userList,
        count: userList.length
    };

    users.forEach((userData) => {
        safeSend(userData.ws, message);
    });

    console.log(`📋 廣播用戶列表: [${userList.join(', ')}] (${userList.length} 人在線)`);
}

// 清理斷線用戶
function cleanupUser(ws: WebSocket): void {
    const userId = connections.get(ws);
    if (userId) {
        users.delete(userId);
        connections.delete(ws);
        console.log(`🚪 用戶 ${userId} 已斷線`);
        broadcastUserList();
    }
}

// 處理用戶註冊
function handleRegister(ws: WebSocket, data: SignalingMessage): void {
    const { id } = data;

    if (!id) {
        safeSend(ws, { type: 'register_error', message: 'Missing user id' });
        return;
    }

    if (users.has(id)) {
        // 關閉舊連接，更新為新連接
        const oldUser = users.get(id);
        if (oldUser) {
            oldUser.ws.close();
            connections.delete(oldUser.ws);
        }
    }

    // 註冊新連接
    users.set(id, {
        ws: ws,
        info: { registeredAt: new Date(), lastSeen: new Date() }
    });
    connections.set(ws, id);

    safeSend(ws, { type: 'register_success', id: id });
    broadcastUserList();
}

// 處理通話請求
function handleCall(data: SignalingMessage): void {
    const { from, to } = data;
    if (typeof from !== 'string' || typeof to !== 'string') return;

    const targetUser = users.get(to);

    if (!targetUser) {
        const caller = users.get(from);
        if (caller) {
            safeSend(caller.ws, {
                type: 'user_offline',
                user: to
            });
        }
        return;
    }

    safeSend(targetUser.ws, {
        type: 'incoming_call',
        from: from
    });

    console.log(`📞 通話請求: ${from} → ${to}`);
}

// 處理通話回應
function handleCallResponse(data: SignalingMessage, accepted: boolean): void {
    const { from, to } = data;
    if (typeof from !== 'string' || typeof to !== 'string') return;

    const targetUser = users.get(to);
    if (!targetUser) return;

    const responseType = accepted ? 'call_accepted' : 'call_declined';
    safeSend(targetUser.ws, {
        type: responseType,
        peer: from
    });

    console.log(`${accepted ? '✅' : '❌'} 通話${accepted ? '接聽' : '拒絕'}: ${from} ← ${to}`);
}

// 中繼 WebRTC 信令
function relaySignaling(data: SignalingMessage): void {
    const { from, to, type } = data;

    if (typeof to !== 'string') {
        console.log(`❌ 目標用戶 ${to} 離線，信令 ${type} 無法送達`);
        return;
    }

    const targetUser = users.get(to);
    if (!targetUser) {
        console.log(`❌ 目標用戶 ${to} 離線，信令 ${type} 無法送達`);
        return;
    }

    const success = safeSend(targetUser.ws, data);
    console.log(`${success ? '✅' : '❌'} 信令轉發: ${type} ${from} → ${to}`);
}

// WebSocket 連接處理
wss.on('connection', (ws: WebSocket, req) => {
    const clientIP = req.socket.remoteAddress;
    console.log(`🔗 新連接來自: ${clientIP}`);

    // 心跳檢測
    (ws as any).isAlive = true;
    ws.on('pong', () => {
        (ws as any).isAlive = true;
    });

    // 訊息處理
    ws.on('message', (message: WebSocket.Data) => {
        try {
            const data: SignalingMessage = JSON.parse(message.toString());
            const { type } = data;

            console.log(`📨 收到訊息: ${type} from ${data.from || 'unknown'} to ${data.to || 'unknown'}`);
            console.log(`📝 完整訊息內容:`, JSON.stringify(data, null, 2));

            // 更新用戶活躍時間
            const userId = connections.get(ws);
            if (userId && users.has(userId)) {
                users.get(userId)!.info.lastSeen = new Date();
            }

            switch (type) {
                case 'register':
                    console.log(`📝 處理註冊請求: ${data.id}`);
                    handleRegister(ws, data);
                    break;

                case 'unregister':
                    console.log(`📝 處理取消註冊請求`);
                    cleanupUser(ws);
                    break;

                case 'call':
                    console.log(`📝 處理通話請求: ${data.from} → ${data.to}`);
                    handleCall(data);
                    break;

                case 'call_answer':
                    console.log(`📝 處理接聽回應: ${data.from} ← ${data.to}`);
                    handleCallResponse(data, true);
                    break;

                case 'call_decline':
                    console.log(`📝 處理拒絕回應: ${data.from} ← ${data.to}`);
                    handleCallResponse(data, false);
                    break;

                case 'offer':
                case 'answer':
                case 'ice_candidate':
                case 'call_ended':
                    console.log(`📝 處理 WebRTC 信令: ${type}`);
                    relaySignaling(data);
                    break;

                default:
                    console.log(`❓ 未知訊息類型: ${type}`);
                    console.log(`📝 未知訊息內容:`, JSON.stringify(data, null, 2));
            }

        } catch (error) {
            console.error('❌ 訊息處理錯誤:', error);
            console.error('❌ 原始訊息:', message.toString());
        }
    });

    // 連接關閉處理
    ws.on('close', (code: number, reason: Buffer) => {
        console.log(`🔌 連接關閉 [${code}]: ${reason.toString()}`);
        cleanupUser(ws);
    });

    // 錯誤處理
    ws.on('error', (error: Error) => {
        console.error('❌ WebSocket 錯誤:', error);
        cleanupUser(ws);
    });
});

// 心跳檢測 (每30秒)
setInterval(() => {
    const deadConnections: WebSocket[] = [];

    wss.clients.forEach((ws: WebSocket) => {
        if (!(ws as any).isAlive) {
            deadConnections.push(ws);
            return;
        }

        (ws as any).isAlive = false;
        ws.ping();
    });

    // 清理死連接
    deadConnections.forEach(ws => {
        cleanupUser(ws);
        ws.terminate();
    });

    if (deadConnections.length > 0) {
        console.log(`💀 清理了 ${deadConnections.length} 個死連接`);
    }
}, 30000);

// 狀態監控 (每分鐘)
setInterval(() => {
    console.log(`📊 服務器狀態: ${users.size} 用戶在線, ${wss.clients.size} 個連接`);
}, 60000);

// 啟動服務器
const PORT = Number(process.env.PORT) || 8666;
const HOST = process.env.HOST || '0.0.0.0';

server.listen(PORT, HOST, () => {
    console.log(`🌟 WebRTC 信令服務器已啟動!`);
    console.log(`📍 地址: ws://${HOST}:${PORT}`);
    console.log(`🔧 本機測試: ws://localhost:${PORT}`);
    console.log(`🌐 區網訪問: ws://[你的IP]:${PORT}`);
    console.log(`⏹️  按 Ctrl+C 停止服務器\n`);
});

// 優雅關閉
process.on('SIGTERM', shutdown);
process.on('SIGINT', shutdown);

function shutdown(): void {
    console.log('\n🛑 正在關閉服務器...');

    // 通知所有用戶
    users.forEach((userData) => {
        safeSend(userData.ws, {
            type: 'server_shutdown',
            message: 'Server is shutting down'
        });
    });

    server.close(() => {
        console.log('✅ 服務器已關閉');
        process.exit(0);
    });
}

// 錯誤處理
process.on('unhandledRejection', (error: any) => {
    console.error('❌ 未處理的 Promise 拒絕:', error);
});

process.on('uncaughtException', (error: Error) => {
    console.error('❌ 未捕獲的異常:', error);
    process.exit(1);
});