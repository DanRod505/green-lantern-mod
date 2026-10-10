#!/usr/bin/env python3
"""Hero kit: creates the skeleton of a new hero from its approved hero sheet.

    python3 tools/new_hero.py docs/heroes/<id>.md

Reads the "Identidade" and "Poderes" tables of the sheet (see docs/heroes/_modelo.md) and writes,
already compiling and passing the hero contract tests:

  * the hero package: <Class>Hero (the HeroDefinition), <Class>Power (the powers), <Class>Server
    (what each power does: a placeholder that spends energy until the real power is written),
    <Class>Content (its own items, sounds and energy component), <Class>Config (its own config file)
  * the client half: <Class>Client with the standard HUD (client/HeroHud) and its Guide section
  * texts in Portuguese and English and the suit sounds (src/main/heroes/<id>/)
  * placeholder textures in the hero's colours: item, suit pieces, suit on the body, wheel icons
  * recipe and recipe advancement of the hero's item
  * gametests (gametest/heroes/<Class>Tests) and a screenshot script (gametest/scripts/<Class>Script)
  * one line each in HeroRegistry, ModGameTests, ClientScripts and .github/screenshot-scripts.txt

Only the standard library is used. Run it from anywhere; paths are relative to the repository.
"""
import argparse
import json
import os
import re
import struct
import sys
import zlib

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
MODID = 'greenlantern'
JAVA = os.path.join(ROOT, 'src/main/java/com/danrod505/greenlantern')
GAMETEST = os.path.join(ROOT, 'src/gametest/java/com/danrod505/greenlantern/gametest')
ASSETS = os.path.join(ROOT, 'src/main/resources/assets', MODID)
DATA = os.path.join(ROOT, 'src/main/resources/data', MODID)
HEROES = os.path.join(ROOT, 'src/main/heroes')
MARKER = 'tools/new_hero.py adds new heroes above this line'

IDENTITY_FIELDS = {
    'id': 'id', 'Classe': 'cls', 'Nome (pt)': 'name_pt', 'Nome (en)': 'name_en', 'Artigo (pt)': 'article_pt',
    'Item': 'item', 'Item (pt)': 'item_pt', 'Item (en)': 'item_en', 'Artigo do item (pt)': 'item_article_pt',
    'Recurso': 'energy', 'Recurso (pt)': 'energy_pt', 'Recurso (en)': 'energy_en', 'Capacidade': 'capacity',
    'Recarga por segundo': 'recharge', 'Cor principal': 'main', 'Cor de destaque': 'accent', 'Cor do brilho': 'glow',
}


def fail(message):
    print('new_hero: ' + message, file=sys.stderr)
    sys.exit(1)


# ---- reading the hero sheet ------------------------------------------------------------------------

def tables(markdown):
    """Markdown tables by the heading (##) above them: {heading: [rows as lists of cells]}."""
    found, heading, rows = {}, None, None
    for line in markdown.splitlines() + ['']:
        stripped = line.strip()
        if stripped.startswith('## '):
            heading = stripped[3:].strip()
        if stripped.startswith('|'):
            cells = [c.strip() for c in stripped.strip('|').split('|')]
            if rows is None:
                rows = []
            if not all(re.fullmatch(r':?-+:?', c) for c in cells if c):
                rows.append(cells)
        elif rows is not None:
            found.setdefault(heading, rows)
            rows = None
    return found


def plain(cell):
    """A cell without markdown code marks and without the explanation in parentheses after a code value."""
    m = re.match(r'`([^`]*)`', cell)
    return m.group(1).strip() if m else cell.strip()


def colour(value, field):
    m = re.fullmatch(r'#?([0-9a-fA-F]{6})', value.strip())
    if not m:
        fail(f'"{field}" must be a colour like #3050C0, got {value!r}')
    return int(m.group(1), 16)


def read_sheet(path):
    found = tables(open(path, encoding='utf-8').read())
    identity_rows = found.get('Identidade')
    power_rows = found.get('Poderes')
    if not identity_rows or not power_rows:
        fail('the sheet needs the tables "## Identidade" and "## Poderes" (see docs/heroes/_modelo.md)')
    hero = {}
    for row in identity_rows[1:]:
        if len(row) >= 2 and row[0] in IDENTITY_FIELDS:
            hero[IDENTITY_FIELDS[row[0]]] = plain(row[1])
    missing = [field for field, key in IDENTITY_FIELDS.items() if not hero.get(key)]
    if missing:
        fail('missing in "Identidade": ' + ', '.join(missing))
    for key in ('id', 'item', 'energy'):
        if not re.fullmatch(r'[a-z][a-z0-9_]*', hero[key]):
            fail(f'{key} must be lowercase letters, digits and _ (got {hero[key]!r})')
    if not re.fullmatch(r'[A-Z][A-Za-z0-9]*', hero['cls']):
        fail(f'Classe must be a Java class name like Cyborg (got {hero["cls"]!r})')
    for key in ('capacity', 'recharge'):
        if not hero[key].isdigit():
            fail(f'{key} must be a whole number (got {hero[key]!r})')
        hero[key] = int(hero[key])
    for key, field in (('main', 'Cor principal'), ('accent', 'Cor de destaque'), ('glow', 'Cor do brilho')):
        hero[key] = colour(hero[key], field)
    powers = []
    for row in power_rows[1:]:
        if len(row) < 6 or not plain(row[0]):
            continue
        power = {'id': plain(row[0]), 'pt': row[1], 'en': row[2], 'cost': plain(row[3]), 'desc_pt': row[4], 'desc_en': row[5]}
        if not re.fullmatch(r'[a-z][a-z0-9_]*', power['id']):
            fail(f'power id must be lowercase letters, digits and _ (got {power["id"]!r})')
        if not power['cost'].isdigit():
            fail(f'power {power["id"]}: Custo must be a whole number')
        power['cost'] = int(power['cost'])
        powers.append(power)
    if not powers:
        fail('"Poderes" has no powers')
    if len({p['id'] for p in powers}) != len(powers):
        fail('two powers with the same id')
    if not 5 <= len(powers) <= 8:
        print(f'new_hero: warning: {len(powers)} powers (the definition of done asks for 5 to 8)')
    hero['powers'] = powers
    hero['pkg'] = hero['id'].replace('_', '')
    hero['const'] = hero['id'].upper()
    return hero


# ---- checks against what already exists -----------------------------------------------------------

def existing_lang_keys():
    keys = set()
    for part in os.listdir(HEROES):
        path = os.path.join(HEROES, part, 'lang', 'en_us.json')
        if os.path.isfile(path):
            keys |= set(json.load(open(path, encoding='utf-8')))
    return keys


