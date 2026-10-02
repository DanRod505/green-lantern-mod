# Green Lantern Corps — mod para Minecraft Java 1.21.11 (Forge)

> *"No dia mais claro, na noite mais densa, o mal sucumbirá ante a minha presença!"*

Mod do Lanterna Verde para **Minecraft Java 1.21.11** com **Forge 61.2.0** (compilado com **Java 25**).

## Capturas (tiradas automaticamente no jogo)

| | |
| --- | --- |
| ![Uniforme](docs/screenshots/02_uniform_front.jpg) | ![Metralhadora](docs/screenshots/05c_minigun_first_person.jpg) |
| ![Bolha](docs/screenshots/06_bubble.jpg) | ![Serra gigante](docs/screenshots/07b_saw_front.jpg) |
| ![Martelo gigante](docs/screenshots/08c_hammer_impact.jpg) | ![Recarga na Bateria de Poder](docs/screenshots/10b_charging.jpg) |

### Voo de poder

| | |
| --- | --- |
| ![Quebrando a barreira do som](docs/screenshots/flight_f05_sonic_boom.jpg) | ![Supersônico](docs/screenshots/flight_f06_supersonic.jpg) |
| ![Curva com rastro](docs/screenshots/flight_f09_turn.jpg) | ![Primeira pessoa inclinando na curva](docs/screenshots/flight_f09c_turn_first_person_bank.jpg) |
| ![Barrel roll](docs/screenshots/flight_f13_barrel_roll.jpg) | ![Pouso de herói](docs/screenshots/flight_f11b_hero_landing.jpg) |

## Conteúdo

| Item / recurso | Descrição |
| --- | --- |
| **Anel de Poder** | Um anel recém-fabricado vem **descarregado**: carregue-o na Bateria de Poder antes de usar. Veste o uniforme dos Lanternas Verdes, dá voo e cria construtos de luz sólida. Tem **energia finita** (barra no slot, tooltip e HUD). |
| **Bateria de Poder (lanterna)** | Bloco 3D animado que recarrega o anel. Clique com o anel na mão e fique perto: a energia flui da lanterna para o anel enquanto o juramento aparece na tela. |
| **Uniforme** | Máscara, uniforme, calças e botas de luz sólida (proteção equivalente a diamante). Guardam a armadura que você usava e a devolvem ao remover o uniforme. Não podem ser tirados nem dropados. |
| **Voo de poder** | Com o uniforme: dois toques no pulo para decolar (com explosão de energia no chão). Segure **para frente** e a velocidade vai **aumentando sem parar** até **quebrar a barreira do som** (estrondo sônico, anéis de vapor, flash na tela) e passar de Mach 1. Ver detalhes em *Voo de poder* abaixo. |

### Construtos

| # | Construto | Uso | Efeito |
| --- | --- | --- | --- |
| 1 | **Disparo de Energia** | Clique direito | Esfera pesada de luz sólida que explode no impacto (dano em área, sem quebrar blocos). |
| 2 | **Metralhadora** | Segure o clique direito | Metralhadora giratória de 6 canos flutua ao lado da mão e dispara uma rajada contínua. |
| 3 | **Bolha de Proteção** | Clique para ligar/desligar | Esfera ao redor do jogador: absorve 85% do dano, empurra criaturas e reflete projéteis. Consome energia por segundo. |
| 4 | **Serra Gigante** | Clique para invocar/dispensar | Veículo: você fica em pé numa prancha de luz com uma serra circular gigante na frente. **W/S** acelera/ré, **A/D** desliza para os lados, o **mouse** dá a direção, **pulo** salta e **agachar** sai. Retalha criaturas e corta folhas, troncos e plantas. |
| 5 | **Martelo Gigante** | Clique direito | Um martelo gigante surge no ponto para onde você olha, golpeia o chão e solta uma onda de choque que causa dano e arremessa tudo em volta (com tremor de câmera). |

### Voo de poder

