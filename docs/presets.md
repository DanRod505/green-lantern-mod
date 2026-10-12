# Presets de habilidade

Uma habilidade da ficha de um personagem (`data/greenlantern/characters/<id>.json`) escolhe um preset e preenche os números; ninguém escreve código para um personagem só. O código fica em `engine/preset/`, uma classe por família.

```json
{
  "id": "rajada_do_anel",
  "nome": "Rajada do Anel",
  "classe": "disparo",
  "tipo": "principal",
  "preset": "projetil",
  "numeros": { "dano": 8.5, "custo": 3, "recarga": 1, "alcance": 48 },
  "ajustes": { "velocidade": 1.8, "perseguicao": 0.3, "elemento": "energia" }
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
