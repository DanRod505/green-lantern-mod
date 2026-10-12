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