def check_free(hero):
    if os.path.exists(os.path.join(JAVA, hero['pkg'])):
        fail(f'package {hero["pkg"]} already exists: this hero was already generated')
    if os.path.exists(os.path.join(HEROES, hero['id'])):
        fail(f'src/main/heroes/{hero["id"]} already exists')
    keys = existing_lang_keys()
    for power in hero['powers']:
        if f'power.{MODID}.{power["id"]}' in keys:
            fail(f'power id {power["id"]} is already used by another hero (lang keys are shared): pick another one')
    if f'item.{MODID}.{hero["item"]}' in keys:
        fail(f'item {hero["item"]} already exists')


# ---- small PNG writer and placeholder art -----------------------------------------------------------

def png(path, width, height, pixels):
    """pixels: function (x, y) -> 0xAARRGGBB."""
    raw = bytearray()
    for y in range(height):
        raw.append(0)
        for x in range(width):
            p = pixels(x, y)
            raw += bytes(((p >> 16) & 255, (p >> 8) & 255, p & 255, (p >> 24) & 255))

    def chunk(kind, data):
        return struct.pack('>I', len(data)) + kind + data + struct.pack('>I', zlib.crc32(kind + data) & 0xFFFFFFFF)

    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'wb') as f:
        f.write(b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', struct.pack('>IIBBBBB', width, height, 8, 6, 0, 0, 0))
                + chunk(b'IDAT', zlib.compress(bytes(raw), 9)) + chunk(b'IEND', b''))


def shade(rgb, factor):
    r, g, b = (rgb >> 16) & 255, (rgb >> 8) & 255, rgb & 255
    clamp = lambda v: max(0, min(255, int(v)))
    return 0xFF000000 | clamp(r * factor) << 16 | clamp(g * factor) << 8 | clamp(b * factor)


# 3x5 letters for the wheel icons (anything else draws a dot).
FONT = {
    'A': '111101111101101', 'B': '110101110101110', 'C': '111100100100111', 'D': '110101101101110', 'E': '111100110100111',
    'F': '111100110100100', 'G': '111100101101111', 'H': '101101111101101', 'I': '111010010010111', 'J': '001001001101111',
    'K': '101101110101101', 'L': '100100100100111', 'M': '101111111101101', 'N': '111101101101101', 'O': '111101101101111',
    'P': '111101111100100', 'Q': '111101101111001', 'R': '111101110101101', 'S': '111100111001111', 'T': '111010010010010',
    'U': '101101101101111', 'V': '101101101101010', 'W': '101101111111101', 'X': '101101010101101', 'Y': '101101010010010',
    'Z': '111001010100111',
}


def letter(name):
    import unicodedata
    for ch in unicodedata.normalize('NFD', name).upper():
        if ch in FONT:
            return ch
    return None


def icon_pixels(hero, index, ch):
    """A round badge: main colour with an accent rim and the letter in the glow colour, scaled 2x."""
    glyph = FONT.get(ch)

    def pixel(x, y):
        dx, dy = x - 7.5, y - 7.5
        d = (dx * dx + dy * dy) ** 0.5
        if d > 7.6:
            return 0
        if d > 6.4:
            return shade(hero['accent'], 1.0)
        gx, gy = (x - 5) // 2, (y - 3) // 2
        if glyph and 0 <= gx < 3 and 0 <= gy < 5 and 5 <= x < 11 and 3 <= y < 13 and glyph[gy * 3 + gx] == '1':
            return shade(hero['glow'], 1.0)
        if not glyph and d < 2.2:
            return shade(hero['glow'], 1.0)
        return shade(hero['main'], 0.75 + 0.25 * (1 - d / 6.4) + 0.05 * (index % 2))

    return pixel


def item_pixels(hero, shape):
    """16x16 item sprites: the hero's item (an emblem) and the four suit pieces."""
    main, accent, glow = hero['main'], hero['accent'], hero['glow']
    masks = {
        'item': lambda x, y: ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5 <= 6.5,
        'head': lambda x, y: 3 <= x <= 12 and 3 <= y <= 11 and not (5 <= x <= 10 and y >= 9),
        'chest': lambda x, y: (2 <= x <= 13 and 3 <= y <= 13) and not (x in (5, 6, 9, 10) and y <= 4) and not (y >= 7 and (x <= 3 or x >= 12)),
        'legs': lambda x, y: 4 <= x <= 11 and 2 <= y <= 14 and not (7 <= x <= 8 and y >= 6),
        'feet': lambda x, y: 9 <= y <= 13 and (2 <= x <= 6 or 9 <= x <= 13),
    }
    mask = masks[shape]

    def pixel(x, y):
        if not mask(x, y):
            return 0
        edge = not (mask(x - 1, y) and mask(x + 1, y) and mask(x, y - 1) and mask(x, y + 1))
        if edge:
            return shade(main, 0.45)
        if shape == 'item' and ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5 <= 2.6:
            return shade(glow, 1.0)
        if shape == 'item' and ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5 <= 4.2:
            return shade(accent, 1.0)
        if shape == 'chest' and 6 <= x <= 9 and 6 <= y <= 9:
            return shade(accent, 1.0)
        if shape == 'chest' and y == 11:
            return shade(accent, 0.9)
        return shade(main, 1.05 - 0.02 * y)

    return pixel


def armor_pixels(hero, leggings):
    """64x32 suit on the body (vanilla armor layout): a solid suit with an accent emblem and belt, and a
    domino mask on the head. The pieces worn decide which parts show."""
    main, accent = hero['main'], hero['accent']

    def pixel(x, y):
        if leggings:
            # Legs (0-16 x 16-32) and the waist of the body (16-40 x 16-32).
            if y >= 16 and x < 40:
                return shade(accent, 0.9) if y == 20 and 16 <= x < 40 else shade(main, 0.95)
            return 0
        if y < 16:
            # Head: only a band across the eyes (front, sides and back faces are at y 8-16).
            return shade(main, 0.6) if 10 <= y <= 11 and x < 32 and y >= 8 else 0
        if y >= 16 and x < 16:
            return shade(main, 0.9)  # boots (lower legs)
        if 16 <= x < 40 or 40 <= x < 56:
            if 20 <= x < 28 and 22 <= y < 26 and 21 <= x <= 26:
                return shade(accent, 1.0)  # emblem on the chest
            if 20 <= x < 28 and y == 30:
                return shade(accent, 0.85)  # belt
            return shade(main, 1.0 if x < 40 else 0.9)
        return 0

    return pixel


# ---- writing files --------------------------------------------------------------------------------

written = []


