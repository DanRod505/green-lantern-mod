# Ficha do herói: Ciborgue

> **Status:** rascunho. Troque para **aprovada** quando o Dalton aprovar; só então a thread começa o código.
>
> As duas tabelas (Identidade e Poderes) são lidas pelo gerador (`python3 tools/new_hero.py docs/heroes/cyborg.md`). O resto da ficha é para pessoas.

## Identidade

| Campo | Valor |
|---|---|
| id | `cyborg` |
| Classe | `Cyborg` |
| Nome (pt) | Ciborgue |
| Nome (en) | Cyborg |
| Artigo (pt) | o |
| Item | `mother_box` |
| Item (pt) | Caixa Materna |
| Item (en) | Mother Box |
| Artigo do item (pt) | a |
| Recurso | `cyborg_power` |
| Recurso (pt) | Bateria do Ciborgue |
| Recurso (en) | Cyborg Battery |
| Capacidade | 1200 |
| Recarga por segundo | 6 |
| Cor principal | #B8BEC8 |
| Cor de destaque | #C81E1E |
| Cor do brilho | #FF5A3C |

## Poderes

Sete poderes, na ordem da roda. O voo com propulsores não entra na roda: usa as teclas de voo, como o Superman e a Mulher Maravilha.

| id | Nome (pt) | Nome (en) | Custo | O que faz (pt) | What it does (en) |
|---|---|---|---|---|---|
| `sonic_cannon` | Canhão Sônico | Sonic Cannon | 60 | O braço vira canhão e solta uma onda sônica em cone que causa dano, empurra os inimigos e estilhaça vidro e gelo. | The arm becomes a cannon and fires a sonic wave in a cone that damages, knocks back enemies and shatters glass and ice. |
| `shoulder_missiles` | Mísseis do Ombro | Shoulder Missiles | 90 | Quatro microfoguetes saem do ombro e perseguem os inimigos mais próximos, explodindo sem quebrar blocos. | Four micro-rockets launch from the shoulder and chase the nearest enemies, exploding without breaking blocks. |
| `tech_scan` | Varredura | Tech Scan | 15 | Ligado, marca inimigos e minérios em 24 blocos através das paredes e mostra a vida do alvo na mira. Custa por segundo. | While on, highlights enemies and ores within 24 blocks through walls and shows the health of the target in sight. Costs per second. |
| `machine_hack` | Invasão | Machine Hack | 80 | Invade o que é máquina na mira: um golem de ferro vira aliado por um minuto, e portas, alçapões, pistões e trilhos obedecem ao toque. | Hacks the machine in sight: an iron golem becomes an ally for a minute, and doors, trapdoors, pistons and rails obey the touch. |
| `emp_burst` | Pulso Eletromagnético | EMP Burst | 120 | Um pulso em volta atordoa os inimigos por alguns segundos, derruba os voadores e apaga a redstone por perto por um instante. | A pulse around him stuns enemies for a few seconds, grounds flying mobs and switches off nearby redstone for a moment. |
| `self_repair` | Autorreparo | Self Repair | 100 | Os nanorrobôs consertam o corpo e o traje: cura aos poucos e recupera a durabilidade das peças. | Nanobots fix body and suit: heals over a few seconds and restores the suit's durability. |
| `boom_tube` | Tubo de Explosão | Boom Tube | 250 | Abre um túnel de luz que leva o Ciborgue e quem estiver perto até o ponto marcado (agachado e usando marca o ponto; sem marca, vai para a cama ou o spawn). | Opens a tunnel of light that takes Cyborg and anyone near him to the marked point (sneak and use to mark it; without a mark, to the bed or spawn). |

## Recurso e recarga

A Bateria do Ciborgue é a energia da Caixa Materna fundida ao corpo dele. Enche sozinha devagar (6 por segundo) e o dobro perto de redstone ligada (bloco de redstone, tocha ou fio a até 4 blocos), porque ele puxa energia das máquinas. Um raio que caia perto enche a bateria inteira. Na água funda ela não recarrega (curto-circuito), mas também não gasta. O voo com propulsores gasta 4 por segundo; parado no ar não gasta.

## Mobilidade

Voo com propulsores nos pés e nas costas, reaproveitando o `FlightProfile` compartilhado. Fica entre a Mulher Maravilha e o Superman: velocidade máxima 2.6, sem barreira do som, com rastro de fogo azul dos propulsores e pouso amortecido (sem dano de queda enquanto o traje está ligado).

## Momento épico

O Tubo de Explosão: um túnel de luz branca e azul que se abre na frente dele, com o som característico de estrondo, e transporta o grupo para o ponto marcado, inclusive entre o mundo normal, o Nether e Oa. Quem estiver a até 4 blocos vai junto (jogadores, montarias e aliados).

## Traje e paleta

- Cabeça: metade do rosto humana e metade metálica, com o olho vermelho brilhando (cor do brilho).
- Peito: placas prateadas com detalhes em cinza escuro, o núcleo vermelho no centro do peito e a Caixa Materna embutida.
- Costas: dois propulsores com luz azul, linhas de circuito vermelhas.
- Lados: braço direito com o canhão recolhido, ombro esquerdo com o lançador de mísseis.
- Peças: cabeça, peito, pernas e pés.
- Cores: prata metálico (#B8BEC8) como principal, vermelho (#C81E1E) nos detalhes e vermelho alaranjado (#FF5A3C) no brilho do olho e do núcleo.

## Música e sons

Tema original eletrônico: a base é um pulso sintetizado grave com batida mecânica, e o pico entra com guitarra distorcida e sintetizadores épicos. Sons principais, todos originais: traje montando peça por peça (servos e travas), canhão sônico carregando e disparando, mísseis saindo, bipe da varredura, chiado digital da invasão, estalo do pulso eletromagnético, zumbido dos nanorrobôs, propulsores e o estrondo do Tubo de Explosão.

## Prints

| Print | O que mostra |
|---|---|
| `cy00_suit_front` | traje de frente |
| `cy01_suit_back` | traje de costas, com os propulsores |
| `cy02_suit_side` | traje de lado, com canhão e lançador de mísseis |
| `cy03_hud` | HUD com a bateria |
| `cy04_wheel` | roda de poderes |
| `cy05_sonic_cannon` | onda sônica empurrando zumbis e quebrando vidro |
| `cy06_shoulder_missiles` | mísseis perseguindo inimigos |
| `cy07_tech_scan` | inimigos e minérios marcados através da parede |
| `cy08_machine_hack` | golem de ferro invadido atacando um zumbi |
| `cy09_emp_burst` | pulso atordoando inimigos e derrubando um phantom |
| `cy10_self_repair` | nanorrobôs curando |
| `cy11_flight` | voando com o rastro dos propulsores |
| `cy12_recharge` | recarga perto de redstone |
| `cy13_boom_tube_open` | Tubo de Explosão se abrindo |
| `cy14_boom_tube_arrive` | chegada no ponto marcado com o grupo |
| `cy15_guide` | seção do Ciborgue no Guia dos Heróis |

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
