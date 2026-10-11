# Ficha do herói: Supergirl

> **Status:** aprovada pelo Dalton em 2026-10-11.
>
> As duas tabelas (Identidade e Poderes) são lidas pelo gerador (`python3 tools/new_hero.py docs/heroes/supergirl.md`). O resto da ficha é para pessoas.

A base é a do Superman (energia solar, voo pelo `FlightProfile`, visão de calor, sopro, força), mas cada poder é uma variação própria: onde ele é mais forte e mais pesado, ela é mais rápida, mais ágil e mais explosiva.

## Identidade

| Campo | Valor |
|---|---|
| id | `supergirl` |
| Classe | `Supergirl` |
| Nome (pt) | Supergirl |
| Nome (en) | Supergirl |
| Artigo (pt) | a |
| Item | `argo_pendant` |
| Item (pt) | Pingente de Argo |
| Item (en) | Argo Pendant |
| Artigo do item (pt) | o |
| Recurso | `supergirl_solar` |
| Recurso (pt) | Energia Solar da Supergirl |
| Recurso (en) | Supergirl's Solar Energy |
| Capacidade | 900 |
| Recarga por segundo | 7 |
| Cor principal | #2A6FE0 |
| Cor de destaque | #D8202E |
| Cor do brilho | #FFD447 |

## Poderes

Sete poderes, na ordem da roda. O voo não entra na roda: usa as teclas de voo, como o Superman, a Mulher Maravilha e o Ciborgue.

| id | Nome (pt) | Nome (en) | Custo | O que faz (pt) | What it does (en) |
|---|---|---|---|---|---|
| `heat_bolts` | Rajadas de Calor | Heat Bolts | 35 | Em vez do feixe contínuo do Superman, os olhos disparam rajadas rápidas de calor, uma por clique, que explodem pequeno no alvo e acendem fogo sem quebrar blocos. | Instead of Superman's steady beam, her eyes fire quick heat bolts, one per click, that burst on the target and light fires without breaking blocks. |
| `meteor_dash` | Investida Meteoro | Meteor Dash | 70 | Ela se lança em linha reta como um cometa por 12 blocos, no chão ou no ar, atropelando e arremessando tudo no caminho. | She launches herself in a straight line like a comet for 12 blocks, on the ground or in the air, ramming and flinging everything in the way. |
| `thunder_clap` | Palmas Trovão | Thunder Clap | 80 | Bate as mãos e solta uma onda de choque em cone que atordoa os inimigos, apaga tochas e estilhaça vidro. | She claps her hands and sends a shockwave in a cone that stuns enemies, snuffs torches and shatters glass. |
| `frost_wall` | Muralha de Gelo | Frost Wall | 60 | Um sopro gelado que congela a água, apaga o fogo e levanta uma muralha de gelo à frente, que derrete sozinha depois de 30 segundos. | A freezing breath that freezes water, puts out fire and raises a wall of ice in front of her, which melts on its own after 30 seconds. |
| `super_hearing` | Superaudição | Super Hearing | 10 | Ligada, mostra ondas de som em volta de cada criatura a até 40 blocos (mesmo atrás de paredes) e setas no HUD de onde vêm os passos. Não mostra minérios, como o raio X do Superman. Custa por segundo. | While on, shows sound ripples around every creature within 40 blocks (even behind walls) and arrows on the HUD pointing to footsteps. Unlike Superman's X-ray, it shows no ores. Costs per second. |
| `kryptonian_throw` | Arremesso Kryptoniano | Kryptonian Throw | 50 | Agarra a criatura ou o bloco solto na mira, carrega acima da cabeça e arremessa longe com o próximo uso. | Grabs the creature or loose block in sight, lifts it overhead and hurls it far on the next use. |
| `solar_flare` | Explosão Solar | Solar Flare | 400 | Descarrega toda a energia de uma vez numa explosão de luz em volta dela, com dano maior quanto mais energia sobrar. Depois fica sem poderes nem voo por 20 segundos, recarregando. | Unleashes all her energy at once in a burst of light around her, stronger the more energy is left. Afterwards she has no powers or flight for 20 seconds while she recharges. |

## Recurso e recarga

A Energia Solar da Supergirl vem do sol amarelo, como a do Superman, mas o corpo dela é mais jovem e absorve mais rápido e guarda menos: capacidade 900 (o Superman guarda mais) e recarga 7 por segundo sob o sol, 2 por segundo à sombra ou à noite, nada no Nether e no subterrâneo sem céu. A diferença própria dela é o combate: cada soco acertado devolve 5 de energia, então ela se recarrega lutando corpo a corpo. A Explosão Solar zera a energia e trava a recarga por 20 segundos.

## Mobilidade

Voo pelo `FlightProfile` compartilhado, com o perfil próprio dela: a mais ágil de todos e a segunda mais rápida.

| | Mulher Maravilha | Ciborgue | Lanterna Verde | **Supergirl** | Superman |
|---|---|---|---|---|---|
| Velocidade máxima | 2.3 | 2.5 | 4.0 | **5.5** | 7.5 |
| Barreira do som (2.6) | não | não | sim | **sim** | sim |
| Segundos até a barreira | | | | **1.0** | 1.6 |
| Pouso de herói | | | 1.0 | **1.2** | 1.8 |

