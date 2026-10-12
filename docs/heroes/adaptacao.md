# Adaptação dos 9 heróis

## Lote 1: as habilidades do catálogo

Etapa 3 do guia mestre, lote 1 (versão 2.8.0). A roda de cada um dos 9 heróis tem agora as habilidades do catálogo no modelo novo: 5 a 8 habilidades, as de luta primeiro, as de mundo depois e a suprema por último (borda dourada), cada uma com até 3 funções escolhidas pelo jeito de apertar V, pela mira e pelo lugar. Os poderes de hoje continuam iguais e entram como funções (presets `poder_de_hoje` e `construto_de_hoje`, ver [presets.md](../presets.md)). O traje e o visual mudam no lote 2 (abaixo); os stats, a energia e a receita, no lote 3.

As passivas das fichas que o código de cada herói já fazia (voo, planar, aquático, supervelocidade) continuam no código dele; as outras (superforça, invulnerável, visão no escuro) agora valem para eles também.

### Lanterna Verde

Passivas: voo.

| Habilidade | Classe | Toque | Outras funções |
|---|---|---|---|
| Construtos da Vontade | invocacao | construto de hoje `hammer` | segurar: construto de hoje `minigun` |
| Rajada do Anel | disparo | construto de hoje `energy_blast` | mirando fornalha: `gelo_e_calor` |
| Bolha Verde | defesa | construto de hoje `bubble` | na água: `bolha` |
| Mão Verde | controle | `prender` | mirando bau: `mover_blocos` |
| Broca Esmeralda | coleta_e_terreno | construto de hoje `drill` | mirando madeira: construto de hoje `saw` |
| Ponte de Luz | construcao | `luz` | segurar: `construir` |
| Portal para Oa | mobilidade | `levar_junto` | segurar: construto de hoje `portal` |
| Mecha Gigante (suprema) | invocacao | construto de hoje `mecha` | segurar, mirando bloco: `ima_de_itens` |

### Flash

Passivas: supervelocidade.

| Habilidade | Classe | Toque | Outras funções |
|---|---|---|---|
| Rajada de Socos | golpe | `soco_forte` | mirando pedra: `minerar_area` |
| Vibrar | evasao | poder de hoje `phase` | mirando bloco: `atravessar` |
| Tornado de Braços | controle | poder de hoje `tornado` | segurar: `ima_de_itens` |
| Força de Aceleração | fortalecimento | `cura` | segurar: `plantar_e_colher` |
| Mapa Relâmpago | mobilidade | `teleporte` | segurar: `supervelocidade` |
| Corrida no Tempo (suprema) | natureza | poder de hoje `lightning` | segurar: `plantar_e_colher` |

### Aquaman

Passivas: aquatico, superforca.

| Habilidade | Classe | Toque | Outras funções |
|---|---|---|---|
| Tridente | golpe | poder de hoje `trident` | mirando agua: `pescar` |
| Pele Atlante | defesa | `pele_dura` | — |
| Chamado do Mar | controle | poder de hoje `sea_call` | segurar: poder de hoje `shark` |
| Fúria do Oceano | fortalecimento | `aura` | segurar: `plantar_e_colher` |
| Corrente Marinha | mobilidade | `nado_rapido` | — |
| Tesouros do Mar | deteccao | `procurar` | — |
| Kraken (suprema) | invocacao | poder de hoje `kraken` | segurar: poder de hoje `atlantis_portal` |

### Batman

Passivas: visao_no_escuro, planar.

| Habilidade | Classe | Toque | Outras funções |
|---|---|---|---|
| Combate de Gotham | golpe | `sequencia` | mirando maquina: `golpe_de_arma` |
| Batarangue | disparo | poder de hoje `batarang` | mirando folhas: `trocar_bloco` |
| Enxame de Morcegos | controle | poder de hoje `bat_swarm` | segurar: `procurar` |
| Bomba de Fumaça | evasao | `fumaca` | — |
| Gancho | mobilidade | poder de hoje `grapple` | — |
| Modo Detetive | deteccao | `procurar` | — |
| Batcaverna (suprema) | construcao | poder de hoje `batmobile` | segurar: `grande_obra` |

