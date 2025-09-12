---
marp: true
theme: default
paginate: true
backgroundColor: #ffffff
color: #2c3e50
style: |
  section {
    font-family: 'Segoe UI', 'PingFang TC', 'Microsoft YaHei', sans-serif;
    font-size: 28px;
    line-height: 1.6;
    padding: 60px;
  }
  h1 {
    color: #2c3e50;
    text-align: center;
    font-size: 48px;
    font-weight: 700;
    margin-bottom: 40px;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    background-clip: text;
    text-shadow: 0 2px 4px rgba(0,0,0,0.1);
  }
  h2 {
    color: #3498db;
    font-size: 36px;
    font-weight: 600;
    border-left: 6px solid #3498db;
    padding-left: 20px;
    margin: 30px 0 20px 0;
    background: linear-gradient(90deg, rgba(52,152,219,0.1) 0%, rgba(255,255,255,0) 100%);
    padding: 15px 0 15px 20px;
    border-radius: 0 8px 8px 0;
  }
  h3 {
    color: #27ae60;
    font-size: 30px;
    font-weight: 500;
    margin: 20px 0 15px 0;
  }
  .highlight {
    background: linear-gradient(135deg, #fff3cd 0%, #ffeaa7 100%);
    padding: 20px;
    border-radius: 12px;
    border-left: 5px solid #fdcb6e;
    margin: 20px 0;
    box-shadow: 0 4px 6px rgba(0,0,0,0.1);
  }
  .method-box {
    background: linear-gradient(135deg, #e3f2fd 0%, #bbdefb 100%);
    padding: 25px;
    border-radius: 12px;
    margin: 20px 0;
    box-shadow: 0 4px 12px rgba(0,0,0,0.15);
    border: 1px solid #90caf9;
  }
  .info-card {
    background: linear-gradient(135deg, #f8f9fa 0%, #e9ecef 100%);
    padding: 25px;
    border-radius: 15px;
    margin: 20px 0;
    box-shadow: 0 6px 20px rgba(0,0,0,0.1);
    border: 2px solid #dee2e6;
  }
  .columns {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 2rem;
    margin: 20px 0;
  }
  .tricolumns {
    display: grid;
    grid-template-columns: repeat(3, minmax(0, 1fr));
    gap: 1.5rem;
    margin: 20px 0;
  }
  .quacolumns {
    display: grid;
    grid-template-columns: repeat(4, minmax(0, 1fr));
    gap: 1rem;
    margin: 20px 0;
  }
  .rows {
    display: grid;
    grid-template-rows: repeat(2, minmax(0, 1fr));
    gap: 1.5rem;
    margin: 20px 0;
  }
  .accent-text {
    color: #e74c3c;
    font-weight: 600;
  }
  .subtitle {
    color: #7f8c8d;
    font-size: 22px;
    text-align: center;
    margin-top: -20px;
    margin-bottom: 40px;
  }
  ul, ol {
    margin: 15px 0;
    padding-left: 30px;
  }
  li {
    margin: 10px 0;
    line-height: 1.5;
  }
  strong {
    color: #2c3e50;
    font-weight: 600;
  }
  footer {
    position: absolute;
    bottom: 20px;
    right: 30px;
    font-size: 16px;
    color: #7f8c8d;
  }
---

# 114-1 Psy5314 社會機器人專題

<div class="subtitle">Social Robotics Seminar</div>

<div class="info-card">

**📚 課程資訊**

- **授課教師**：岳修平 教授
- **時間教室**：週五 7,8,9 節（南館地下A）
- **授課對象**：心理學系所研究生

</div>

---

## 課前要求

- 註冊 Github 帳號
- 在終端機登入、設定 PAT

- 下載 VSCode
- 安裝 Python
- 安裝 Jupyter Notebook

- 安裝 Conda

---

## 1. 註冊 GitHub 帳號

前往 [github.com](https://github.com) 註冊帳號，記住您的使用者名稱和密碼。

---

## 2. 安裝 VSCode

---

### 2.1 Windows

1. 前往 [code.visualstudio.com](https://code.visualstudio.com)
2. 下載 Windows 版本
3. 執行 `.exe` 檔案，依照指示安裝

### 2.2 macOS

1. 前往 [code.visualstudio.com](https://code.visualstudio.com)
2. 下載 macOS 版本
3. 開啟 `.dmg` 檔案，將 VSCode 拖拉到 Applications 資料夾

---

## 3. VSCode 擴充套件安裝

開啟 VSCode 後，按 `Ctrl+Shift+X` (Windows) 或 `Cmd+Shift+X` (macOS) 開啟擴充套件面板：

![alt text](<../../../../.images/截圖 2025-09-08 下午1.53.28.jpg>)

1. **Python**：搜尋並安裝 "Python" (Microsoft 官方版)
2. **Jupyter**：搜尋並安裝 "Jupyter" (Microsoft 官方版)

---

## 4. 安裝 [Conda](https://www.youtube.com/watch?v=-MSLJKjH8U0)

### 4.1 vscode for Windows

1. 前往 [anaconda.com/download](https://www.anaconda.com/download)
2. 選擇 Miniconda Installers，下載 Windows 版 Anaconda
3. 執行安裝檔，**勾選 "Add to PATH"** 選項
4. 重新啟動電腦

### 4.2 vscode for macOS

1. 前往 [anaconda.com/download](https://www.anaconda.com/download)
2. 選擇 Miniconda Installers，下載 macOS 版 Anaconda
3. 執行 `.pkg` 檔案安裝

---

## 5. 驗證安裝

開啟終端機（Terminal/命令提示字元），執行：

```bash
conda --version
python --version
```

應該會顯示版本號碼。

---

## 6. 設定 GitHub PAT (Personal Access Token)

### 步驟 1：產生 PAT

1. 登入 GitHub
2. 點選右上角頭像 → Settings
3. 左側選單選擇 "Developer settings"
4. 選擇 "Personal access tokens" → "Tokens (classic)"
5. 點選 "Generate new token (classic)"
6. 設定名稱，勾選 `repo` 權限
7. 複製產生的 token（只會顯示一次！）

---

### 步驟 2：在終端機設定

#### Windows (Git Bash 或 PowerShell)

```bash
git config --global user.name "你的GitHub使用者名稱"
git config --global user.email "你的GitHub信箱"
```

#### macOS (Terminal)

```bash
git config --global user.name "你的GitHub使用者名稱"
git config --global user.email "你的GitHub信箱"
```

---

### 步驟 3：首次推送時輸入認證

當第一次執行 `git push` 時：

- 使用者名稱：輸入 GitHub 使用者名稱
- 密碼：輸入剛才複製的 PAT (不是 GitHub 密碼)

---

## 7. 測試完整環境（optional）

### 7.1 建立測試專案

```bash
mkdir test-project
cd test-project
git init
echo "# Test Project" > README.md
git add README.md
git commit -m "Initial commit"
```

---

### 7.2 在 VSCode 中測試

1. 開啟 VSCode
2. 開啟 test-project 資料夾
3. 建立 `test.ipynb` 檔案
4. 執行以下 Python 程式碼：

```python
import sys
print("Python 版本:", sys.version)
print("環境設定完成！")
```

---

## 🚨 常見問題

### Windows 使用者

- 如果 `conda` 指令無法執行，重新安裝時務必勾選 "Add to PATH"
- 建議使用 Git Bash 作為終端機

### macOS 使用者

- 如果 Terminal 找不到 `conda`，執行：

  ```bash
  echo 'export PATH="/Users/你的使用者名稱/anaconda3/bin:$PATH"' >> ~/.zshrc
  source ~/.zshrc
  ```

---

### PAT 相關

- PAT 只會顯示一次，請務必複製保存
- 如果忘記 PAT，需要重新產生新的

---

**完成以上步驟後，您就準備好開始課程了！** 🎉