- Acelera mais rápido que o Superman e faz curvas mais fechadas, mas chega a uma velocidade máxima menor e o pouso de herói abre uma cratera menor.
- **Pirueta:** dois toques no pulo durante o voo fazem um giro rápido de lado que esquiva (meio segundo sem tomar dano de projéteis), com custo de 20 de energia.
- Rastro próprio: faixa vermelha da capa curta com faíscas douradas, diferente do rastro azul do Superman.
- O voo gasta 2 por segundo, e o dobro acima da barreira do som.

## Momento épico

**Krypto, o Supercão.** Um companheiro que fica com ela o tempo todo, não uma invocação que some depois do uso.

- **Chegada:** na primeira vez que ela veste o traje, o Krypto desce do céu com a capinha vermelha esvoaçando e late para ela. Depois disso ele é dela, com nome e vida guardados, como um lobo domado.
- **Segue e voa junto:** anda atrás dela no chão e voa ao lado quando ela voa, acompanhando até acima da barreira do som. Se ficar longe demais, aparece de novo do lado dela num risco de luz.
- **Comandos:** clique com a mão vazia faz ele sentar ou voltar a seguir. Agachada e usando o Pingente de Argo, ela assobia e ele vem de onde estiver.
- **Combate:** ataca quem ela ataca e quem machuca ela, com mordidas fortes que derrubam os inimigos. De vez em quando solta uma rajada curta de visão de calor pelos olhos. Não ataca jogadores, aldeões nem os animais domados de ninguém.
- **Junto com os poderes dela:** com a Superaudição ligada, ele fareja e late na direção do inimigo mais perto. Na Explosão Solar, ele protege ela e fica na frente enquanto ela recarrega.
- **Buscar:** pega os itens que caem dos inimigos derrotados e traz até ela.
- **Não morre:** com a vida zerada, ele voa para o céu ganindo e volta depois de 60 segundos. Osso ou carne na mão dela curam ele.
- **Visual:** cão branco de porte médio, capa vermelha curta com o S dourado na coleira, olhos dourados que brilham ao usar a visão de calor.

## Traje e paleta

Inspirado no visual moderno da personagem, bem distinto do Superman:

- Cabeça: cabelo loiro solto e longo (na camada do capacete), sem máscara, com os olhos brilhando em dourado ao usar a visão de calor.
- Peito: azul mais claro que o do Superman (#2A6FE0), o escudo do S vermelho com fundo dourado (cor do brilho) e mangas compridas.
- Costas: capa vermelha curta, até a cintura, que balança mais ao andar e voar.
- Pernas: saia vermelha curta com cinto dourado, meia-calça azul.
- Pés: botas vermelhas de cano alto.
- Lados: o escudo do S aparece também no fecho dourado da capa.
- Cores: azul claro (#2A6FE0) como principal, vermelho (#D8202E) na capa, saia e botas, e dourado (#FFD447) no brilho dos olhos, do S e do pingente.

## Música e sons

Tema original, mais leve e otimista que o do Superman: a base é um ostinato de cordas rápido com piano, e o pico entra com metais heroicos, bateria acelerada e um coro curto. Sons principais, todos originais: traje se formando em luz dourada, rajadas de calor curtas, o zumbido da Investida Meteoro, o estalo das Palmas Trovão, gelo rachando ao subir a muralha, o zumbido da superaudição, o grunhido do arremesso, a Explosão Solar crescendo e estourando, o assobio da pirueta e os latidos, o ganido e o voo do Krypto.

## Prints

| Print | O que mostra |
|---|---|
| `sg00_suit_front` | traje de frente |
| `sg01_suit_back` | traje de costas, com a capa curta |
| `sg02_suit_side` | traje de lado, com a saia e as botas |
| `sg03_hud` | HUD com a energia solar |
| `sg04_wheel` | roda de poderes |
| `sg05_heat_bolts` | rajadas de calor acertando zumbis |
| `sg06_meteor_dash` | investida no meio de um grupo de inimigos |
| `sg07_thunder_clap` | onda de choque atordoando e estilhaçando vidro |
| `sg08_frost_wall` | muralha de gelo bloqueando esqueletos |
| `sg09_super_hearing` | ondas de som atrás da parede e setas no HUD |
| `sg10_kryptonian_throw` | criatura erguida acima da cabeça e arremessada |
| `sg11_solar_flare` | explosão solar em volta dela |
| `sg12_flight` | voando acima da barreira do som com o rastro vermelho e dourado |
| `sg13_barrel_roll` | pirueta esquivando de flechas |
| `sg14_krypto_arrives` | Krypto descendo do céu na primeira vez que ela veste o traje |
| `sg15_krypto_fight` | Krypto mordendo um zumbi ao lado dela |
| `sg15b_krypto_flight` | Krypto voando ao lado dela |
| `sg16_guide` | seção da Supergirl no Guia dos Heróis |

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
