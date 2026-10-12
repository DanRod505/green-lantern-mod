#!/usr/bin/env python3
"""Versions and releases of the mod.

  python3 tools/release.py version              prints mod_version from gradle.properties
  python3 tools/release.py bump major|minor|patch  raises mod_version (a new hero is minor, a fix is patch, a new engine is major)
  python3 tools/release.py notes [version]      prints the release notes: the README section "(novo na <version>)"
  python3 tools/release.py check                every hero with a sheet in docs/heroes is in the mod description
                                                and has its README section

The Release workflow (.github/workflows/release.yml) publishes a GitHub Release with the jar and the
CurseForge profile whenever main has a mod_version that has no tag yet. tools/new_hero.py uses the
functions here to raise the version, add the hero to the mod description and start its README section.
"""
import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
PROPERTIES = os.path.join(ROOT, 'gradle.properties')
README = os.path.join(ROOT, 'README.md')
SHEETS = os.path.join(ROOT, 'docs/heroes')


def fail(message):
    print('release: ' + message, file=sys.stderr)
    sys.exit(1)


def read(path):
    return open(path, encoding='utf-8').read()


def write(path, text):
    with open(path, 'w', encoding='utf-8', newline='\n') as f:
        f.write(text)


def prop(name):
    m = re.search(rf'^{name}=(.*)$', read(PROPERTIES), re.M)
    if not m:
        fail(f'{name} is missing in gradle.properties')
    return m.group(1).strip()


def set_prop(name, value):
    text = read(PROPERTIES)
    text, n = re.subn(rf'^{name}=.*$', lambda _: f'{name}={value}', text, count=1, flags=re.M)
    if n != 1:
        fail(f'{name} is missing in gradle.properties')
    write(PROPERTIES, text)


def version():
    v = prop('mod_version')
    if not re.fullmatch(r'\d+\.\d+\.\d+', v):
        fail(f'mod_version must look like 1.17.0 (got {v!r})')
    return v


def bump(part):
    major, minor, patch = (int(x) for x in version().split('.'))
    if part == 'major':
        new = f'{major + 1}.0.0'
    elif part == 'minor':
        new = f'{major}.{minor + 1}.0'
    else:
        new = f'{major}.{minor}.{patch + 1}'
    set_prop('mod_version', new)
    return new


def add_to_description(sentence):
    """Appends one sentence about a hero to mod_description (shown in the game's mod list)."""
    description = prop('mod_description')
    if sentence not in description:
        set_prop('mod_description', description.rstrip() + ' ' + sentence)


def add_readme_section(heading, body):
    """Puts a "## <heading>" section above the first hero section of the README (newest first)."""
    text = read(README)
    if f'## {heading}\n' in text:
        return
    m = re.search(r'^## ', text, re.M)
    if not m:
        fail('README.md has no "## " section')
    write(README, text[:m.start()] + f'## {heading}\n\n{body.rstrip()}\n\n' + text[m.start():])


def notes(v):
    """Release notes for version v: its README section, plus how to install."""
    text = read(README)
    sections = re.split(r'^(?=## )', text, flags=re.M)
    news = [s.strip() for s in sections if re.match(rf'## .*\(novo na {re.escape(v)}\)', s)]
    mc = prop('minecraft_version')
    forge = prop('forge_version')
    out = [f'Heróis do Universo DC {v} para Minecraft {mc} com Forge {forge}.', '']
    out += news or ['Correções e melhorias internas, sem mudanças nos heróis.']
    out += ['', '## Como instalar', '',
            f'* **CurseForge:** baixe `greenlantern-{mc}-{v}-curseforge-profile.zip` aqui embaixo e, no app do CurseForge, '
            'use **Create Custom Profile > Import**. O perfil já vem com o Minecraft, o Forge e o mod.',
            f'* **Só o mod:** coloque `greenlantern-{mc}-{v}.jar` na pasta `mods` de uma instalação com o Forge {forge}.']
    return '\n'.join(out) + '\n'


def sheets():
    """Heroes with a sheet in docs/heroes: (file, English name, Portuguese name)."""
    found = []
    for name in sorted(os.listdir(SHEETS)):
        if not name.endswith('.md') or name.startswith('_') or name == 'README.md':
            continue
        text = read(os.path.join(SHEETS, name))
        en = re.search(r'^\| Nome \(en\) \| (.+?) \|', text, re.M)
        pt = re.search(r'^\| Nome \(pt\) \| (.+?) \|', text, re.M)
        if en and pt:
            found.append((name, en.group(1).strip(), pt.group(1).strip()))
    return found


def check():
    description = prop('mod_description')
    readme = read(README)
    problems = []
    for name, en, pt in sheets():
        if f'{en}:' not in description:
            problems.append(f'{name}: "{en}:" is missing in mod_description (gradle.properties)')
        if not re.search(rf'^## .*{re.escape(pt)}.*\(novo na \d+\.\d+\.\d+\)', readme, re.M):
            problems.append(f'{name}: README.md has no "## ... {pt} (novo na <version>)" section')
    for p in problems:
        print('release: ' + p, file=sys.stderr)
    if problems:
        sys.exit(1)
    print(f'release: mod {version()}, {len(sheets())} heroes with a sheet are in the description and the README')


def main():
    args = sys.argv[1:]
    if args == ['version']:
        print(version())
    elif len(args) == 2 and args[0] == 'bump' and args[1] in ('major', 'minor', 'patch'):
        print(bump(args[1]))
    elif args[:1] == ['notes'] and len(args) <= 2:
        sys.stdout.write(notes(args[1] if len(args) == 2 else version()))
    elif args == ['check']:
        check()
    else:
        print(__doc__, file=sys.stderr)
        sys.exit(2)


if __name__ == '__main__':
    main()
