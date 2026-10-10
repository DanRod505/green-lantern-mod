# Textos e sons por herói

Cada pasta aqui guarda os textos (`lang/en_us.json`, `lang/pt_br.json`) e os sons (`sounds.json`) de uma parte do mod:

| Pasta | O que tem |
|---|---|
| `common` | o que todo herói usa: aba criativa, teclas, roda de poderes, Guia dos Heróis, sons do voo |
| `lantern` | Lanterna Verde, construtos, Mecha e Oa |
| `flash`, `aquaman` (com Atlântida, Kraken e montarias), `batman`, `superman`, `wonder_woman` | cada herói |
| `trench` | O Fosso |

O Minecraft só lê um arquivo de idioma por língua e um `sounds.json`, então a tarefa `mergeHeroResources` do Gradle junta tudo no build. Uma mesma chave em duas pastas faz o build falhar, com o nome das duas.

Os arquivos de som (`.ogg`) e as texturas continuam em `src/main/resources/assets/greenlantern/`. O teste `hero_contracts` confere que todo herói tem os textos dos poderes, do item e do traje nas duas línguas, e os sons do traje aqui.
