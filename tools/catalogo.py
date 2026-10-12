#!/usr/bin/env python3
"""
Gerador do catálogo (etapa 2, lote 7: ferramentas de produção).

Lê o catálogo de personagens (tools/catalogo/personagens.json, cópia do plano do guia mestre) e
escreve o rascunho de cada personagem que ainda não está no mod:

  data/greenlantern/characters/<id>.json   a ficha: classe, nível, esqueleto, transformação, trilha,
                                           passivas e as 5 a 8 habilidades com as funções por
                                           contexto, cada uma num preset da biblioteca
  data/greenlantern/recipe/<id>.json       a receita do item, do catálogo
  assets/greenlantern/...                  a skin rascunho (e as alternativas), o ícone e os
                                           arquivos do item e das 4 peças do traje

O preset de cada função sai da classe dela e de palavras do efeito ("árvore" vira cortar_arvore);
a condição sai do texto do catálogo ("Mirando mob", "Segurar", "Na água"). O que o jogo ainda não
sabe ler fica anotado no "_rascunho" da ficha, para a etapa Personagens e habilidades acertar à mão.

Uso:  pip install pillow
      python3 tools/catalogo.py --lote 1            os personagens do lote 1 (tools/catalogo/lotes.json)
      python3 tools/catalogo.py --ids "Kilowog,Zatanna"
      python3 tools/catalogo.py --todos
      python3 tools/catalogo.py --todos --apagar     apaga os rascunhos gerados (só os que ainda são rascunho)
      opções: --destino <pasta com data/ e assets/> (src/main/resources), --listar (só mostra)

Uma ficha que já existe e não é mais rascunho (alguém tirou o "_comentario" de rascunho para
acertá-la à mão) nunca é sobrescrita nem apagada.
"""
import argparse
import colorsys
import hashlib
import json
import os
import re
import sys
import unicodedata

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
CATALOG = os.path.join(ROOT, "tools", "catalogo", "personagens.json")
LOTS = os.path.join(ROOT, "tools", "catalogo", "lotes.json")

FIGHT = ["golpe", "disparo", "explosao", "controle", "defesa", "invocacao", "evasao", "fortalecimento"]
WORLD = ["mobilidade", "coleta_e_terreno", "construcao", "transmutacao", "natureza", "deteccao", "logistica"]
CLASS_ALIASES = {"coleta": "coleta_e_terreno"}
TRAIT_ALIASES = {"visao_escuro": "visao_no_escuro", "imune_veneno": "imune_a_veneno", "sentidos": "sentidos_agucados"}
TRAITS = ["voo", "levitar", "supervelocidade", "aquatico", "planar", "superforca", "invulneravel", "visao_no_escuro",
          "sentidos_agucados", "pulo_alto", "escalar", "regeneracao", "elastico", "imune_a_veneno"]
TRANSFORMATIONS = ["raio", "anel", "sol", "velocidade", "sombras", "armadura", "mistico", "divino", "mare", "elemento",
                   "fera", "tubo", "arsenal", "espetaculo", "asas"]
# O catálogo chama de "caos" a transformação que o mod chama de Espetáculo (confete e gargalhada).
# Itens do catálogo que mudaram de nome no Minecraft 1.21.11.
ITEM_ALIASES = {"chain": "iron_chain"}
TRANSFORMATION_ALIASES = {"caos": "espetaculo"}
DAMAGES = {"queda": "queda", "fogo": "fogo", "frio": "frio", "afogamento": "afogamento"}

