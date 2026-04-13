// robot-controller.js - 主要機器人控制邏輯
class RobotController {
  constructor() {
    this.ws = null;
    this.isConnected = false;
    this.robots = new Map();
    this.currentScript = null;
    this.playingLines = new Set();

    this.init();
  }

  init() {
    this.connectWebSocket();
    this.setupEventListeners();
    this.updateConnectionStatus();
  }

  connectWebSocket() {
    const wsUrl = "wss://sociallab.duckdns.org/ntl_demo/";
    // const wsUrl = 'ws://localhost:8666'; // For local development

    this.ws = new WebSocket(wsUrl);

    this.ws.onopen = () => {
      this.isConnected = true;
      this.updateConnectionStatus();
      this.register();
      this.log("Connected to server");
    };

    this.ws.onmessage = (event) => {
      const data = JSON.parse(event.data);
      this.handleMessage(data);
    };

    this.ws.onerror = (error) => {
      this.log("WebSocket error: " + error.message);
    };

    this.ws.onclose = () => {
      this.isConnected = false;
      this.updateConnectionStatus();
      this.log("Disconnected from server");

      // Reconnect after 3 seconds
      setTimeout(() => this.connectWebSocket(), 3000);
    };
  }

  register() {
    if (this.ws && this.ws.readyState === WebSocket.OPEN) {
      this.ws.send(
        JSON.stringify({
          type: "register",
          id: "controller_" + Date.now(),
          role: "controller", // Changed from 'type' to 'role'
        })
      );
    }
  }

  handleMessage(data) {
    switch (data.type) {
      case "register_success":
        this.log("Registered as controller");
        break;

      case "robot_list":
        this.updateRobotList(data.robots);
        break;

      case "command_sent":
        this.log(`Command sent to ${data.targetRobot}: ${data.action}`);
        break;

      case "script_loaded":
        this.log("Script loaded successfully");
        break;

      case "video_call_request":
        this.handleVideoCall(data);
        break;

      default:
        this.log(`Unknown message: ${data.type}`);
    }
  }
  assignRoles() {
    const assignments = {};
    let roleIndex = 0;
    const roles = ["AI閱", "AI讀"];

    this.robots.forEach((robot, robotId) => {
      if (roleIndex < roles.length) {
        assignments[robotId] = roles[roleIndex];
        roleIndex++;
      }
    });

    this.ws.send(
      JSON.stringify({
        type: "assign_roles",
        assignments: assignments,
      })
    );
  }

  startControlMode() {
    this.ws.send(
      JSON.stringify({
        type: "start_control_mode",
        participants: Array.from(this.robots.keys()),
      })
    );
  }

  updateConnectionStatus() {
    const dot = document.getElementById("connectionDot");
    const status = document.getElementById("connectionStatus");

    if (this.isConnected) {
      dot.classList.add("connected");
      status.textContent = "Connected";
    } else {
      dot.classList.remove("connected");
      status.textContent = "Disconnected";
    }
  }

  updateRobotList(robots) {
    const container = document.getElementById("robotList");
    container.innerHTML = "";

    if (robots.length === 0) {
      container.innerHTML = `
                <div class="robot-item">
                    <span class="robot-emoji">🤖</span>
                    <span>No robots connected</span>
                </div>
            `;

      return;
    }

    robots.forEach((robot) => {
      const item = document.createElement("div");
      item.className = "robot-item online";
      item.innerHTML = `
            <span class="robot-emoji">🤖</span>
            <span>${robot.name || robot.id}</span>
        `;
      container.appendChild(item);

      this.robots.set(robot.name || robot.id, robot);
    });

    this.log(
      `Updated robot list: ${robots.map((r) => r.name || r.id).join(", ")}`
    );
  }

  setupEventListeners() {
    document.getElementById("scriptFile").addEventListener("change", (e) => {
      this.loadScript(e.target.files[0]);
    });
  }

  async loadScript(file) {
    if (!file) return;

    try {
      const content = await file.text();
      this.currentScript = this.parseScript(content);
      this.renderScript();

      // Send script to server
      if (this.ws && this.ws.readyState === WebSocket.OPEN) {
        this.ws.send(
          JSON.stringify({
            type: "load_script",
            scriptId: "current_script",
            scriptContent: content,
          })
        );
      }

      this.log(`Loaded script: ${file.name}`);
    } catch (error) {
      this.log(`Error loading script: ${error.message}`);
    }
  }

  parseScript(content) {
    const lines = content.split("\n");
    const script = {
      title: "",
      sections: [],
    };

    let currentSection = null;
    let lineId = 0;

    for (const line of lines) {
      const trimmed = line.trim();
      if (!trimmed) continue;

      // Section headers
      if (trimmed.startsWith("**【") && trimmed.endsWith("】**")) {
        currentSection = {
          title: trimmed.replace(/\*\*【|】\*\*/g, ""),
          lines: [],
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
          robot: this.extractRobotName(speaker),
          actions: this.extractActions(content),
        });
      }
    }

