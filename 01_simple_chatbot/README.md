# 01 - 在 Jupyternotebook 建立自己的對話機器人！

- Clone 課程專案並驗證環境

---

## 按照 [這裡](00_building_env/01_Git.md) 的步驟下載專案

## [實作] 測試完整環境

- 進入 VSCode，選擇左側的 01_simple_chatbot
- 打開 `simple_chatbot.ipynb`
- 依照指示執行

```python
# 測試 Python 環境是否正常，按左側的按鈕執行，如果有提示要安裝，就按 install
print("Hello, Social Robots!")
print("Python 環境設定成功！")
```

- 如果有印出上面的

```sh
Hello, Social Robots!
Python 環境設定成功！
```

> 就代表完成了！
> 完成以上環境建置後，就準備好開始第一個專案了！

如果有任何問題，歡迎隨時[寄信給我](mailto:r13227136@ntu.edu.tw)

---

## 常見問題

### Windows 使用者

- `conda` 指令無法執行？
  → 重新安裝時勾選 **"Add to PATH"**
- 建議使用 **Git Bash** 作為終端機

### macOS 使用者

- Terminal 找不到 `conda`？
  → 執行：

```bash
echo 'export PATH="~/anaconda3/bin:$PATH"' >> ~/.zshrc
source ~/.zshrc
```

---