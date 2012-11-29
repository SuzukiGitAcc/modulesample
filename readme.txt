ビルド手順
（要）linphone-android
linphone-androidをビルド後、libs/armeabi、libs/armeabi-v7a、をmodulesample/ndklibにコピー
libs/aXMLRPC.jarをmodulesampleのlibにコピー
.soライブラリの更新なければ既存のものでOK

ターミナルからmodulesampleへディレクトリ移動
以下実行
sh ./build.sh

distフォルダにモジュールのzipファイルが作成される

linphoneライブラリ変更点
121030 ./submodule/linphone/coreapi/linphnecore.c L1861
受信時のタイムアウトをなくす

121121 ./submodules/linphone/mediastreamer2/src/msspeex.c L573, L578
speexのデコード時音量下げる