# O preset padrão de cada classe e, antes dele, as palavras do efeito que pedem outro.
PRESETS = {
    "golpe": ("soco_forte", [("avanc|investida|atropel|carga", "investida"), ("espada|machado|lamina|arma|tridente|maca|cajado|martelo", "golpe_de_arma"),
                             ("pisao|pisa|chao treme|salta e cai|mergulha", "pisao"), ("agarra|arremess|joga longe|levanta", "agarrar_e_arremessar"),
                             ("combo|sequencia|rajada de socos|varios golpes", "sequencia")]),
    "disparo": ("projetil", [("feixe|visao de calor|raio continuo|laser", "feixe_continuo"), ("leque|varios|rajada", "leque"),
                             ("volta|bumerangue|batarangue|ricochete", "arremesso_que_volta"), ("flecha|bala|tiro|pistola|arco|municao", "balistico"),
                             ("ceu|relampago|raio cai", "raio_do_ceu")]),
    "explosao": ("onda_em_volta", [("ponto|granada|bomba|onde mira", "explosao_no_ponto"), ("sopro|cone|grito|hálito|halito", "cone"),
                                   ("pulso|atordo|choque", "pulso"), ("chuva|tempestade de", "chuva")]),
    "controle": ("prender", [("puxa|laco|chicote|corrente", "puxar"), ("lent|congela", "lentidao"), ("medo|confus|ilus|panico", "medo_e_confusao"),
                             ("domina|controla|mente|hipno", "dominar"), ("levita|levanta|ergue", "levitar")]),
    "defesa": ("bolha", [("bloque|de frente|escudo na mao|apara", "bloqueio"), ("pele|resist|dura", "pele_dura"),
                         ("parede|muralha|barreira", "parede"), ("reflet|devolve", "refletir")]),
    "invocacao": ("ajudante", [("construto|martelo|serra|broca", "construto"), ("veiculo|montaria|moto|carro|jato|nave|cavalo", "veiculo"),
                               ("enxame|morcego|inseto|passaros|corvos", "enxame"), ("copia|clone|duplicata|ilusao", "copia"),
                               ("torre|torreta|canhao", "torre")]),
    "evasao": ("esquiva", [("invis|some", "invisivel"), ("fumaca|neblina", "fumaca"), ("encolhe|minusculo", "encolher"),
                           ("atravess|intangiv|vibra", "atravessar"), ("sombra|forma|poca|vira|transforma", "fuga_de_forma")]),
    "fortalecimento": ("aura", [("cura|regenera", "cura"), ("cresce|gigante", "crescer"), ("furia|raiva", "furia"),
                                ("sentido|visao|enxerga", "sentidos")]),
    "mobilidade": ("teleporte", [("gancho|corda|cabo", "gancho"), ("salto|pula", "salto_gigante"), ("voa|voo", "voo"),
                                 ("corre|velocidade", "supervelocidade"), ("nada|mergulh", "nado_rapido"), ("plana|asa", "planar"),
                                 ("leva|amigos|carga|grupo", "levar_junto"), ("atravess|parede", "atravessar")]),
    "coleta_e_terreno": ("minerar_area", [("arvore|tronco|madeira|lenha", "cortar_arvore"), ("pesca|peixe", "pescar"),
                                          ("aplana|nivela", "aplanar"), ("tunel|caminho|clareira|abre", "abrir_caminho"),
                                          ("cava|terra|areia|cascalho|neve|folhas", "cavar_solto")]),
    "construcao": ("construir", [("luz|ilumina|lanterna|tocha", "luz"), ("projeto|planta|fantasma|molde", "projeto_em_fantasma"),
                                 ("obra|fortaleza|base|torre|castelo", "grande_obra")]),
    "transmutacao": ("trocar_bloco", [("maquina|redstone|liga", "ligar_maquina"),
                                      ("acende|derrete|congela|gelo|calor|fogo|queima|esfria|cozinha", "gelo_e_calor")]),
    "natureza": ("plantar_e_colher", [("doma|animal|guia|montar", "domar_e_guiar"), ("chuva|sol|tempestade|clima|tempo|raio", "clima")]),
    "deteccao": ("procurar", [("rastro|trilha|caminho de volta|estrutura|encontra o caminho|segue", "rastro")]),
    "logistica": ("ima_de_itens", [("ajudante|trabalha|robo|drone", "ajudante_de_trabalho"), ("organiza|arruma|ordena", "organizar"),
                                   ("carrega|move|bloco|bau|animal", "mover_blocos")]),
}
# A suprema de luta sem palavra própria vira a suprema de combate; a de construção, a grande obra.
SUPREME_DEFAULT = {"golpe": "suprema_de_combate", "disparo": "suprema_de_combate", "explosao": "suprema_de_combate",
                   "construcao": "grande_obra"}

