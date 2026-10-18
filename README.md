# JEI History (1.19.2 Forge)

Client-side recipe lookup history for **JEI 11.6.0.1024** on **Minecraft 1.19.2 Forge**,
implemented as a Mixin patch mod. The JEI jar itself is neither modified nor replaced.

- Press R / U on an ingredient to look up its recipe / uses. The ingredient is added to a
  history area rendered below the JEI ingredient list.
- History entries support tooltips, clicking, dragging, and R/U lookups.
- Configuration: `config/jeihistory-client.toml`
  - `open_history` (default `true`)
  - `history_rows` (default `2`, range `1..10`)

## Build

Requires JDK 17 (a portable Temurin 17 is available in `../tools/jdk-17.0.20.1+1`).

```bash
export JAVA_HOME="E:/Mod_Composing/Minecraft/1.19.2/jei_history_1.19.2/tools/jdk-17.0.20.1+1"
./gradlew build
```

The jar is written to `build/libs/jeihistory-1.0.0.jar`.

## Credits

Ported from [JEI Recipe History](https://github.com/Christofmeg/JEI-Recipe-History) by
Christofmeg (MIT). The dashed border rendering is based on
[Roughly Enough Items](https://github.com/shedaniel/RoughlyEnoughItems) by shedaniel.
