package com.danrod505.greenlantern.engine.preset;

import com.danrod505.greenlantern.GreenLantern;
import com.danrod505.greenlantern.engine.CharacterData;
import com.danrod505.greenlantern.engine.EngineContent;
import com.danrod505.greenlantern.engine.EngineHero;
import com.danrod505.greenlantern.hero.power.Fx;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

/**
 * The 15 transformations of the catalog: the animation (about 2 seconds) that plays when a character
 * of the engine puts its suit on. A character names one in its file ({@code "transformacao"}) and
 * sets it up under {@code "visual": {"transformacao": {...}}}: {@code cor} and {@code cor2} (the
 * character's main and second colours by default), {@code som} (a sound id, or {@code "nenhum"}),
 * {@code duracao} (seconds), {@code tipo} (the animals of the shadows, the skin of the beast, the
 * fun of the show) and {@code elemento} (of the element one). {@code "padrao"} plays none.
 * <ul>
 * <li>{@code raio} Raio Mágico: a bolt falls on the player, a white flash.</li>
 * <li>{@code anel} Luz do Anel: the colour climbs the body from the feet, the oath on the screen.</li>
 * <li>{@code sol} Sol de Krypton: a beam of sunlight, the symbol glows, the cape opens in the wind.</li>
 * <li>{@code velocidade} Vulto Veloz: a spinning blur of lightning.</li>
 * <li>{@code sombras} Das Sombras: smoke and bats (or cats, or birds) cover the player.</li>
 * <li>{@code armadura} Armadura em Montagem: the suit pieces fly in one by one, the visor lights last.</li>
 * <li>{@code mistico} Círculo Místico: runes in a circle on the ground, smoke rising.</li>
 * <li>{@code divino} Bênção Divina: golden light and olive leaves from the sky.</li>
 * <li>{@code mare} Maré: a column of water rises around the player and falls in foam.</li>
 * <li>{@code elemento} Elemento: the element swirls in and settles on the body.</li>
 * <li>{@code fera} Fera: the body shakes and grows with a roar; fur, scales or clay.</li>
 * <li>{@code tubo} Tubo de Explosão: a Boom Tube opens behind, the player goes through and back.</li>
 * <li>{@code arsenal} Arsenal: the weapons spin around and drop into place, the mask last.</li>
 * <li>{@code espetaculo} Espetáculo: confetti, cards, coloured smoke or fear gas, and a laugh.</li>
 * <li>{@code asas} Asas de Metal Nth: wings open on the back and the player rises a little.</li>
 * </ul>
 */
public final class Transformations {
    public static final String NONE = "padrao";
    public static final List<String> IDS = List.of("raio", "anel", "sol", "velocidade", "sombras", "armadura", "mistico", "divino", "mare",
            "elemento", "fera", "tubo", "arsenal", "espetaculo", "asas");
    private static final Identifier GROW = GreenLantern.id("transformation_grow");

    /** What a transformation draws: everything it needs about the player and its settings. */
    record Look(ServerPlayer player, EngineHero hero, String id, int color, int second, int light, int ticks, String kind, String element) {
        ServerLevel level() {
            return player.level();
        }

        Vec3 feet() {
            return player.position();
        }

        /** Where the player faces, flat. */
        Vec3 forward() {
            Vec3 look = player.getLookAngle();
            Vec3 flat = new Vec3(look.x, 0, look.z);
            return flat.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 1) : flat.normalize();
        }

        Vec3 right() {
            Vec3 f = forward();
            return new Vec3(-f.z, 0, f.x);
        }

        void particle(ParticleOptions particle, Vec3 at, int count, double spread, double speed) {
            level().sendParticles(particle, at.x, at.y, at.z, count, spread, spread, spread, speed);
        }

