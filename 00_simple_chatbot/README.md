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
  code {
    background: #f0f0f0;
    padding: 2px 8px;
    border-radius: 4px;
    font-size: 0.9em;
  }
  pre code {
    background: none;
    padding: 0;
  }
---

# 00 - 環境建置與工具介紹

<div class="subtitle">VSCode、Git、GitHub、Python、Conda</div>

<div class="info-card">

**本節重點**

- 安裝 VSCode 開發環境
- 認識 Git 與 GitHub
- 設定 GitHub PAT（個人存取權杖）
- 安裝 Python 與 Conda 依賴管理
- Clone 課程專案並驗證環境

</div>

---

## 課前準備清單

- [ ] 註冊 GitHub 帳號
- [ ] 下載並安裝 VSCode
- [ ] 安裝 Python + Jupyter 擴充套件
- [ ] 安裝 Conda
- [ ] 在終端機設定 GitHub PAT

---

## 1. 註冊 GitHub 帳號

前往 [github.com](https://github.com) 註冊帳號，記住您的使用者名稱和密碼。

---

## 2. 安裝 VSCode

<div class="columns">

<div class="method-box">

### Windows

1. 前往 [code.visualstudio.com](https://code.visualstudio.com)
2. 下載 Windows 版本
3. 執行 `.exe` 檔案，依照指示安裝

</div>

<div class="method-box">

### macOS

1. 前往 [code.visualstudio.com](https://code.visualstudio.com)
2. 下載 macOS 版本
3. 開啟 `.dmg` 檔案，將 VSCode 拖拉到 Applications 資料夾

</div>

</div>

---

## 3. VSCode 擴充套件安裝

開啟 VSCode 後，按 `Ctrl+Shift+X`（Windows）或 `Cmd+Shift+X`（macOS）開啟擴充套件面板：

1. **Python**：搜尋並安裝 "Python"（Microsoft 官方版）
2. **Jupyter**：搜尋並安裝 "Jupyter"（Microsoft 官方版）

---

## 什麼是 Git / GitHub？

<div class="columns">

<div class="method-box">

### Git

- 版本控制工具
- 追蹤檔案的每次修改
- 可以回到任何一個歷史版本
- 在你的電腦上運作

</div>

<div class="method-box">

### GitHub

- Git 的「雲端空間」
- 把程式碼放到網路上
- 和其他人協作
- 像是程式碼的 Google Drive

</div>

</div>

> Git 管理版本，GitHub 負責分享與協作。

---

## 實作時間一：Clone 課程專案

打開終端機，進入理想的資料儲存位置，輸入：

```bash
git clone https://github.com/hungchunchang/SocialRoboticsProgram.git
```

然後在 VSCode 中開啟這個資料夾！

<div class="highlight">

`git clone` = 把 GitHub 上的專案完整複製到你的電腦。

</div>

---

## Python 是什麼？

Python 就像一種「通用語言」，讓你可以跟電腦溝通，告訴它要做什麼事情。

就像你用中文跟朋友溝通一樣，用 Python 可以跟電腦溝通。

<div class="info-card">

在這門課中，Python 主要用來：
- 建立聊天機器人的後端伺服器
- 呼叫 OpenAI API 產生回覆
- 處理 HTTP / WebSocket 通訊

</div>

---

## 什麼是「依賴」（Dependencies）？

依賴就是工具包！想像你要做一道菜：

- 你有基本的廚房設備（Python）
- 但你還需要食材和調料（這些就是「依賴」）

<div class="columns">

<div class="method-box">

### 依賴 = 別人寫好的工具包

- 想做網站？需要 `Flask`
- 想呼叫 AI？需要 `openai`
- 想處理 HTTP？需要 `requests`

</div>

<div class="method-box">

### 為什麼需要依賴？

- 不用從零開始寫所有功能
- 就像買現成的醬油，不用自己釀造
- 站在巨人的肩膀上，事半功倍

</div>

</div>

---

## 什麼是「依賴管理」？

<div class="columns">

<div class="info-card">

### 問題

- 不同專案需要不同套件
- 套件有版本相容問題
- 有些套件會互相衝突
- 全裝在一起容易出問題

</div>

<div class="info-card">

### 解決方案 = Conda

就像一個超級智慧的食材管理員：
- **自動採購**：自動裝好需要的套件
- **版本控制**：確保版本正確
- **衝突處理**：知道哪些不能共存
- **環境隔離**：每個專案一個廚房

</div>

</div>

---

## Conda 的「環境」概念

<div class="columns">

<div class="method-box">

### 想像你有多個專門的廚房

- 「聊天機器人廚房」：裝了 Flask、OpenAI
- 「資料分析廚房」：裝了 pandas、matplotlib
- 「網頁開發廚房」：裝了 Django、React

</div>

<div class="method-box">

### 每個廚房（環境）都是獨立的

- 不會搞混工具
- 可以使用不同版本的 Python
- 專案之間不會互相干擾

</div>

</div>

---

## 4. 安裝 Conda

<div class="columns">

<div class="method-box">

### Windows

1. 前往 [anaconda.com/download](https://www.anaconda.com/download)
2. 選擇 Miniconda Installers，下載 Windows 版
3. 執行安裝檔，**勾選 "Add to PATH"**
4. 重新啟動電腦

</div>

<div class="method-box">

### macOS

1. 前往 [anaconda.com/download](https://www.anaconda.com/download)
2. 選擇 Miniconda Installers，下載 macOS 版
3. 執行 `.pkg` 檔案安裝

</div>

</div>

---

## 5. 驗證安裝

開啟終端機（Terminal / 命令提示字元），執行：

```bash
conda --version
python --version
```

應該會顯示版本號碼。

---

## 實作時間二：建立 Conda 環境

```bash
conda create -n socialrobot python=3.12
conda activate socialrobot
```

<div class="highlight">

- `conda create -n socialrobot` = 建立一個叫 `socialrobot` 的環境
- `conda activate socialrobot` = 啟用這個環境
- 之後安裝的所有套件都只存在這個環境中

</div>

---

## 6. 設定 GitHub PAT

![alt text](how_it_works.png)

### 步驟 1：產生 PAT

1. 登入 GitHub
2. 點選右上角頭像 → **Settings**
3. 左側選單選擇 **Developer settings**
4. 選擇 **Personal access tokens** → **Tokens (classic)**
5. 點選 **Generate new token (classic)**
6. 設定名稱，勾選 `repo` 權限
7. 複製產生的 token（只會顯示一次！）

---

### 步驟 2：在終端機設定 Git

```bash
git config --global user.name "你的GitHub使用者名稱"
git config --global user.email "你的GitHub信箱"
```

### 步驟 3：首次推送時輸入認證

當第一次執行 `git push` 時：

- **使用者名稱**：輸入 GitHub 使用者名稱
- **密碼**：輸入剛才複製的 **PAT**（不是 GitHub 密碼）

<div class="highlight">

PAT 只會顯示一次，請務必複製保存！如果忘記，需要重新產生。

</div>

---

## 7. 測試完整環境（optional）

### 建立測試專案

```bash
mkdir test-project
cd test-project
git init
echo "# Test Project" > README.md
git add README.md
git commit -m "Initial commit"
```

### 在 VSCode 中測試

1. 開啟 VSCode → 開啟 `test-project` 資料夾
2. 建立 `test.ipynb` 檔案
3. 執行：

```python
import sys
print("Python 版本:", sys.version)
print("環境設定完成！")
```

---

## 常見問題

<div class="columns">

<div class="info-card">

### Windows 使用者

- `conda` 指令無法執行？
  → 重新安裝時勾選 **"Add to PATH"**
- 建議使用 **Git Bash** 作為終端機

</div>

<div class="info-card">

### macOS 使用者

- Terminal 找不到 `conda`？
  → 執行：

```bash
echo 'export PATH="~/anaconda3/bin:$PATH"' >> ~/.zshrc
source ~/.zshrc
```

</div>

</div>

---

## 課程專案總覽

<div class="info-card">

| 編號 | 專案 | 學習重點 |
|------|------|---------|
| 01 | FastAPI Chat | HTTP / REST API 基礎 |
| 02 | HTTP Chat (Android) | Android App + Flask |
| 03 | Audio WebSocket | WebSocket 即時語音 |
| 04 | Video Streaming | WebRTC 影像串流 |
| 05 | Video Call | 雙向視訊通話 |
| 06 | WoZ Controller | 整合所有技術遙控機器人 |

</div>

> 完成以上環境建置後，就準備好開始第一個專案了！