# "Mirando ..." do catálogo para a mira que o jogo entende, na ordem em que são procuradas.
AIMS = [("mob|aldeao|animal|inimigo|criatura|amigo|aliado", "mob"), ("arvore|tronco|madeira", "madeira"), ("minerio", "minerio"),
        ("folha|mato", "folhas"), ("planta|flor|plantacao|comida", "planta"), ("agua", "agua"), ("lava", "lava"), ("fogo", "fogo"),
        ("gelo|neve", "gelo"), ("fornalha", "fornalha"), ("bau", "bau"), ("redstone|maquina|porta|cobre|para-raios", "maquina"),
        ("terra|areia|cascalho|morro", "terra"), ("pedra|rocha", "pedra"), ("vazio|o ar|um lugar|longe", "vazio"),
        ("bloco|parede|chao|construcao|vidro|la|concreto|fresta|item", "bloco")]
PLACES = [("na agua|nadando|submerso", "agua"), ("no ar|no alto|voando", "ar"), ("correndo", "correndo")]
SLIM = {"feminino", "jovem"}


def plain(text):
    """Sem acentos, em minúsculas: o texto em que as palavras-chave são procuradas."""
    return unicodedata.normalize("NFKD", text).encode("ascii", "ignore").decode().lower()


def slug(text):
    return re.sub(r"[^a-z0-9]+", "_", plain(text)).strip("_")


def first_match(rules, text):
    for pattern, value in rules:
        if re.search(pattern, text):
            return value
    return None


def earliest_match(rules, text):
    """O valor da regra cuja palavra aparece primeiro no texto ("fornalha ou comida" é fornalha)."""
    best = None
    for pattern, value in rules:
        m = re.search(r"\b(?:%s)" % pattern, text)
        if m and (best is None or m.start() < best[0]):
            best = (m.start(), value)
    return best[1] if best else None


def preset_for(kind, effect, supreme):
    default, rules = PRESETS[kind]
    found = first_match([(plain(p), v) for p, v in rules], plain(effect))
    if found:
        return found
    if supreme and kind in SUPREME_DEFAULT:
        return SUPREME_DEFAULT[kind]
    return default


def condition(text):
    """A condição ("quando") de uma função a partir do texto do catálogo, e o que dele ficou de fora."""
    t = plain(text)
    when = {}
    if "segurar e soltar" in t or "soltar" in t:
        when["aperto"] = "soltar"
    elif "segurar" in t:
        when["aperto"] = "segurar"
    if "mirando" in t:
        aim = earliest_match(AIMS, t.split("mirando", 1)[1])
        if aim:
            when["mira"] = aim
    place = first_match(PLACES, t)
    if place:
        when["lugar"] = place
    understood = {"toque", "segurar", "segurar e soltar"}
    left = None if t in understood or (when and re.fullmatch(r"(toque|segurar)?\s*(e soltar)?\s*(mirando .*|na agua|no ar|no alto)?", t)) else text
    return when, left


def palette(char_id):
    """Três cores do rascunho, estáveis para cada personagem."""
    h = int(hashlib.sha1(char_id.encode()).hexdigest(), 16)
    hue = (h % 360) / 360.0

    def hexa(hh, s, v):
        r, g, b = colorsys.hsv_to_rgb(hh % 1.0, s, v)
        return "#%02X%02X%02X" % (round(r * 255), round(g * 255), round(b * 255))

    return hexa(hue, 0.65, 0.75), hexa(hue + 0.5, 0.45, 0.35), hexa(hue + 0.08, 0.35, 1.0)


