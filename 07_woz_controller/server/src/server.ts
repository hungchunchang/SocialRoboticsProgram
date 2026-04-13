// Enhanced server supporting both robot control and video calling
import WebSocket from 'ws';
import { createServer } from 'http';
import { promises as fs } from 'fs';
import path from 'path';

// Types
interface UserData {
    ws: WebSocket;
    type: 'robot' | 'controller';
    robotName?: string; // 'AI閱' | 'AI讀' | 'OREO'
    info: {
        registeredAt: Date;
        lastSeen: Date;
    };
}

interface RobotCommand {
    type: 'robot_command';
    targetRobot: string;
    action: 'speak' | 'move' | 'expression' | 'gesture';
    content: string;
    parameters?: {
        emotion?: string;
        gesture?: string;
        volume?: number;
        speed?: number;
    };
}

interface VideoCallMessage {
    type: 'video_call_request' | 'video_call_accept' | 'video_call_end';
    from: string;
    to: string;
    callId?: string;
}

interface ScriptMessage {
    type: 'load_script' | 'play_line' | 'pause_script' | 'stop_script';
    scriptId?: string;
    lineId?: string;
    scriptContent?: string;
}

console.log('🚀 Enhanced Robot Control & Video Server Starting...');

const server = createServer();
const wss = new WebSocket.Server({
    server,
    perMessageDeflate: false
});

// Storage
const users = new Map<string, UserData>();
const connections = new Map<WebSocket, string>();
const activeScripts = new Map<string, any>();

// Utility functions
function safeSend(ws: WebSocket, data: object): boolean {
    if (ws.readyState === WebSocket.OPEN) {
        ws.send(JSON.stringify(data));
        return true;
    }
    return false;
}

function broadcastToType(userType: 'robot' | 'controller', message: object): void {
    users.forEach((userData, userId) => {
        if (userData.type === userType) {
            safeSend(userData.ws, message);
        }
    });
}

function sendToRobot(robotName: string, command: RobotCommand): boolean {
    for (const [userId, userData] of users.entries()) {
        if (userData.type === 'robot' && userData.robotName === robotName) {
            return safeSend(userData.ws, command);
        }
    }
    console.log(`❌ Robot ${robotName} not found or offline`);
    return false;
}

function broadcastUserList(): void {
    const robotList = Array.from(users.entries())
        .filter(([_, userData]) => userData.type === 'robot')
        .map(([userId, userData]) => ({
            id: userId,
            name: userData.robotName,
            online: true
        }));

    console.log(`📋 Broadcasting robot list (${robotList.length} robots):`);
    robotList.forEach(robot => {
        console.log(`  - ${robot.name || 'No Role'} (${robot.id})`);
    });

    const message = {
        type: 'robot_list',
        robots: robotList,
        count: robotList.length
    };

    broadcastToType('controller', message);
}

function cleanupUser(ws: WebSocket): void {
    const userId = connections.get(ws);
    if (userId) {
        const userData = users.get(userId);
        
        // 如果是機器人斷線，需要清理角色分配
        if (userData?.type === 'robot' && userData.robotName) {
            console.log(`🤖 Robot ${userId} (${userData.robotName}) disconnected, clearing role assignment`);
            
            // 廣播角色被清除的消息
            const message = {
                type: 'role_cleared',
                robotId: userId,
                roleName: userData.robotName,
                timestamp: new Date().toISOString()
            };
            
            // 通知所有用戶該角色已被清除
            users.forEach((user) => {
                if (user.ws !== ws) { // 不發送給斷線的用戶
                    safeSend(user.ws, message);
                }
            });
        }
        
        users.delete(userId);
        connections.delete(ws);
        console.log(`🚪 User ${userId} disconnected`);
        broadcastUserList();
    }
}

function resetAllRoles(): void {
    users.forEach((userData, userId) => {
        if (userData.type === 'robot') {
            userData.robotName = undefined;
        }
    });
    
    const message = {
        type: 'all_roles_reset',
        timestamp: new Date().toISOString()
    };
    
    users.forEach((userData) => {
        safeSend(userData.ws, message);
    });
    
    console.log('🔄 All robot roles have been reset');
}

