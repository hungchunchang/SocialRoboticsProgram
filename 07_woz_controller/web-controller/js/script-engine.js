// script-engine.js - 演出腳本解析與執行引擎
class PerformanceScriptEngine {
    constructor(robotController) {
        this.controller = robotController;
        this.currentScript = null;
        this.executionState = {
            isPlaying: false,
            isPaused: false,
            currentSectionIndex: 0,
            currentLineIndex: 0,
            playingLines: new Map()
        };
        this.videoCallManager = new VideoCallManager(robotController);
    }

    parseScript(content) {
        const lines = content.split('\n');
        const script = {
            title: this.extractTitle(content),
            metadata: this.extractMetadata(content),
            sections: []
        };
        
        let currentSection = null;
        let lineId = 0;
        
        for (const line of lines) {
            const trimmed = line.trim();
            if (!trimmed || trimmed.startsWith('//')) continue;
            
            // Section headers: **【section_name】**
            if (trimmed.match(/^\*\*【.+】\*\*$/)) {
                currentSection = {
                    title: trimmed.replace(/\*\*【|】\*\*/g, ''),
                    lines: [],
                    metadata: this.extractSectionMetadata(trimmed)
                };
                script.sections.push(currentSection);
                continue;
            }
            
            // Dialog lines: **speaker**：content
            const dialogMatch = trimmed.match(/^\*\*(.*?)\*\*：(.+)$/);
            if (dialogMatch && currentSection) {
                const [, speaker, content] = dialogMatch;
                const parsedLine = this.parseDialogLine(++lineId, speaker, content);
                currentSection.lines.push(parsedLine);
            }
        }
        
        return script;
    }

    parseDialogLine(id, speaker, content) {
        const line = {
            id,
            speaker: speaker.trim(),
            content: content.trim(),
            robot: this.extractRobotName(speaker),
            actions: this.extractActions(content),
            timing: this.extractTiming(content),
            emotions: this.extractEmotions(content)
        };
        
        // Clean content of action markers
        line.cleanContent = this.cleanContent(content);
        
        return line;
    }

    extractRobotName(speaker) {
        const robotMap = {
            'AI閱': 'AI閱',
            'AI讀': 'AI讀', 
            'OREO': 'OREO',
            'Ai閱': 'AI閱',
            'Ai讀': 'AI讀'
        };
        
        for (const [pattern, robotName] of Object.entries(robotMap)) {
            if (speaker.includes(pattern)) {
                return robotName;
            }
        }
        
        return '';
    }

    extractActions(content) {
        const actions = [];
        
        // Extract gestures in parentheses: (gesture)
        const gestureMatches = content.match(/\(([^)]+)\)/g);
        if (gestureMatches) {
            gestureMatches.forEach(match => {
                const gesture = match.replace(/[()]/g, '').trim();
                actions.push({
                    type: 'gesture',
                    value: gesture,
                    timing: 'during_speech'
                });
            });
        }
        
