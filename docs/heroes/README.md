# Kit de herói

Como um herói novo entra no mod, do papel ao PR.

> **Desde a 2.0.0 existe a engine de personagens.** Um personagem pode ser só uma ficha de dados em
> `src/main/resources/data/greenlantern/characters/<id>.json` (classe, nível, esqueleto, 5 a 8 habilidades,
> passivas, skins, fraquezas e cores), sem pacote Java próprio: a engine cria o item, o traje, a energia da classe,
> a tela e a roda. As habilidades dela vêm da biblioteca de presets (ver [`docs/presets.md`](../presets.md));
> uma habilidade com preset que não existe avisa que chega numa próxima versão. O item de cada personagem precisa
> de `assets/greenlantern/items/<id>.json` e dos 4 `items/<id>_cabeca|peito|pernas|pes.json` (que apontam
> para os modelos `item/skin_suit_*`), e a skin fica em `textures/entity/skin/<id do skin>.png` (com
> `_brilho` opcional). Exemplos: os personagens de teste em `src/gametest/resources`, feitos por
> `python3 tools/generate_engine_textures.py --test`. O kit abaixo continua valendo para heróis com código próprio.

## 1. Ficha do herói (antes do código)

Copie [`_modelo.md`](_modelo.md) para `docs/heroes/<id>.md` e preencha: identidade, recurso e recarga, mobilidade, de 5 a 8 poderes, momento épico, traje e paleta, música e lista de prints. O Dalton aprova a ficha; só então começa o código. Confira a ficha com:

```
python3 tools/new_hero.py docs/heroes/<id>.md --check
```

## 2. Esqueleto

```
python3 tools/new_hero.py docs/heroes/<id>.md
```

Cria, já compilando e passando nos testes de contrato:

| O quê | Onde |
|---|---|
| Herói, poderes, servidor, conteúdo (itens, sons, energia) e config própria | `src/main/java/.../<pacote>/` |
| HUD padrão e seção do Guia dos Heróis | `src/main/java/.../client/<pacote>/` |
| Textos em pt e en, sons do traje | `src/main/heroes/<id>/` |
| Texturas provisórias nas cores da ficha, receita e conquista | `src/main/resources/` |
| Scripts de arte e som do herói, sobre `tools/herolib` | `tools/generate_<id>_textures.py` e `_audio.py` |
| Gametests e roteiro de prints | `src/gametest/.../heroes/` e `.../scripts/` |
| Uma linha em `HeroRegistry`, `ModGameTests`, `ClientScripts` e `.github/screenshot-scripts.txt` | |
| Próxima versão menor, uma frase na descrição do mod e a seção do herói no README | `gradle.properties`, `README.md` |

Cada poder começa com um efeito provisório que gasta energia e avisa "ainda não foi construído". A thread troca cada um pelo poder de verdade em `<Classe>Server.usePower`, faz os sons e texturas originais e escreve um gametest por poder.

### Blocos de poder (`hero/power/`)

Os poderes novos se montam com estes blocos, em vez de reescrever o que outro herói já fez. Cada um tem gametest próprio em `PowerKitTests`.

| Bloco | Para quê |
|---|---|
| `Targets` | quem o poder acerta (nunca jogadores, suportes de armadura nem animais domados), inimigos, criaturas num raio ou num cone, a mira do herói |
| `Area` | os blocos numa esfera ou num cone |
| `BlockEffects` | estilhaçar vidro e gelo fino, apagar fogo e tochas, congelar água |
| `Hits` | dano que acerta mesmo em golpes seguidos, empurrar para longe, arremessar, atordoar |
| `Dash` | avanço em linha reta que para na parede e atropela cada criatura uma vez |
| `Bolts` | rajadas que voam alguns ticks, param no primeiro bloco ou criatura e podem perseguir um alvo |
| `Fx` | som para todos ou só para o herói, chão abaixo, poeira, anel e linha de partículas |

### Arte e som (`tools/herolib/`)

O gerador cria `tools/generate_<id>_textures.py` e `tools/generate_<id>_audio.py`, que já rodam e usam a biblioteca comum: `herolib.pixel` (paleta tirada das cores da ficha, mapas de letras, camadas do traje 64x32, ícones 16x16 e a folha da roda) e `herolib.audio` (ruído, varreduras, sinos, impactos, bateria e sintetizadores, os instrumentos de orquestra e `Theme`, que grava o tema em duas camadas que fazem loop). A thread redesenha os mapas e compõe os sons nesses arquivos. Os heróis feitos antes da biblioteca continuam com os scripts deles.

## 3. Testes que todo herói ganha

Em `HeroContractTests`, rodando para cada herói de `HeroRegistry`:

- `hero_contracts`: id único, item carregado na aba criativa, textos em pt e en do item, do traje, de cada poder e da roda, folha de ícones com a largura certa, sons do traje no `sounds.json`.
- `hero_suits_contract`: o traje veste com o item na mão, tira o traje do herói anterior, as teclas falam com este herói, e tirar o traje devolve a armadura de antes.
- `hero_powers_contract`: cada poder é selecionado na roda e usado sem erro, sem nunca aumentar a energia.

## 4. Antes de cada push

```
./gradlew compileJava -Pgametests
./gradlew runGameTestServer -Pgametests --no-configuration-cache
```

Os prints rodam no workflow Screenshots quando a branch bate com uma linha de `.github/screenshot-scripts.txt` (o gerador já adiciona `claude/<id-com-hifens>-`), ou à mão com qualquer roteiro. Além dos prints, o workflow gera uma folha de contato com todos eles. O job "Kit de herói" do Build gera o herói de [`tools/hero_kit/heroi_teste.md`](../../tools/hero_kit/heroi_teste.md) a cada push para garantir que o gerador continua funcionando.

## 5. Versão e lançamento

O gerador já sobe a versão menor (`mod_version`), põe o herói na descrição do mod e começa a seção dele no README, com os poderes da ficha. A thread completa essa seção com o que o herói faz de verdade: ela vira as notas da Release.

Quando o PR entra na `main`, o workflow Release publica a versão no GitHub (tag `v<versão>`) com o .jar, o perfil do CurseForge e o pacote de música. Os arquivos de uma Release baixam sem login e não expiram; o link para entregar é `https://github.com/DanRod505/green-lantern-mod/releases/latest`.

O Build confere em todo PR (`python3 tools/release.py check`) que cada herói com ficha está na descrição do mod e no README, e que um PR que muda o mod depois de a versão ter sido lançada sobe a versão: `python3 tools/release.py bump minor` para herói novo, `bump patch` para o resto.

## Definição de pronto

Está no fim do modelo da ficha: item com receita e conquista, recurso próprio, 5 a 8 poderes, mobilidade, momento épico, sons e tema originais em duas camadas, HUD, guia, textos em pt e en, gametest por poder, prints de traje e de todos os poderes, e os três testes de contrato passando.
