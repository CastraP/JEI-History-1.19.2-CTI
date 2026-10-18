# JEI History (1.19.2 Forge)

Client-side recipe lookup history for **JEI 11.6.0.1024** on **Minecraft 1.19.2 Forge**.

- Configuration: `config/jeihistory-client.toml`
  - `open_history` (default `true`)
  - `history_rows` (default `2`, range `1..10`)

## History storage

The history is saved per world, using the same directory layout as JEI bookmarks:

- Singleplayer: `config/jei/world/local/<levelId>/history.ini`
- Multiplayer: `config/jei/world/server/<serverName>_<ipHash>/history.ini`

## Credits

Ported from [JEI Recipe History](https://github.com/Christofmeg/JEI-Recipe-History) by Christofmeg (MIT). 