def ability_file(ability, used_ids, notes):
    kind = CLASS_ALIASES.get(ability["c"], ability["c"])
    supreme = ability["t"] == "u"
    aid = slug(ability["n"]) or "habilidade"
    while aid in used_ids:
        aid += "_2"
    used_ids.add(aid)
    functions = []
    for fn in ability["f"]:
        fkind = CLASS_ALIASES.get(fn["c"], fn["c"])
        when, left = condition(fn["q"])
        functions.append({"preset": preset_for(fkind, fn["e"], supreme and fkind == kind), "quando": when, "texto": fn["q"],
                          "efeito": fn["e"], "left": left})
    # A função padrão é a primeira de toque simples (ou sem condição que o jogo entenda); as outras pedem uma condição.
    default = next((f for f in functions if not f["quando"]), functions[0])
    extras = [f for f in functions if f is not default]
    data = {"id": aid, "nome": ability["n"], "classe": kind}
    if supreme:
        data["suprema"] = True
    data["preset"] = default["preset"]
    num = {k: v for k, v in ability["num"].items() if v}
    data["numeros"] = num
    if default["left"] and default["texto"].lower() != "toque":
        notes.append(f"{aid}: a condição \"{default['texto']}\" ainda não existe no jogo; virou o uso padrão")
    out = []
    taken = []
    for f in extras:
        when = f["quando"]
        if not when or when in taken:
            press = "segurar" if {"aperto": "segurar"} not in taken else "soltar"
            when = {"aperto": press}
            notes.append(f"{aid}: \"{f['texto']}\" ({f['efeito']}) ficou no aperto {press}")
        elif f["left"]:
            notes.append(f"{aid}: de \"{f['texto']}\" o jogo só entende {when}")
        if len(out) == 2:
            notes.append(f"{aid}: a função \"{f['texto']}\" ({f['efeito']}) passa do limite de 3 e ficou de fora")
            continue
        taken.append(when)
        out.append({"preset": f["preset"], "quando": when})
    if out:
        data["funcoes"] = out
    return data


def character_file(entry, cid):
    notes = []
    used = set()
    abilities = [ability_file(a, used, notes) for a in entry["hab"]]
    # Na roda: as de luta, depois as de mundo e por último a suprema.
    order = lambda a: (1 if a.get("suprema") else 0, 0 if a["classe"] in FIGHT else 1)
    abilities.sort(key=order)
    traits = []
    for p in entry["pas"]:
        kind = TRAIT_ALIASES.get(p["id"], p["id"])
        if kind not in TRAITS:
            notes.append(f"passiva {p['id']} não existe no jogo")
            continue
        trait = {"tipo": kind}
        if kind == "voo" and entry["st"].get("voo"):
            trait["ajustes"] = {"velocidade_maxima": entry["st"]["voo"]}
        if kind == "invulneravel" and p.get("det"):
            danos = [d for d in DAMAGES if d in plain(p["det"])]
            if danos:
                trait["ajustes"] = {"danos": danos}
        traits.append(trait)
    transformation = TRANSFORMATION_ALIASES.get(entry["tr"], entry["tr"])
    if transformation not in TRANSFORMATIONS:
        notes.append(f"transformação {entry['tr']} não existe; ficou padrao")
        transformation = "padrao"
    main, second, glow = palette(cid)
    skins = [{"id": cid, "nome": entry["nome"] if entry["nome"] != entry["id"] else entry["id"]}]
    for skin in entry["skins"]:
        skins.append({"id": f"{cid}_{slug(skin['n'])}", "nome": skin["n"], "liberada_por": skin.get("f") or "molde"})
    data = {
        "_comentario": f"Rascunho gerado por tools/catalogo.py a partir do catálogo (n.º {entry['n']}, {entry['nome']}).",
        "id": cid,
        "nome": entry["nome"] if list(e["nome"] for e in CATALOG_ENTRIES).count(entry["nome"]) == 1 else f"{entry['nome']} ({entry['id']})",
        "classe": entry["cls"],
        "nivel": max(1, min(4, int(entry["st"].get("nivel") or 2))),
        "esqueleto": entry["esq"],
        "transformacao": transformation,
        "trilha": entry.get("mus") or "padrao",
        "habilidades": abilities,
        "passivas": traits[:4],
        "skins": skins,
        "cores": {"principal": main, "secundaria": second, "brilho": glow},
    }
    if len(traits) > 4:
        notes.append("mais de 4 passivas; ficaram as 4 primeiras")
    if notes:
        data["_rascunho"] = notes
    return data


def recipe_file(entry, cid):
    item = entry["item"]
    key = {}
    for letter, name in item["ids"].items():
        name = ITEM_ALIASES.get(name, name)
        key[letter] = name if ":" in name else f"minecraft:{name}"
    return {"type": "minecraft:crafting_shaped", "category": "equipment", "pattern": item["shape"], "key": key,
            "result": {"id": f"greenlantern:{cid}", "count": 1}}


def write_json(path, data):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(data, f, indent=2, ensure_ascii=False)
        f.write("\n")