* **Aceleração contínua:** segure **W** voando e a velocidade cresce de cruzeiro até Mach 1 em ~5 s (com **Ctrl/correr**, mais rápido) e continua até ~Mach 1,5.
* **Voe para onde olha:** a direção segue a câmera, com mais inércia quanto mais rápido (curvas largas e pesadas, a câmera inclina nas curvas).
* **Pose de super-herói:** em alta velocidade o Lanterna voa deitado, punho à frente; subindo na vertical, fica em pé como numa decolagem.
* **Aura verde** pulsante em volta do corpo e **rastro de energia** longo atrás de você (maior e mais duradouro em velocidade supersônica), além de faíscas e rajadas de luz.
* **Sensação de velocidade:** FOV aumenta com a velocidade, linhas de velocidade na tela, vento que sobe de tom, tremor da câmera em velocidade supersônica, medidor de Mach na tela.
* **Barreira do som:** estrondo sônico, anéis de vapor perpendiculares ao voo, flash e "SUPERSÔNICO" no HUD. Os outros jogadores ouvem o estrondo de longe.
* **Manobras:**
  * **Barrel roll** – toque duas vezes **A** ou **D** em alta velocidade: giro de 360° com esquiva lateral (a câmera gira junto).
  * **Freio aéreo** – segure **S** em alta velocidade: o Lanterna se endireita e freia.
  * **Decolagem explosiva** – comece a voar perto do chão.
  * **Pouso de herói** – mergulhe em alta velocidade contra o chão: onda de choque que causa dano e arremessa criaturas em volta (você não toma dano).
* **Trilha sonora do voo — "Lanterns Flight Theme":** ao voar rápido a música começa do início; quando você **quebra a barreira do som**, ela salta (com crossfade) direto para o **auge**, sincronizado com o estrondo sônico. Se você já passou do auge, ela segue normalmente; no fim, recomeça. Some suavemente quando você desacelera e usa o volume de *Música*.
* **Trilha alternativa (original):** a trilha orquestral procedural continua no mod. Para voltar a ela, use `flightTheme = "ORIGINAL"` em `config/greenlantern-client.toml` (ali também dá para desligar a música).
* Bater de frente numa parede em alta velocidade faz você perder o embalo (o anel te protege do dano).
* Voar mais rápido consome mais energia (até 3× em velocidade máxima).

#### Usando sua própria música

Por direitos autorais, o mod não pode incluir músicas de terceiros, como a trilha da série *Lanterns*. Mas você pode trocar a trilha do voo por qualquer música **para uso pessoal**: o build gera `greenlantern-custom-flight-music-resourcepack.zip` (fonte em `extras/custom-flight-music-pack/`). Coloque sua música convertida para `.ogg` como `assets/greenlantern/sounds/music/custom_flight_theme.ogg` dentro do zip, ponha o zip em `resourcepacks/` e ative-o em *Opções → Pacotes de Recursos*. Instruções completas no `LEIA-ME.txt` do pacote.

### Controles (configuráveis em *Opções → Controles → Atalhos → Lanterna Verde*)

| Tecla | Ação |
| --- | --- |
| **Clique direito** com o anel | Sem uniforme: veste o uniforme. Com uniforme: usa o construto selecionado. |
| **G** | Vestir / remover o uniforme |
| **R** / **V** | Próximo / construto anterior |
| **Shift + roda do mouse** (anel na mão) | Troca de construto |
| **Dois toques no pulo** (com uniforme) | Decolar / parar de voar |
| **W** voando (segure) / **Ctrl** | Acelerar até a barreira do som / acelerar mais rápido |
| **A A** ou **D D** voando rápido | Barrel roll |
| **S** voando rápido | Freio aéreo |
| Clique direito na **Bateria de Poder** com o anel | Começa ou interrompe a recarga |

### Receitas

```
Anel de Poder            Bateria de Poder
 .  D  .                  I  G  I
 E  .  E                  G  L  G
 .  E  .                  I  B  I
D = diamante, E = esmeralda      I = barra de ferro, G = vidro tingido de lima,
                                 L = lanterna, B = bloco de esmeralda
```