def write(path, text):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w', encoding='utf-8', newline='\n') as f:
        f.write(text)
    written.append(os.path.relpath(path, ROOT))


def write_json(path, obj):
    write(path, json.dumps(obj, ensure_ascii=False, indent=2) + '\n')


def add_import(text, import_line):
    lines = text.split('\n')
    imports = [n for n, l in enumerate(lines) if l.startswith('import ') and not l.startswith('import static ')]
    block = sorted(set(lines[imports[0]:imports[-1] + 1] + [import_line]))
    lines[imports[0]:imports[-1] + 1] = block
    return '\n'.join(lines)


def insert_before_marker(path, line, import_line):
    text = open(path, encoding='utf-8').read()
    if MARKER not in text:
        fail(f'{os.path.relpath(path, ROOT)} lost the line "// {MARKER}"')
    lines = text.split('\n')
    i = next(n for n, l in enumerate(lines) if MARKER in l)
    lines.insert(i, line)
    with open(path, 'w', encoding='utf-8', newline='\n') as f:
        f.write(add_import('\n'.join(lines), import_line))
    written.append(os.path.relpath(path, ROOT) + ' (+1 line)')


def add_to_registry(hero):
    path = os.path.join(JAVA, 'hero/HeroRegistry.java')
    text = open(path, encoding='utf-8').read()
    m = re.search(r'(List\.of\(\n(?:\s+\w+\.INSTANCE,\n)*\s+\w+\.INSTANCE)\);', text)
    if not m:
        fail('could not find the hero list in HeroRegistry.java')
    text = text[:m.end(1)] + f',\n            {hero["cls"]}Hero.INSTANCE' + text[m.end(1):]
    with open(path, 'w', encoding='utf-8', newline='\n') as f:
        f.write(add_import(text, f'import com.danrod505.greenlantern.{hero["pkg"]}.{hero["cls"]}Hero;'))
    written.append('src/main/java/com/danrod505/greenlantern/hero/HeroRegistry.java (+1 line)')


def java_string(s):
    return json.dumps(s, ensure_ascii=False)