        // Extract video call actions
        if (content.includes('打視訊') || content.includes('視訊')) {
            const videoMatch = content.match(/打視訊給(.+?)[）)]/);
            const target = videoMatch ? videoMatch[1].trim() : 'AI讀';
            actions.push({
                type: 'video_call',
                target: target,
                timing: 'during_speech'
            });
        }
        
        // Extract movement actions
        const movementPatterns = [
            { pattern: /走.*?到(.+?)/, type: 'move_to' },
            { pattern: /帶.*?去(.+?)/, type: 'guide_to' },
            { pattern: /轉向|轉身/, type: 'turn' },
            { pattern: /前進|走向前/, type: 'move_forward' }
        ];
        
        movementPatterns.forEach(({ pattern, type }) => {
            const match = content.match(pattern);
            if (match) {
                actions.push({
                    type: 'movement',
                    subtype: type,
                    target: match[1] || '',
                    timing: 'during_speech'
                });
            }
        });
        
        return actions;
    }

    extractTiming(content) {
        const timing = {
            pause_before: 0,
            pause_after: 0,
            speed: 'normal'
        };
        
        // Extract timing markers
        const pauseMatch = content.match(/\[pause:(\d+)\]/);
        if (pauseMatch) {
            timing.pause_after = parseInt(pauseMatch[1]);
        }
        
        const speedMatch = content.match(/\[speed:(slow|normal|fast)\]/);
        if (speedMatch) {
            timing.speed = speedMatch[1];
        }
        
        return timing;
    }

    extractEmotions(content) {
        const emotions = [];
        
        const emotionPatterns = [
            { pattern: /興奮|激動/, emotion: 'excited', intensity: 0.8 },
            { pattern: /溫和|平靜/, emotion: 'calm', intensity: 0.6 },
            { pattern: /熱情|活潑/, emotion: 'enthusiastic', intensity: 0.9 },
            { pattern: /專業|正式/, emotion: 'professional', intensity: 0.7 }
        ];
        
        emotionPatterns.forEach(({ pattern, emotion, intensity }) => {
            if (content.match(pattern)) {
                emotions.push({ emotion, intensity });
            }
        });
        
        return emotions.length > 0 ? emotions : [{ emotion: 'neutral', intensity: 0.5 }];
    }

    cleanContent(content) {
        return content
            .replace(/\([^)]+\)/g, '') // Remove gesture markers
            .replace(/\[.*?\]/g, '') // Remove timing markers
            .replace(/-->.+?$/, '') // Remove stage directions
            .trim();
    }

    extractTitle(content) {
        const lines = content.split('\n');
        for (const line of lines) {
            if (line.trim() && !line.startsWith('**【')) {
                return line.trim();
            }
        }
        return 'Untitled Performance';
    }

    extractMetadata(content) {
        return {
            robots: this.getRequiredRobots(content),
            estimatedDuration: this.estimateDuration(content),
            complexity: this.assessComplexity(content)
        };
    }

    getRequiredRobots(content) {
        const robots = new Set();
        const matches = content.match(/\*\*(.*?)\*\*：/g);
        if (matches) {
            matches.forEach(match => {
                const speaker = match.replace(/\*\*|：/g, '');
                const robot = this.extractRobotName(speaker);
                if (robot) robots.add(robot);
            });
        }
        return Array.from(robots);
    }

    estimateDuration(content) {
        const lines = content.split('\n').filter(line => 
            line.trim() && line.includes('：')
        );
        return lines.length * 4; // 4 seconds per line estimate
    }

    assessComplexity(content) {
        let complexity = 'simple';
        
        if (content.includes('視訊') || content.includes('video')) {
            complexity = 'complex';
        } else if (content.match(/\([^)]+\)/g)?.length > 5) {
            complexity = 'medium';
        }
        
        return complexity;
    }

    async executeScript(script) {
        this.currentScript = script;
        this.executionState.isPlaying = true;
        this.executionState.isPaused = false;
        this.executionState.currentSectionIndex = 0;
        this.executionState.currentLineIndex = 0;
        
        this.controller.log(`Starting script execution: ${script.title}`);
        this.controller.log(`Required robots: ${script.metadata.robots.join(', ')}`);
        
        try {
            for (let sectionIndex = 0; sectionIndex < script.sections.length; sectionIndex++) {
                if (!this.executionState.isPlaying) break;
                
                this.executionState.currentSectionIndex = sectionIndex;
                await this.executeSection(script.sections[sectionIndex]);
            }
            
            this.controller.log('Script execution completed');
        } catch (error) {
            this.controller.log(`Script execution error: ${error.message}`);
        } finally {
            this.executionState.isPlaying = false;
        }
    }

    async executeSection(section) {
        this.controller.log(`Executing section: ${section.title}`);
        
        for (let lineIndex = 0; lineIndex < section.lines.length; lineIndex++) {
            if (!this.executionState.isPlaying) break;
            
            // Wait if paused
            while (this.executionState.isPaused && this.executionState.isPlaying) {
                await this.delay(100);
            }
            
            this.executionState.currentLineIndex = lineIndex;
            await this.executeLine(section.lines[lineIndex]);
        }
    }

    async executeLine(line) {
        if (!line.robot) {
            this.controller.log(`Skipping line ${line.id}: No robot specified`);
            return;
        }
        
        this.controller.log(`Executing line ${line.id}: ${line.robot} - "${line.cleanContent}"`);
        
        // Mark line as playing
        this.executionState.playingLines.set(line.id, true);
        this.controller.updatePlayButton(line.id, true);
        
        try {
            // Execute pre-speech pause
            if (line.timing.pause_before > 0) {
                await this.delay(line.timing.pause_before * 1000);
            }
            
            // Send speech command
            if (line.cleanContent) {
                await this.sendSpeechCommand(line);
            }
            
            // Execute actions
            await this.executeActions(line.actions, line.robot);
            
            // Wait for speech completion
            const speechDuration = this.estimateSpeechDuration(line.cleanContent, line.timing.speed);
            await this.delay(speechDuration);
            
            // Execute post-speech pause
            if (line.timing.pause_after > 0) {
                await this.delay(line.timing.pause_after * 1000);
            }
            
        } finally {
            // Mark line as completed
            this.executionState.playingLines.delete(line.id);
            this.controller.updatePlayButton(line.id, false);
        }
    }

    async sendSpeechCommand(line) {
        const emotion = line.emotions.length > 0 ? line.emotions[0].emotion : 'neutral';
        const speed = this.convertSpeedToNumeric(line.timing.speed);
        
        this.controller.sendRobotCommand(line.robot, 'speak', line.cleanContent, {
            emotion: emotion,
            speed: speed,
            volume: 0.8
        });
    }

    async executeActions(actions, robotName) {
        for (const action of actions) {
            await this.executeAction(action, robotName);
        }
    }

    async executeAction(action, robotName) {
        switch (action.type) {
            case 'gesture':
                this.controller.sendRobotCommand(robotName, 'gesture', action.value);
                break;
            case 'expression':
                this.controller.sendRobotCommand(robotName, 'expression', action.emotion || action.value);
                break;
            case 'movement':
                this.controller.sendRobotCommand(robotName, 'move', action.target || action.value);
                break;
            case 'video_call':
                await this.executeVideoCall(action);
                break;
        }
    }

    async executeVideoCall(action) {
        this.controller.log(`Initiating video call to: ${action.target}`);
        await this.videoCallManager.initiateCall(action.target);
    }

    convertSpeedToNumeric(speed) {
        const speedMap = {
            'slow': 0.7,
            'normal': 1.0,
            'fast': 1.3
        };
        return speedMap[speed] || 1.0;
    }

    estimateSpeechDuration(text, speed = 'normal') {
        const baseCharsPerSecond = 3; // Chinese characters per second
        const speedMultiplier = {
            'slow': 0.7,
            'normal': 1.0,
            'fast': 1.3
        };
        
        const duration = (text.length / baseCharsPerSecond / speedMultiplier[speed]) * 1000;
        return Math.max(duration, 1000); // Minimum 1 second
    }

    delay(ms) {
        return new Promise(resolve => setTimeout(resolve, ms));
    }

    stopExecution() {
        this.executionState.isPlaying = false;
        this.controller.log('Script execution stopped');
        
        // Clear all playing states
        this.executionState.playingLines.clear();
        document.querySelectorAll('.play-button.playing').forEach(button => {
            button.classList.remove('playing');
            button.innerHTML = '▶️';
        });
    }

    pauseExecution() {
        this.executionState.isPaused = !this.executionState.isPaused;
        this.controller.log(`Script execution ${this.executionState.isPaused ? 'paused' : 'resumed'}`);
    }
}

