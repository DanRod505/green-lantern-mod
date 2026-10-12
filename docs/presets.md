# Presets de habilidade

Uma habilidade da ficha de um personagem (`data/greenlantern/characters/<id>.json`) escolhe um preset e preenche os números; ninguém escreve código para um personagem só. O código fica em `engine/preset/`, uma classe por família.

```json
{
  "id": "rajada_do_anel",
  "nome": "Rajada do Anel",
  "classe": "disparo",
  "preset": "projetil",
  "numeros": { "dano": 8.5, "custo": 3, "recarga": 1, "alcance": 48 },
  "ajustes": { "velocidade": 1.8, "perseguicao": 0.3, "elemento": "energia" },
  "funcoes": [
    { "preset": "leque", "quando": { "aperto": "segurar" }, "ajustes": { "quantidade": 5 } }
  ]
}
```

- `numeros` (etapa 15 do catálogo): `dano`, `custo`, `recarga` (segundos), `alcance`, `raio`, `duracao` (segundos). Um número 0 usa o padrão do preset.
- `ajustes`: o que cada preset lê, com o padrão entre parênteses. Ajuste desconhecido é ignorado.
- `elemento` (qualquer preset): `energia` (na cor do personagem), `raio`, `fogo`, `gelo`, `agua`, `sombra`, `magia`, `luz`, `fumaca`, `terra`, `gas`, `som`. Dá a partícula e o efeito em quem é atingido (fogo queima, gelo deixa lento, gás envenena, sombra cega, som e raio atordoam).
- Se a habilidade não puder sair (nada para agarrar, sem munição, mira no vazio), a energia volta e o jogador lê o motivo.
- Nenhum preset fere jogadores, suportes de armadura ou animais domados.

## Lote 1 (2.1.0): Golpe, Disparo e Explosão

| Preset | Números | Ajustes |
|---|---|---|
| `soco_forte` | dano, alcance (4) | `empurrao` (1.5), `altura` (0.35), `tremor` (0.35) |
| `investida` | dano, alcance = distância (8) | `velocidade` em blocos por tick (1.2), `empurrao` (1.0) |
| `golpe_de_arma` | dano, alcance (3.5) | `arma`: espada, machado, tridente, maca; `item` (id de item mostrado); `angulo` do arco inteiro (120) |
| `pisao` | dano, raio (4) | `altura` (0.9). No ar, mergulha e pisa ao pousar |
| `agarrar_e_arremessar` | dano (no pouso), alcance (5) | `peso_maximo` em vida (60), `distancia` (12), `blocos` (true) |
| `sequencia` | dano, alcance (3.5), recarga (só depois do último golpe) | `golpes` (3), `intervalo` em segundos (1.0), `danos` (lista; o último vale 1,5 x dano se faltar) |
| `projetil` | dano, alcance (32) | `velocidade` (1.6), `perseguicao` de 0 a 1 (0) |
| `feixe_continuo` | dano por segundo, alcance (24), duracao (2) | `cor` (`"#RRGGBB"`) |
| `leque` | dano de cada tiro, alcance (24) | `quantidade` (5), `abertura` em graus (40), `velocidade` (1.6) |
| `arremesso_que_volta` | dano, alcance (16) | `quicadas` (0), `velocidade` (1.2), `item` |
| `balistico` | dano, alcance (64), raio da ponta explosiva (2.5) | `ponta`: normal, explosiva, rede, gelo, fogo, eletrica; `velocidade` (2.5), `gravidade` (0.05), `municao` (id de item; grátis no criativo) |
| `raio_do_ceu` | dano, alcance (32), raio (2.5) | `atraso` em segundos (0.6), `visual`: relampago ou feixe |
| `onda_em_volta` | dano, raio (5) | `empurrao` (1.4) |
| `explosao_no_ponto` | dano (metade na borda), alcance (32), raio (3) | `quebra_blocos` (false), `fogo` (false) |
| `cone` | dano total, alcance (6), duracao (1) | `angulo` da abertura inteira (50) |
| `pulso` | dano, raio (6), duracao do atordoamento (3) | elemento padrão `raio` |
| `chuva` | dano de cada golpe, alcance (24), raio da área (4), duracao (3) | `golpes_por_segundo` (6) |