    return script;
  }

  extractRobotName(speaker) {
    if (speaker.includes("AI閱")) return "AI閱";
    if (speaker.includes("AI讀")) return "AI讀";
    if (speaker.includes("OREO")) return "OREO";
    return "";
  }

  extractActions(content) {
    const actions = [];

    // Extract gestures
    const gestureMatches = content.match(/\(([^)]+)\)/g);
    if (gestureMatches) {
      gestureMatches.forEach((match) => {
        const gesture = match.replace(/[()]/g, "");
        actions.push({ type: "gesture", value: gesture });
      });
    }

    // Extract video call
    if (content.includes("打視訊") || content.includes("視訊")) {
      actions.push({ type: "video_call", target: "AI讀" });
    }

    return actions;
  }

  renderScript() {
    const container = document.getElementById("scriptContent");
    container.innerHTML = "";

    if (!this.currentScript || !this.currentScript.sections.length) {
      container.innerHTML = `
                <div style="text-align: center; color: #666; margin-top: 100px;">
                    <h3>No script loaded</h3>
                    <p>Upload a script file to begin</p>
                </div>
            `;
      return;
    }

    this.currentScript.sections.forEach((section) => {
      const sectionDiv = document.createElement("div");
      sectionDiv.className = "script-section";

      sectionDiv.innerHTML = `
                <div class="section-header">${section.title}</div>
            `;

      section.lines.forEach((line) => {
        const lineDiv = document.createElement("div");
        lineDiv.className = "script-line";
        lineDiv.innerHTML = this.renderScriptLine(line);
        sectionDiv.appendChild(lineDiv);
      });

      container.appendChild(sectionDiv);
    });
  }

  renderScriptLine(line) {
    const robotClass = this.getRobotClass(line.robot);
    const actionsHtml = line.actions
      .map(
        (action) =>
          `<span class="action-tag ${action.type}">${
            action.value || action.type
          }</span>`
      )
      .join("");

    return `
            <button class="play-button" onclick="controller.playLine(${
              line.id
            })">
                ▶️
            </button>
            <div class="robot-indicator ${robotClass}">
                ${line.robot || "?"}
            </div>
            <div class="line-content">
                <div class="speaker ${robotClass.replace("robot-", "")}">${
      line.speaker
    }</div>
                <div class="dialogue">${line.content}</div>
                <div class="actions">${actionsHtml}</div>
            </div>
        `;
  }

  getRobotClass(robotName) {
    switch (robotName) {
      case "AI閱":
        return "robot-ai-yue";
      case "AI讀":
        return "robot-ai-read";
      case "OREO":
        return "robot-oreo";
      default:
        return "robot-unknown";
    }
  }

  playLine(lineId) {
    if (!this.currentScript) return;

    // Find the line
    let targetLine = null;
    for (const section of this.currentScript.sections) {
      targetLine = section.lines.find((line) => line.id === lineId);
      if (targetLine) break;
    }

    if (!targetLine || !targetLine.robot) {
      this.log(`Line ${lineId} not found or no robot specified`);
      return;
    }

    // Check if robot is available
    if (!this.robots.has(targetLine.robot)) {
      this.log(`Robot ${targetLine.robot} not available`);
      return;
    }

    // Mark as playing
    this.playingLines.add(lineId);
    this.updatePlayButton(lineId, true);

    // Send command to server
    if (this.ws && this.ws.readyState === WebSocket.OPEN) {
      this.ws.send(
        JSON.stringify({
          type: "play_line",
          scriptId: "current_script",
          lineId: lineId,
        })
      );
    }

    this.log(
      `Playing line ${lineId}: ${targetLine.robot} - "${targetLine.content}"`
    );

    // Simulate completion after 3 seconds
    setTimeout(() => {
      this.playingLines.delete(lineId);
      this.updatePlayButton(lineId, false);
    }, 3000);
  }

  updatePlayButton(lineId, isPlaying) {
    const buttons = document.querySelectorAll(".play-button");
    buttons.forEach((button, index) => {
      if (index + 1 === lineId) {
        if (isPlaying) {
          button.classList.add("playing");
          button.innerHTML = "⏸️";
        } else {
          button.classList.remove("playing");
          button.innerHTML = "▶️";
        }
      }
    });
  }

  sendRobotCommand(robotName, action, content, parameters = {}) {
    if (this.ws && this.ws.readyState === WebSocket.OPEN) {
      this.ws.send(
        JSON.stringify({
          type: "robot_command",
          targetRobot: robotName,
          action: action,
          content: content,
          parameters: parameters,
        })
      );

      this.log(`Command sent to ${robotName}: ${action} - "${content}"`);
    }
  }

  handleVideoCall(data) {
    this.log(`Video call initiated: ${data.from} -> ${data.to}`);
    document.getElementById("videoOverlay").style.display = "flex";
  }

  log(message) {
    const logArea = document.getElementById("logArea");
    const timestamp = new Date().toLocaleTimeString();
    const entry = document.createElement("div");
    entry.className = "log-entry";
    entry.innerHTML = `
            <span class="timestamp">[${timestamp}]</span>
            <span>${message}</span>
        `;

    logArea.appendChild(entry);
    logArea.scrollTop = logArea.scrollHeight;

    // Keep only last 50 entries
    while (logArea.children.length > 50) {
      logArea.removeChild(logArea.firstChild);
    }
  }
}

// Global functions
function stopAllRobots() {
  controller.log("Emergency stop - all robots stopped");

  // Send stop commands to all robots
  controller.robots.forEach((robot, name) => {
    controller.sendRobotCommand(name, "stop", "");
  });

  // Clear playing states
  controller.playingLines.clear();
  document.querySelectorAll(".play-button.playing").forEach((button) => {
    button.classList.remove("playing");
    button.innerHTML = "▶️";
  });
}

function endVideoCall() {
  document.getElementById("videoOverlay").style.display = "none";
  controller.log("Video call ended");

  // Send end call message
  if (controller.ws && controller.ws.readyState === WebSocket.OPEN) {
    controller.ws.send(
      JSON.stringify({
        type: "video_call_end",
        from: "controller",
        to: "all",
      })
    );
  }
}

// Initialize controller when page loads
let controller;
document.addEventListener("DOMContentLoaded", () => {
  controller = new RobotController();
});
