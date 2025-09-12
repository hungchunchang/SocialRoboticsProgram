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

## 工作坊內容

簡介 GitHub, python, dependencies, vscode, conda

---

## 什麼是 Git, Github?

![alt text](../../../../.images/git.png)

---

## 🕰️ 實作時間一

- [GitHub 頁面](https://github.com/hungchunchang/114fallSocialRobotics.git)

- 打開終端機，進入理想的資料儲存位置，輸入：

```shell

git clone https://github.com/hungchunchang/114fallSocialRobotics.git

```

- 在 VSCode 中開啟！

---


## 🐍 Python 是什麼？

Python就像一種「通用語言」，讓你可以跟電腦溝通，告訴它要做什麼事情。就像你用中文跟朋友溝通一樣，用Python可以跟電腦溝通。

---

## 📦 什麼是「依賴」？

依賴就是工具包！想像你要做一道菜：

你有基本的廚房設備（Python）
但你還需要食材和調料（這些就是「依賴」）
比如做蛋炒飯需要：雞蛋、米飯、醬油、蔥花

---

## 在Python世界裡

<div class="columns">

## 依賴 = 別人寫好的程式碼工具包

## 為什麼需要依賴？

想做數據分析？需要 pandas 這個工具包
想畫圖表？需要 matplotlib 這個工具包
想做網站？需要 Flask 或 Django 這些工具包

不用從零開始寫所有功能
就像買現成的醬油，不用自己釀造
站在巨人的肩膀上，事半功倍

</div>

---

## 🛠️ 什麼是「[依賴管理](https://www.youtube.com/watch?v=jd1aRE5pJWc)」？

<div class="columns">

## 繼續用廚房比喻

## 解決方案 = 依賴管理工具（Conda）

<div>
問題來了：

- 不同菜需要不同食材
- 食材有保存期限
- 有些食材會互相衝突（比如某些調料不能混用）
- 廚房空間有限

</div>

<div>
就像一個超級智慧的食材管理員：

- 自動採購：你說要做什麼菜，它自動買齊所有需要的食材
- 版本控制：確保買到對的版本（不會買到過期的）
- 衝突處理：知道哪些食材不能放在一起
- 環境隔離：中式廚房放中式調料、西式廚房放西式調料、互不干擾

</div>

</div>

---

## 🏠 Conda 的「環境」概念

<div class="columns">

## 想像你有多個專門的廚房

## 每個廚房（環境）都是獨立的

「數據分析廚房」：裝滿數據分析的工具
「網頁開發廚房」：裝滿網頁開發的工具
「機器學習廚房」：裝滿AI相關的工具

不會搞混工具
可以使用不同版本的Python
專案之間不會互相干擾

</div>

---

## 🕰️ 實作時間二

- 打開終端機
- 輸入

```shell

conda create -n <name_as_you_want> python=3.12
```