Os testes ficam em `src/gametest/.../heroes/PresetTests.java` e os prints no roteiro `presets`.

## Lote 2 (2.2.0): Movimento, Defesa e Evasão

Os presets que duram (marcados com *) desligam ao apertar a tecla de novo ou ao tirar o traje, e a recarga só começa quando terminam.

| Preset | Números | Ajustes |
|---|---|---|
| `voo` | | `velocidade_maxima` em blocos por tick (o stat `voo`, ou 2.5), `velocidade_inicial` (0.7), `aceleracao` em segundos até a barreira do som (5), `gasto_por_segundo` (2), `impulso` da decolagem (0.9), `pouso` (força do pouso de herói, 1). Um personagem com essa habilidade voa como os heróis (dois toques no pulo); a tecla decola ou pousa |
| `supervelocidade`* | duracao (10) | `velocidade` extra (1.5 = +150%), `rastro` (elemento), `gasto_por_segundo` (0), `agua` (true), `paredes` (true) |
| `planar`* | duracao (30) | `queda_por_segundo` em blocos (2), `controle` = velocidade para a frente (0.6). Termina ao pousar |
| `salto_gigante` | dano no pouso, raio (3) | `altura` (8), `distancia` (10) |
| `gancho` | alcance (24), dano (opcional, em quem estiver no caminho) | `velocidade` (1.2) |
| `teleporte` | alcance (16) | `tempo` entre sumir e aparecer em segundos (0.25), `visual`: energia, tubo, sombra, magia |
| `nado_rapido` | duracao (20) | `velocidade` (1), `folego` (true) |
| `atravessar` | duracao da travessia (0.3) | `espessura` máxima (3), `visual`: vibrar, intangivel |
| `bolha`* | raio (1.6), duracao (20) | `reducao` (0.6), `gasto_por_segundo` (2), `devolve` (false) |
| `bloqueio` | duracao (2) | `reducao` (0.8), `angulo` da abertura inteira (120), `devolve` (false) |
| `pele_dura` | duracao (8) | `reducao` (0.4) |
| `parede` | duracao (8) | `material`: energia (vidro na cor do personagem), gelo, pedra, terra, areia, madeira ou id de bloco; `largura` (5), `altura` (3), `distancia` (2) |
| `refletir` | duracao (1.5) | `chance` (1) |
| `esquiva` | alcance = distância (4) | `janela` intocável em segundos (0.5), `lado`: auto, esquerda, direita, tras |
| `invisivel`* | duracao (15) | `quebra_ao_atacar` (true) |
| `fumaca` | raio (4), duracao (6) | |
| `encolher`* | duracao (30) | `tamanho` (0.25) |
| `fuga_de_forma` | duracao (5) | `forma`: sombra, animal, poca; `velocidade` extra (0.6). Atacar termina |

Os testes ficam em `src/gametest/.../heroes/PresetLot2Tests.java` e os prints no roteiro `presets_lote2`.

## Lote 3 (2.3.0): Controle, Invocação, Fortalecimento e Suprema

As criaturas chamadas ou dominadas lutam pelo dono: vão atrás dos monstros perto dele e de quem o ataca, nunca de jogadores nem umas das outras, e somem quando o tempo acaba (uma que sobrar depois de reiniciar o servidor não volta). Os presets nunca ferem as criaturas chamadas. Os de fortalecimento marcados com * desligam ao apertar a tecla de novo ou ao tirar o traje.

