# libs/

Jars necesarios para compilar y lanzar el `runClient` / `runAutotest` en **NeoForge 1.21.1** (no se suben
al repo). Nombres exactos:

| Archivo | De dónde sale |
|---|---|
| `pantographsandwires-0.2.3.jar` | Create: Pantographs & Wires `1.21.1-beta-0.2.3-C6` (neoforge), **sin** `META-INF/jars/` ni `META-INF/jarjar/` |
| `geckolib-4.8.4.jar`, `mclib-20.jar` | sacados de `META-INF/jars/` del jar de P&W |
| `dragonlib-3.0.28.jar` | DragonLib `1.21.1-beta-3.0.28` (neoforge), sin su jar-in-jar |
| `architectury-13.0.6.jar`, `forgeconfigapiport-21.1.1.jar` | sacados de `META-INF/jars/` del jar de DragonLib |
| `create-6.0.8.jar` | Create `1.21.1-6.0.8` (neoforge), sin `META-INF/jarjar/` (si no, Registrate & co. se cargan dos veces) |
| `flywheel-1.0.5.jar`, `ponder-1.0.64.jar`, `registrate-1.3.0.jar` | sacados de `META-INF/jarjar/` del jar de Create |

Todos se descargan de Modrinth. `mclib` no es un mod: va también en `additionalRuntimeClasspath`.
