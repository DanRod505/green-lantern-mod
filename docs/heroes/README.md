# Kit de herói

Como um herói novo entra no mod, do papel ao PR.

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
| Gametests e roteiro de prints | `src/gametest/.../heroes/` e `.../scripts/` |
| Uma linha em `HeroRegistry`, `ModGameTests`, `ClientScripts` e `.github/screenshot-scripts.txt` | |

Cada poder começa com um efeito provisório que gasta energia e avisa "ainda não foi construído". A thread troca cada um pelo poder de verdade em `<Classe>Server.usePower`, faz os sons e texturas originais (os geradores em `tools/` mostram como) e escreve um gametest por poder.

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

## Definição de pronto

Está no fim do modelo da ficha: item com receita e conquista, recurso próprio, 5 a 8 poderes, mobilidade, momento épico, sons e tema originais em duas camadas, HUD, guia, textos em pt e en, gametest por poder, prints de traje e de todos os poderes, e os três testes de contrato passando.