| Preset | Números | Ajustes |
|---|---|---|
| `puxar` | alcance (12), dano (opcional) | `forca` (1.2), `laco` (elemento da linha) |
| `prender` | alcance (12), duracao (4), dano (opcional) | `material`: energia, gelo, vinha, areia; `vida` da prisão (10): o dano que ela aguenta antes de quebrar |
| `lentidao` | alcance (16) ou raio (em volta), duracao (5) | `forca` máxima da lentidão (3); com o elemento gelo também congela |
| `medo_e_confusao` | raio (8), duracao (5) | `tipo`: medo (fogem) ou confusao (atacam uns aos outros). Nesse tempo deixam os jogadores em paz |
| `dominar` | alcance (12), duracao (15) | `vida_maxima` de quem pode ser dominado (40) |
| `levitar` | alcance (16), duracao (4) | `altura` (3), `peso` = vida máxima da criatura (40). Também levanta o bloco mirado |
| `ajudante` | duracao (30), dano (o ataque deles; 0 mantém) | `criatura` (minecraft:wolf), `quantidade` (1), `vida` (0 mantém) |
| `construto` | alcance (10), duracao (8), dano por golpe | `forma`: martelo, serra, broca; `intervalo` entre golpes (1.5) |
| `veiculo` | duracao (60), dano dos tiros | `entidade` (minecraft:horse; camelo, ghast feliz...), `velocidade` extra (0.5), `vida`, `armas` (false). Termina ao descer |
| `enxame` | alcance (12), duracao (6), dano por mordida | `criatura` (minecraft:bat), `quantidade` (8) |
| `copia` | duracao (8), dano | `quantidade` (2), `vida` (6). Os monstros vão atrás das cópias |
| `torre` | alcance (16), duracao (15), dano por tiro | `cadencia` em segundos (1), `velocidade` do tiro (1.4) |
| `aura`* | duracao (10) | `atributo`: dano, velocidade, defesa ou todos; `quanto` (0.5 = +50%, ou metade do dano a menos); `cor` |
| `cura` | | `quanto` (8 de vida), `tempo` em segundos (0 = de uma vez). Com a vida cheia não sai |
| `crescer`* | duracao (20) | `tamanho` (1.8), `dano_extra` (4), `vida_extra` (10), `firmeza` contra empurrões (0.6). Precisa de espaço em cima |
| `furia`* | duracao (8) | `troca` (0.5 = +50% de dano dado e recebido) |
| `sentidos`* | alcance (24, até 32), duracao (10) | `mostra`: mobs, minerios ou tudo. Visão no escuro; as criaturas brilham e os 32 minérios mais perto aparecem através das paredes |
| `suprema_de_combate` | dano (20), raio (8) | `carga` em segundos (1), `visual`: energia, solar (queima) ou raio |
| `grande_obra` | | `estrutura` (id de uma estrutura do jogo ou de um pacote de dados; minecraft:igloo/top), `tempo` de montagem (4), `distancia` (3). Só ocupa ar, água e plantas; apertar de novo desmonta |

Os testes ficam em `src/gametest/.../heroes/PresetLot3Tests.java` e os prints no roteiro `presets_lote3`.

## Lote 4 (2.4.0): Usos no mundo

Na 2.4.0 os usos no mundo iam no campo `"uso_no_mundo"` e saíam agachado; desde a 2.6.0 são presets como os outros, de uma habilidade de mundo ou de uma função dela (ver o Lote 6). Só mexem em blocos que o jogador pode mudar ali, nunca em blocos que guardam coisas (baús, fornalhas...), e um uso que não muda nada devolve a energia. Os números vêm da mesma habilidade.

| Preset | Números | Ajustes |
|---|---|---|
| `minerar_area` | alcance (6) | `tamanho` do quadrado (3), `profundidade` (1), `ferramenta` que decide o que sai e o que dá para quebrar (minecraft:iron_pickaxe), `dureza` máxima (5; pedra é 1.5, obsidiana 50) |
| `construir` | duracao (30, só energia) | `forma`: ponte (no chão à frente), escada (subindo) ou parede; `comprimento` (8), `largura` (1; a parede 5), `altura` (3); `material`: energia (vidro na cor do personagem), inventario (gasta os blocos do inventário) ou id de bloco; `fica` (a energia some, os outros ficam) |
| `plantar_e_colher` | raio (5) | `colhe` (true: colhe o que está maduro e replanta), `cresce` (true: como farinha de osso, também mudas), `vezes` (2) |
| `gelo_e_calor` | raio (5) | `efeito`: gelo (congela água, lava vira obsidiana, apaga fogo) ou calor (derrete gelo e neve, acende fogueiras e velas) |
| `luz` | raio (8), duracao (60) | `de_vez` (false): luzes invisíveis a cada poucos blocos |
| `mover_blocos` | alcance (8), duracao (30) | `peso` = dureza máxima (3). Pega o bloco mirado e o carrega flutuando; a tecla de novo o põe onde o jogador mira. No fim do tempo ele volta ao lugar |
| `abrir_caminho` | duracao (20, só água) | `remove`: terreno (plantas, folhas, neve, terra, areia e cascalho da altura dos pés para cima), arvores (as árvores que o caminho toca, troncos e folhas) ou agua (a água se abre e volta depois); `comprimento` (10), `largura` (3), `altura` (3) |
| `procurar` | raio (16, até 32), duracao (10) | `mostra`: baus (tudo que guarda itens), minerios ou mobs. Os blocos aparecem através das paredes (os 32 mais perto) |

