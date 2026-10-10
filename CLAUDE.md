# Heróis do Universo DC (mod id `greenlantern`)

Forge 1.21.11, Java 25 compilando para 21. O dono fala português: responda em português.

- Compile antes de cada push: `./gradlew compileJava -Pgametests` (testes: `./gradlew runGameTestServer -Pgametests --no-configuration-cache`).
- Herói novo: siga [`docs/heroes/README.md`](docs/heroes/README.md). Ficha aprovada em `docs/heroes/<id>.md`, depois `python3 tools/new_hero.py docs/heroes/<id>.md`. Cada herói é um pacote próprio mais uma linha em `hero/HeroRegistry.java`.
- Textos e sons ficam por herói em `src/main/heroes/<parte>/` (juntados no build pela tarefa `mergeHeroResources`), não em `assets/greenlantern/lang`.
- Gametests por herói em `src/gametest/.../gametest/heroes/`, cada um com um `test_instance/<nome>.json`. Roteiros de prints em `.../gametest/scripts/`, listados em `ClientScripts`; a branch escolhe o roteiro por `.github/screenshot-scripts.txt`.
- Dentro do jogo, mudanças de fundação não podem alterar os heróis existentes: os gametests e os prints provam isso.