Também estão na aba criativa **Tropa dos Lanternas Verdes** (inclui um anel já carregado).

### Configuração

Todos os números de balanceamento (energia máxima, custo de cada construto, dano, velocidades do voo, Mach 1, tempo até a barreira do som, pouso de herói, se a serra corta plantas etc.) ficam em `config/greenlantern-common.toml`, gerado na primeira execução.

Preferências visuais e de som ficam em `config/greenlantern-client.toml`: escolher a trilha do voo (`LANTERNS` ou `ORIGINAL`), ligar/desligar a música e seu volume, efeitos de câmera (tremor, inclinação, giro), linhas de velocidade/medidor de Mach e rastros.

---

## Instalação (para jogar)

### Opção A — CurseForge App, importando o perfil pronto (mais fácil)

O build gera `greenlantern-1.21.11-1.2.0-curseforge-profile.zip`, um perfil do CurseForge que já configura o **Minecraft 1.21.11 + Forge 61.2.0** e já traz o mod dentro.

1. Abra o **CurseForge App** → **Minecraft** → **Create Custom Profile**.
2. Clique em **Import** (canto superior da janela) e escolha o arquivo `...-curseforge-profile.zip`.
3. Aguarde o CurseForge baixar o Minecraft e o Forge e clique em **Play**.

### Opção B — CurseForge App, perfil manual

1. Abra o **CurseForge App** → **Minecraft** → **Create Custom Profile**.
2. Em *Game Version* escolha **1.21.11**, em *Modloader* escolha **Forge** e selecione a versão **61.2.0** (ou mais recente da série 61.x). Clique em **Create**.
3. No perfil criado, clique nos **três pontinhos (⋮)** → **Open Folder**. Entre na pasta `mods` (crie se não existir).
4. Copie o arquivo `greenlantern-1.21.11-1.2.0.jar` para dentro de `mods`.
5. Volte ao CurseForge e clique em **Play**. O mod aparece na lista *Mods* do menu principal do jogo.

> Não é preciso instalar Java separadamente: o CurseForge e o launcher oficial usam o Java que vem com o Minecraft 1.21.11. O jar foi compilado com o JDK 25, mas gera bytecode Java 21, então roda tanto no Java 21 do launcher quanto no Java 25.

### Opção C — Launcher oficial

1. Baixe o instalador do Forge **1.21.11 – 61.2.0** em <https://files.minecraftforge.net/net/minecraftforge/forge/index_1.21.11.html> e execute-o (*Install client*).
2. Abra a pasta do jogo (`%appdata%\.minecraft` no Windows, `~/Library/Application Support/minecraft` no macOS, `~/.minecraft` no Linux) e coloque o jar em `mods/`.
3. No launcher, escolha o perfil **forge 1.21.11** e clique em **Jogar**.

### Servidor dedicado

Instale o servidor Forge 1.21.11-61.2.0 (*Install server* no mesmo instalador) e coloque o jar em `mods/` do servidor. **Todos os jogadores** também precisam do mod.

---

## Como testar no jogo (roteiro rápido)

1. Crie um mundo (Criativo facilita; em Sobrevivência dá para testar o consumo de energia).
2. Pegue o **Anel de Poder** e a **Bateria de Poder** na aba criativa *Tropa dos Lanternas Verdes*.
3. Clique direito com o anel (ou **G**): o uniforme aparece com som e partículas, e o HUD de energia surge no canto superior esquerdo.
4. Dois toques no pulo para **voar**.
5. Troque de construto com **R** e teste cada um. Invoque alguns zumbis como alvo (`/summon zombie`).
6. Em Sobrevivência, gaste a energia e depois coloque a **Bateria de Poder** no chão, clique nela com o anel na mão e fique perto até carregar.

---

## Compilação (para desenvolvedores)

### Requisitos

* **JDK 25** (Temurin/Adoptium recomendado). Se não estiver instalado, o Gradle baixa sozinho um JDK 25 compatível (via *foojay toolchain resolver*); basta ter **qualquer Java 17+** para iniciar o Gradle.
* Internet na primeira compilação (Forge, Minecraft e bibliotecas são baixados e ficam em cache).
* Nada mais: o **Gradle Wrapper** (`gradlew`) já está no projeto.

