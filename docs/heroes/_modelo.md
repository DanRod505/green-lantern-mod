# Ficha do herói: NOME

> **Status:** rascunho. Troque para **aprovada** quando o Dalton aprovar; só então a thread começa o código.
>
> Copie este arquivo para `docs/heroes/<id>.md` e preencha. As duas tabelas (Identidade e Poderes) são lidas pelo gerador (`python3 tools/new_hero.py docs/heroes/<id>.md`): mantenha os nomes da primeira coluna e a ordem das colunas. O resto da ficha é para pessoas.

## Identidade

| Campo | Valor |
|---|---|
| id | `exemplo` (minúsculas e `_`; vira o nome dos arquivos, do pacote e das chaves) |
| Classe | `Exemplo` (nome das classes Java: `ExemploHero`, `ExemploPower`...) |
| Nome (pt) | Exemplo |
| Nome (en) | Example |
| Artigo (pt) | o |
| Item | `exemplo_item` (id do item que chama o traje e guarda a energia) |
| Item (pt) | Item do Exemplo |
| Item (en) | Example Item |
| Artigo do item (pt) | o |
| Recurso | `exemplo_energy` (id do recurso) |
| Recurso (pt) | Energia do Exemplo |
| Recurso (en) | Example Energy |
| Capacidade | 1000 |
| Recarga por segundo | 5 |
| Cor principal | #3050C0 |
| Cor de destaque | #F0C020 |
| Cor do brilho | #FFF0A0 |

## Poderes

De 5 a 8 poderes, na ordem da roda. O custo é do recurso acima (por segundo, se o poder fica ligado).

| id | Nome (pt) | Nome (en) | Custo | O que faz (pt) | What it does (en) |
|---|---|---|---|---|---|
| `exemplo_golpe` | Golpe | Strike | 40 | Um golpe que empurra tudo à frente. | A blow that pushes everything ahead. |

## Recurso e recarga

Como o recurso enche e esvazia, e por que combina com o personagem (sol para o Superman, Força de Aceleração para o Flash...).

## Mobilidade

A mobilidade característica (voo, corrida, nado, planar, outra) e se ela reaproveita uma que já existe (voo do `FlightProfile`, supervelocidade, nado 3D, planar e gancho).

## Momento épico

Veículo, montaria, invocação ou lugar novo.

## Traje e paleta

O que o traje tem na frente, nas costas e dos lados; quais peças (cabeça, peito, pernas, pés); as três cores acima.

## Música e sons

Clima do tema (base e pico) e os sons principais. Tudo original.

## Prints

O roteiro de prints cobre o traje (frente, costas, lado), o HUD, cada poder, a mobilidade, o momento épico, a roda de poderes e o guia.

| Print | O que mostra |
|---|---|
| `xx00_suit_front` | traje de frente |

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