### Superman

Passivas: voo, superforca, invulneravel.

| Habilidade | Classe | Toque | Outras funções |
|---|---|---|---|
| Supersoco | golpe | poder de hoje `super_punch` | mirando bloco: `minerar_area` |
| Visão de Calor | disparo | poder de hoje `heat_vision` | mirando bloco: `gelo_e_calor` |
| Sopro Congelante | explosao | poder de hoje `super_breath` | mirando agua: `gelo_e_calor` |
| Pele de Aço | defesa | `pele_dura` | — |
| Visão de Raio X | deteccao | poder de hoje `xray_vision` | — |
| Fortaleza da Solidão (suprema) | construcao | `grande_obra` | no ar: `pulso` |

### Mulher-Maravilha

Passivas: voo, superforca.

| Habilidade | Classe | Toque | Outras funções |
|---|---|---|---|
| Espada e Escudo | golpe | poder de hoje `sword_and_shield` | segurar: poder de hoje `shield_throw`; mirando madeira: `cortar_arvore` |
| Braceletes da Vitória | defesa | poder de hoje `bracelet_guard` | mirando #minecraft:campfires: `gelo_e_calor` |
| Laço da Verdade | controle | poder de hoje `lasso_pull` | segurar: poder de hoje `lasso_capture`; soltar depois de segurar: poder de hoje `lasso_spin` |
| Bênção dos Deuses | natureza | `aura` | segurar: `plantar_e_colher` |
| Jato Invisível | mobilidade | poder de hoje `invisible_jet` | — |
| Choque dos Braceletes (suprema) | explosao | poder de hoje `bracelet_shockwave` | segurar: `construir` |

### Ciborgue

Passivas: voo, invulneravel.

| Habilidade | Classe | Toque | Outras funções |
|---|---|---|---|
| Canhão Sônico | disparo | poder de hoje `sonic_cannon` | mirando pedra: `minerar_area` |
| Invasão de Sistema | controle | poder de hoje `machine_hack` | segurar: poder de hoje `emp_burst`; mirando maquina: `ligar_maquina` |
| Blindagem | defesa | `pele_dura` | segurar: poder de hoje `self_repair` |
| Caixa Materna | invocacao | `ajudante` | mirando mob: poder de hoje `shoulder_missiles`; segurar: `minerar_area` |
| Scanner | deteccao | poder de hoje `tech_scan` | — |
| Braço Guindaste | logistica | `ima_de_itens` | — |
| Tubo de Explosão (suprema) | mobilidade | poder de hoje `boom_tube` | segurar: `levar_junto` |

### Supergirl

Passivas: voo, superforca, invulneravel.

| Habilidade | Classe | Toque | Outras funções |
|---|---|---|---|
| Soco em Velocidade | golpe | poder de hoje `meteor_dash` | mirando mob: poder de hoje `kryptonian_throw`; mirando pedra: `minerar_area` |
| Visão de Calor | disparo | poder de hoje `heat_bolts` | mirando minerio: `trocar_bloco` |
| Palmas Trovejantes | explosao | poder de hoje `thunder_clap` | mirando folhas: `minerar_area`; segurar: poder de hoje `frost_wall` |
| Visão de Raio X | deteccao | `procurar` | — |
| Entrega Kryptoniana | logistica | `mover_blocos` | — |
| Krypto ao Ataque (suprema) | invocacao | poder de hoje `solar_flare` | segurar: `procurar` |

### Shazam

Passivas: voo, superforca.

