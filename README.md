アップデート機能用のページです

## 🚀 GitHub Actions による APK 自動ビルド機能

本リポジトリでは GitHub Actions により、プッシュ時や手動トリガー時に自動で Android APK がビルドされるように設定されています（`.github/workflows/build-apk.yml`）。

### 📥 APKのダウンロード手順
1. GitHub リポジトリの **「Actions」** タブを開きます。
2. 一覧から最新の **「Android APK Build」** ワークフロー実行ログをクリックします。
3. ページ下部の **「Artifacts」**（成果物）セクションにある **`android-dashboard-debug-apk`** をクリックすると、ビルド済みの APK が ZIP 形式でダウンロードできます。
4. ZIP を展開した中に入っている `android-dashboard-debug.apk` を Android 端末に転送または直接ダウンロードしてインストールしてください。

### 🔄 手動でビルドを実行する場合（workflow_dispatch）
1. GitHub リポジトリの **「Actions」** タブを開きます。
2. 左サイドバーから **「Android APK Build」** を選択します。
3. **「Run workflow」** ドロップダウンボタンをクリックし、**「Run workflow」** を実行すると、コードを変更しなくても即座に最新 APK がビルドされます。

### 🏷️ リリース公開時の自動添付
GitHub でリリースタグ（例: `v1.0.1` など）を作成・公開すると、自動的にそのリリースページに APK ファイルがアタッチされます。アプリ内の「ソフトウェア更新」機能からも直接ダウンロード・アップデートが可能になります。

