package com.danrod505.greenlantern.gametest.scripts;

import static com.danrod505.greenlantern.gametest.ClientScript.*;

import com.danrod505.greenlantern.gametest.ClientScript;
import com.danrod505.greenlantern.registry.ModItems;
import com.danrod505.greenlantern.wonderwoman.WonderWomanHero;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

/** Screenshot script "wonderwoman": Wonder Woman: armor, lasso, bracelets, sword and shield, flight and the Invisible Jet. */
public final class WonderWomanScript {
    public static final ClientScript.Script SCRIPT = new ClientScript.Script("wonderwoman", WonderWomanScript::build, WonderWomanScript::log, false);

    private WonderWomanScript() {}

    public static BlockPos wwBase = BlockPos.ZERO;

    public static void wwPower(com.danrod505.greenlantern.wonderwoman.AmazonPower power) {
        server(sp -> {
            ItemStack tiara = com.danrod505.greenlantern.wonderwoman.WonderWomanHelper.findTiara(sp);
            com.danrod505.greenlantern.wonderwoman.AmazonPower.POWERS.select(tiara, power);
            sp.getCooldowns().removeCooldown(sp.getCooldowns().getCooldownGroup(tiara));
            boolean used = com.danrod505.greenlantern.wonderwoman.WonderWomanServer.usePower(sp, tiara, power);
            com.danrod505.greenlantern.GreenLantern.LOGGER.info("CLIENTSCRIPT power {} used={}", power.id(), used);
        });
    }

    public static void wwTp(double dx, double dy, double dz, float yaw, float pitch) {
        server(sp -> {
            if (sp.isPassenger()) sp.stopRiding();
            sp.teleportTo(wwBase.getX() + dx, wwBase.getY() + dy, wwBase.getZ() + dz);
            sp.setDeltaMovement(Vec3.ZERO);
        });
        look(yaw, pitch);
    }