// Message handlers
function handleRegister(ws: WebSocket, data: any): void {
    const { id, role, robotName } = data;

    if (!id || !role) {
        safeSend(ws, { type: 'register_error', message: 'Missing required fields' });
        return;
    }

    if (users.has(id)) {
        const oldUser = users.get(id);
        if (oldUser) {
            oldUser.ws.close();
            connections.delete(oldUser.ws);
        }
    }

    users.set(id, {
        ws: ws,
        type: role, // Use role for internal storage
        robotName: robotName,
        info: { registeredAt: new Date(), lastSeen: new Date() }
    });
    connections.set(ws, id);

    safeSend(ws, { 
        type: 'register_success', 
        id: id,
        userType: role,
        robotName: robotName 
    });
    
    // Send robot list immediately to controllers
    if (role === 'controller') {
        const robotList = Array.from(users.entries())
            .filter(([_, userData]) => userData.type === 'robot')
            .map(([userId, userData]) => ({
                id: userId,
                name: userData.robotName,
                online: true
            }));

        safeSend(ws, {
            type: 'robot_list',
            robots: robotList,
            count: robotList.length
        });
    }
    
    broadcastUserList();
    console.log(`✅ Registered ${role}: ${id}${robotName ? ` (${robotName})` : ''}`);
}

function handleUnregister(data: any): void {
    const { id: unregisterId } = data;
    
    if (!unregisterId) {
        console.log('❌ No id provided for unregister');
        return;
    }
    
    if (users.has(unregisterId)) {
        const userData = users.get(unregisterId);
        if (userData) {
            // Remove from connections map first
            connections.delete(userData.ws);
            // Then remove from users map
            users.delete(unregisterId);
            console.log(`🗑️ User ${unregisterId} unregistered`);
            broadcastUserList();
        }
    } else {
        console.log(`❌ User ${unregisterId} not found for unregistration`);
    }
}

function handleRobotCommand(data: RobotCommand): void {
    const { targetRobot, action, content, parameters } = data;
    
    console.log(`🤖 Command to ${targetRobot}: ${action} - "${content}"`);
    
    const success = sendToRobot(targetRobot, {
        type: 'robot_command',
        targetRobot,
        action,
        content,
        parameters
    });

    if (success) {
        // Broadcast command status to controllers
        broadcastToType('controller', {
            type: 'command_sent',
            targetRobot,
            action,
            content,
            timestamp: new Date().toISOString()
        });
    }
}

function handleVideoCall(data: VideoCallMessage): void {
    const { type, from, to } = data;
    
    console.log(`📹 Video call: ${type} from ${from} to ${to}`);
    
    // Find target robot
    const targetUser = Array.from(users.entries())
        .find(([_, userData]) => userData.robotName === to);
    
    if (!targetUser) {
        console.log(`❌ Target robot ${to} not found`);
        return;
    }

    // Forward video call message
    safeSend(targetUser[1].ws, {
        ...data,
        callId: `${from}_${to}_${Date.now()}`
    });
}

// Missing method implementations for the enhanced server

function handleReadyForControl(ws: WebSocket, data: any): void {
    const userId = connections.get(ws);
    if (!userId) return;

    const userData = users.get(userId);
    if (!userData || userData.type !== 'controller') return;

    console.log(`🎮 Controller ${userId} ready for control`);
    
    // Broadcast to all robots that a controller is ready
    broadcastToType('robot', {
        type: 'controller_ready',
        controllerId: userId,
        timestamp: new Date().toISOString()
    });

    // Send current robot list to the controller
    broadcastUserList();
}

function handleRobotStatus(data: any): void {
    const { robotId, status, battery, currentAction } = data;
    
    console.log(`🤖 Robot status update: ${robotId} - ${status}`);
    
    // Broadcast robot status to all controllers
    const statusMessage = {
        type: 'robot_status_update',
        robotId,
        status,
        battery,
        currentAction,
        timestamp: new Date().toISOString()
    };

    broadcastToType('controller', statusMessage);
}

function handleRaiseHandCommand(data: any): void {
    const { robotName, action } = data;
    
    console.log(`✋ Raise hand command: ${robotName} - ${action}`);
    
    const command: RobotCommand = {
        type: 'robot_command',
        targetRobot: robotName,
        action: 'gesture',
        content: action === 'raise' ? '舉手' : '放下手',
        parameters: {
            gesture: action === 'raise' ? 'raise_hand' : 'lower_hand'
        }
    };

    sendToRobot(robotName, command);
}