Os testes ficam em `src/gametest/.../heroes/PresetLot4Tests.java` e os prints no roteiro `presets_lote4`.

## Lote 5 (2.5.0): Visuais, sons e as 15 transformações

### Ajustes de visual e som de qualquer habilidade

Valem para todos os presets, de luta e de uso no mundo, no `"ajustes"` da habilidade:

| Ajuste | O que faz |
|---|---|
| `cor` | Cor das partículas e do feixe (`"#FF3020"`); sem ela, a cor principal do personagem |
| `particula_tamanho` (1) e `particula_quantidade` (1) | Tamanho das partículas de pó e quantas saem (0 desliga) |
| `elemento` | A família de partículas: energia, raio, fogo, gelo, agua, sombra, magia, luz, fumaca, terra, gas, som, plantas ou vento. Cada uma tem uma partícula principal e uma de detalhe |
| `som` (true), `som_altura` (1), `som_volume` (0.7), `som_timbre` | O som da classe da habilidade (10 conjuntos com 3 variações). `som_timbre`: padrao, grave, agudo, metal, magia, energia ou natural |
| `zoom`, `zoom_duracao` (0.8 s) | A câmera fecha (0.6) ou abre (1.3) por um instante |
| `camera_lenta` (segundos), `camera_lenta_velocidade` (0.4) | Câmera lenta curta: sozinho num mundo de um jogador o jogo fica mais lento; com outros jogadores, só as criaturas perto ficam lentas |
| `icone`, `icone_cor` | O desenho e a cor do ícone na roda e no HUD (sem eles, o desenho do preset na cor da habilidade) |

`tremor` continua nos presets que já o tinham.

### Feixes e construtos

| Preset | Ajustes novos |
|---|---|
| `feixe_continuo` | `forma`: reto, espiral ou raio; `espessura` (1) |
| `construto` | `visual`: solido (objeto de energia translúcido, o padrão) ou particulas; `escala` (1) |

### Rastro e aura do personagem

No arquivo do personagem, em `"visual"`:

```json
"visual": {
  "transformacao": {"cor": "#FFD700", "duracao": 2, "tipo": "morcegos"},
  "rastro": {"cor": "#40FF60", "largura": 0.3, "brilho": 0.5},
  "aura": {"cor": "#40FF60", "largura": 0.6, "brilho": 0.5, "quando": "sempre"}
}
```

O rastro aparece quando o personagem voa, corre ou cai rápido. A aura `quando`: sempre, parado ou voando.

### As 15 transformações

Tocam ao vestir o traje (tecla G). O campo `"transformacao"` do personagem escolhe uma; `padrao` não toca nenhuma. Ajustes em `"visual": {"transformacao": {...}}`: `cor` e `cor2` (as cores do personagem), `som` (id de um som, ou `nenhum`), `duracao` (2 segundos), `tipo` e `elemento`.