| Habilidade | Classe | Toque | Outras funções |
|---|---|---|---|
| Força de Hércules | golpe | poder de hoje `hercules_slam` | mirando pedra: `minerar_area` |
| Raio de Zeus | disparo | poder de hoje `zeus_lightning` | mirando maquina: `ligar_maquina` |
| Vigor de Atlas | defesa | poder de hoje `atlas_stamina` | segurar: poder de hoje `achilles_roar` |
| Sabedoria de Salomão | deteccao | poder de hoje `solomon_wisdom` | mirando mob: `procurar`; segurar: `onda_em_volta` |
| Velocidade de Mercúrio | mobilidade | poder de hoje `mercury_dash` | segurar: `levar_junto` |
| SHAZAM! (suprema) | explosao | poder de hoje `thunder_call` | segurar: `clima` |

### O que ficou de fora da roda

- Aquaman: o Portal para Atlântida virou a função "segurar" do Kraken e continua no mundo no Portão de Atlântida.
- Supergirl: a Superaudição sai da roda (no plano, vira o aviso de perigo da tela); a Muralha de Gelo ficou como a função "segurar" das Palmas Trovejantes.
- Mulher-Maravilha: "mirando aldeão mostra a melhor troca" não coube no limite de 3 funções do Laço (puxar, prender e girar).

## Lote 2: skins, transformações e skins alternativas (versão 2.9.0)

Com o traje completo, o traje deixa de ser desenhado por cima da skin do jogador e vira a skin do herói, como nos personagens da engine: o molde do esqueleto (Masculino; Feminino, de braço fino, para a Mulher-Maravilha e a Supergirl), a camada de cima com o volume do traje (cabelo, cinto, ombreiras, botas) e a camada que brilha no escuro (`_brilho`: olhos, anel, símbolos, luzes). A capa, as orelhas do Batman, o cabelo da Supergirl e as outras peças animadas continuam desenhadas por cima, como antes. Com o traje incompleto, as peças aparecem como hoje.

As skins saem de `python3 tools/generate_hero_skins.py` (com `--preview folha.png`, uma folha com a frente e as costas de todas), a partir das cores e do desenho dos trajes de hoje. Ficam em `assets/greenlantern/textures/entity/skin/<id>.png`.

| Herói | Transformação ao vestir | Skins alternativas (como conseguir) |
|---|---|---|
| Lanterna Verde | Luz do Anel (`anel`) | Lanterna Branco (arco), Parallax (chefe) |
| Flash | Vulto Veloz (`velocidade`) | Clássico (receita), Flash Negro (chefe), Ponto de Ignição (multiverso) |
| Aquaman | Maré (`mare`) | Clássico (receita), Mão de Arpão (chefe) |
| Batman | Das Sombras (`sombras`) | Clássico (receita), Armadura Anti-Superman (missão), Batman do Futuro (multiverso), Gotham à Luz de Lampião (multiverso) |
| Superman | Sol de Krypton (`sol`) | Clássico (receita), Traje Negro (arco), Reino do Amanhã (multiverso), Foice e Martelo (multiverso) |
| Mulher-Maravilha | Bênção Divina (`divino`) | Clássica (receita), Armadura Dourada (chefe), Reino do Amanhã (multiverso) |
| Ciborgue | Armadura em Montagem (`armadura`) | Titãs Clássico (receita) |
| Supergirl | Sol de Krypton (`sol`) | Anos 80 (receita), Lanterna Vermelha (multiverso) |
| Shazam | O raio de hoje, que já era o Raio Mágico do catálogo | Capitão Marvel Clássico (receita), Shazam Negro (chefe) |

A transformação toca no lugar das partículas de vestir de hoje; a do Shazam continua a dele (o raio que dá dano em volta e recarrega a energia). As skins alternativas trocam no Guarda-Roupa dos Heróis ou com agachar + K, como nos personagens da engine; por enquanto só vêm liberadas no criativo (os Moldes de Traje são de outra etapa). A trilha de cada um continua a mesma.

O que fica para o lote 3: a capa e as peças animadas ainda usam a cor do traje de hoje em todas as skins, os stats, a energia, as receitas e os prints novos de referência.