def generate(hero):
    h = hero
    cls, pkg, hid, item, energy = h['cls'], h['pkg'], h['id'], h['item'], h['energy']
    powers = h['powers']
    n = len(powers)
    base = f'com.danrod505.greenlantern.{pkg}'
    rgb = lambda v: f'0x{v:06X}'
    argb = lambda v, a=0xFF: f'0x{a:02X}{v:06X}'
    dark = lambda v, f: int(shade(v, f)) & 0xFFFFFF
    art = h['article_pt']
    of_item = {'o': 'do', 'a': 'da', 'os': 'dos', 'as': 'das'}.get(h['item_article_pt'], 'de')
    of_hero = {'o': 'do', 'a': 'da', 'os': 'dos', 'as': 'das'}.get(art, 'de')
    hue = lambda v: __import__('colorsys').rgb_to_hsv(((v >> 16) & 255) / 255, ((v >> 8) & 255) / 255, (v & 255) / 255)[0]
    # ---- Java: hero package -------------------------------------------------------------------------
    enum_lines = ',\n'.join(f'    /** {p["en"]}: {p["desc_en"]} */\n    {p["id"].upper()}("{p["id"]}")' for p in powers)
    cost_cases = '\n'.join(f'            case {p["id"].upper()} -> {cls}Config.{p["id"].upper()}_COST.get();' for p in powers)
    write(os.path.join(JAVA, pkg, f'{cls}Power.java'), f'''package {base};

import com.danrod505.greenlantern.hero.HeroPower;
import com.danrod505.greenlantern.hero.PowerSet;

/**
 * Powers of {h["name_en"]}, in wheel order (hold the wheel key to pick, the power key or right click on
 * the {h["item_en"]} to use).
 * <p>
 * To add a power: add a constant here (its icon goes in {{@code textures/gui/{hid}_powers.png}} at the
 * same index, before the emblem), its cost to {{@link {cls}Config}}, its name and description to
 * {{@code src/main/heroes/{hid}/lang}} and its behaviour to {{@link {cls}Server#usePower}}.
 */
public enum {cls}Power implements HeroPower {{
{enum_lines};

    /** The powers in wheel order, and the selection kept on the hero's item. */
    public static final PowerSet<{cls}Power> POWERS = new PowerSet<>(values());

    private final String id;

    {cls}Power(String id) {{
        this.id = id;
    }}

    @Override
    public String id() {{
        return id;
    }}

    @Override
    public int cost() {{
        return switch (this) {{
{cost_cases}
        }};
    }}
}}
''')
    cost_fields = '\n'.join(f'    public static final ForgeConfigSpec.IntValue {p["id"].upper()}_COST;' for p in powers)
    cost_defs = '\n'.join(f'        {p["id"].upper()}_COST = builder.comment({java_string(p["en"] + ": " + h["energy_en"] + " it costs")})\n'
                          f'                .defineInRange("{p["id"]}_cost", {p["cost"]}, 0, 1_000_000);' for p in powers)
    write(os.path.join(JAVA, pkg, f'{cls}Config.java'), f'''package {base};

import net.minecraftforge.common.ForgeConfigSpec;

/** Settings of {h["name_en"]} (config/greenlantern-{hid}.toml): the {h["energy_en"]} and what each power costs. */
public final class {cls}Config {{
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.IntValue CAPACITY;
    public static final ForgeConfigSpec.IntValue RECHARGE_PER_SECOND;
{cost_fields}

    static {{
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("{hid}");
        CAPACITY = builder.comment({java_string("How much " + h["energy_en"] + " the " + h["item_en"] + " holds")})
                .defineInRange("capacity", {h["capacity"]}, 1, 1_000_000);
        RECHARGE_PER_SECOND = builder.comment({java_string(h["energy_en"] + " that comes back every second")})
                .defineInRange("recharge_per_second", {h["recharge"]}, 0, 1_000_000);
{cost_defs}
        builder.pop();
        SPEC = builder.build();
    }}

    private {cls}Config() {{}}
}}
''')
    write(os.path.join(JAVA, pkg, f'{cls}Content.java'), f'''package {base};

import com.danrod505.greenlantern.GreenLantern;
import com.danrod505.greenlantern.item.HeroItem;
import com.danrod505.greenlantern.item.SuitArmorItem;
import com.danrod505.greenlantern.ring.RingEnergy;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraftforge.eventbus.api.bus.BusGroup;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/** Everything {h["name_en"]} adds to the game, registered by the hero itself (see {{@link {cls}Hero#register}}). */
public final class {cls}Content {{
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, GreenLantern.MODID);
    private static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, GreenLantern.MODID);
    private static final DeferredRegister<DataComponentType<?>> COMPONENTS = DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, GreenLantern.MODID);

    /** {h["energy_en"]} stored in the {h["item_en"]} (the selected power is the shared {{@code selected_power}}). */
    public static final RegistryObject<DataComponentType<RingEnergy>> ENERGY = COMPONENTS.register("{energy}",
            () -> DataComponentType.<RingEnergy>builder().persistent(RingEnergy.CODEC).networkSynchronized(RingEnergy.STREAM_CODEC).build());

    public static final RegistryObject<SoundEvent> SUIT_UP = sound("{hid}_suit_up");
    public static final RegistryObject<SoundEvent> SUIT_DOWN = sound("{hid}_suit_down");

    /** The {h["item_en"]}: it calls the suit and holds the {h["energy_en"]}. */
    public static final RegistryObject<HeroItem> ITEM = ITEMS.register("{item}", () -> new HeroItem(new Item.Properties()
            .setId(ITEMS.key("{item}"))
            .stacksTo(1)
            .rarity(Rarity.EPIC)
            .fireResistant(), () -> {cls}Hero.INSTANCE, {hue(h["main"]):.3f}F, {hue(h["accent"]):.3f}F, ChatFormatting.GOLD));

    public static final ResourceKey<EquipmentAsset> ASSET = ResourceKey.create(EquipmentAssets.ROOT_ID, GreenLantern.id("{hid}"));

    /** The suit: as tough as diamond. Tune it to the hero. */
    public static final ArmorMaterial MATERIAL = new ArmorMaterial(
            1000,
            Map.of(ArmorType.HELMET, 3, ArmorType.CHESTPLATE, 8, ArmorType.LEGGINGS, 6, ArmorType.BOOTS, 3, ArmorType.BODY, 8),
            1,
            SoundEvents.ARMOR_EQUIP_GENERIC,
            2.0F,
            0.0F,
            ItemTags.REPAIRS_DIAMOND_ARMOR,
            ASSET);

    public static final RegistryObject<SuitArmorItem> MASK = suit("{hid}_mask", ArmorType.HELMET);
    public static final RegistryObject<SuitArmorItem> SUIT = suit("{hid}_suit", ArmorType.CHESTPLATE);
    public static final RegistryObject<SuitArmorItem> LEGGINGS = suit("{hid}_leggings", ArmorType.LEGGINGS);
    public static final RegistryObject<SuitArmorItem> BOOTS = suit("{hid}_boots", ArmorType.BOOTS);

    private {cls}Content() {{}}

    static void register(BusGroup modBus) {{
        SOUNDS.register(modBus);
        COMPONENTS.register(modBus);
        ITEMS.register(modBus);
    }}

    private static RegistryObject<SoundEvent> sound(String name) {{
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(GreenLantern.id(name)));
    }}

    private static RegistryObject<SuitArmorItem> suit(String name, ArmorType type) {{
        return ITEMS.register(name, () -> new SuitArmorItem(type, SuitArmorItem.properties(MATERIAL, type).setId(ITEMS.key(name))));
    }}
}}
''')
    wheel_args = (f'{rgb(dark(h["main"], 0.18))}, {rgb(dark(h["main"], 0.08))}, {rgb(h["accent"])}, {rgb(dark(h["main"], 0.7))}, '
                  f'{rgb(dark(h["main"], 0.14))}, {rgb(dark(h["main"], 0.3))},\n                    {rgb(dark(h["main"], 0.6))}, '
                  f'{rgb(h["main"])}, {rgb(h["glow"])}, {rgb(h["glow"])}, {rgb(h["accent"])}')
    write(os.path.join(JAVA, pkg, f'{cls}Hero.java'), f'''package {base};

import com.danrod505.greenlantern.GreenLantern;
import com.danrod505.greenlantern.client.HeroClient;
import com.danrod505.greenlantern.client.{pkg}.{cls}Client;
import com.danrod505.greenlantern.hero.HeroDefinition;
import com.danrod505.greenlantern.hero.HeroEnergy;
import com.danrod505.greenlantern.hero.HeroPowers;
import com.danrod505.greenlantern.hero.SuitModifier;
import com.danrod505.greenlantern.hero.SuitSet;
import com.danrod505.greenlantern.hero.WheelStyle;
import com.danrod505.greenlantern.hero.WheelTheme;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

/**
 * {h["name_en"]}: the {h["item_en"]} holds the {h["energy_en"]} and calls the suit. Made with the hero kit from
 * {{@code docs/heroes/{hid}.md}}; see {{@link HeroDefinition}} for every hook a hero can use (flight,
 * fall damage, damage taken, logout...).
 */
public final class {cls}Hero extends HeroDefinition {{
    /** The {h["energy_en"]}, kept in the {h["item_en"]}: it pays for the powers and comes back on its own. */
    public static final HeroEnergy ENERGY = new HeroEnergy({cls}Content.ENERGY, () -> {cls}Config.CAPACITY.get());

    public static final WheelStyle WHEEL = new WheelStyle(
            new WheelTheme("{hid}_wheel", {wheel_args}),
            GreenLantern.id("textures/gui/{hid}_powers.png"), {(n + 1) * 16}, "wheel.greenlantern.{energy}_cost", "tooltip.greenlantern.{energy}",
            {argb(dark(h["main"], 0.1), 0x66)}, {argb(h["glow"])}, {argb(h["accent"])}, 0xFFE8E8E8, 0xFFB0B0B0, {argb(h["glow"])}, 0xFFB0B0B0);

    public static final {cls}Hero INSTANCE = new {cls}Hero();

    private final HeroPowers<{cls}Power> powers = new HeroPowers<>({cls}Power.POWERS, ENERGY, ChatFormatting.GOLD,
            0.85F, 0.06F, {cls}Server::usePower, WHEEL);

    private {cls}Hero() {{
        super("{hid}", stack -> stack.is({cls}Content.ITEM.get()), new SuitSet({cls}Content.MASK, {cls}Content.SUIT, {cls}Content.LEGGINGS,
                {cls}Content.BOOTS, "message.greenlantern.no_{item}", {cls}Content.SUIT_UP, {cls}Content.SUIT_DOWN, List.of(
                        SuitModifier.add(Attributes.ATTACK_DAMAGE, "{hid}_strength", 3.0),
                        SuitModifier.add(Attributes.MAX_HEALTH, "{hid}_health", 8.0),
                        SuitModifier.multiply(Attributes.MOVEMENT_SPEED, "{hid}_speed", 0.15))));
    }}

    @Override
    public void register(FMLJavaModLoadingContext context) {{
        {cls}Content.register(context.getModBusGroup());
        context.registerConfig(ModConfig.Type.COMMON, {cls}Config.SPEC, "greenlantern-{hid}.toml");
    }}

    @Override
    public void creativeTabItems(CreativeModeTab.Output output) {{
        output.accept({cls}Content.ITEM.get().charged({cls}Content.ITEM.get().getDefaultInstance()));
        output.accept({cls}Content.ITEM.get());
    }}

    @Override
    public Supplier<HeroClient> client() {{
        return {cls}Client::new;
    }}

    @Override
    protected void suitUpEffects(ServerLevel level, ServerPlayer player) {{
        level.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 1.0, player.getZ(), 40, 0.45, 0.9, 0.45, 0.05);
    }}

    @Override
    protected void suitDownEffects(ServerLevel level, ServerPlayer player) {{
        level.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 1.0, player.getZ(), 16, 0.45, 0.9, 0.45, 0.05);
    }}

    @Override
    public HeroPowers<{cls}Power> powers() {{
        return powers;
    }}

    @Override
    public void tick(ServerPlayer player) {{
        {cls}Server.tick(player);
    }}

    @Override
    public void onLogout(ServerPlayer player) {{
        if (isSuited(player)) dismissSuit(player, false);
    }}
}}
''')
    cases = '\n'.join(f'            case {p["id"].upper()} -> placeholder(player, power);' for p in powers)
    write(os.path.join(JAVA, pkg, f'{cls}Server.java'), f'''package {base};

import com.danrod505.greenlantern.GreenLantern;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/** The server side of {h["name_en"]}: what each power does, and the {h["energy_en"]} coming back. */
public final class {cls}Server {{
    private static final int GLOW = {rgb(h["glow"])};

    private {cls}Server() {{}}

    /** Uses a power with the {h["item_en"]}; returns whether it went off. */
    public static boolean usePower(ServerPlayer player, ItemStack item, {cls}Power power) {{
        if (!{cls}Hero.INSTANCE.isSuited(player)) return false;
        if (player.getCooldowns().isOnCooldown(item)) return false;
        if (!player.isCreative() && !{cls}Hero.ENERGY.tryConsume(item, power.cost())) {{
            player.displayClientMessage(Component.translatable("message.greenlantern.no_{energy}").withStyle(ChatFormatting.RED), true);
            return false;
        }}
        // Each power still runs the placeholder: write the real one here (see the hero sheet).
        return switch (power) {{
{cases}
        }};
    }}

    /** Until the power is written: a flash of the hero's colour in front of them, a sound and a note in the action bar. */
    private static boolean placeholder(ServerPlayer player, {cls}Power power) {{
        ServerLevel level = player.level();
        Vec3 at = player.getEyePosition().add(player.getLookAngle().scale(1.5));
        level.sendParticles(new DustParticleOptions(GLOW, 1.5F), at.x, at.y, at.z, 30, 0.4, 0.4, 0.4, 0.0);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.6F, 1.6F);
        player.displayClientMessage(Component.translatable("message.greenlantern.power_todo", power.displayName()), true);
        player.getCooldowns().addCooldown(player.getMainHandItem(), 10);
        GreenLantern.LOGGER.debug("{hid}: placeholder power {{}}", power.id());
        return true;
    }}

    /** Every second the {h["energy_en"]} in the {h["item_en"]} comes back a little. */
    public static void tick(ServerPlayer player) {{
        if (player.tickCount % 20 != 0) return;
        ItemStack item = {cls}Hero.INSTANCE.findItem(player);
        if (!item.isEmpty()) {cls}Hero.ENERGY.add(item, {cls}Config.RECHARGE_PER_SECOND.get());
    }}
}}
''')
    # ---- Java: client ---------------------------------------------------------------------------
    write(os.path.join(JAVA, 'client', pkg, f'{cls}Client.java'), f'''package com.danrod505.greenlantern.client.{pkg};

import com.danrod505.greenlantern.GreenLantern;
import com.danrod505.greenlantern.client.GuideScreen;
import com.danrod505.greenlantern.client.HeroClient;
import com.danrod505.greenlantern.client.HeroHud;
import {base}.{cls}Content;
import {base}.{cls}Hero;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.client.event.AddGuiOverlayLayersEvent;
import net.minecraftforge.client.gui.overlay.ForgeLayeredDraw;

/** Client half of {h["name_en"]}: the standard hero HUD and the hero's section in the Heroes' Guide. */
public final class {cls}Client implements HeroClient {{
    /** Same grid as {{@code data/greenlantern/recipe/{item}.json}}. */
    private static final GuideScreen.Recipe RECIPE = new GuideScreen.Recipe("{item}", () -> new ItemStack({cls}Content.ITEM.get()), () -> GuideScreen.grid(
            Items.GOLD_INGOT, Items.ENDER_PEARL, Items.GOLD_INGOT,
            Items.IRON_INGOT, Items.DIAMOND, Items.IRON_INGOT,
            null, null, null));

    private final HeroHud hud = new HeroHud({cls}Hero.INSTANCE, {rgb(h["main"])}, {rgb(h["glow"])}, {argb(h["accent"], 0xC0)}, {argb(h["glow"])}, null);

    @Override
    public void addHud(AddGuiOverlayLayersEvent event) {{
        event.getLayeredDraw().add(ForgeLayeredDraw.POST_SLEEP_STACK, GreenLantern.id("{hid}_hud"), hud::render);
    }}

    @Override
    public List<GuideScreen.Section> guideSections() {{
        return List.of(new GuideScreen.Section("{hid}", () -> {cls}Content.ITEM.get().charged({cls}Content.ITEM.get().getDefaultInstance()), {argb(h["accent"])},
                List.of(GuideScreen.text("{hid}"), GuideScreen.text("{hid}_powers"), GuideScreen.text("controls_{hid}"), GuideScreen.recipes("{hid}", RECIPE))));
    }}
}}
''')
    # ---- texts and sounds ---------------------------------------------------------------------------
    powers_pt = '\n'.join(f'§6{p["pt"]}§r: {p["desc_pt"]}' for p in powers)
    powers_en = '\n'.join(f'§6{p["en"]}§r: {p["desc_en"]}' for p in powers)
    pt = {
        f'item.{MODID}.{item}': h['item_pt'],
        f'item.{MODID}.{hid}_mask': f'Máscara {of_hero} {h["name_pt"]}',
        f'item.{MODID}.{hid}_suit': f'Traje {of_hero} {h["name_pt"]}',
        f'item.{MODID}.{hid}_leggings': f'Calças {of_hero} {h["name_pt"]}',
        f'item.{MODID}.{hid}_boots': f'Botas {of_hero} {h["name_pt"]}',
        f'message.{MODID}.no_{item}': f'Você precisa {of_item} {h["item_pt"]}',
        f'message.{MODID}.no_{energy}': f'Sem {h["energy_pt"].lower()}: espere ela voltar',
        f'tooltip.{MODID}.{energy}': f'{h["energy_pt"]}: %s / %s',
        f'tooltip.{MODID}.{hid}_hint': 'Botão direito: vestir o traje / usar poder · R: menu · V: usar',
        f'hud.{MODID}.{hid}_suit_hint': f'Aperte %s ou clique com o botão direito para vestir o traje {of_hero} {h["name_pt"]}',
        f'wheel.{MODID}.{energy}_cost': 'Custo: %s',
        f'subtitles.{MODID}.{hid}_suit_up': f'{h["name_pt"]} veste o traje',
        f'subtitles.{MODID}.{hid}_suit_down': f'{h["name_pt"]} tira o traje',
        f'guide.{MODID}.section.{hid}': h['name_pt'],
        f'guide.{MODID}.{hid}.title': h['name_pt'],
        f'guide.{MODID}.{hid}': f'§6{h["item_pt"]}§r\nFabrique {h["item_article_pt"]} §6{h["item_pt"]}§r e clique com o botão direito ou aperte §eG§r para vestir o traje {of_hero} {h["name_pt"]}.\n\n§6{h["energy_pt"]}§r\nOs poderes gastam {h["energy_pt"].lower()}, que volta sozinha aos poucos.',
        f'guide.{MODID}.{hid}_powers.title': 'Poderes',
        f'guide.{MODID}.{hid}_powers': powers_pt,
        f'guide.{MODID}.controls_{hid}.title': 'Controles',
        f'guide.{MODID}.controls_{hid}': f'§eBotão direito§r ({h["item_pt"]}): vestir o traje ou usar o poder.\n§eG§r: vestir / tirar o traje.\n§eR§r (segurar / tocar): menu de poderes / próximo poder.\n§eV§r: usa o poder selecionado.',
        f'guide.{MODID}.recipe.{item}': 'Barras de ouro nos cantos de cima com uma pérola do End no meio; embaixo, um diamante entre duas barras de ferro.',
    }
    en = {
        f'item.{MODID}.{item}': h['item_en'],
        f'item.{MODID}.{hid}_mask': f'{h["name_en"]}\'s Mask',
        f'item.{MODID}.{hid}_suit': f'{h["name_en"]}\'s Suit',
        f'item.{MODID}.{hid}_leggings': f'{h["name_en"]}\'s Leggings',
        f'item.{MODID}.{hid}_boots': f'{h["name_en"]}\'s Boots',
        f'message.{MODID}.no_{item}': f'You need the {h["item_en"]}',
        f'message.{MODID}.no_{energy}': f'Out of {h["energy_en"]}: wait for it to come back',
        f'tooltip.{MODID}.{energy}': f'{h["energy_en"]}: %s / %s',
        f'tooltip.{MODID}.{hid}_hint': 'Right-click: put on the suit / use power · R: menu · V: use',
        f'hud.{MODID}.{hid}_suit_hint': f'Press %s or right-click to put on {h["name_en"]}\'s suit',
        f'wheel.{MODID}.{energy}_cost': 'Cost: %s',
        f'subtitles.{MODID}.{hid}_suit_up': f'{h["name_en"]} suits up',
        f'subtitles.{MODID}.{hid}_suit_down': f'{h["name_en"]} takes the suit off',
        f'guide.{MODID}.section.{hid}': h['name_en'],
        f'guide.{MODID}.{hid}.title': h['name_en'],
        f'guide.{MODID}.{hid}': f'§6The {h["item_en"]}§r\nCraft the §6{h["item_en"]}§r and right-click or press §eG§r to put on {h["name_en"]}\'s suit.\n\n§6{h["energy_en"]}§r\nThe powers spend {h["energy_en"]}, which slowly comes back on its own.',
        f'guide.{MODID}.{hid}_powers.title': 'Powers',
        f'guide.{MODID}.{hid}_powers': powers_en,
        f'guide.{MODID}.controls_{hid}.title': 'Controls',
        f'guide.{MODID}.controls_{hid}': f'§eRight-click§r ({h["item_en"]}): put on the suit or use the power.\n§eG§r: put on / take off the suit.\n§eR§r (hold / tap): power menu / next power.\n§eV§r: uses the selected power.',
        f'guide.{MODID}.recipe.{item}': 'Gold ingots in the top corners with an ender pearl in the middle; below, a diamond between two iron ingots.',
    }
    for p in powers:
        pt[f'power.{MODID}.{p["id"]}'] = p['pt']
        pt[f'power.{MODID}.{p["id"]}.desc'] = p['desc_pt']
        en[f'power.{MODID}.{p["id"]}'] = p['en']
        en[f'power.{MODID}.{p["id"]}.desc'] = p['desc_en']
    write_json(os.path.join(HEROES, hid, 'lang/pt_br.json'), pt)
    write_json(os.path.join(HEROES, hid, 'lang/en_us.json'), en)
    # Placeholder suit sounds (vanilla ones) until the hero gets its own .ogg files.
    write_json(os.path.join(HEROES, hid, 'sounds.json'), {
        f'{hid}_suit_up': {'subtitle': f'subtitles.{MODID}.{hid}_suit_up', 'sounds': [{'name': 'minecraft:item.armor.equip_netherite', 'type': 'event'}]},
        f'{hid}_suit_down': {'subtitle': f'subtitles.{MODID}.{hid}_suit_down', 'sounds': [{'name': 'minecraft:item.armor.equip_generic', 'type': 'event'}]},
    })
    # ---- models, textures, recipe -----------------------------------------------------------------
    for name, shape in [(item, 'item'), (f'{hid}_mask', 'head'), (f'{hid}_suit', 'chest'), (f'{hid}_leggings', 'legs'), (f'{hid}_boots', 'feet')]:
        write_json(os.path.join(ASSETS, 'items', name + '.json'), {'model': {'type': 'minecraft:model', 'model': f'{MODID}:item/{name}'}})
        write_json(os.path.join(ASSETS, 'models/item', name + '.json'), {'parent': 'minecraft:item/generated', 'textures': {'layer0': f'{MODID}:item/{name}'}})
        png(os.path.join(ASSETS, 'textures/item', name + '.png'), 16, 16, item_pixels(h, shape))
        written.append(f'src/main/resources/assets/{MODID}/textures/item/{name}.png')
    write_json(os.path.join(ASSETS, 'equipment', hid + '.json'), {'layers': {
        'humanoid': [{'texture': f'{MODID}:{hid}'}], 'humanoid_leggings': [{'texture': f'{MODID}:{hid}'}]}})
    png(os.path.join(ASSETS, 'textures/entity/equipment/humanoid', hid + '.png'), 64, 32, armor_pixels(h, False))
    png(os.path.join(ASSETS, 'textures/entity/equipment/humanoid_leggings', hid + '.png'), 64, 32, armor_pixels(h, True))
    letters = [letter(p['pt']) for p in powers] + [letter(h['name_pt'])]
    icons = [icon_pixels(h, i, ch) for i, ch in enumerate(letters)]
    png(os.path.join(ASSETS, 'textures/gui', hid + '_powers.png'), 16 * (n + 1), 16, lambda x, y: icons[x // 16](x % 16, y))
    written.append(f'src/main/resources/assets/{MODID}/textures/entity/equipment/humanoid(_leggings)/{hid}.png, textures/gui/{hid}_powers.png')
    write_json(os.path.join(DATA, 'recipe', item + '.json'), {
        'type': 'minecraft:crafting_shaped', 'category': 'equipment', 'pattern': ['GEG', 'IDI'],
        'key': {'G': 'minecraft:gold_ingot', 'E': 'minecraft:ender_pearl', 'I': 'minecraft:iron_ingot', 'D': 'minecraft:diamond'},
        'result': {'id': f'{MODID}:{item}', 'count': 1}})
    write_json(os.path.join(DATA, 'advancement/recipes', item + '.json'), {
        'parent': 'minecraft:recipes/root',
        'criteria': {
            'has_pearl': {'trigger': 'minecraft:inventory_changed', 'conditions': {'items': [{'items': 'minecraft:ender_pearl'}]}},
            'has_the_recipe': {'trigger': 'minecraft:recipe_unlocked', 'conditions': {'recipe': f'{MODID}:{item}'}}},
        'requirements': [['has_pearl', 'has_the_recipe']],
        'rewards': {'recipes': [f'{MODID}:{item}']}})
    # ---- tests and screenshots ----------------------------------------------------------------------
    write(os.path.join(GAMETEST, 'heroes', f'{cls}Tests.java'), f'''package com.danrod505.greenlantern.gametest.heroes;

import static com.danrod505.greenlantern.gametest.GameTestKit.*;

import com.danrod505.greenlantern.gametest.HeroContractTests;
import {base}.{cls}Config;
import {base}.{cls}Hero;
import {base}.{cls}Power;
import java.util.function.Consumer;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;

/**
 * {h["name_en"]}. The hero contract tests already check the suit, the texts and that every power runs;
 * these check what is particular to the hero. Give each finished power its own test here (and a
 * {{@code test_instance/<name>.json}} next to the others).
 */
public final class {cls}Tests {{
    private {cls}Tests() {{}}

    public static void register(DeferredRegister<Consumer<GameTestHelper>> tests) {{
        tests.register("{hid}_energy_recharges", () -> {cls}Tests::energyRecharges);
        tests.register("{hid}_powers_spend_energy", () -> {cls}Tests::powersSpendEnergy);
    }}

    private static ItemStack giveItem(ServerPlayer player, int energy) {{
        ItemStack item = HeroContractTests.chargedItem({cls}Hero.INSTANCE);
        {cls}Hero.ENERGY.set(item, energy);
        player.setItemInHand(InteractionHand.MAIN_HAND, item);
        return player.getMainHandItem();
    }}

    public static void energyRecharges(GameTestHelper helper) {{
        ServerPlayer player = player(helper, 7.5, 1, 7.5, 0, 0);
        ItemStack item = giveItem(player, 0);
        {cls}Hero.INSTANCE.summonSuit(player);
        helper.startSequence()
                .thenExecuteFor(41, player::doTick)
                .thenExecute(() -> {{
                    int stored = {cls}Hero.ENERGY.get(item).stored();
                    int expected = 2 * {cls}Config.RECHARGE_PER_SECOND.get();
                    helper.assertTrue(stored >= expected, "the energy should come back every second, got " + stored + " (expected " + expected + ")");
                    remove(player);
                }})
                .thenSucceed();
    }}

    public static void powersSpendEnergy(GameTestHelper helper) {{
        for ({cls}Power power : {cls}Power.values()) {{
            ServerPlayer player = player(helper, 7.5, 1, 7.5, 0, 0);
            ItemStack item = giveItem(player, {cls}Hero.ENERGY.capacity());
            {cls}Hero.INSTANCE.summonSuit(player);
            helper.assertTrue({cls}Hero.INSTANCE.powers().use(player, player.getMainHandItem(), power.ordinal()), power.id() + " should go off");
            int stored = {cls}Hero.ENERGY.get(player.getMainHandItem()).stored();
            helper.assertTrue(stored == {cls}Hero.ENERGY.capacity() - power.cost(), power.id() + " should cost " + power.cost() + ", left " + stored);
            remove(player);
        }}
        helper.succeed();
    }}
}}
''')
    for test, ticks in ((f'{hid}_energy_recharges', 200), (f'{hid}_powers_spend_energy', 200)):
        write_json(os.path.join(ROOT, 'src/gametest/resources/data', MODID, 'test_instance', test + '.json'), {
            'type': 'minecraft:function', 'function': f'{MODID}:{test}', 'environment': 'minecraft:default',
            'structure': f'{MODID}:arena', 'max_ticks': ticks, 'required': True})
    code = re.sub(r'[^a-z]', '', hid)[:3] or 'hx'
    power_steps = '\n'.join(f'''        step(20, () -> power({cls}Power.{p["id"].upper()}));
        step(6, () -> shot("{code}{i + 2:02d}_{p["id"]}"));''' for i, p in enumerate(powers))
    write(os.path.join(GAMETEST, 'scripts', f'{cls}Script.java'), f'''package com.danrod505.greenlantern.gametest.scripts;

import static com.danrod505.greenlantern.gametest.ClientScript.*;

import com.danrod505.greenlantern.GreenLantern;
import com.danrod505.greenlantern.client.GuideScreen;
import com.danrod505.greenlantern.client.KeyBindings;
import com.danrod505.greenlantern.gametest.ClientScript;
import {base}.{cls}Content;
import {base}.{cls}Hero;
import {base}.{cls}Power;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;

/**
 * Screenshot script "{pkg}": {h["name_en"]}'s suit (front, back, side), the HUD, every power, the power
 * wheel and the guide. Made by the hero kit: add the mobility and the epic moment as they are built.
 */
public final class {cls}Script {{
    public static final ClientScript.Script SCRIPT = new ClientScript.Script("{pkg}", {cls}Script::build, {cls}Script::log, false);

    private {cls}Script() {{}}

    private static void power({cls}Power power) {{
        server(sp -> {{
            ItemStack item = {cls}Hero.INSTANCE.findItem(sp);
            sp.getCooldowns().removeCooldown(sp.getCooldowns().getCooldownGroup(item));
            boolean used = {cls}Hero.INSTANCE.powers().use(sp, item, power.ordinal());
            GreenLantern.LOGGER.info("CLIENTSCRIPT power {{}} used={{}}", power.id(), used);
        }});
    }}

    private static void build() {{
        step(60, () -> {{
            command("time set 6000");
            command("weather clear");
            command("gamerule advance_time false");
            command("gamerule spawn_mobs false");
            command("gamerule advance_weather false");
            server(sp -> {{
                sp.setGameMode(GameType.SURVIVAL);
                sp.setItemInHand(InteractionHand.MAIN_HAND, {cls}Content.ITEM.get().charged(new ItemStack({cls}Content.ITEM.get())));
                for (int i = -1; i <= 1; i++) {{
                    var zombie = EntityType.ZOMBIE.create(sp.level(), EntitySpawnReason.COMMAND);
                    zombie.snapTo(sp.getX() + i * 2.5, sp.getY(), sp.getZ() + 8.5, 180, 0);
                    zombie.setNoAi(true);
                    zombie.setPersistenceRequired();
                    sp.level().addFreshEntity(zombie);
                }}
                {cls}Hero.INSTANCE.summonSuit(sp);
            }});
            look(0, 5);
        }});
        step(40, () -> {{
            camera(CameraType.THIRD_PERSON_FRONT);
            look(0, 10);
        }});
        step(10, () -> clean("{code}00_suit_front"));
        step(2, () -> camera(CameraType.THIRD_PERSON_BACK));
        step(4, () -> clean("{code}00b_suit_back"));
        step(2, () -> look(90, 15));
        step(4, () -> clean("{code}00c_suit_side"));
        step(2, () -> {{
            mc().options.hideGui = false;
            mc().gui.getChat().clearMessages(false);
            look(0, 10);
        }});
        step(4, () -> shot("{code}01_hud"));
{power_steps}
        // Power wheel.
        step(20, () -> key(KeyBindings.CONSTRUCT_WHEEL, true));
        step(8, () -> {{
            var screen = mc().screen;
            if (screen == null) return;
            double angle = Math.toRadians(-90 + 45);
            screen.mouseMoved(screen.width / 2.0 + Math.cos(angle) * 60, screen.height * 0.46 + Math.sin(angle) * 60);
        }});
        step(6, () -> shot("{code}{n + 2:02d}_power_wheel"));
        step(1, () -> {{
            key(KeyBindings.CONSTRUCT_WHEEL, false);
            if (mc().screen != null) mc().screen.keyReleased(new net.minecraft.client.input.KeyEvent(org.lwjgl.glfw.GLFW.GLFW_KEY_R, 0, 0));
        }});
        // The guide.
        step(5, () -> mc().setScreen(GuideScreen.atChapter("{hid}")));
        step(10, () -> shot("{code}{n + 3:02d}_guide"));
        step(2, () -> mc().setScreen(GuideScreen.atChapter("{hid}_powers")));
        step(10, () -> shot("{code}{n + 3:02d}b_guide_powers"));
        step(20, () -> mc().stop());
    }}

    /** Logged every 10 ticks while the script runs. */
    private static void log(Minecraft mc) {{
        ItemStack item = {cls}Hero.INSTANCE.findItem(mc.player);
        GreenLantern.LOGGER.info("CLIENTSCRIPT t={{}} pos={{}} suited={{}} energy={{}}", mc.player.tickCount, mc.player.blockPosition(),
                {cls}Hero.INSTANCE.isSuited(mc.player), item.isEmpty() ? -1 : {cls}Hero.ENERGY.get(item).stored());
    }}
}}
''')
    # ---- the hero sheet stays where it is; one line in each list ------------------------------------
    add_to_registry(h)
    insert_before_marker(os.path.join(GAMETEST, 'ModGameTests.java'), f'        {cls}Tests.register(TESTS);',
                         f'import com.danrod505.greenlantern.gametest.heroes.{cls}Tests;')
    clients = os.path.join(GAMETEST, 'scripts/ClientScripts.java')
    text = open(clients, encoding='utf-8').read()
    text = re.sub(r'(\w+Script\.SCRIPT)(\n\s+// ' + re.escape(MARKER) + ')', r'\1,\n            ' + cls + r'Script.SCRIPT\2', text)
    with open(clients, 'w', encoding='utf-8', newline='\n') as f:
        f.write(text)
    written.append('src/gametest/java/com/danrod505/greenlantern/gametest/scripts/ClientScripts.java (+1 line)')
    mapping = os.path.join(ROOT, '.github/screenshot-scripts.txt')
    with open(mapping, 'a', encoding='utf-8', newline='\n') as f:
        f.write(f'claude/{hid.replace("_", "-")}- {pkg}\n')
    written.append('.github/screenshot-scripts.txt (+1 line)')
    return code


def main():
    parser = argparse.ArgumentParser(description='Creates the skeleton of a new hero from its hero sheet (docs/heroes/<id>.md).')
    parser.add_argument('sheet', help='the approved hero sheet, e.g. docs/heroes/cyborg.md')
    parser.add_argument('--check', action='store_true', help='only read and check the sheet, write nothing')
    args = parser.parse_args()
    hero = read_sheet(args.sheet)
    check_free(hero)
    if args.check:
        print(f'new_hero: the sheet of {hero["name_en"]} ({hero["id"]}) is fine: {len(hero["powers"])} powers')
        return
    code = generate(hero)
    print(f'new_hero: created {hero["name_en"]} ({hero["id"]}):')
    for path in written:
        print('  ' + path)
    print(f'''
Next:
  ./gradlew compileJava -Pgametests                      (it compiles as it is)
  ./gradlew runGameTestServer -Pgametests --no-configuration-cache
                                                         (hero_contracts, hero_suits_contract, hero_powers_contract
                                                          and {hero["id"]}_* must pass)
  GL_CLIENT_SCRIPT={hero["pkg"]} screenshots: push to a branch named claude/{hero["id"].replace("_", "-")}-...
Then write each power in {hero["cls"]}Server.usePower, its own sounds and textures, and its tests.''')


if __name__ == '__main__':
    main()
