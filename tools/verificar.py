#!/usr/bin/env python3
"""Verificador antes do PR: confere o que o jogo só descobriria tarde (ou nunca).

    python3 tools/verificar.py                 # o mod e os personagens de teste
    python3 tools/verificar.py <pasta> ...     # mais pastas de recursos (rascunhos gerados fora)

O que ele confere:
  * receitas únicas: duas receitas com a mesma forma e os mesmos ingredientes brigam na bancada;
  * números das habilidades dentro dos limites (dano, custo, recarga, alcance, raio, duração);
  * nomes: ids de personagem e de habilidade únicos, nomes não vazios, textos de en_us e pt_br
    com as mesmas chaves em cada parte de src/main/heroes;
  * arquivos que faltam: item, peças do traje, modelo, ícone e skins de cada personagem da engine,
    e os .ogg que os sounds.json citam.

Sai com 1 e lista os problemas quando algo falha; com 0 e um resumo quando está tudo certo.
"""
import json
import pathlib
import re
import sys

ROOT = pathlib.Path(__file__).resolve().parent.parent
MODID = "greenlantern"
RESOURCES = [ROOT / "src/main/resources", ROOT / "src/gametest/resources"]
HEROES = ROOT / "src/main/heroes"
LOCALES = ("en_us", "pt_br")
PIECES = ("cabeca", "peito", "pernas", "pes")
# Receitas repetidas que já estavam no mod antes do verificador (kit de herói, v1.16.0 a v1.18.0).
# Mudá-las mexe em heróis existentes, então ficam como aviso até a adaptação dos 9 heróis.
KNOWN_RECIPES = {frozenset({"argo_pendant", "mother_box", "wizard_bolt"})}
# Limites dos números de uma habilidade (os mesmos da biblioteca de presets).
LIMITS = {"dano": (0, 60), "custo": (0, 100), "recarga": (0, 600), "alcance": (0, 128), "raio": (0, 96), "duracao": (0, 600)}


def load(path):
    with open(path, encoding="utf-8") as f:
        return json.load(f)


def recipe_signature(recipe):
    """A receita como a bancada a vê: a forma (com as letras trocadas pelos ingredientes) ou a lista sem ordem."""
    kind = recipe.get("type", "")
    key = recipe.get("key", {})

    def ingredient(value):
        if isinstance(value, list):
            return "|".join(sorted(ingredient(v) for v in value))
        if isinstance(value, dict):
            return value.get("item") or value.get("tag") or json.dumps(value, sort_keys=True)
        return str(value).removeprefix("minecraft:")

    if kind.endswith("crafting_shaped"):
        rows = recipe.get("pattern", [])
        # Linhas e colunas vazias nas bordas não contam: " A " e "A" são a mesma receita.
        while rows and not rows[0].strip():
            rows = rows[1:]
        while rows and not rows[-1].strip():
            rows = rows[:-1]
        if not rows:
            return None
        width = max(len(r) for r in rows)
        rows = [r.ljust(width) for r in rows]
        left = min((len(r) - len(r.lstrip()) for r in rows if r.strip()), default=0)
        right = max((len(r.rstrip()) for r in rows), default=0)
        rows = [r[left:right] for r in rows]
        return "shaped:" + "/".join("".join("." if ch == " " else "<" + ingredient(key.get(ch, ch)) + ">" for ch in r) for r in rows)
    if kind.endswith("crafting_shapeless"):
        return "shapeless:" + ",".join(sorted(ingredient(i) for i in recipe.get("ingredients", [])))
    return None


def vanilla_items():
    """Os ids dos itens do Minecraft, lidos do client.jar que o Gradle baixou (None se não houver)."""
    import zipfile
    cache = pathlib.Path.home() / ".gradle/caches/minecraftforge"
    for jar in sorted(cache.glob("**/minecraft_tasks/*/client.jar")) if cache.is_dir() else []:
        with zipfile.ZipFile(jar) as z:
            names = [n for n in z.namelist() if n.startswith("assets/minecraft/items/") and n.endswith(".json")]
        if names:
            return {pathlib.PurePosixPath(n).stem for n in names}
    return None


def recipe_items(recipe):
    """Os ids de item que a receita usa (as tags, com #, ficam de fora)."""
    found = []

    def walk(value):
        if isinstance(value, list):
            for v in value:
                walk(v)
        elif isinstance(value, dict):
            if "item" in value:
                walk(value["item"])
        elif isinstance(value, str) and not value.startswith("#"):
            found.append(value if ":" in value else "minecraft:" + value)

    walk(list(recipe.get("key", {}).values()) + recipe.get("ingredients", []))
    return found