| Id | Transformação | O que acontece | `tipo` |
|---|---|---|---|
| `raio` | Raio Mágico | Faíscas se juntam no céu, o raio cai e vem o clarão | |
| `anel` | Luz do Anel | A cor sobe pelo corpo e o juramento aparece na tela | |
| `sol` | Sol de Krypton | Feixe de sol, o símbolo brilha no peito e a capa abre no vento | |
| `velocidade` | Vulto Veloz | Um vulto girando com raios da cor do personagem | |
| `sombras` | Das Sombras | Fumaça e morcegos cobrem o jogador | morcegos, gatos, passaros |
| `armadura` | Armadura em Montagem | As peças do traje voam e encaixam uma a uma; o visor acende por último | |
| `mistico` | Círculo Místico | Runas girando no chão e fumaça subindo | |
| `divino` | Bênção Divina | Luz dourada e folhas de oliveira caindo, brilho da armadura | |
| `mare` | Maré | Uma coluna de água sobe e cai em espuma | |
| `elemento` | Elemento | O elemento gira em volta e assenta no corpo | (`elemento`: fogo, gelo, plantas, vento, raio, luz...) |
| `fera` | Fera | O corpo treme, cresce por um instante e ruge | pelos, escamas, barro |
| `tubo` | Tubo de Explosão | Um portal abre atrás, o jogador some nele e volta | |
| `arsenal` | Arsenal | As armas giram em volta e caem no lugar; a máscara por último | |
| `espetaculo` | Espetáculo | Confete e gargalhada | confete, cartas, fumaca, gas |
| `asas` | Asas de Metal Nth | As asas abrem nas costas e o jogador sobe um pouco | |

Os sons (10 classes e 15 transformações) são originais, gerados por `tools/generate_engine_audio.py`. Os ícones da roda são desenhados no jogo a partir do preset e da cor (`engine/AbilityIcons.java`). Os testes ficam em `src/gametest/.../heroes/PresetLot5Tests.java` e os prints no roteiro `presets_lote5`.

## Lote 6 (2.6.0): O novo modelo de habilidades

### A ficha

- `classe` de cada habilidade, uma das 15: de luta `golpe`, `disparo`, `explosao`, `controle`, `defesa`, `invocacao`, `evasao`, `fortalecimento`; de mundo `mobilidade`, `coleta_e_terreno`, `construcao`, `transmutacao`, `natureza`, `deteccao`, `logistica`.
- A roda tem de 5 a 8 habilidades, dentro do arsenal da classe do personagem (`arsenal` em `data/greenlantern/engine/classes.json`: `roda`, `luta` e `mundo`), com pelo menos 3 de luta e 2 de mundo. As de luta vêm primeiro, depois as de mundo, e por último a única com `"suprema": true` (borda dourada, recarga longa e um som quando fica pronta). Não há mais barra nem tecla da Suprema.
- `funcoes`: até 2 funções além do preset da habilidade. Cada uma tem `preset`, `quando` e, se quiser, `numeros` e `ajustes` próprios (o que não diz vem da habilidade; custo e recarga são sempre os da habilidade). V usa a primeira função cujo `quando` vale agora; se nenhuma vale, o preset da habilidade.
  - `aperto`: `toque`, `segurar` (V seguro por quase meio segundo) ou `soltar` (ao soltar V depois de segurar).
  - `mira` (o que está a até 8 blocos, ou no `alcance` da função): `mob`, `bloco`, `vazio`, `minerio`, `madeira`, `folhas`, `planta`, `terra`, `pedra`, `agua`, `lava`, `fogo`, `gelo`, `fornalha`, `bau`, `maquina`, um id de bloco ou uma `#tag`.
  - `lugar`: `chao`, `ar`, `agua` ou `correndo`.
- `passivas`: até 4, cada uma `{"tipo": ..., "ajustes": {...}}`, sempre ligadas com o traje. O voo do personagem vem da passiva `voo` (ou `levitar`).
- A tela mostra, ao lado do nome, o ícone da função que vai sair; quando é de mundo, o bloco mirado ganha contorno (do tamanho do `tamanho`, se houver). As passivas aparecem como ícones, acesos quando estão agindo. Teclas diretas de 1 a 8.