    public static void wwMobs(double dx, double dz, int count, double spread) {
        server(sp -> {
            ServerLevel level = sp.level();
            for (int i = 0; i < count; i++) {
                var mob = EntityType.HUSK.create(level, EntitySpawnReason.COMMAND);
                double a = i * Math.PI * 2 / count;
                mob.snapTo(wwBase.getX() + dx + Math.cos(a) * spread, wwBase.getY(), wwBase.getZ() + dz + Math.sin(a) * spread, 180, 0);
                // Rooted in place, but not frozen: the lasso, the shockwave and the shield can still throw them around.
                mob.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.SLOWNESS, 100000, 9, false, false));
                mob.setPersistenceRequired();
                level.addFreshEntity(mob);
            }
        });
    }

    /** Arrows fired at her from a husk standing in front of her. */
    public static void wwArrows(int count) {
        server(sp -> {
            ServerLevel level = sp.level();
            var shooter = level.getEntitiesOfClass(net.minecraft.world.entity.monster.zombie.Husk.class, sp.getBoundingBox().inflate(16)).stream().findFirst().orElse(null);
            Vec3 eye = sp.getEyePosition().add(0, -0.4, 0);
            Vec3 from = shooter != null ? shooter.getEyePosition() : eye.add(sp.getLookAngle().scale(10));
            for (int i = 0; i < count; i++) {
                var arrow = new net.minecraft.world.entity.projectile.arrow.Arrow(level, from.x + (i - count / 2.0) * 0.4, from.y, from.z, new ItemStack(net.minecraft.world.item.Items.ARROW), null);
                if (shooter != null) arrow.setOwner(shooter);
                Vec3 to = eye.subtract(arrow.position());
                arrow.shoot(to.x, to.y, to.z, 1.6F, 0.0F);
                level.addFreshEntity(arrow);
            }
        });
    }

    /** Wonder Woman: armor, the Lasso of Truth, the bracelets, sword and shield, flight, the Invisible Jet, the wheel and the guide. */
    private static void build() {
        step(60, () -> {
            command("time set 6000");
            command("weather clear");
            command("gamerule advance_time false");
            command("gamerule spawn_mobs false");
            command("gamerule advance_weather false");
            server(sp -> {
                sp.setGameMode(GameType.SURVIVAL);
                sp.setItemInHand(InteractionHand.MAIN_HAND,
                        com.danrod505.greenlantern.item.AmazonTiaraItem.charged(new ItemStack(ModItems.AMAZON_TIARA.get())));
                ServerLevel level = sp.level();
                BlockPos base = sp.blockPosition();
                wwBase = base;
                // Glass panes and ice around the spot of the shockwave, to shatter.
                for (int i = -3; i <= 3; i++) {
                    for (int y = 0; y < 2; y++) {
                        level.setBlock(base.offset(i, y, 44), net.minecraft.world.level.block.Blocks.GLASS.defaultBlockState(), 2);
                        level.setBlock(base.offset(-4, y, 40 + i), net.minecraft.world.level.block.Blocks.ICE.defaultBlockState(), 2);
                    }
                }
                // Pillars along the flight path give a sense of speed.
                for (int z = 120; z < 600; z += 16) {
                    for (int side = -1; side <= 1; side += 2) {
                        for (int y = 0; y < 8 + (z / 16) % 5; y++) {
                            level.setBlock(base.offset(side * 14, y, z), net.minecraft.world.level.block.Blocks.QUARTZ_PILLAR.defaultBlockState(), 2);
                        }
                    }
                }
                WonderWomanHero.INSTANCE.summonSuit(sp);
            });
            look(0, 5);
        });
        step(40, () -> {
            camera(CameraType.THIRD_PERSON_FRONT);
            look(0, 10);
        });
        step(20, () -> {
            mc().gui.getChat().clearMessages(false);
            mc().options.hideGui = true;
        });
        step(10, () -> clean("ww00_armor_front"));
        step(2, () -> camera(CameraType.THIRD_PERSON_BACK));
        step(4, () -> clean("ww00b_armor_back"));
        step(2, () -> look(90, 15));
        step(4, () -> clean("ww00c_armor_side"));
        step(2, () -> {
            mc().options.hideGui = false;
            mc().gui.getChat().clearMessages(false);
            look(0, 10);
        });
        step(4, () -> shot("ww01_hud"));
        // Lasso of Truth: capture a husk, then swing it around and hurl it.
        step(10, () -> {
            wwTp(0.5, 0, 2.5, 16, 6);
            wwMobs(-1.5, 9.5, 1, 0);
        });
        step(10, () -> wwPower(com.danrod505.greenlantern.wonderwoman.AmazonPower.LASSO_CAPTURE));
        step(1, () -> look(50, 10));
        step(2, () -> shot("ww02_lasso_throw"));
        step(14, () -> shot("ww02b_lasso_caught"));
        step(1, () -> camera(CameraType.THIRD_PERSON_FRONT));
        step(4, () -> shot("ww02c_lasso_caught_front"));
        step(1, () -> camera(CameraType.THIRD_PERSON_BACK));
        step(2, () -> wwPower(com.danrod505.greenlantern.wonderwoman.AmazonPower.LASSO_PULL));
        step(4, () -> shot("ww03_lasso_pull"));
        step(16, () -> {
            look(30, 15);
            wwPower(com.danrod505.greenlantern.wonderwoman.AmazonPower.LASSO_SPIN);
        });
        step(8, () -> shot("ww04_lasso_swing"));
        step(8, () -> shot("ww04b_lasso_swing"));
        step(20, () -> shot("ww04c_lasso_hurl"));
        step(20, () -> clearMobs());
        // The lasso whirling around her with nothing caught.
        step(10, () -> wwMobs(0.5, 2.5, 6, 3.2));
        step(10, () -> {
            look(0, 25);
            wwPower(com.danrod505.greenlantern.wonderwoman.AmazonPower.LASSO_SPIN);
        });
        step(6, () -> shot("ww05_lasso_whirl"));
        step(1, () -> camera(CameraType.THIRD_PERSON_FRONT));
        step(4, () -> clean("ww05b_lasso_whirl_front"));
        step(1, () -> {
            mc().options.hideGui = false;
            camera(CameraType.THIRD_PERSON_BACK);
        });
        step(20, () -> clearMobs());
        // Bracelets of Submission: arrows bounce back where they came from.
        step(10, () -> {
            wwTp(0.5, 0, 20.5, 0, 0);
            wwMobs(0.5, 32.5, 1, 0);
        });
        step(10, () -> wwPower(com.danrod505.greenlantern.wonderwoman.AmazonPower.BRACELET_GUARD));
        step(4, () -> camera(CameraType.THIRD_PERSON_FRONT));
        step(4, () -> clean("ww06_bracelets_guard"));
        step(1, () -> {
            mc().options.hideGui = false;
            camera(CameraType.THIRD_PERSON_BACK);
            look(0, 2);
        });
        step(2, () -> wwArrows(3));
        step(5, () -> shot("ww06b_arrows_incoming"));
        step(3, () -> shot("ww06c_deflect"));
        step(6, () -> shot("ww06d_arrows_back"));
        step(10, () -> wwPower(com.danrod505.greenlantern.wonderwoman.AmazonPower.BRACELET_GUARD));
        step(20, () -> clearMobs());
        // The shockwave: the bracelets clash in a ring of husks, by the glass and ice.
        step(10, () -> {
            wwTp(0.5, 0, 40.5, 0, 15);
            wwMobs(0.5, 40.5, 8, 3.5);
        });
        step(10, () -> shot("ww07_before_shockwave"));
        step(1, () -> wwPower(com.danrod505.greenlantern.wonderwoman.AmazonPower.BRACELET_SHOCKWAVE));
        step(2, () -> shot("ww07b_shockwave"));
        step(4, () -> shot("ww07c_shockwave_ring"));
        step(12, () -> shot("ww07d_shockwave_after"));
        step(30, () -> clearMobs());
        // Sword and shield.
        step(10, () -> {
            wwTp(0.5, 0, 60.5, 0, 10);
            mc().player.getInventory().setSelectedSlot(1);
            server(sp -> sp.getInventory().setSelectedSlot(1));
        });
        step(5, () -> wwPower(com.danrod505.greenlantern.wonderwoman.AmazonPower.SWORD_AND_SHIELD));
        step(10, () -> shot("ww08_sword_shield"));
        step(1, () -> camera(CameraType.THIRD_PERSON_FRONT));
        step(4, () -> clean("ww08b_sword_shield_front"));
        step(1, () -> {
            mc().options.hideGui = false;
            camera(CameraType.FIRST_PERSON);
        });
        step(4, () -> shot("ww08c_sword_shield_first_person"));
        step(1, () -> key(mc().options.keyUse, true));
        step(8, () -> shot("ww08d_shield_block_first_person"));
        step(1, () -> camera(CameraType.THIRD_PERSON_FRONT));
        step(4, () -> clean("ww08e_shield_block_front"));
        step(1, () -> {
            key(mc().options.keyUse, false);
            mc().options.hideGui = false;
            camera(CameraType.THIRD_PERSON_BACK);
        });
        // The shield thrown: it bounces between the husks and comes back.
        step(10, () -> {
            wwMobs(0.5, 67.5, 1, 0);
            wwMobs(-3.5, 70.5, 1, 0);
            wwMobs(3.5, 69.5, 1, 0);
            look(0, 4);
        });
        step(10, () -> wwPower(com.danrod505.greenlantern.wonderwoman.AmazonPower.SHIELD_THROW));
        step(3, () -> shot("ww09_shield_throw"));
        step(5, () -> shot("ww09b_shield_bounce"));
        step(6, () -> shot("ww09c_shield_bounce"));
        watch(60, () -> com.danrod505.greenlantern.wonderwoman.WonderWomanHelper.shieldSlot(mc().player) >= 0, "ww09d_shield_back");
        step(4, () -> shot("ww09e_shield_caught"));
        step(20, () -> clearMobs());
        step(2, () -> wwPower(com.danrod505.greenlantern.wonderwoman.AmazonPower.SWORD_AND_SHIELD));
        // Flight: take off and accelerate (slower than the Lantern, never supersonic).
        step(10, () -> {
            mc().player.getInventory().setSelectedSlot(0);
            server(sp -> sp.getInventory().setSelectedSlot(0));
            wwTp(0.5, 0, 100.5, 0, 5);
        });
        step(5, () -> {
            mc().player.getAbilities().flying = true;
            mc().player.onUpdateAbilities();
        });
        step(4, () -> key(mc().options.keyJump, true));
        step(10, () -> {
            key(mc().options.keyJump, false);
            look(0, 0);
        });
        step(10, () -> clean("ww10_hover"));
        step(1, () -> camera(CameraType.THIRD_PERSON_FRONT));
        step(4, () -> clean("ww10b_hover_front"));
        step(1, () -> {
            mc().options.hideGui = false;
            camera(CameraType.THIRD_PERSON_BACK);
            look(0, -6);
            key(mc().options.keyUp, true);
            key(mc().options.keySprint, true);
        });
        step(20, () -> shot("ww11_accelerating"));
        step(60, () -> shot("ww11b_top_speed"));
        step(1, () -> camera(CameraType.THIRD_PERSON_FRONT));
        step(4, () -> clean("ww11c_flying_front"));
        step(1, () -> {
            mc().options.hideGui = false;
            camera(CameraType.THIRD_PERSON_BACK);
        });
        for (int i = 0; i < 24; i++) {
            final float yaw = (i + 1) * 3.75F;
            step(1, () -> look(yaw, 0));
            if (i == 14) step(0, () -> shot("ww12_turn_trail"));
        }
        step(1, () -> {
            key(mc().options.keyUp, false);
            key(mc().options.keySprint, false);
            mc().player.getAbilities().flying = false;
            mc().player.onUpdateAbilities();
        });
        watch(80, () -> mc().player.onGround(), "ww12b_landing");
        // The Invisible Jet.
        step(10, () -> wwTp(0.5, 0, 140.5, 0, 10));
        step(10, () -> wwPower(com.danrod505.greenlantern.wonderwoman.AmazonPower.INVISIBLE_JET));
        step(20, () -> shot("ww13_invisible_jet"));
        step(1, () -> camera(CameraType.THIRD_PERSON_FRONT));
        step(4, () -> clean("ww13b_jet_front"));
        step(1, () -> server(sp -> {
            if (sp.getVehicle() instanceof com.danrod505.greenlantern.entity.InvisibleJetEntity jet) {
                sp.stopRiding();
                sp.teleportTo(jet.getX() + 6.5, jet.getY() + 1.0, jet.getZ() + 1.0);
            }
        }));
        step(3, () -> {
            camera(CameraType.FIRST_PERSON);
            look(90, 15);
        });
        step(4, () -> clean("ww13c_jet_side"));
        step(2, () -> {
            mc().options.hideGui = false;
            camera(CameraType.THIRD_PERSON_BACK);
            wwPower(com.danrod505.greenlantern.wonderwoman.AmazonPower.INVISIBLE_JET);
        });
        step(6, () -> {
            look(0, -12);
            key(mc().options.keyUp, true);
        });
        step(30, () -> shot("ww14_jet_flight"));
        step(1, () -> key(mc().options.keySprint, true));
        step(20, () -> shot("ww14b_jet_afterburner"));
        step(1, () -> camera(CameraType.FIRST_PERSON));
        step(4, () -> shot("ww14c_jet_first_person"));
        step(1, () -> {
            camera(CameraType.THIRD_PERSON_BACK);
            key(mc().options.keySprint, false);
            look(0, 0);
        });
        step(10, () -> com.danrod505.greenlantern.network.ModNetwork.sendToServer(new com.danrod505.greenlantern.network.JetCloakPacket()));
        step(20, () -> shot("ww15_jet_cloaked"));
        step(1, () -> camera(CameraType.THIRD_PERSON_FRONT));
        step(4, () -> shot("ww15b_jet_cloaked_front"));
        step(1, () -> {
            camera(CameraType.THIRD_PERSON_BACK);
            key(mc().options.keyUp, false);
            com.danrod505.greenlantern.network.ModNetwork.sendToServer(new com.danrod505.greenlantern.network.JetCloakPacket());
        });
        step(20, () -> wwPower(com.danrod505.greenlantern.wonderwoman.AmazonPower.INVISIBLE_JET));
        step(10, () -> wwTp(0.5, 0, 160.5, 0, 10));
        // Power wheel.
        step(20, () -> key(com.danrod505.greenlantern.client.KeyBindings.CONSTRUCT_WHEEL, true));
        step(8, () -> {
            var screen = mc().screen;
            if (screen == null) return;
            double angle = Math.toRadians(-90 + 45);
            double gx = screen.width / 2.0 + Math.cos(angle) * 60;
            double gy = screen.height * 0.46 + Math.sin(angle) * 60;
            screen.mouseMoved(gx, gy);
        });
        step(6, () -> shot("ww16_power_wheel"));
        step(1, () -> {
            key(com.danrod505.greenlantern.client.KeyBindings.CONSTRUCT_WHEEL, false);
            if (mc().screen != null) mc().screen.keyReleased(new net.minecraft.client.input.KeyEvent(org.lwjgl.glfw.GLFW.GLFW_KEY_R, 0, 0));
        });
        step(10, () -> com.danrod505.greenlantern.GreenLantern.LOGGER.info("CLIENTSCRIPT power selected={}",
                com.danrod505.greenlantern.wonderwoman.AmazonPower.POWERS.selected(com.danrod505.greenlantern.wonderwoman.WonderWomanHelper.findTiara(mc().player)).id()));
        // The guide.
        step(5, () -> mc().setScreen(com.danrod505.greenlantern.client.GuideScreen.atChapter("wonder_woman")));
        step(10, () -> shot("ww17_guide"));
        step(2, () -> mc().setScreen(com.danrod505.greenlantern.client.GuideScreen.atChapter("wonder_woman_powers")));
        step(10, () -> shot("ww17b_guide_powers"));
        step(20, () -> mc().stop());
    }

    /** Logged every 10 ticks while the script runs. */
    private static void log(Minecraft mc) {
        var tiara = com.danrod505.greenlantern.wonderwoman.WonderWomanHelper.findTiara(mc.player);
        com.danrod505.greenlantern.GreenLantern.LOGGER.info("CLIENTSCRIPT t={} pos={} flying={} speed={} flags={} divine={} jet={} music: {}", mc.player.tickCount,
                mc.player.blockPosition(), com.danrod505.greenlantern.client.flight.FlightController.isPowerFlying(),
                String.format("%.2f", com.danrod505.greenlantern.client.flight.FlightController.speed()),
                com.danrod505.greenlantern.client.wonderwoman.WonderWomanVisuals.flags(mc.player.getId()),
                tiara.isEmpty() ? -1 : com.danrod505.greenlantern.wonderwoman.WonderWomanHero.DIVINE_POWER.get(tiara).stored(),
                mc.player.getVehicle() instanceof com.danrod505.greenlantern.entity.InvisibleJetEntity jet
                        ? jet.blockPosition() + " boost=" + jet.isBoosting() + " cloak=" + jet.isCloaked() : "-",
                com.danrod505.greenlantern.client.flight.FlightAudio.describeMusic());
    }
}