function handleAssignRoles(data: any): void {
    const { assignments } = data; // 應該是 {"Robot_8110": "AI閱", "Robot_9905": "AI讀"}
    
    console.log(`👥 Role assignments:`, assignments);
    
    // 清理所有現有角色
    users.forEach((userData, userId) => {
        if (userData.type === 'robot') {
            userData.robotName = undefined;
        }
    });
    
    // 分配新角色
    for (const [robotId, roleName] of Object.entries(assignments)) {
        const userData = users.get(robotId);
        if (userData && userData.type === 'robot') {
            userData.robotName = roleName as string;
            console.log(`🤖 Robot ${robotId} assigned role: ${roleName}`);
        }
    }
    
    // 廣播角色分配
    const message = {
        type: 'roles_assigned',
        assignments,
        timestamp: new Date().toISOString()
    };

    users.forEach((userData) => {
        safeSend(userData.ws, message);
    });
    
    // 更新機器人列表
    broadcastUserList();
}

function handleStartControlMode(data: any): void {
    const { mode, participants } = data;
    
    console.log(`🎯 Starting control mode: ${mode}`);
    
    // Notify all participants about the control mode start
    const message = {
        type: 'control_mode_started',
        mode,
        participants,
        timestamp: new Date().toISOString()
    };

    participants?.forEach((participantId: string) => {
        const userData = users.get(participantId);
        if (userData) {
            safeSend(userData.ws, message);
        }
    });

    broadcastToType('robot', message);
}

function handleStopControlMode(data: any): void {
    console.log(`🛑 Stopping control mode`);
    
    // 廣播停止控制模式給所有機器人
    const message = {
        type: 'control_mode_stopped',
        timestamp: new Date().toISOString()
    };

    broadcastToType('robot', message);
    
    console.log('🔄 Control mode stopped - robots returning to standby');
}

// 同時修正 handlePlayScriptLine 函數，添加完整 line 資料：
function handlePlayScriptLine(data: any): void {
    const { scriptId, lineId, line } = data;  // 添加 line 參數
    
    // 如果傳入了完整的 line 數據，直接使用
    let targetLine = line;
    
    if (!targetLine) {
        // 如果沒有傳入 line，則從腳本中查找（保持向後兼容）
        const script = activeScripts.get(scriptId);
        if (!script) {
            console.log(`❌ Script ${scriptId} not found`);
            return;
        }

        for (const section of script.sections) {
            targetLine = section.lines.find((line: any) => line.id === lineId);
            if (targetLine) break;
        }
    }

    if (!targetLine) {
        console.log(`❌ Line ${lineId} not found`);
        return;
    }

    console.log(`🎭 Playing line: ${targetLine.speaker} - ${targetLine.content}`);

    // 解析目標機器人
    const targetRobots = targetLine.robot ? targetLine.robot.split(',').map((r: string) => r.trim()) : [];
    
    if (targetRobots.length === 0) {
        console.log(`❌ No target robots specified for line ${lineId}`);
        return;
    }

    // 發送給指定機器人
    const message = {
        type: 'play_line',
        scriptId,
        lineId,
        line: targetLine,
        targetRobots: targetRobots,
        timestamp: new Date().toISOString()
    };

    // 只發送給匹配的機器人
    users.forEach((userData, userId) => {
        if (userData.type === 'robot' && userData.robotName && targetRobots.includes(userData.robotName)) {
            safeSend(userData.ws, message);
            console.log(`📤 Sent line ${lineId} to robot ${userData.robotName} (${userId})`);
        }
    });

    // 廣播執行狀態給控制器
    broadcastToType('controller', {
        type: 'line_executed',
        scriptId,
        lineId,
        line: targetLine,
        targetRobots: targetRobots,
        timestamp: new Date().toISOString()
    });
}

