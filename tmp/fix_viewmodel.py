import os

file_path = "/app/src/main/java/com/example/ui/DashboardViewModel.kt"

with open(file_path, "r", encoding="utf-8") as f:
    lines = f.readlines()

# Let's locate the line: "private fun useFallbackData(warningMessage: String) {" (which is around index 228, 0-indexed)
# and the line "// --- Task Database Actions (Chore / Checklist widget) ---" (which is around index 391, 0-indexed)

start_idx = -1
end_idx = -1

for idx, line in enumerate(lines):
    if "private fun useFallbackData(" in line and start_idx == -1:
        start_idx = idx
    if "// --- Task Database Actions (Chore / Checklist widget) ---" in line:
        end_idx = idx
        break

if start_idx != -1 and end_idx != -1:
    print(f"Found block from line {start_idx + 1} to {end_idx + 1}")
    
    # Let's build the replacement content
    replacement = """    private fun useFallbackData(warningMessage: String) {
        val cal = Calendar.getInstance()
        val month = cal.get(Calendar.MONTH) + 1
        val day = cal.get(Calendar.DAY_OF_MONTH)
        val region = _selectedRegion.value

        val fallbackContent = when (region.id) {
            "otsu" -> DashboardAiContent(
                news = listOf(
                    NewsItem("大津港から出発する『ミシガンクルーズ』、季節限定夜間便が運行決定", "観光", "1時間前"),
                    NewsItem("比叡山延暦寺にて国宝根本中堂の保存修理現場を公開する特別ツアー", "歴史・文化", "3時間前"),
                    NewsItem("大津市のびわ湖大津プリンスホテルにて地元の夏食材フェア開催", "経済・グルメ", "5時間前")
                ),
                trends = listOf(
                    TrendItem("びわ湖大花火大会", "大津港沖で打ち上げられる大規模花火。有料観覧席の先行販売が始まってSNSで大注目。"),
                    TrendItem("幻の三井寺力餅", "きな粉と青海苔の風味が特徴の大津の伝統銘菓。お土産や食べ歩きとして人気が再燃。")
                ),
                anniversaries = listOf(
                    "${month}月${day}日: 大津市制記念・びわ湖の日キャンペーン期間。",
                    "近江の歴史探訪デー: 壬申の乱から続く歴史遺跡を巡るウォークイベント開催中。"
                ),
                omihachimanTips = "大津市は琵琶湖の美しい夕景が素晴らしいですよ。ミシガンクルーズでの湖上散歩や、石山寺での紫式部ゆかりの歴史散策をお楽しみください！"
            )
            "hikone" -> DashboardAiContent(
                news = listOf(
                    NewsItem("国宝・彦根城にて『ひこにゃん』との毎日特別撮影会が再開", "観光", "1時間前"),
                    NewsItem("彦根港から竹生島へのクルーズ、夏期臨時便の運航開始", "地域・旅行", "3時間前"),
                    NewsItem("彦根東高校の伝統行事『彦根城クリーンアップ』に多数の市民が参加", "社会", "5時間前")
                ),
                trends = listOf(
                    TrendItem("彦根城の天守閣", "現存天守の一つとして知られる美しさ。城下町のキャッスルロード周辺の古民家カフェ巡りも話題。"),
                    TrendItem("ひこにゃんファンクラブ", "全国から届くお便りへの丁寧なお返事がSNSで温かい話題を呼んでいます。")
                ),
                anniversaries = listOf(
                    "${month}月${day}日: 井伊直弼公功績顕彰の日。",
                    "彦根藩ゆかり of 茶会: 伝統的な抹茶と和菓子を楽しむ地域文化イベント開催。"
                ),
                omihachimanTips = "彦根市は美しい国宝天守と美味しい近江牛の食べ歩きが魅力です！キャッスルロードでお団子を片手に、のんびりと歴史情緒をお楽しみくださいね。"
            )
            "kyoto" -> DashboardAiContent(
                news = listOf(
                    NewsItem("京都・鴨川の夏の風物詩『納涼床』が今シーズンも大盛況", "伝統・グルメ", "1時間前"),
                    NewsItem("嵐山・渡月橋周辺でのライトアップイベントが今年も計画決定", "観光・社会", "3時間前"),
                    NewsItem("京都市内の和菓子老舗が若い世代向けの新感覚マカロン羊羹を発売", "トレンド・経済", "5時間前")
                ),
                trends = listOf(
                    TrendItem("清水寺の舞台", "美しい新緑と壮大な見晴らしがSNSに多数投稿され、トレンド急上昇。"),
                    TrendItem("京都の宇治抹茶パフェ", "祇園や宇治の甘味処で味わう濃厚な抹茶スイーツを求めて、行列ができる店が続出。")
                ),
                anniversaries = listOf(
                    "${month}月${day}日: 京都の伝統工芸を称える記念日。",
                    "古都の歳時記: 今日から市内の寺院で特別な虫干しと特別拝観が開催されます。"
                ),
                omihachimanTips = "古都・京都はどこを切り取っても絵になる美しさですね。祇園の細道を歩いたり、ひんやりとした宇治抹茶を味わって、贅沢なひとときをどうぞ！"
            )
            "osaka" -> DashboardAiContent(
                news = listOf(
                    NewsItem("大阪城公園でフードフェスティバル開幕、国内外のご当地グルメ集結", "グルメ", "1時間前"),
                    NewsItem("道頓堀のシンボル周辺が遊歩道として整備、新たな観光名所に", "地域・開発", "3.5時間前"),
                    NewsItem("関西経済連合会が未来のスマートモビリティ導入実験を大阪万博跡地で実施", "テクノロジー", "6時間前")
                ),
                trends = listOf(
                    TrendItem("たこ焼き食べ比べ", "なんば・梅田周辺で異なるダシや焼き方の名店をハシゴする観光がSNSでバズり中。"),
                    TrendItem("あべのハルカス展望台", "地上300メートルからの大阪平野一望パノラマと、天空庭園での限定イベントが話題。")
                ),
                anniversaries = listOf(
                    "${month}月${day}日: 大阪なにわ食い倒れの日。",
                    "豊臣期歴史発掘シンポジウム: 大阪城の歴史を深く学ぶ記念講習が開催中。"
                ),
                omihachimanTips = "活気みなぎる大阪は、たこ焼きやお好み焼きの食べ歩きに熱狂的な魅力がありますよ！あべのハルカスから大都市の息吹を感じてください！"
            )
            "tokyo" -> DashboardAiContent(
                news = listOf(
                    NewsItem("東京駅丸の内駅舎前の広場にて、最新のフラワーアート展が開幕", "社会・エンタメ", "1時間前"),
                    NewsItem("千代田区が推進する秋葉原駅周辺のスマートシティデジタルガイドが稼働", "テクノロジー", "3時間前"),
                    NewsItem("皇居外苑での緑豊かなジョギングコースが健康トレンドとして再注目", "スポーツ・生活", "5時間前")
                ),
                trends = listOf(
                    TrendItem("丸の内カフェ巡り", "高層ビルの中庭にあるテラス席で楽しむ最先端サステナブルランチが大人気。"),
                    TrendItem("秋葉原アニメフェス", "国内外のポップカルチャーファンが集結する限定展示会にチケット完売の勢い。")
                ),
                anniversaries = listOf(
                    "${month}月${day}日: 千代田区緑化推進の日。",
                    "丸の内ビジネスフォーラム: 働き方改革を考える最先端セミナーが開催されます。"
                ),
                omihachimanTips = "東京都千代田区は、最先端のトレンドと皇居周辺の伝統が調和した素晴らしい街です。秋葉原のデジタルカルチャーや丸の内の並木道散策を満喫してください！"
            )
            "sapporo" -> DashboardAiContent(
                news = listOf(
                    NewsItem("大通公園の夏ビアガーデン、過去最大規模での開催準備が整う", "グルメ・イベント", "1時間前"),
                    NewsItem("札幌市時計台にて、開館記念の特別夜間無料公開イベントを実施", "歴史・観光", "3時間前"),
                    NewsItem("羊ヶ丘展望台から望むラベンダー畑が見頃を迎え、観光客が増加中", "地域", "5時間前")
                ),
                trends = listOf(
                    TrendItem("本場の味噌ラーメン", "すすきの周辺の行列店で味わう、濃厚スープとちぢれ麺の王道の一杯が再びバズり中。"),
                    TrendItem("大倉山ジャンプ競技場", "リフトで登った展望台からの札幌市内のパノラマ絶景が美しいと人気。")
                ),
                anniversaries = listOf(
                    "${month}月${day}日: 札幌市開拓の歴史を振り返る市民の日。",
                    "北海道大自然感謝デー: 地元の特産品をお得に楽しめるマルシェが開催されます。"
                ),
                omihachimanTips = "札幌市はさわやかな風と美味しい大地の恵みが自慢です！大通公園の木陰でのんびり過ごしたり、贅沢な海鮮丼を堪能して、心身ともに満たされてくださいね！"
            )
            "fukuoka" -> DashboardAiContent(
                news = listOf(
                    NewsItem("博多駅前の新商業施設オープンに朝から数千人の行列", "経済", "1時間前"),
                    NewsItem("大濠公園ボートハウスにて、夜景を楽しめるテラスBBQプランが開始", "観光・グルメ", "3時間前"),
                    NewsItem("太宰府天満宮の美しい風鈴まつり、涼やかな音色が参道に響き渡る", "歴史・伝統", "5時間前")
                ),
                trends = listOf(
                    TrendItem("博多屋台の豚骨ラーメン", "中洲や天神の夜空の下で、アツアツの細麺とコク深い白濁スープを楽しむ体験が国内外の旅人に大ウケ。"),
                    TrendItem("キャナルシティの噴水ショー", "最新のプロジェクションマッピングと融合した水と光のアートパフォーマンスが話題。")
                ),
                anniversaries = listOf(
                    "${month}月${day}日: 博多どんたく・祇園山笠記念ウィーク。",
                    "天神イノベーションデー: 地元の最先端ベンチャー企業が集結する展示会が開催中。"
                ),
                omihachimanTips = "福岡市は活気あふれる屋台文化とおいしいグルメの宝庫です！中洲の夜風を感じながら食べる一杯のラーメンや、太宰府天満宮での参拝をお楽しみください！"
            )
            else -> DashboardAiContent(
                news = listOf(
                    NewsItem("滋賀・びわ湖周辺海底調査、新たな中世湖底遺跡発見か", "歴史・科学", "2時間前"),
                    NewsItem("八幡堀周辺の伝統的な蔵元が若者向けのスパークリング日本酒を発表", "経済・グルメ", "4時間前"),
                    NewsItem("近江八幡市の町並み保存地区、ボランティアによる一斉清掃実施", "社会・ボランティア", "6時間前")
                ),
                trends = listOf(
                    TrendItem("琵琶湖水郷めぐり", "滋賀県近江八幡市にある日本初の重要文化的景観を巡る手漕ぎ和船ツアーが、癒やしスポットとしてSNSでトレンド入り。"),
                    TrendItem("近江牛ローストビーフ", "最高級近江牛を使った極上ローストビーフを、古い酒蔵を改装したレストランで味わう旅が若い層に大人気。"),
                    TrendItem("赤こんにゃくレシピ", "滋賀県ならではの真っ赤なこんにゃくを使った、ピリ辛煮物やヘルシー炒め物がクックパッドで急増。")
                ),
                anniversaries = listOf(
                    "${month}月${day}日: 近江商人ゆかりの日。",
                    "「三方よし（売り手よし、買い手よし、世間よし）」の思想を伝える市民向けフォーラム開催中。"
                ),
                omihachimanTips = "近江八幡市は美しい八幡堀や手漕ぎの水郷めぐりなど、見どころがいっぱいの伝統の街ですよ。ぜひ美味しい近江牛ランチと一緒に穏やかな旅情をお楽しみくださいね！"
            )
        }

        _aiContentState.value = AiContentUiState.Success(fallbackContent, isMock = true)
    }

"""
    new_lines = lines[:start_idx] + [replacement] + lines[end_idx:]
    with open(file_path, "w", encoding="utf-8") as f:
        f.writelines(new_lines)
    print("Replacement successful!")
else:
    print("Error: Indexes not found!")
