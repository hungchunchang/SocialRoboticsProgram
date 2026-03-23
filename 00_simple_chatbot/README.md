# 00 - 環境建置與工具介紹

<div class="subtitle">VSCode、Git、GitHub、Python、Conda</div>

<div class="info-card">

**本節重點**

- 安裝 VSCode 開發環境
- 認識 Git 與 GitHub
- 安裝 Python 與 Conda 依賴管理
- Clone 課程專案並驗證環境

</div>

---

---

## 1. 安裝 VSCode

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

### [實作] VSCode 擴充套件安裝

開啟 VSCode 後，按 `Ctrl+Shift+X`（Windows）或 `Cmd+Shift+X`（macOS）開啟擴充套件面板：

1. **Python**：搜尋並安裝 "Python"（Microsoft 官方版）
2. **Jupyter**：搜尋並安裝 "Jupyter"（Microsoft 官方版）

---

## 2. 什麼是 Git / GitHub？

<div class="columns">

<div class="method-box">

### [概念] Git

- 版本控制工具
- 追蹤檔案的每次修改
- 可以回到任何一個歷史版本
- 在你的電腦上運作

</div>

<div class="method-box">

### [概念] GitHub

- Git 的「雲端空間」
- 把程式碼放到網路上
- 和其他人協作
- 像是程式碼的 Google Drive

</div>

</div>

> Git 管理版本，GitHub 負責分享與協作。

---

### [實作] 註冊 GitHub 帳號

- 下載 [githun桌面版](https://desktop.github.com/download/)，並安裝
- 在 Github 桌面版創建帳號（如果還沒有）

### [實作] Clone 課程專案

桌面版和終端機二選一即可

### 使用桌面版

- 登入後，選擇「clone a Repository from the internet」
- 選擇 URL, 貼上 `https://github.com/hungchunchang/SocialRoboticsProgram.git`
- `Local Path` 欄位可以選擇自己想要存放檔案的位置（上層資料夾名稱盡量不要有中文、空格）
- 選擇右下角 `clone` 按鈕
- 複製完成之後，點選 `Open in Visual Studio Cod`

### 使用終端機

打開終端機，進入你希望資料儲存的位置（上層資料夾名稱盡量不要有中文、空格），輸入：

```bash
git clone https://github.com/hungchunchang/SocialRoboticsProgram.git
```

然後在 VSCode 中開啟這個資料夾！

<div class="highlight">

note: `git clone` = 把 GitHub 上的專案完整複製到你的電腦。

</div>

---

## 5. Python and Conda

介紹 python and conda(miniforge)

### [概念] Python 和 Conda 是什麼？

Python 就像一種「通用語言」，讓你可以跟電腦溝通，告訴它要做什麼事情。

就像你用中文跟朋友溝通一樣，用 Python 可以跟電腦溝通。

<div class="info-card">

在這門課中，Python 主要用來：
- 建立聊天機器人的後端伺服器
- 呼叫 OpenAI API 產生回覆
- 處理 HTTP / WebSocket 通訊

</div>

---

###  [概念] 什麼是「依賴」（Dependencies）？

依賴就是工具包，想像你要做一道菜：

- 你有基本的廚房設備（Python）
- 但你還需要食材和調料（這些就是「依賴」）

<div class="columns">

<div class="method-box">

#### 依賴 = 別人寫好的工具包

- 想做網站？需要 `Flask`
- 想呼叫 AI？需要 `openai`
- 想處理 HTTP？需要 `requests`

</div>

<div class="method-box">

#### 為什麼需要依賴？

- 不用從零開始寫所有功能
- 就像買現成的醬油，不用自己釀造
- 站在巨人的肩膀上，事半功倍

</div>

</div>

---

### 什麼是「依賴管理」？

<div class="columns">

<div class="info-card">

#### 問題

- 不同專案需要不同套件
- 套件有版本相容問題
- 有些套件會互相衝突
- 全裝在一起容易出問題

</div>

<div class="info-card">

#### 解決方案 = Conda(or uv, pyenv etc...)

就像一個超級智慧的食材管理員：
- **自動採購**：自動裝好需要的套件
- **版本控制**：確保版本正確
- **衝突處理**：知道哪些不能共存
- **環境隔離**：每個專案一個廚房

</div>

</div>

<div>
> 你已經完成這個章節的 80%!!
</div>div>


---

### Conda 的「環境」概念

<div class="columns">

<div class="method-box">

#### 想像你有多個專門的廚房

- 「聊天機器人廚房」：裝了 Flask、OpenAI
- 「資料分析廚房」：裝了 pandas、matplotlib
- 「網頁開發廚房」：裝了 Django、React

</div>

<div class="method-box">

#### 每個廚房（環境）都是獨立的

- 不會搞混工具
- 可以使用不同版本的 Python
- 專案之間不會互相干擾

</div>

</div>

---

### [實作] 安裝 Conda

<div class="columns">

<div class="method-box">

#### Windows

1. 前往 [conda-forge.org/download](https://conda-forge.org/download/)
2. 選擇 Miniconda Installers，下載 Windows 版
3. 執行安裝檔，**勾選 "Add to PATH"**
4. 重新啟動電腦

</div>

<div class="method-box">

#### macOS

1. 前往 [conda-forge.org/download](https://conda-forge.org/download/)
2. 選擇 Miniconda Installers，下載 macOS 版（.sh檔案）
3. 在終端機（terminal）中打開該檔案夾（e.g. 在終端機中輸入 cd Downloads/my_files，會進入名為 my_files 的資料夾）
4. 輸入這段指令 `bash Miniforge3-$(uname)-$(uname -m).sh`，就會開始安裝
5. 當第一次出現一大串文字，按 enter or down arror, 直到他問 yes|no, 輸入 yes
6. 接著一路按 enter, 直到他再次問你 yes|no, 輸入 yes

</div>

</div>

---

### 驗證安裝

開啟終端機（Terminal / 命令提示字元），執行：

```bash
conda --version
python --version
```

應該會顯示版本號碼。

---

### [實作]建立 Conda 環境

```bash
conda create -n socialrobot python=3.12
conda activate socialrobot
```

<div class="highlight">

說明：

- `conda create -n socialrobot` = 建立一個叫 `socialrobot` 的環境，你也可以取任何你想要的名稱
- `conda activate socialrobot` = 啟用這個環境
- 之後安裝的所有套件都只存在這個環境中

</div>

---

##  [實作] 測試完整環境

- 進入 VSCode，選擇左側的 00_simple_chatbot
- 打開 `simple_chatbot.ipynb`
- 依照指示執行

```python
# 測試 Python 環境是否正常，按左側的按鈕執行，如果有提示要安裝，就按 install
print("Hello, Social Robots!")
print("Python 環境設定成功！")
```
- 如果有印出

```sh
Hello, Social Robots!
Python 環境設定成功！
```


> 就代表完成了！
> 完成以上環境建置後，就準備好開始第一個專案了！

如果有任何問題，歡迎隨時[寄信給我](mailto:r13227136@ntu.edu.tw)
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