function handleStartVideoCall(data: any): void {
    const { from, to, callType = 'video' } = data;
    
    console.log(`📹 Starting video call: ${from} → ${to}`);
    
    // Find the target robot
    const targetUser = Array.from(users.entries())
        .find(([_, userData]) => userData.robotName === to || userData.robotName?.includes(to));
    
    if (!targetUser) {
        // Send error back to caller
        const callerUser = users.get(from);
        if (callerUser) {
            safeSend(callerUser.ws, {
                type: 'video_call_error',
                message: `Robot ${to} not found or offline`,
                target: to
            });
        }
        return;
    }

    const callId = `call_${from}_${to}_${Date.now()}`;
    
    // Send call request to target robot
    safeSend(targetUser[1].ws, {
        type: 'incoming_video_call',
        from,
        to,
        callId,
        callType,
        timestamp: new Date().toISOString()
    });

    // Confirm call initiation to caller
    const callerUser = users.get(from);
    if (callerUser) {
        safeSend(callerUser.ws, {
            type: 'video_call_initiated',
            to,
            callId,
            status: 'calling'
        });
    }

    // Broadcast call status to robots
    broadcastToType('robot', {
        type: 'start_video_call',
        from,
        to,
        callId,
        timestamp: new Date().toISOString()
    });
}

function relayVideoSignaling(data: any): void {
    const { type, from, to } = data;
    
    if (!from || !to) {
        console.log('❌ Missing from/to in video signaling');
        return;
    }

    console.log(`📡 Relaying video signal: ${type} from ${from} to ${to}`);
    
    // First try to find by exact user ID
    let targetUser = users.get(to);
    
    if (!targetUser) {
        // Prioritize video call accounts for WebRTC signaling
        const videoCallEntry = Array.from(users.entries())
            .find(([userId, userData]) => {
                return userData.robotName === to && 
                       userData.type === 'robot' && 
                       userId.startsWith('VideoCall_');
            });
        
        if (videoCallEntry) {
            targetUser = videoCallEntry[1];
        } else {
            // Fall back to regular robot accounts
            const regularEntry = Array.from(users.entries())
                .find(([userId, userData]) => {
                    return userData.robotName === to && 
                           userData.type === 'robot' && 
                           !userId.startsWith('VideoCall_');
                });
            
            if (regularEntry) {
                targetUser = regularEntry[1];
            }
        }
    }

    if (!targetUser) {
        console.log(`❌ Video signaling target ${to} not found`);
        return;
    }

    // Relay the signaling message
    safeSend(targetUser.ws, data);
}

async function handleScriptLoad(data: ScriptMessage): Promise<void> {
    try {
        if (data.scriptContent) {
            // Parse script content
            const script = parseScript(data.scriptContent);
            const scriptId = data.scriptId || `script_${Date.now()}`;
            
            activeScripts.set(scriptId, script);
            
            // Broadcast script to controllers
            broadcastToType('controller', {
                type: 'script_loaded',
                scriptId,
                script
            });
            
            console.log(`📝 Script loaded: ${scriptId}`);
        }
    } catch (error) {
        console.error('❌ Script loading error:', error);
    }
}

function parseScript(content: string): any {
    const lines = content.split('\n');
    const script = {
        title: '',
        sections: [] as any[]
    };
    
    let currentSection: any = null;
    let lineId = 0;
    
    for (const line of lines) {
        const trimmed = line.trim();
        if (!trimmed) continue;
        
        // Section headers
        if (trimmed.startsWith('**【') && trimmed.endsWith('】**')) {
            currentSection = {
                title: trimmed.replace(/\*\*【|】\*\*/g, ''),
                lines: [] as any[]
            };
            script.sections.push(currentSection);
            continue;
        }
        
        // Dialog lines
        const dialogMatch = trimmed.match(/^\*\*(.*?)\*\*：(.+)$/);
        if (dialogMatch && currentSection) {
            const [, speaker, content] = dialogMatch;
            currentSection.lines.push({
                id: ++lineId,
                speaker: speaker.trim(),
                content: content.trim(),
                robot: extractRobotName(speaker),
                actions: extractActions(content)
            });
        }
    }
    
    return script;
}

function extractRobotName(speaker: string): string {
    // 先檢查組合情況
    if (speaker.includes('AI閱、AI讀') || speaker.includes('AI閱 AI讀')) {
        return 'AI閱,AI讀';
    }
    // 再檢查單個機器人
    if (speaker.includes('AI閱')) return 'AI閱';
    if (speaker.includes('AI讀')) return 'AI讀';
    if (speaker.includes('OREO')) return 'OREO';
    return '';
}