// Video Call Manager for Robot-to-Robot Communication
class VideoCallManager {
    constructor(robotController) {
        this.controller = robotController;
        this.activeCall = null;
        this.localStream = null;
        this.remoteStream = null;
        this.peerConnection = null;
        this.isInitiator = false;
    }

    async initiateCall(targetRobot) {
        try {
            this.controller.log(`Initiating video call to ${targetRobot}`);
            
            // Show video overlay
            document.getElementById('videoOverlay').style.display = 'flex';
            
            // Get user media (camera and microphone)
            this.localStream = await navigator.mediaDevices.getUserMedia({
                video: true,
                audio: true
            });
            
            // Display local video
            const localVideo = document.getElementById('localVideo');
            localVideo.srcObject = this.localStream;
            
            // Send call request through WebSocket
            if (this.controller.ws && this.controller.ws.readyState === WebSocket.OPEN) {
                this.controller.ws.send(JSON.stringify({
                    type: 'video_call_request',
                    from: 'controller',
                    to: targetRobot,
                    callId: `call_${Date.now()}`
                }));
            }
            
            this.isInitiator = true;
            this.activeCall = targetRobot;
            
        } catch (error) {
            this.controller.log(`Error initiating video call: ${error.message}`);
            this.endCall();
        }
    }

    endCall() {
        this.controller.log('Ending video call');
        
        // Stop local stream
        if (this.localStream) {
            this.localStream.getTracks().forEach(track => track.stop());
            this.localStream = null;
        }
        
        // Clear video elements
        const localVideo = document.getElementById('localVideo');
        const remoteVideo = document.getElementById('remoteVideo');
        if (localVideo) localVideo.srcObject = null;
        if (remoteVideo) remoteVideo.srcObject = null;
        
        // Hide video overlay
        document.getElementById('videoOverlay').style.display = 'none';
        
        this.activeCall = null;
        this.isInitiator = false;
    }
}