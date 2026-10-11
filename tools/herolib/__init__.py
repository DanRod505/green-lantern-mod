"""Shared helpers for the art and sound generators of each hero (tools/generate_<id>_textures.py and
tools/generate_<id>_audio.py).

  from herolib.pixel import Palette, Icon, ...     original pixel art: items, suit layers, power icons
  from herolib.audio import Theme, whoosh, ...     original synthesized sounds and two-layer themes

The heroes made before the library keep their own scripts; new heroes start from the scripts that
tools/new_hero.py writes, which already use it.
"""
