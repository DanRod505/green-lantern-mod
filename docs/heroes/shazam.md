# Ficha do herói: Shazam

> **Status:** aprovada pelo Dalton em 2026-10-11.
>
> As duas tabelas (Identidade e Poderes) são lidas pelo gerador (`python3 tools/new_hero.py docs/heroes/shazam.md`). O resto da ficha é para pessoas.

Shazam é o herói para testar a biblioteca de poderes e arte (PR #19): quase todos os poderes se montam com os blocos de `hero/power/` (`Bolts`, `Dash`, `Hits`, `Area`, `BlockEffects`, `Targets`, `Fx`) e o voo reaproveita o `FlightProfile`. O que é novo de verdade é a transformação pela palavra mágica. Cada poder vem de um dos seis anciãos do nome: **S**alomão, **H**ércules, **A**tlas, **Z**eus, **A**quiles e **M**ercúrio.

## Identidade

| Campo | Valor |
|---|---|
| id | `shazam` |
| Classe | `Shazam` |
| Nome (pt) | Shazam |
| Nome (en) | Shazam |
| Artigo (pt) | o |
| Item | `wizard_bolt` |
| Item (pt) | Raio do Mago |
| Item (en) | Wizard's Bolt |
| Artigo do item (pt) | o |
| Recurso | `shazam_magic` |
| Recurso (pt) | Magia de Shazam |
| Recurso (en) | Shazam's Magic |
| Capacidade | 1000 |
| Recarga por segundo | 4 |
| Cor principal | #C81E1E |
| Cor de destaque | #F2B705 |
| Cor do brilho | #FFF6B0 |

## Poderes

Sete poderes, na ordem da roda. O voo não entra na roda: usa as teclas de voo, como o Superman e a Supergirl.

| id | Nome (pt) | Nome (en) | Custo | O que faz (pt) | What it does (en) |
|---|---|---|---|---|---|
| `zeus_lightning` | Raio de Zeus | Zeus's Lightning | 35 | Dispara da mão um raio dourado que voa até o primeiro inimigo, persegue o alvo se ele desviar e salta para mais dois inimigos próximos. | Fires a golden bolt from his hand that flies to the first enemy, chases it if it dodges and jumps to two more enemies nearby. |
| `thunder_call` | Chamado do Trovão | Thunder Call | 90 | Ergue o braço e um relâmpago cai do céu no ponto da mira, ferindo e arremessando os inimigos em volta e estilhaçando o vidro. Funciona até debaixo da terra. | He raises his arm and lightning strikes from the sky where he aims, hurting and flinging the enemies around and shattering glass. Works even underground. |
| `mercury_dash` | Velocidade de Mercúrio | Speed of Mercury | 60 | Avança 14 blocos em linha reta num borrão vermelho e dourado, no chão ou no ar, atropelando cada inimigo uma vez e parando na parede. | Dashes 14 blocks in a straight line in a red and gold blur, on the ground or in the air, ramming each enemy once and stopping at walls. |
| `hercules_slam` | Força de Hércules | Strength of Hercules | 70 | Soca o chão e solta uma onda de choque em volta que joga os inimigos para longe e levanta poeira. | He punches the ground and sends out a shockwave around him that throws enemies away and raises dust. |
| `achilles_roar` | Coragem de Aquiles | Courage of Achilles | 80 | Um grito de guerra em cone que atordoa os inimigos à frente e apaga fogo e tochas. | A battle cry in a cone that stuns the enemies ahead and puts out fire and torches. |
| `atlas_stamina` | Vigor de Atlas | Stamina of Atlas | 8 | Ligado, ele fica pesado como uma montanha: não é empurrado, toma metade do dano e não cansa. Custa por segundo. | While on, he is as heavy as a mountain: he can't be knocked back, takes half damage and doesn't get hungry. Costs per second. |
| `solomon_wisdom` | Sabedoria de Salomão | Wisdom of Solomon | 6 | Ligada, faz brilhar cada criatura hostil a até 32 blocos, mesmo atrás de paredes, e mostra no HUD a vida de quem está na mira. Custa por segundo. | While on, makes every hostile creature within 32 blocks glow, even behind walls, and shows the health of whoever he aims at on the HUD. Costs per second. |

## Recurso e recarga

A Magia de Shazam vem do Mago e chega pelo raio. Ela enche devagar sozinha (4 por segundo), três vezes mais rápido durante tempestades (12 por segundo), e de uma vez quando ele chama o raio da transformação: cada "Shazam!" devolve 300 de magia. Ser atingido por um raio natural numa tempestade enche a magia toda, em vez de ferir. A magia não enche enquanto ele está na forma de Billy (sem o traje).

## Mobilidade

Voo pelo `FlightProfile` compartilhado, com o perfil próprio dele: mais rápido que o Lanterna, mais lento que a Supergirl e o Superman.

| | Mulher Maravilha | Ciborgue | Lanterna Verde | **Shazam** | Supergirl | Superman |
|---|---|---|---|---|---|---|
| Velocidade máxima | 2.3 | 2.5 | 4.0 | **4.8** | 5.5 | 7.5 |
| Barreira do som (2.6) | não | não | sim | **sim** | sim | sim |
| Pouso de herói | | | 1.0 | **1.4** | 1.2 | 1.8 |

- Rastro próprio: faíscas douradas e pequenos arcos de raio, diferente do rastro azul do Superman e do vermelho da Supergirl.
- O voo gasta 2 por segundo, e o dobro acima da barreira do som.

## Momento épico

**A palavra mágica.** É a parte nova, que a biblioteca não cobre.

- **Transformação:** usar o Raio do Mago faz ele gritar "Shazam!" e um relâmpago cair do céu nele. O raio forma o traje na hora, com um clarão dourado, um trovão que todos ouvem e um anel de faíscas no chão. O raio não fere o herói nem incendeia o chão em volta dele, mas fere e empurra os inimigos colados nele.
- **Volta a ser Billy:** usar o item de novo chama outro raio, que tira o traje e devolve a armadura de antes. Assim o traje se veste e se tira sempre com o raio, não em silêncio como nos outros heróis.
- **Recarga pelo raio:** cada transformação devolve 300 de magia, com uma espera de 10 segundos entre um "Shazam!" e outro para não virar recarga infinita.
- **Debaixo da terra:** sem céu acima, o raio atravessa a rocha (só o efeito, sem quebrar blocos), para funcionar em cavernas.

## Traje e paleta

Inspirado no visual clássico do personagem:

- Cabeça: cabelo preto curto, sem máscara.
- Peito: vermelho (#C81E1E) com o raio dourado grande no peito e o brilho claro (#FFF6B0) em volta do raio.
- Costas: capa branca curta com borda dourada, presa nos ombros por dois botões dourados.
- Pernas: calça vermelha, faixa dourada larga na cintura.
- Pés: botas douradas de cano alto.
- Lados: os punhos dourados das luvas e o fecho da capa.
- Cores: vermelho (#C81E1E) como principal, dourado (#F2B705) no raio, faixa, punhos e botas, e brilho claro (#FFF6B0) nos raios e nas faíscas.

## Música e sons

Tema original, aventureiro e um pouco brincalhão, para combinar com um herói que por dentro é um garoto: a base é um ostinato de cordas em pizzicato com caixa de marcha, e o pico entra com metais heroicos, sinos e um trovão marcando o tempo. Sons principais, todos originais: o grito "Shazam!" (sintetizado, sem voz gravada de ninguém) com o trovão da transformação, o estalo do Raio de Zeus, o relâmpago do Chamado do Trovão, o zumbido da Velocidade de Mercúrio, o impacto da Força de Hércules, o grito da Coragem de Aquiles, o ronco grave do Vigor de Atlas e o sino suave da Sabedoria de Salomão.

## Prints

| Print | O que mostra |
|---|---|
| `sz00_suit_front` | traje de frente, com o raio no peito |
| `sz01_suit_back` | traje de costas, com a capa branca |
| `sz02_suit_side` | traje de lado, com a faixa e as botas |
| `sz03_hud` | HUD com a Magia de Shazam |
| `sz04_wheel` | roda de poderes |
| `sz05_transform` | relâmpago caindo no herói na transformação |
| `sz05b_transform_back` | segundo raio tirando o traje |
| `sz06_zeus_lightning` | raio saltando entre três zumbis |
| `sz07_thunder_call` | relâmpago caindo na mira e arremessando inimigos |
| `sz08_mercury_dash` | borrão vermelho e dourado atravessando um grupo |
| `sz09_hercules_slam` | onda de choque e poeira em volta do soco no chão |
| `sz10_achilles_roar` | cone do grito atordoando inimigos e apagando tochas |
| `sz11_atlas_stamina` | herói parado sem ser empurrado por uma explosão |
| `sz12_solomon_wisdom` | criaturas brilhando atrás da parede e vida no HUD |
| `sz13_flight` | voando acima da barreira do som com o rastro dourado |
| `sz14_guide` | seção do Shazam no Guia dos Heróis |

## Definição de pronto

- [ ] Item de ativação com receita e conquista, traje com frente, costas e lado bem resolvidos
- [ ] Recurso próprio com identidade e regra de recarga
- [ ] De 5 a 8 poderes na roda, com ícone, nome, descrição e custo configurável
- [ ] Uma mobilidade característica
- [ ] Um momento épico
- [ ] Sons e tema musical originais, em duas camadas (base e pico)
- [ ] HUD, seção no Guia dos Heróis, textos em português e inglês
- [ ] Gametest para cada poder, roteiro de prints cobrindo traje e todos os poderes
- [ ] `hero_contracts`, `hero_suits_contract` e `hero_powers_contract` passando
