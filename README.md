# OmniMiner

OmniMiner は、同じ種類の連結ブロックをまとめて破壊する Fabric Mod です。

`all-blocks` 系では、適正な工具で素早く採掘できるブロックを対象にします。
チェストなどのブロックエンティティは、安全のため既定では対象外です。

## 対応環境

- Minecraft 1.21.11
- Fabric Loader 0.18.2 以降
- Fabric API
- Java 21
- Cloth Config
- Mod Menu は任意
- Geyser と Floodgate は統合版プレイヤー対応を使う場合のみ必要

現時点では、実際にビルドと起動確認を行う Minecraft 1.21.11 のみを
メタデータ上でも許可しています。

## 操作

- `V`：一括採掘を有効化します。
- `O`：設定画面を開きます。
- トグルモードが無効の場合、`V` を押している間だけ有効です。
- トグルモードが有効の場合、`V` を押すたびに切り替わります。

専用サーバーでは、採掘数や探索方法などの採掘設定をサーバーが管理します。
クライアントのプレビューには、接続時と設定変更時にサーバー設定が同期されます。

## サーバーコマンド

設定を変更するコマンドは、ゲームマスター権限を持つ実行者だけが使用できます。

```text
/omniminer config
/omniminer set maxBlocks <1-512>
/omniminer set searchDiagonal <true|false>
/omniminer set autoCollect <true|false>
/omniminer set autoCollectExp <true|false>
/omniminer set breakLeaves <true|false>
/omniminer set includeBlockEntities <true|false>
/omniminer help
```

## 採掘仕様

- 開始ブロックと追加ブロックは、Minecraft の標準破壊処理を通ります。
- 保護 Mod の Fabric ブロック破壊イベントは、追加ブロックごとに呼び出されます。
- 一括採掘1回につき、工具耐久の消費判定は開始ブロックの1回分だけです。
- 一 tick に処理する追加ブロックは最大32個です。
- 統合版プレビューは、一回の更新につき最大128ブロック分を順番に表示します。
- 原木と葉の合計は `maxBlocks` を超えません。
- `logs chopped` の完了表示は原木だけを数え、同時に破壊した葉は含めません。
- 永続化された葉と、標準の葉プロパティを持たない Mod 葉は自動破壊しません。
- 自動回収でインベントリへ入らなかった残りは、アイテムエンティティとして残ります。
- プレビューは候補数です。保護 Mod の拒否や、別の原木につながっている葉により、
  実際の破壊数が少なくなる場合があります。

## 設定ファイル

設定は `config/omniminer.json` に保存されます。
JSON が壊れている場合は日時付きの `omniminer.invalid-*.json` へ退避し、
既定設定を再生成します。

## ビルド

```powershell
.\gradlew build --console plain
```

生成物は `build/libs` に出力されます。