def check(roots):
    problems = []
    warnings = []
    counts = {"receitas": 0, "personagens": 0, "habilidades": 0, "textos": 0, "sons": 0}

    def exists(rel):
        return any((r / rel).is_file() for r in roots)

    # ---- receitas únicas --------------------------------------------------------------------------
    seen = {}
    vanilla = vanilla_items()
    if vanilla is None:
        warnings.append("sem o client.jar do Minecraft no cache do Gradle: os ingredientes do jogo não foram conferidos")
    for root in roots:
        for path in sorted((root / "data" / MODID / "recipe").glob("*.json")):
            recipe = load(path)
            for item in recipe_items(recipe):
                namespace, _, name = item.partition(":")
                missing = (namespace == "minecraft" and vanilla is not None and name not in vanilla) or (
                        namespace == MODID and not exists(f"assets/{MODID}/items/{name}.json"))
                if missing:
                    problems.append(f"receita {path.relative_to(root).as_posix()}: o item {item} não existe")
            sig = recipe_signature(recipe)
            if sig is None:
                continue
            counts["receitas"] += 1
            name = path.relative_to(root).as_posix()
            if sig in seen and any({path.stem, pathlib.Path(seen[sig]).stem} <= k for k in KNOWN_RECIPES):
                warnings.append(f"receita repetida (já conhecida): {name} e {seen[sig]}")
            elif sig in seen:
                problems.append(f"receita repetida: {name} e {seen[sig]} pedem os mesmos ingredientes na mesma forma")
            else:
                seen[sig] = name

    # ---- personagens: números, nomes e arquivos ---------------------------------------------------
    ids = {}
    for root in roots:
        for path in sorted((root / "data" / MODID / "characters").glob("*.json")):
            data = load(path)
            cid = data.get("id", path.stem)
            who = f"personagem {cid}"
            counts["personagens"] += 1
            if cid in ids:
                problems.append(f"{who}: o id está em {ids[cid]} e em {path}")
            ids[cid] = path
            if not str(data.get("nome", "")).strip():
                problems.append(f"{who}: sem nome")
            ability_ids = set()
            for ability in data.get("habilidades", []):
                aid = ability.get("id", "?")
                counts["habilidades"] += 1
                if aid in ability_ids:
                    problems.append(f"{who}: duas habilidades com o id {aid}")
                ability_ids.add(aid)
                if not data.get("legado") and not str(ability.get("nome", "")).strip():
                    problems.append(f"{who}, habilidade {aid}: sem nome")
                for number, value in ability.get("numeros", {}).items():
                    if number in LIMITS and isinstance(value, (int, float)):
                        low, high = LIMITS[number]
                        if not low <= value <= high:
                            problems.append(f"{who}, habilidade {aid}: {number} {value} fora de {low} a {high}")
            if data.get("legado"):
                continue  # os 9 heróis antigos têm itens e texturas próprios
            assets = f"assets/{MODID}"
            needed = [f"{assets}/items/{cid}.json", f"{assets}/models/item/{cid}.json", f"{assets}/textures/item/{cid}.png"]
            needed += [f"{assets}/items/{cid}_{piece}.json" for piece in PIECES]
            for skin in data.get("skins", []):
                sid = skin.get("id", "?")
                needed += [f"{assets}/textures/entity/skin/{sid}.png", f"{assets}/textures/entity/skin/{sid}_brilho.png"]
            for rel in needed:
                if not exists(rel):
                    problems.append(f"{who}: falta {rel}")

    # ---- textos em en_us e pt_br ------------------------------------------------------------------
    for part in sorted(p for p in HEROES.iterdir() if p.is_dir()):
        langs = {}
        for locale in LOCALES:
            path = part / "lang" / f"{locale}.json"
            if path.is_file():
                langs[locale] = set(load(path))
        if not langs:
            continue
        everything = set().union(*langs.values())
        counts["textos"] += len(everything)
        for locale in LOCALES:
            missing = sorted(everything - langs.get(locale, set()))
            if missing:
                shown = ", ".join(missing[:5]) + (f" e mais {len(missing) - 5}" if len(missing) > 5 else "")
                problems.append(f"textos de {part.name}: faltam em {locale}: {shown}")

    # ---- sons citados que não existem -------------------------------------------------------------
    for sounds in sorted(HEROES.glob("*/sounds.json")):
        for event, body in load(sounds).items():
            for sound in body.get("sounds", []):
                name = sound if isinstance(sound, str) else sound.get("name", "")
                if isinstance(sound, dict) and sound.get("type") == "event":
                    continue
                namespace, _, file = name.partition(":") if ":" in name else ("minecraft", "", name)
                if namespace != MODID:
                    continue
                counts["sons"] += 1
                if not exists(f"assets/{MODID}/sounds/{file}.ogg"):
                    problems.append(f"som {event} ({sounds.parent.name}): falta assets/{MODID}/sounds/{file}.ogg")

    return problems, warnings, counts


def main(argv):
    extra = [pathlib.Path(a).resolve() for a in argv]
    for path in extra:
        if not path.is_dir():
            print(f"pasta não encontrada: {path}", file=sys.stderr)
            return 2
    roots = [r for r in RESOURCES if r.is_dir()] + extra
    problems, warnings, counts = check(roots)
    for warning in warnings:
        print("AVISO " + warning)
    for problem in problems:
        print("ERRO " + problem)
    summary = ", ".join(f"{v} {k}" for k, v in counts.items())
    if problems:
        print(f"{len(problems)} problema(s) em {summary}")
        return 1
    print(f"tudo certo: {summary}")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