| Passiva | Ajustes |
|---|---|
| `voo` | os do voo: `velocidade_maxima`, `velocidade_inicial`, `aceleracao`, `gasto_por_segundo`, `pouso` |
| `levitar` | os mesmos, devagar (`velocidade_maxima` 0.6) |
| `supervelocidade` | correndo acelera sozinho até `velocidade` (1.5 = +150%) em `aceleracao` segundos (2); sobe blocos, corre na água e nas paredes (`agua`, `paredes`) |
| `aquatico` | na água nada rápido (`velocidade`, 0 a 2), respira e enxerga |
| `planar` | pulando enquanto cai, plana (`queda_por_segundo` 2, `controle` 0.6), sem dano de queda |
| `superforca` | quebra blocos mais rápido (`forca`, +100%) |
| `invulneravel` | sem dano de `danos` (queda, fogo, frio, afogamento; todos por padrão) |
| `visao_no_escuro` | enxerga no escuro |
| `sentidos_agucados` | agachado e parado, as criaturas a até `raio` (16) brilham através das paredes |
| `pulo_alto` | pula mais alto (`altura`, +0.3), sem dano de queda |
| `escalar` | andando contra a parede, sobe (`velocidade` 0.2); agachado, se segura |
| `regeneracao` | depois de `espera` segundos (5) sem dano, a vida volta (`cura`, 1 por segundo) |
| `elastico` | alcança blocos e criaturas `alcance` (3) blocos mais longe, sem dano de queda |
| `imune_a_veneno` | veneno, comida estragada e gases não fazem efeito |

### Presets de mundo novos

Como os do Lote 4, só mexem no que o jogador pode mudar ali, e um uso que não muda nada devolve a energia. `gancho`, `teleporte` e `grande_obra` agora contam como presets de mundo; a montaria é o preset `veiculo`.

| Preset | Classe | Números | Ajustes |
|---|---|---|---|
| `cortar_arvore` | Coleta e terreno | alcance (6) | derruba a árvore inteira do tronco mirado; `altura_maxima` (32), `replanta` (true) |
| `cavar_solto` | Coleta e terreno | alcance (6) | cava terra, areia, cascalho, neve, argila, folhas e plantas, nunca pedra; `tamanho` (3), `profundidade` (3) |
| `aplanar` | Coleta e terreno | raio (4) | tira o que está acima dos pés (`altura` 4, `dureza` 5) e enche os buracos (`fundo` 3) com `material` (minecraft:dirt, ou inventario) |
| `pescar` | Coleta e terreno | alcance (16) | mirando a água, tira `quantidade` (3) pescas de uma vez, como a vara; `tesouro` (0.1) |
| `projeto_em_fantasma` | Construção | duracao (60) | mostra a `estrutura` (minecraft:igloo/top) em vidro à `distancia` (3); a tecla de novo constrói com os blocos do inventário |
| `trocar_bloco` | Transmutação | alcance (8), raio (0) | `modo`: fornalha (areia vira vidro...), tabela (`trocas`: {"minecraft:stone": "minecraft:iron_block"}) ou cor (`cor_nova`, red) |
| `ligar_maquina` | Transmutação | alcance (16), duracao (2) | vira a alavanca, aperta o botão ou dá sinal de redstone ao que estiver mirado |
| `domar_e_guiar` | Natureza | alcance (8), raio (6), duracao (30) | doma lobos, gatos, papagaios e cavalos; os outros animais seguem na corda; `quantos` (4) |
| `clima` | Natureza | duracao (120) | `tipo`: chuva, sol ou tempestade |
| `rastro` | Detecção | raio (48), duracao (10) | trilha de partículas até o `alvo`: mob, estrutura (`estrutura`, uma tag, minecraft:village) ou casa |
| `ima_de_itens` | Logística | alcance (6), raio (8), duracao (5) | mirando um baú, traz o que cabe para o inventário; senão, puxa itens e experiência em volta |
| `ajudante_de_trabalho` | Logística | raio (6), duracao (30) | um ajudante voa ao lado e a cada segundo faz a `tarefa`: colher, guardar ou minerar |
| `organizar` | Logística | alcance (6) | junta e ordena o baú mirado pela `regra`: nome, quantidade ou tipo |
| `levar_junto` | Mobilidade | raio (4), duracao (30) | os `quantos` (3) animais, aldeões ou criaturas do personagem mais perto vão junto, flutuando atrás dele |

Os sons das 6 classes de mundo novas e o da suprema são originais, gerados por `tools/generate_engine_audio.py`. Os testes ficam em `src/gametest/.../heroes/PresetLot6Tests.java` e os prints no roteiro `presets_lote6`.