        void play(SoundEvent sound, float volume, float pitch) {
            Fx.play(level(), player, sound, volume, pitch);
        }
    }

    /** One run of a transformation; what it put in the world goes when it ends. */
    private abstract static class Animation {
        final List<Entity> spawned = new ArrayList<>();

        abstract void tick(Look l, int age, float progress);

        void end(Look l) {
            spawned.forEach(Summons::dismiss);
        }
    }

    private static final Map<String, Supplier<Animation>> ANIMATIONS = Map.ofEntries(
            Map.entry("raio", Lightning::new), Map.entry("anel", Ring::new), Map.entry("sol", Sun::new), Map.entry("velocidade", Blur::new),
            Map.entry("sombras", Shadows::new), Map.entry("armadura", Armor::new), Map.entry("mistico", Mystic::new),
            Map.entry("divino", Divine::new), Map.entry("mare", Tide::new), Map.entry("elemento", Element::new), Map.entry("fera", Beast::new),
            Map.entry("tubo", BoomTube::new), Map.entry("arsenal", Arsenal::new), Map.entry("espetaculo", Show::new), Map.entry("asas", Wings::new));

    private Transformations() {}

    /** Whether {@code id} is one of the 15 (or {@code padrao}, none). */
    public static boolean exists(String id) {
        return id.equals(NONE) || ANIMATIONS.containsKey(id);
    }

    /** Plays the character's transformation on {@code player}. */
    public static void play(ServerPlayer player, EngineHero hero) {
        play(player, hero, hero.data().transformation());
    }

    /** Plays transformation {@code id} on {@code player} with the character's settings; false when it is none or unknown. */
    public static boolean play(ServerPlayer player, EngineHero hero, String id) {
        Supplier<Animation> make = ANIMATIONS.get(id);
        if (make == null) return false;
        CharacterData data = hero.data();
        CharacterData.Settings s = data.visual().transformation();
        int ticks = Mth.clamp((int) Math.round(s.number("duracao", 2.0) * 20), 10, 100);
        String element = s.text("elemento", data.source().filter(PresetFx::known).orElse("fogo"));
        Look look = new Look(player, hero, id, s.color("cor", data.colors().main()), s.color("cor2", data.colors().second()), data.colors().light(),
                ticks, s.text("tipo", ""), element);
        sound(s, id).ifPresent(sound -> look.play(sound, 1.2F, 1.0F));
        Animation animation = make.get();
        PresetRuntime.start(player, PresetRuntime.Task.ending(age -> {
            if (!player.isAlive()) return false;
            animation.tick(look, age, Math.min(1.0F, (float) age / ticks));
            return age < ticks;
        }, () -> animation.end(look)));
        return true;
    }

    private static Optional<SoundEvent> sound(CharacterData.Settings s, String id) {
        String chosen = s.text("som", "");
        if (chosen.equals("nenhum")) return Optional.empty();
        if (!chosen.isEmpty()) {
            Identifier name = Identifier.tryParse(chosen);
            if (name != null) return Optional.of(BuiltInRegistries.SOUND_EVENT.getOptional(name).orElseGet(() -> SoundEvent.createVariableRangeEvent(name)));
        }
        return Optional.of(EngineContent.TRANSFORM_SOUNDS.get(id).get());
    }

    /** Points on a flat circle around {@code center}, turned by {@code turn} radians. */
    private static void circle(Look l, ParticleOptions particle, Vec3 center, double radius, int points, double turn) {
        for (int i = 0; i < points; i++) {
            double a = turn + i * Math.PI * 2 / points;
            l.particle(particle, center.add(Math.cos(a) * radius, 0, Math.sin(a) * radius), 1, 0, 0);
        }
    }

    // ---- the 15 ---------------------------------------------------------------------------------------

    /** Raio Mágico: sparks gather above, the bolt falls, a white flash. */
    private static final class Lightning extends Animation {
        @Override
        void tick(Look l, int age, float p) {
            Vec3 feet = l.feet();
            int strike = (int) (l.ticks() * 0.7);
            if (age < strike) {
                l.particle(ParticleTypes.ELECTRIC_SPARK, feet.add(0, 3.5 + l.level().random.nextDouble(), 0), 3, 1.2, 0.05);
                if (age % 3 == 0) l.particle(ParticleTypes.LARGE_SMOKE, feet.add(0, 5, 0), 4, 1.5, 0.0);
            } else if (age == strike) {
                LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(l.level(), EntitySpawnReason.TRIGGERED);
                if (bolt != null) {
                    bolt.snapTo(feet);
                    bolt.setVisualOnly(true);
                    l.level().addFreshEntity(bolt);
                }
                Vec3 middle = feet.add(0, 1, 0);
                l.particle(ParticleTypes.END_ROD, middle, 40, 0.6, 0.25);
                l.particle(ParticleTypes.FIREWORK, middle, 30, 0.5, 0.2);
                l.particle(PresetFx.dust(l.second(), 1.4F), middle, 30, 0.6, 0.0);
                circle(l, ParticleTypes.ELECTRIC_SPARK, feet.add(0, 0.1, 0), 2.5, 28, 0);
            }
        }
    }

    /** Luz do Anel: the colour climbs the body from the feet; the oath on the screen. */
    private static final class Ring extends Animation {
        @Override
        void tick(Look l, int age, float p) {
            if (age == 1 && l.player().connection != null) {
                Component oath = Component.translatable("transformation.greenlantern.oath").withColor(l.color());
                l.player().connection.send(new ClientboundSetTitlesAnimationPacket(5, l.ticks(), 15));
                l.player().connection.send(new ClientboundSetTitleTextPacket(Component.empty()));
                l.player().connection.send(new ClientboundSetSubtitleTextPacket(oath));
            }
            Vec3 feet = l.feet();
            double height = 2.0 * p;
            circle(l, PresetFx.dust(l.color(), 1.0F), feet.add(0, height, 0), 0.55, 14, age * 0.4);
            l.particle(PresetFx.dust(l.color(), 0.8F), feet.add(0, height * 0.5, 0), 4, 0.25, 0.0);
            if (age % 4 == 0) l.particle(PresetFx.dust(l.light(), 0.6F), feet.add(0, height, 0), 3, 0.3, 0.0);
            if (age == l.ticks()) {
                l.particle(ParticleTypes.END_ROD, feet.add(0, 1, 0), 24, 0.5, 0.15);
                l.particle(PresetFx.dust(l.color(), 1.5F), feet.add(0, 1, 0), 30, 0.6, 0.0);
            }
        }
    }

    /** Sol de Krypton: a beam of sunlight, the symbol glows on the chest, the cape opens in the wind. */
    private static final class Sun extends Animation {
        @Override
        void tick(Look l, int age, float p) {
            Vec3 feet = l.feet();
            if (p < 0.75F) {
                for (int i = 0; i < 6; i++) {
                    double y = l.level().random.nextDouble() * 14;
                    l.particle(i % 2 == 0 ? ParticleTypes.END_ROD : PresetFx.dust(0xFFE58A, 1.5F), feet.add(0, y, 0), 1, 0.25, 0.0);
                }
            }
            Vec3 chest = feet.add(0, 1.3, 0).add(l.forward().scale(0.35));
            if (p > 0.45F && p < 0.8F) l.particle(PresetFx.dust(l.color(), 0.9F), chest, 4, 0.12, 0.0);
            if (p > 0.6F) {
                Vec3 back = feet.add(0, 1.2, 0).subtract(l.forward().scale(0.4));
                Vec3 wind = l.forward().scale(-0.35);
                l.level().sendParticles(ParticleTypes.CLOUD, back.x, back.y, back.z, 0, wind.x, 0.05, wind.z, 1.0);
                l.level().sendParticles(PresetFx.dust(l.second(), 1.2F), back.x, back.y - 0.3, back.z, 3, 0.3, 0.3, 0.3, 0.0);
            }
        }
    }

    /** Vulto Veloz: the player spins in a blur of lightning of the speedster's colour. */
    private static final class Blur extends Animation {
        @Override
        void tick(Look l, int age, float p) {
            Vec3 feet = l.feet();
            double spin = age * (0.6 + p * 1.2);
            for (int i = 0; i < 4; i++) {
                double a = spin + i * Math.PI / 2;
                Vec3 at = feet.add(Math.cos(a) * 0.8, 0.2 + ((age * 0.15 + i * 0.5) % 2.0), Math.sin(a) * 0.8);
                l.particle(ParticleTypes.ELECTRIC_SPARK, at, 1, 0.02, 0.0);
                l.particle(PresetFx.dust(l.color(), 1.0F), at, 1, 0.05, 0.0);
            }
            if (age == l.ticks()) {
                l.particle(ParticleTypes.ELECTRIC_SPARK, feet.add(0, 1, 0), 30, 0.6, 0.3);
                l.particle(ParticleTypes.CLOUD, feet.add(0, 0.1, 0), 12, 0.6, 0.05);
            }
        }
    }

    /** Das Sombras: smoke and bats (tipo gatos: cats, passaros: birds) cover the player and go. */
    private static final class Shadows extends Animation {
        @Override
        void tick(Look l, int age, float p) {
            Vec3 feet = l.feet();
            if (age == 1) {
                EntityType<?> type = switch (l.kind()) {
                    case "gatos" -> EntityType.CAT;
                    case "passaros" -> EntityType.PARROT;
                    default -> EntityType.BAT;
                };
                int count = type == EntityType.BAT ? 6 : 3;
                for (int i = 0; i < count; i++) {
                    Entity animal = type.create(l.level(), EntitySpawnReason.TRIGGERED);
                    if (animal == null) continue;
                    double a = i * Math.PI * 2 / count;
                    animal.snapTo(feet.add(Math.cos(a) * 0.8, 0.6 + i % 2, Math.sin(a) * 0.8));
                    animal.setInvulnerable(true);
                    spawned.add(Summons.spawn(l.player(), animal));
                }
            }
            if (p < 0.85F) {
                l.particle(ParticleTypes.LARGE_SMOKE, feet.add(0, 1, 0), 5, 0.45, 0.01);
                l.particle(ParticleTypes.SQUID_INK, feet.add(0, 1, 0), 2, 0.4, 0.01);
                l.particle(PresetFx.dust(0x101018, 2.0F), feet.add(0, 1, 0), 4, 0.5, 0.0);
            }
            if (age == l.ticks()) l.particle(ParticleTypes.POOF, feet.add(0, 1, 0), 25, 0.6, 0.08);
        }
    }

    /** Armadura em Montagem: the suit pieces fly from the item onto the body, one at a time; the visor lights last. */
    private static final class Armor extends Animation {
        private final List<Display.ItemDisplay> pieces = new ArrayList<>();

        @Override
        void tick(Look l, int age, float p) {
            List<Supplier<? extends Item>> order = List.of(l.hero().suit().feet(), l.hero().suit().legs(), l.hero().suit().chest(), l.hero().suit().head());
            double[] heights = {0.15, 0.7, 1.25, 1.75};
            int step = Math.max(2, (int) (l.ticks() * 0.8) / order.size());
            int flight = Math.max(2, step - 1);
            Vec3 feet = l.feet();
            if (age == 1) {
                for (int i = 0; i < order.size(); i++) {
                    double a = i * Math.PI / 2 + Math.PI / 4;
                    Vec3 start = feet.add(l.forward().scale(1.8)).add(Math.cos(a) * 0.9, 1.0 + Math.sin(a) * 0.6, 0);
                    Display.ItemDisplay piece = Displays.item(l.player(), new ItemStack(order.get(i).get()), start, 0.7F, true);
                    pieces.add(piece);
                    if (piece != null) spawned.add(piece);
                }
            }
            for (int i = 0; i < pieces.size(); i++) {
                Display.ItemDisplay piece = pieces.get(i);
                if (piece == null || piece.isRemoved()) continue;
                int from = 2 + i * step;
                if (age < from) continue;
                float t = Math.min(1.0F, (age - from) / (float) flight);
                Vec3 target = feet.add(0, heights[i], 0);
                piece.setPos(piece.position().lerp(target, t));
                if (t >= 1.0F) {
                    l.particle(ParticleTypes.ELECTRIC_SPARK, target, 12, 0.25, 0.15);
                    l.particle(ParticleTypes.CRIT, target, 8, 0.25, 0.2);
                    l.play(SoundEvents.ARMOR_EQUIP_IRON.value(), 0.7F, 1.2F + 0.1F * i);
                    Summons.dismiss(piece);
                }
            }
            if (age == l.ticks()) {
                Vec3 visor = l.player().getEyePosition().add(l.forward().scale(0.3));
                l.particle(PresetFx.dust(l.light(), 0.8F), visor, 10, 0.12, 0.0);
                l.particle(ParticleTypes.END_ROD, visor, 6, 0.1, 0.05);
            }
        }
    }

    /** Círculo Místico: runes turn in a circle on the ground and smoke rises around the player. */
    private static final class Mystic extends Animation {
        @Override
        void tick(Look l, int age, float p) {
            Vec3 ground = l.feet().add(0, 0.08, 0);
            circle(l, PresetFx.dust(l.color(), 1.0F), ground, 1.6, 24, age * 0.08);
            circle(l, PresetFx.dust(l.second(), 0.8F), ground, 1.1, 5, -age * 0.12);
            if (age % 2 == 0) {
                for (int i = 0; i < 5; i++) { // a star of lines between the points of the inner circle
                    double a = -age * 0.12 + i * Math.PI * 2 / 5;
                    double b = a + Math.PI * 4 / 5;
                    Vec3 from = ground.add(Math.cos(a) * 1.1, 0, Math.sin(a) * 1.1);
                    Vec3 to = ground.add(Math.cos(b) * 1.1, 0, Math.sin(b) * 1.1);
                    Fx.line(l.level(), from, to, PresetFx.dust(l.light(), 0.5F), 3);
                }
            }
            l.particle(ParticleTypes.ENCHANT, ground.add(0, 1.0, 0), 6, 0.8, 0.5);
            if (p > 0.3F) l.particle(ParticleTypes.LARGE_SMOKE, ground.add(0, 0.3, 0), (int) (p * 6), 0.5, 0.03);
            if (age == l.ticks()) l.particle(ParticleTypes.WITCH, ground.add(0, 1, 0), 30, 0.5, 0.1);
        }
    }

    /** Bênção Divina: golden light comes down from the sky with olive leaves; a flash of the armor. */
    private static final class Divine extends Animation {
        @Override
        void tick(Look l, int age, float p) {
            Vec3 feet = l.feet();
            for (int i = 0; i < 4; i++) {
                Vec3 from = feet.add((l.level().random.nextDouble() - 0.5) * 2.4, 3 + l.level().random.nextDouble() * 4, (l.level().random.nextDouble() - 0.5) * 2.4);
                l.level().sendParticles(PresetFx.dust(0xFFD86B, 1.3F), from.x, from.y, from.z, 0, 0, -1, 0, 0.3);
                if (i == 0) l.level().sendParticles(ParticleTypes.END_ROD, from.x, from.y, from.z, 0, 0, -1, 0, 0.15);
            }
            if (age % 2 == 0) {
                Vec3 leaf = feet.add((l.level().random.nextDouble() - 0.5) * 3, 2.5, (l.level().random.nextDouble() - 0.5) * 3);
                l.particle(PresetFx.dust(0x7A8C3A, 0.9F), leaf, 2, 0.2, 0.0);
            }
            if (age == l.ticks()) {
                l.particle(ParticleTypes.FIREWORK, feet.add(0, 1, 0), 30, 0.5, 0.15);
                l.particle(PresetFx.dust(0xFFD86B, 1.6F), feet.add(0, 1, 0), 30, 0.6, 0.0);
                l.play(SoundEvents.ARMOR_EQUIP_GOLD.value(), 1.0F, 1.0F);
            }
        }
    }

    /** Maré: a column of water rises around the player and falls in foam. */
    private static final class Tide extends Animation {
        @Override
        void tick(Look l, int age, float p) {
            Vec3 feet = l.feet();
            int fall = (int) (l.ticks() * 0.75);
            if (age < fall) {
                double top = 2.8 * age / fall;
                for (double y = 0; y < top; y += 0.5) {
                    circle(l, y % 1.0 < 0.5 ? ParticleTypes.SPLASH : PresetFx.dust(0x3A7BD5, 1.2F), feet.add(0, y, 0), 0.95, 10, age * 0.3 + y);
                }
                l.particle(ParticleTypes.FALLING_WATER, feet.add(0, top, 0), 6, 0.6, 0.0);
            } else if (age == fall) {
                l.particle(ParticleTypes.SPLASH, feet.add(0, 1.5, 0), 80, 1.0, 0.3);
                l.particle(ParticleTypes.CLOUD, feet.add(0, 0.2, 0), 25, 1.2, 0.05);
                l.play(SoundEvents.GENERIC_SPLASH, 1.0F, 0.8F);
            } else {
                circle(l, ParticleTypes.CLOUD, feet.add(0, 0.1, 0), 1.4, 8, age);
            }
        }
    }

    /** Elemento: the element (fogo, gelo, plantas, vento, raio, luz...) swirls in and settles on the body. */
    private static final class Element extends Animation {
        @Override
        void tick(Look l, int age, float p) {
            Vec3 feet = l.feet();
            double radius = 0.4 + 1.6 * (1 - p);
            for (int i = 0; i < 3; i++) {
                double a = age * 0.5 + i * Math.PI * 2 / 3;
                Vec3 at = feet.add(Math.cos(a) * radius, 0.2 + 1.8 * ((age * 0.07 + i * 0.33) % 1.0), Math.sin(a) * radius);
                l.particle(PresetFx.particle(l.element(), l.color(), 1.0F), at, 1, 0.05, 0.0);
                l.particle(PresetFx.accent(l.element(), l.color(), 1.0F), at, 1, 0.1, 0.0);
            }
            if (age == l.ticks()) PresetFx.burst(l.level(), feet.add(0, 1, 0), l.element(), l.color(), 1.2F, 30, 0.5);
        }
    }

    /** Fera: the body shakes and grows with a roar; fur (tipo escamas: scales, barro: clay). */
    private static final class Beast extends Animation {
        private boolean grown;

        @Override
        void tick(Look l, int age, float p) {
            Vec3 feet = l.feet();
            int skin = switch (l.kind()) {
                case "escamas" -> 0x3E7B3A;
                case "barro" -> 0x8A6E52;
                default -> 0x6B4A2B;
            };
            if (age == 1) PresetFx.shake(l.player(), 0.45F, l.ticks());
            l.particle(PresetFx.dust(skin, 1.3F), feet.add(0, 1, 0), (int) (2 + p * 8), 0.4, 0.0);
            if (age % 6 == 0) l.play(SoundEvents.SKELETON_HURT, 0.5F, 0.6F);
            AttributeInstance scale = l.player().getAttribute(Attributes.SCALE);
            if (scale != null) {
                scale.removeModifier(GROW);
                double size = 0.25 * Mth.sin((float) (p * Math.PI));
                if (size > 0.01) {
                    scale.addTransientModifier(new AttributeModifier(GROW, size, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
                    grown = true;
                }
            }
            if (age == (int) (l.ticks() * 0.6)) l.play(SoundEvents.RAVAGER_ROAR, 1.0F, 1.0F);
        }

        @Override
        void end(Look l) {
            super.end(l);
            AttributeInstance scale = l.player().getAttribute(Attributes.SCALE);
            if (grown && scale != null) scale.removeModifier(GROW);
        }
    }

    /** Tubo de Explosão: a Boom Tube opens behind the player, who goes through and comes back suited. */
    private static final class BoomTube extends Animation {
        private boolean hidden;

        @Override
        void tick(Look l, int age, float p) {
            Vec3 forward = l.forward();
            Vec3 right = l.right();
            Vec3 center = l.feet().add(0, 1.1, 0).subtract(forward.scale(1.3));
            double radius = 1.25 * Math.min(1.0, Math.min(p * 4, (1 - p) * 4 + 0.2));
            int color = l.kind().isEmpty() ? 0x9FDCFF : l.color();
            for (int i = 0; i < 18; i++) {
                double a = age * 0.2 + i * Math.PI * 2 / 18;
                Vec3 at = center.add(right.scale(Math.cos(a) * radius)).add(0, Math.sin(a) * radius, 0);
                l.particle(PresetFx.dust(color, 1.1F), at, 1, 0.02, 0.0);
            }
            l.particle(ParticleTypes.ELECTRIC_SPARK, center, 3, radius * 0.4, 0.05);
            int in = (int) (l.ticks() * 0.35);
            int out = (int) (l.ticks() * 0.65);
            if (age == in && !l.player().hasEffect(MobEffects.INVISIBILITY)) {
                l.player().addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, out - in + 2, 0, false, false));
                hidden = true;
                l.particle(ParticleTypes.END_ROD, center, 16, 0.3, 0.15);
            }
            if (age == out) show(l);
        }

        private void show(Look l) {
            if (!hidden) return;
            hidden = false;
            l.player().removeEffect(MobEffects.INVISIBILITY);
            l.particle(ParticleTypes.END_ROD, l.feet().add(0, 1, 0), 20, 0.4, 0.2);
        }

        @Override
        void end(Look l) {
            super.end(l);
            show(l);
        }
    }

    /** Arsenal: the weapons spin around the body and drop into place; the mask is the last piece. */
    private static final class Arsenal extends Animation {
        private final List<Display.ItemDisplay> weapons = new ArrayList<>();

        @Override
        void tick(Look l, int age, float p) {
            Vec3 feet = l.feet();
            List<Item> items = List.of(Items.BOW, Items.IRON_SWORD, Items.CROSSBOW, Items.ARROW);
            if (age == 1) {
                for (Item item : items) {
                    Display.ItemDisplay weapon = Displays.item(l.player(), new ItemStack(item), feet.add(0, 1.2, 0), 0.6F, false);
                    weapons.add(weapon);
                    if (weapon != null) spawned.add(weapon);
                }
            }
            double radius = 1.3 * (1 - p * 0.7);
            for (int i = 0; i < weapons.size(); i++) {
                Display.ItemDisplay weapon = weapons.get(i);
                if (weapon == null || weapon.isRemoved()) continue;
                int drop = (int) (l.ticks() * (0.45 + 0.12 * i));
                if (age >= drop) {
                    l.particle(ParticleTypes.CRIT, weapon.position(), 8, 0.15, 0.1);
                    l.play(i % 2 == 0 ? SoundEvents.CROSSBOW_LOADING_END.value() : SoundEvents.PLAYER_ATTACK_SWEEP, 0.6F, 1.2F);
                    Summons.dismiss(weapon);
                    continue;
                }
                double a = age * 0.45 + i * Math.PI / 2;
                weapon.setPos(feet.add(Math.cos(a) * radius, 1.0 + 0.3 * Math.sin(age * 0.3 + i), Math.sin(a) * radius));
                weapon.setYRot((float) Math.toDegrees(-a));
            }
            if (age == l.ticks()) {
                Vec3 face = l.player().getEyePosition().add(l.forward().scale(0.3));
                l.particle(PresetFx.dust(l.color(), 1.0F), face, 12, 0.15, 0.0);
            }
        }
    }

    /** Espetáculo: confetti (tipo cartas, fumaca, gas) bursts around and a laugh echoes. */
    private static final class Show extends Animation {
        private static final int[] CONFETTI = {0xE83A3A, 0x3AA0E8, 0xF2D43A, 0x52C24A, 0xB05AE0, 0xFF8AC8};

        @Override
        void tick(Look l, int age, float p) {
            if (age % 3 != 0) return;
            Vec3 at = l.feet().add((l.level().random.nextDouble() - 0.5) * 3, 0.6 + l.level().random.nextDouble() * 1.8, (l.level().random.nextDouble() - 0.5) * 3);
            switch (l.kind()) {
                case "cartas" -> {
                    l.particle(PresetFx.dust(0xF4F4F4, 1.2F), at, 8, 0.4, 0.0);
                    l.particle(PresetFx.dust(0xC81E1E, 0.8F), at, 4, 0.4, 0.0);
                }
                case "fumaca" -> l.particle(PresetFx.dust(CONFETTI[age / 3 % CONFETTI.length], 3.0F), at, 6, 0.5, 0.0);
                case "gas" -> {
                    l.particle(PresetFx.dust(0x8FCB3A, 2.5F), at, 8, 0.6, 0.0);
                    l.particle(ParticleTypes.SMOKE, at, 4, 0.5, 0.02);
                }
                default -> {
                    for (int c : CONFETTI) l.particle(PresetFx.dust(c, 0.7F), at, 3, 0.5, 0.0);
                }
            }
            if (age == 3 * (l.ticks() / 3)) l.particle(ParticleTypes.FIREWORK, l.feet().add(0, 1.2, 0), 40, 0.6, 0.2);
        }
    }

    /** Asas de Metal Nth: the wings open on the back with a shine of Nth metal; the player rises a little. */
    private static final class Wings extends Animation {
        @Override
        void tick(Look l, int age, float p) {
            Vec3 forward = l.forward();
            Vec3 right = l.right();
            Vec3 back = l.feet().add(0, 1.45, 0).subtract(forward.scale(0.3));
            double open = Math.toRadians(10 + 60 * Math.min(1.0, p * 1.5));
            int metal = l.kind().isEmpty() ? 0xB8B8C8 : l.color();
            for (int side = -1; side <= 1; side += 2) {
                for (int i = 1; i <= 7; i++) {
                    double along = i * 0.24;
                    double angle = open * (i / 7.0);
                    Vec3 at = back.add(right.scale(side * along * Math.cos(angle) * 1.2)).add(0, along * Math.sin(angle) - 0.15 * i / 7.0, 0)
                            .subtract(forward.scale(0.1 * i / 7.0));
                    l.particle(PresetFx.dust(metal, 1.0F), at, 1, 0.02, 0.0);
                    if (i == 7 && age % 2 == 0) l.particle(ParticleTypes.END_ROD, at, 1, 0.02, 0.0);
                }
            }
            if (age == (int) (l.ticks() * 0.5) && l.player().onGround()) {
                l.player().setDeltaMovement(l.player().getDeltaMovement().add(0, 0.5, 0));
                l.player().hurtMarked = true;
                l.play(SoundEvents.PHANTOM_FLAP, 1.0F, 0.8F);
            }
        }
    }
}