### Comandos

```bash
# Windows: use gradlew.bat no lugar de ./gradlew
./gradlew build                          # gera build/libs/greenlantern-1.21.11-1.2.0.jar
                                         # e build/distributions/...-curseforge-profile.zip
./gradlew runClient                      # abre o Minecraft com o mod (ambiente de desenvolvimento)
./gradlew runServer                      # servidor de desenvolvimento
./gradlew runGameTestServer -Pgametests  # roda os testes automáticos dentro do jogo
```

O **CI do GitHub** (`.github/workflows/build.yml`) compila o mod, roda os testes e publica o jar como *artifact* a cada push. Dá para baixar o jar pronto na aba **Actions** do repositório.

### Testes automáticos

`src/gametest` tem GameTests do próprio Minecraft que rodam num servidor real, com um jogador de verdade em modo sobrevivência:

* uniforme: vestir, guardar e devolver a armadura;
* voo consome energia; o uniforme some quando a energia acaba;
* voo de poder: o estrondo sônico só é aceito acima da barreira do som, o pouso de herói só com velocidade e sem ferir o dono, e o custo de energia cresce com a velocidade;
* disparo de energia, metralhadora, bolha (empurrão, absorção de dano, consumo, desligar), serra (montar, cortar folhas, ferir, sumir ao desmontar), martelo (onda de choque, sem ferir o dono);
* a lanterna recarrega o anel até encher;
* receitas e tipo de dano carregados.

Esses testes só entram no build com `-Pgametests`, então nunca vão para o jar distribuído.

`GL_CLIENT_SCRIPT=1 ./gradlew runClient -Pgametests` roda um roteiro visual automático que cria um mundo plano, usa todos os recursos e salva screenshots em `run/screenshots/`.

### Estrutura do código

```
src/main/java/com/danrod505/greenlantern/
├── GreenLantern.java        # entrada do mod (registros, config, rede)
├── GLConfig.java            # todas as opções de balanceamento
├── registry/                # itens, blocos, entidades, sons, partículas, componentes, tipos de dano
├── ring/                    # energia do anel, uniforme, voo, eventos comuns
├── flight/                  # voo de poder no servidor (validação das manobras, pouso de herói)
├── construct/               # API de construtos + os 5 construtos (impl/)
├── entity/                  # entidades dos construtos e projéteis
├── block/                   # Bateria de Poder (bloco + block entity de recarga)
├── item/                    # Anel de Poder e peças do uniforme
├── network/                 # pacotes cliente → servidor (teclas)
└── client/                  # renderizadores, partículas, HUD, teclas, tremor de câmera
    └── flight/              # física do voo, pose de herói, aura, rastro, câmera, HUD de Mach, vento e trilha
tools/                       # scripts Python que geram TODAS as texturas e sons
```

### Adicionando um novo construto

1. Crie uma classe em `construct/impl` estendendo `Construct` (escolha `INSTANT`, `HOLD` ou `TOGGLE` e implemente `activate`).
2. Registre-a em `ConstructRegistry` com uma linha `register(new MeuConstruto())`.
3. Adicione o ícone 16×16 em `tools/generate_textures.py` (tira `constructs.png`) e os nomes em `lang/en_us.json` e `lang/pt_br.json`.
4. Se precisar de entidade própria, registre em `ModEntities` e o renderizador em `ClientSetup`.

### Regerando texturas e sons

```bash
pip install pillow numpy scipy soundfile
python tools/generate_textures.py
python tools/generate_sounds.py
python tools/generate_flight_audio.py   # sons do voo e a trilha original (duas camadas)
python tools/import_flight_music.py musica.mp3 [segundos_do_auge]   # troca a trilha "Lanterns"
```

## Licença

MIT. Green Lantern é uma marca da DC Comics; este é um projeto de fã, sem fins lucrativos e sem afiliação com a DC.
