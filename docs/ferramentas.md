# Ferramentas de produção

As quatro ferramentas do Lote 7 da Etapa 2 (versão 2.7.0), para produzir os personagens do catálogo em lotes. Nenhuma delas muda o jogo: os rascunhos que geram não ficam no mod até a etapa Personagens e habilidades.

## 1. Gerador do catálogo: `tools/catalogo.py`

Lê o catálogo (`tools/catalogo/personagens.json`, cópia do plano do guia mestre) e escreve o rascunho de cada personagem que ainda não está no mod (os 9 heróis de hoje ficam de fora):

| Arquivo | O que tem |
|---|---|
| `data/greenlantern/characters/<id>.json` | a ficha: classe, nível, esqueleto, transformação, trilha, passivas e as 5 a 8 habilidades com as funções por contexto, cada uma num preset da biblioteca |
| `data/greenlantern/recipe/<id>.json` | a receita do item, do catálogo |
| `assets/greenlantern/textures/entity/skin/` | a skin rascunho e as alternativas (com `_brilho`) |
| `assets/greenlantern/items/`, `models/item/`, `textures/item/` | o item com o ícone rascunho e as 4 peças do traje |

```
pip install pillow
python3 tools/catalogo.py --lote 1                  # os 23 do lote 1 (tools/catalogo/lotes.json)
python3 tools/catalogo.py --ids "Kilowog,Zatanna"   # pelo nome ou pelo nome civil do catálogo
python3 tools/catalogo.py --todos                   # os 91
python3 tools/catalogo.py --lote 1 --listar         # só mostra o que faria
python3 tools/catalogo.py --lote 1 --apagar         # apaga os rascunhos do lote
```

- O id é o nome civil do catálogo (`Jessica Cruz` vira `jessica_cruz`); quando o nome de herói se repete, o nome mostrado ganha o civil: "Lanterna Verde (Jessica Cruz)".
- O preset de cada função sai da classe dela e de palavras do efeito ("árvore" vira `cortar_arvore`, "pele" vira `pele_dura`; sem palavra conhecida, o preset padrão da classe); as supremas sem palavra conhecida usam `suprema_de_combate` ou `grande_obra`.
- A condição sai do texto do catálogo: "Segurar", "Segurar e soltar", "Mirando fornalha ou comida" (a primeira palavra conhecida vale: `fornalha`), "Na água", "No ar", "Correndo". O que o jogo ainda não sabe ler ("à noite", "numa vila") fica anotado em `_rascunho`, na ficha.
- Uma ficha que deixou de ser rascunho (alguém tirou o `_comentario` "Rascunho gerado por tools/catalogo.py" para acertá-la à mão) nunca é sobrescrita nem apagada.
- `--destino <pasta>` escreve em outra pasta com `data/` e `assets/` (o padrão é `src/main/resources`).

## 2. Teste automático: `engine_every_character`

Um gametest (`src/gametest/.../EveryCharacterTests.java`) que confere todo personagem da engine que estiver no mod, gerado ou feito à mão:

- o item está registrado e tem uma receita que carrega (menos os 3 personagens de teste);
- a barra de energia tem espaço, as passivas existem, há de 5 a 8 habilidades e toda função usa um preset que existe;
- o traje entra e as skins dão a volta (no criativo, todas);
- **cada função de cada habilidade** é usada sem nada no caminho (sem recarga, energia ou condição), não dá erro e não aumenta a energia (só a energia por acerto da classe pode voltar).

Entre uma função e outra o teste tira o que ficou (presets que duram, criaturas, o clima), para não mexer nos outros testes. Os personagens da engine saíram de `hero_powers_contract`, que continua conferindo os 9 heróis de hoje.

## 3. Prints por lote: roteiro `personagens`

O roteiro `personagens` (`src/gametest/.../scripts/PersonagensScript.java`) mostra, para cada personagem: o traje entrando, frente e costas, a roda e cada habilidade usada num zumbi do deserto à frente. Sem `GL_PERSONAGENS` (ids separados por vírgula), mostra todos os personagens da engine que não são de teste nem os 9 heróis de hoje, ou seja, o lote gerado.

```
python3 tools/catalogo.py --lote 2
GL_CLIENT_SCRIPT=personagens xvfb-run -a -s "-screen 0 1280x720x24" ./gradlew runClient -Pgametests --no-configuration-cache
python3 tools/contact_sheet.py run/screenshots contact-sheet-lote-2.png
```

No GitHub, o workflow Screenshots faz o mesmo: à mão, com `script` = `personagens` e `lote` = o número; num push, pela terceira coluna de `.github/screenshot-scripts.txt` (`claude/presets-personagem- personagens 1`). A folha de contato sai nos artefatos.

## 4. Verificador: `tools/verificar.py`

Confere, antes do PR, o que o jogo só descobriria tarde:

- **receitas únicas:** duas receitas com a mesma forma e os mesmos ingredientes brigam na bancada; e todo ingrediente existe (os do Minecraft são lidos do `client.jar` que o Gradle baixou);
- **números** das habilidades dentro dos limites: dano 0 a 60, custo 0 a 100, recarga 0 a 600, alcance 0 a 128, raio 0 a 96, duração 0 a 600;
- **nomes:** ids de personagem e de habilidade únicos, nomes não vazios, e os textos de `en_us` e `pt_br` com as mesmas chaves em cada parte de `src/main/heroes`;
- **arquivos que faltam:** item, peças do traje, modelo, ícone e skins de cada personagem da engine, e os `.ogg` que os `sounds.json` citam.

```
python3 tools/verificar.py            # o mod e os personagens de teste
python3 tools/verificar.py <pasta>    # mais uma pasta de recursos (rascunhos gerados com --destino)
```

Sai com erro e a lista dos problemas quando algo falha. As receitas iguais da Caixa Materna, do Pingente de Argo e do Raio do Mago já estavam no mod e aparecem só como aviso, porque mudá-las mexe em heróis existentes.

## No CI

- O job `build` roda o verificador depois de compilar.
- O job `catalogo` gera os 91 personagens no checkout (sem commit), roda o verificador, compila e roda todos os gametests, o automático incluído. Assim cada PR prova que o catálogo inteiro continua carregando e passando no teste.