def load():
    with open(CATALOG, encoding="utf-8") as f:
        return [e for e in json.load(f) if not e.get("mod")]


CATALOG_ENTRIES = []


def pick(args):
    entries = CATALOG_ENTRIES
    if args.todos:
        return entries
    names = []
    if args.lote:
        with open(LOTS, encoding="utf-8") as f:
            lots = json.load(f)["lotes"]
        if args.lote not in lots:
            sys.exit(f"lote {args.lote} não existe (há {', '.join(lots)})")
        names = lots[args.lote]["personagens"]
    if args.ids:
        names += [n.strip() for n in args.ids.split(",") if n.strip()]
    chosen = []
    for name in names:
        found = [e for e in entries if name in (e["nome"], e["id"]) or slug(name) == slug(e["id"])]
        found = [e for e in found if e not in chosen]
        if not found:
            sys.exit(f"{name}: não está no catálogo")
        chosen.append(found[0])
    return chosen


DRAFT_MARK = "Rascunho gerado por tools/catalogo.py"


def is_draft(path):
    """Se o arquivo da ficha não existe ou ainda é um rascunho deste gerador."""
    if not os.path.isfile(path):
        return True
    with open(path, encoding="utf-8") as f:
        return str(json.load(f).get("_comentario", "")).startswith(DRAFT_MARK)


def draft_files(data_dir, assets, data):
    """Todos os arquivos que o gerador escreve para a ficha."""
    cid = data["id"]
    files = [os.path.join(data_dir, "characters", f"{cid}.json"), os.path.join(data_dir, "recipe", f"{cid}.json"),
             os.path.join(assets, "textures", "item", f"{cid}.png"), os.path.join(assets, "models", "item", f"{cid}.json"),
             os.path.join(assets, "items", f"{cid}.json")]
    files += [os.path.join(assets, "items", f"{cid}_{slot}.json") for slot in ("cabeca", "peito", "pernas", "pes")]
    for skin in data["skins"]:
        files += [os.path.join(assets, "textures", "entity", "skin", f"{skin['id']}{suffix}.png") for suffix in ("", "_brilho")]
    return files


def main():
    parser = argparse.ArgumentParser(description="Gera os rascunhos dos personagens do catálogo.")
    parser.add_argument("--lote")
    parser.add_argument("--ids")
    parser.add_argument("--todos", action="store_true")
    parser.add_argument("--destino", default=os.path.join(ROOT, "src", "main", "resources"))
    parser.add_argument("--listar", action="store_true")
    parser.add_argument("--apagar", action="store_true")
    args = parser.parse_args()
    if not (args.lote or args.ids or args.todos):
        parser.error("diga --lote, --ids ou --todos")
    CATALOG_ENTRIES.extend(load())
    chosen = pick(args)
    data_dir = os.path.join(args.destino, "data", "greenlantern")
    assets = os.path.join(args.destino, "assets", "greenlantern")
    if not args.listar and not args.apagar:
        sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
        import generate_engine_textures as textures
    for entry in chosen:
        cid = slug(entry["id"])
        data = character_file(entry, cid)
        print(f"{cid}: {data['nome']}, {data['classe']}, {len(data['habilidades'])} habilidades, "
              f"{len(data.get('_rascunho', []))} notas de rascunho")
        if args.listar:
            continue
        sheet = os.path.join(data_dir, "characters", f"{cid}.json")
        if not is_draft(sheet):
            print(f"  {cid} já foi acertado à mão: fica como está")
            continue
        if args.apagar:
            for path in draft_files(data_dir, assets, data):
                if os.path.isfile(path):
                    os.remove(path)
            continue
        write_json(os.path.join(data_dir, "characters", f"{cid}.json"), data)
        write_json(os.path.join(data_dir, "recipe", f"{cid}.json"), recipe_file(entry, cid))
        main_color, second, glow = palette(cid)
        slim = entry["esq"] in SLIM
        textures.test_character(cid, main_color, second, glow, slim, assets)
        for i, skin in enumerate(data["skins"][1:]):
            m2, s2, g2 = palette(skin["id"])
            textures.test_skin(skin["id"], m2, s2, g2, slim, assets)
    print(f"{len(chosen)} personagens" + (" apagados" if args.apagar else ""))


if __name__ == "__main__":
    main()
