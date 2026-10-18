# JEI配方查询历史记录（1.19.2 Forge）

基于**JEI 11.6.0.1024**的配方查询历史记录功能。

- 配置文件：`config/jeihistory-client.toml`
  - `open_history` (默认 `true`)
  - `history_rows` (默认 `2`, 范围为 `1..10`)

## 历史记录保存

历史记录根据每个存档进行保存：
- 单人存档：`config/jei/world/local/<levelId>/history.ini`
- 多人存档：`config/jei/world/server/<serverName>_<ipHash>/history.ini`

# JEI History (1.19.2 Forge)

Recipe lookup history for **JEI 11.6.0.1024**.

- Configuration: `config/jeihistory-client.toml`
  - `open_history` (default `true`)
  - `history_rows` (default `2`, range `1..10`)

## History storage

The history is saved per world:

- Singleplayer: `config/jei/world/local/<levelId>/history.ini`
- Multiplayer: `config/jei/world/server/<serverName>_<ipHash>/history.ini`