function extractActions(content: string): any[] {
    const actions = [];
    
    // Extract gestures
    const gestureMatch = content.match(/\(([^)]+)\)/g);
    if (gestureMatch) {
        gestureMatch.forEach(match => {
            const gesture = match.replace(/[()]/g, '');
            actions.push({ type: 'gesture', value: gesture });
        });
    }
    
    // Extract video call
    if (content.includes('打視訊')) {
        actions.push({ type: 'video_call', target: 'AI讀' });
    }
    
    return actions;
}

// WebSocket connection handling
wss.on('connection', (ws: WebSocket, req) => {
    const clientIP = req.socket.remoteAddress;
    console.log(`🔗 New connection from: ${clientIP}`);

    (ws as any).isAlive = true;
    ws.on('pong', () => {
        (ws as any).isAlive = true;
    });

    ws.on('message', async (message: WebSocket.Data) => {
        try {
            const data = JSON.parse(message.toString());
            const { type } = data;

            console.log(`📨 Message: ${type}`);

            const userId = connections.get(ws);
            if (userId && users.has(userId)) {
                users.get(userId)!.info.lastSeen = new Date();
            }

            switch (type) {
                case 'register':
                    handleRegister(ws, data);
                    break;
                
                    case 'unregister':
                    console.log(`🗑️ Unregister request for: ${data.id}`);
                    handleUnregister(data);
                    break;

                case 'ready_for_control':
                    handleReadyForControl(ws, data);
                    break;

                case 'robot_status':
                    handleRobotStatus(data);
                    break;

                case 'raise_hand_command':
                    handleRaiseHandCommand(data);
                    break;

                case 'assign_roles':
                    handleAssignRoles(data);
                    break;

                case 'start_control_mode':
                    handleStartControlMode(data);
                    break;
                case 'stop_control_mode':
                    handleStopControlMode(data);
                    break;

                case 'robot_command':
                    handleRobotCommand(data);
                    break;

                case 'start_video_call':
                    handleStartVideoCall(data);
                    break;
                case 'end_video_call':
                    broadcastToType('robot', {
                        type: 'end_video_call',
                        timestamp: new Date().toISOString()
                    });
                    console.log('📞 Video call ended by controller');
                    break;

                case 'load_script':
                    await handleScriptLoad(data);
                    break;

                case 'play_line':
                    handlePlayScriptLine(data);
                    break;

                // Video signaling
                case 'offer':
                case 'answer':
                case 'ice_candidate':
                case 'call_ended':
                    relayVideoSignaling(data);
                    break;

                default:
                    console.log(`❓ Unknown message type: ${type}`);
            }

        } catch (error) {
            console.error('❌ Message handling error:', error);
        }
    });

    ws.on('close', (code: number, reason: Buffer) => {
        console.log(`🔌 Connection closed [${code}]: ${reason.toString()}`);
        cleanupUser(ws);
    });

    ws.on('error', (error: Error) => {
        console.error('❌ WebSocket error:', error);
        cleanupUser(ws);
    });
});

// Heartbeat
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

    deadConnections.forEach(ws => {
        cleanupUser(ws);
        ws.terminate();
    });

    if (deadConnections.length > 0) {
        console.log(`💀 Cleaned ${deadConnections.length} dead connections`);
    }
}, 30000);

// Server startup
const PORT = Number(process.env.PORT) || 2768;
const HOST = process.env.HOST || '0.0.0.0';

server.listen(PORT, HOST, () => {
    console.log(`🌟 Enhanced Robot Control Server Started!`);
    console.log(`📍 Address: ws://${HOST}:${PORT}`);
    console.log(`🔧 Local: ws://localhost:${PORT}`);
    console.log(`⏹️  Press Ctrl+C to stop\n`);
});

process.on('SIGTERM', shutdown);
process.on('SIGINT', shutdown);

function shutdown(): void {
    console.log('\n🛑 Shutting down server...');
    
    // Remove all listeners to prevent memory leaks
    process.removeAllListeners('SIGTERM');
    process.removeAllListeners('SIGINT');
    
    users.forEach((userData) => {
        safeSend(userData.ws, {
            type: 'server_shutdown',
            message: 'Server is shutting down'
        });
        userData.ws.close();
    });
    
    wss.close(() => {
        server.close(() => {
            console.log('✅ Server closed');
            process.exit(0);
        });
    });
    
    // Force exit after timeout
    setTimeout(() => {
        console.log('⚠️ Force exiting...');
        process.exit(1);
    }, 3000);
}