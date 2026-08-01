package com.example.addon.modules;

import baritone.api.BaritoneAPI;
import baritone.api.IBaritone;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Module;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.*;

public class FightBot extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> attackRange = sgGeneral.add(new DoubleSetting.Builder()
            .name("attack-range")
            .description("Rango mínimo para atacar (si estás más cerca te alejas).")
            .defaultValue(3.5)
            .min(2.0)
            .max(6.0)
            .build()
    );

    private final Setting<Double> optimalDistance = sgGeneral.add(new DoubleSetting.Builder()
            .name("optimal-distance")
            .description("Distancia objetivo que intentamos mantener (si estás lejos, te acercas).")
            .defaultValue(4.0)
            .min(2.0)
            .max(8.0)
            .build()
    );

    private final Setting<Integer> searchRadius = sgGeneral.add(new IntSetting.Builder()
            .name("search-radius")
            .description("Radio de búsqueda de enemigos.")
            .defaultValue(30)
            .min(10)
            .max(50)
            .build()
    );

    private final Setting<Boolean> targetPlayers = sgGeneral.add(new BoolSetting.Builder()
            .name("target-players")
            .description("Atacar jugadores.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> targetMobs = sgGeneral.add(new BoolSetting.Builder()
            .name("target-mobs")
            .description("Atacar mobs.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> targetHostileOnly = sgGeneral.add(new BoolSetting.Builder()
            .name("hostile-only")
            .description("Solo atacar mobs hostiles (cuando targetMobs=true).")
            .defaultValue(true)
            .build()
    );

    public enum TargetMode { TODOS, HOSTILES, ESPECIFICOS }

    private final Setting<TargetMode> selectorMode = sgGeneral.add(new EnumSetting.Builder<TargetMode>()
            .name("selector-mode")
            .description("Modo de selección de mobs a atacar.")
            .defaultValue(TargetMode.HOSTILES)
            .build()
    );

    private final Setting<Boolean> sZombie = sgGeneral.add(new BoolSetting.Builder().name("zombie").defaultValue(true).description("Atacar Zombie").visible(() -> selectorMode.get() == TargetMode.ESPECIFICOS).build());
    private final Setting<Boolean> sSkeleton = sgGeneral.add(new BoolSetting.Builder().name("skeleton").defaultValue(true).description("Atacar Skeleton").visible(() -> selectorMode.get() == TargetMode.ESPECIFICOS).build());
    private final Setting<Boolean> sCreeper = sgGeneral.add(new BoolSetting.Builder().name("creeper").defaultValue(true).description("Atacar Creeper").visible(() -> selectorMode.get() == TargetMode.ESPECIFICOS).build());
    private final Setting<Boolean> sEnderman = sgGeneral.add(new BoolSetting.Builder().name("enderman").defaultValue(true).description("Atacar Enderman").visible(() -> selectorMode.get() == TargetMode.ESPECIFICOS).build());
    private final Setting<Boolean> sSpider = sgGeneral.add(new BoolSetting.Builder().name("spider").defaultValue(true).description("Atacar Spider").visible(() -> selectorMode.get() == TargetMode.ESPECIFICOS).build());
    private final Setting<Boolean> sWitch = sgGeneral.add(new BoolSetting.Builder().name("witch").defaultValue(true).description("Atacar Witch").visible(() -> selectorMode.get() == TargetMode.ESPECIFICOS).build());
    private final Setting<Boolean> sDrowned = sgGeneral.add(new BoolSetting.Builder().name("drowned").defaultValue(true).description("Atacar Drowned").visible(() -> selectorMode.get() == TargetMode.ESPECIFICOS).build());
    private final Setting<Boolean> sHusk = sgGeneral.add(new BoolSetting.Builder().name("husk").defaultValue(true).description("Atacar Husk").visible(() -> selectorMode.get() == TargetMode.ESPECIFICOS).build());
    private final Setting<Boolean> sStray = sgGeneral.add(new BoolSetting.Builder().name("stray").defaultValue(true).description("Atacar Stray").visible(() -> selectorMode.get() == TargetMode.ESPECIFICOS).build());
    private final Setting<Boolean> sVindicator = sgGeneral.add(new BoolSetting.Builder().name("vindicator").defaultValue(true).description("Atacar Vindicator").visible(() -> selectorMode.get() == TargetMode.ESPECIFICOS).build());
    private final Setting<Boolean> sEvoker = sgGeneral.add(new BoolSetting.Builder().name("evoker").defaultValue(true).description("Atacar Evoker").visible(() -> selectorMode.get() == TargetMode.ESPECIFICOS).build());
    private final Setting<Boolean> sVex = sgGeneral.add(new BoolSetting.Builder().name("vex").defaultValue(true).description("Atacar Vex").visible(() -> selectorMode.get() == TargetMode.ESPECIFICOS).build());
    private final Setting<Boolean> sPillager = sgGeneral.add(new BoolSetting.Builder().name("pillager").defaultValue(true).description("Atacar Pillager").visible(() -> selectorMode.get() == TargetMode.ESPECIFICOS).build());
    private final Setting<Boolean> sRavager = sgGeneral.add(new BoolSetting.Builder().name("ravager").defaultValue(true).description("Atacar Ravager").visible(() -> selectorMode.get() == TargetMode.ESPECIFICOS).build());
    private final Setting<Boolean> sSilverfish = sgGeneral.add(new BoolSetting.Builder().name("silverfish").defaultValue(true).description("Atacar Silverfish").visible(() -> selectorMode.get() == TargetMode.ESPECIFICOS).build());
    private final Setting<Boolean> sCaveSpider = sgGeneral.add(new BoolSetting.Builder().name("cavespider").defaultValue(true).description("Atacar CaveSpider").visible(() -> selectorMode.get() == TargetMode.ESPECIFICOS).build());
    private final Setting<Boolean> sBlaze = sgGeneral.add(new BoolSetting.Builder().name("blaze").defaultValue(true).description("Atacar Blaze").visible(() -> selectorMode.get() == TargetMode.ESPECIFICOS).build());
    private final Setting<Boolean> sGhast = sgGeneral.add(new BoolSetting.Builder().name("ghast").defaultValue(true).description("Atacar Ghast").visible(() -> selectorMode.get() == TargetMode.ESPECIFICOS).build());
    private final Setting<Boolean> sHoglin = sgGeneral.add(new BoolSetting.Builder().name("hoglin").defaultValue(true).description("Atacar Hoglin").visible(() -> selectorMode.get() == TargetMode.ESPECIFICOS).build());
    private final Setting<Boolean> sPiglinBrute = sgGeneral.add(new BoolSetting.Builder().name("piglin-brute").defaultValue(true).description("Atacar PiglinBrute").visible(() -> selectorMode.get() == TargetMode.ESPECIFICOS).build());
    private final Setting<Boolean> sMagmaCube = sgGeneral.add(new BoolSetting.Builder().name("magma-cube").defaultValue(true).description("Atacar MagmaCube").visible(() -> selectorMode.get() == TargetMode.ESPECIFICOS).build());
    private final Setting<Boolean> sPhantom = sgGeneral.add(new BoolSetting.Builder().name("phantom").defaultValue(true).description("Atacar Phantom").visible(() -> selectorMode.get() == TargetMode.ESPECIFICOS).build());
    private final Setting<Boolean> sShulker = sgGeneral.add(new BoolSetting.Builder().name("shulker").defaultValue(true).description("Atacar Shulker").visible(() -> selectorMode.get() == TargetMode.ESPECIFICOS).build());
    private final Setting<Boolean> sSlime = sgGeneral.add(new BoolSetting.Builder().name("slime").defaultValue(true).description("Atacar Slime").visible(() -> selectorMode.get() == TargetMode.ESPECIFICOS).build());
    private final Setting<Boolean> sZoglin = sgGeneral.add(new BoolSetting.Builder().name("zoglin").defaultValue(true).description("Atacar Zoglin").visible(() -> selectorMode.get() == TargetMode.ESPECIFICOS).build());

    private final Setting<Boolean> autoSwitchWeapon = sgGeneral.add(new BoolSetting.Builder()
            .name("auto-switch-weapon")
            .description("Cambiar automáticamente al mejor arma en el hotbar.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> keepDistance = sgGeneral.add(new BoolSetting.Builder()
            .name("keep-distance")
            .description("Mantener la distancia óptima mientras se ataca.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Integer> attackDelay = sgGeneral.add(new IntSetting.Builder()
            .name("attack-delay")
            .description("Ticks entre ataques.")
            .defaultValue(10)
            .min(5)
            .max(20)
            .build()
    );

    private enum State { BUSCANDO_ENEMIGO, YENDO_ENEMIGO, ATACANDO, AJUSTANDO_DISTANCIA }

    private State state = State.BUSCANDO_ENEMIGO;

    private LivingEntity target = null;
    private int attackDelayCounter = 0;
    private final Set<Class<? extends Entity>> specificSet = new HashSet<>();

    public FightBot() {
        super(Category.Combat, "fight-bot", "Automatiza combate contra mobs/jugadores.");
    }

    @Override
    public void onActivate() {
        state = State.BUSCANDO_ENEMIGO;
        target = null;
        attackDelayCounter = 0;
        rebuildSpecificSet();
        info("FightBot activado");
    }

    @Override
    public void onDeactivate() {
        try {
            IBaritone b = getBaritone();
            if (b != null) b.getCommandManager().execute("stop");
        } catch (Throwable ignored) {}
        target = null;
        state = State.BUSCANDO_ENEMIGO;
        info("FightBot desactivado");
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.world == null) return;

        if (attackDelayCounter > 0) attackDelayCounter--;

        switch (state) {
            case BUSCANDO_ENEMIGO -> {
                target = findNearestTarget();
                if (target != null) {
                    info("Objetivo encontrado: " + target.getType().getName().getString() + " en " + posString(target.getBlockPos()));
                    goToMaintainDistance(target, optimalDistance.get());
                    state = State.YENDO_ENEMIGO;
                }
            }
            case YENDO_ENEMIGO -> {
                if (target == null || !target.isAlive()) {
                    state = State.BUSCANDO_ENEMIGO;
                    return;
                }
                double dist = mc.player.squaredDistanceTo(target);
                double attackRangeSq = attackRange.get() * attackRange.get();
                double optimalSq = optimalDistance.get() * optimalDistance.get();
                if (dist <= optimalSq && dist >= attackRangeSq) {
                    state = State.ATACANDO;
                } else {
                    goToMaintainDistance(target, optimalDistance.get());
                }
            }
            case ATACANDO -> {
                if (target == null || !target.isAlive()) {
                    info("Objetivo muerto o desaparecido. Buscando siguiente.");
                    target = null;
                    state = State.BUSCANDO_ENEMIGO;
                    return;
                }

                double dist = mc.player.squaredDistanceTo(target);
                double attackRangeSq = attackRange.get() * attackRange.get();
                double optimalSq = optimalDistance.get() * optimalDistance.get();

                if (keepDistance.get()) {
                    if (dist > optimalSq + 0.5) {
                        goToMaintainDistance(target, optimalDistance.get());
                        state = State.AJUSTANDO_DISTANCIA;
                        return;
                    } else if (dist < attackRangeSq - 0.5) {
                        Vec3d retreat = retreatPosFrom(target, attackRange.get());
                        IBaritone b = getBaritone();
                        if (b != null) {
                            b.getCommandManager().execute("goto " + retreat.x + " " + retreat.y + " " + retreat.z);
                        }
                        state = State.AJUSTANDO_DISTANCIA;
                        return;
                    }
                }

                if (dist <= attackRangeSq) {
                    if (attackDelayCounter <= 0 && mc.player.getAttackCooldownProgress(0f) >= 0.9f) {
                        if (autoSwitchWeapon.get()) switchToBestWeapon();
                        try {
                            mc.interactionManager.attackEntity(mc.player, target);
                            mc.player.swingHand(Hand.MAIN_HAND);
                            attackDelayCounter = attackDelay.get();
                        } catch (Throwable t) {
                            mc.player.swingHand(Hand.MAIN_HAND);
                            attackDelayCounter = attackDelay.get();
                        }
                    }
                } else {
                    goToMaintainDistance(target, optimalDistance.get());
                    state = State.AJUSTANDO_DISTANCIA;
                }
            }
            case AJUSTANDO_DISTANCIA -> {
                if (target == null || !target.isAlive()) {
                    state = State.BUSCANDO_ENEMIGO;
                    return;
                }
                double dist = mc.player.squaredDistanceTo(target);
                double attackRangeSq = attackRange.get() * attackRange.get();
                double optimalSq = optimalDistance.get() * optimalDistance.get();
                if (dist <= optimalSq && dist >= attackRangeSq) {
                    state = State.ATACANDO;
                } else {
                    IBaritone b = getBaritone();
                    if (b != null) {
                        goToMaintainDistance(target, optimalDistance.get());
                    } else state = State.BUSCANDO_ENEMIGO;
                }
            }
        }
    }

    private void rebuildSpecificSet() {
        specificSet.clear();
        if (sZombie.get()) specificSet.add(ZombieEntity.class);
        if (sSkeleton.get()) specificSet.add(SkeletonEntity.class);
        if (sCreeper.get()) specificSet.add(CreeperEntity.class);
        if (sEnderman.get()) specificSet.add(EndermanEntity.class);
        if (sSpider.get()) specificSet.add(SpiderEntity.class);
        if (sWitch.get()) specificSet.add(WitchEntity.class);
        if (sDrowned.get()) specificSet.add(DrownedEntity.class);
        if (sHusk.get()) specificSet.add(HuskEntity.class);
        if (sStray.get()) specificSet.add(StrayEntity.class);
        if (sVindicator.get()) specificSet.add(VindicatorEntity.class);
        if (sEvoker.get()) specificSet.add(EvokerEntity.class);
        if (sVex.get()) specificSet.add(VexEntity.class);
        if (sPillager.get()) specificSet.add(PillagerEntity.class);
        if (sRavager.get()) specificSet.add(RavagerEntity.class);
        if (sSilverfish.get()) specificSet.add(SilverfishEntity.class);
        if (sCaveSpider.get()) specificSet.add(CaveSpiderEntity.class);
        if (sBlaze.get()) specificSet.add(BlazeEntity.class);
        if (sGhast.get()) specificSet.add(GhastEntity.class);
        if (sHoglin.get()) specificSet.add(HoglinEntity.class);
        if (sPiglinBrute.get()) specificSet.add(PiglinBruteEntity.class);
        if (sMagmaCube.get()) specificSet.add(MagmaCubeEntity.class);
        if (sPhantom.get()) specificSet.add(PhantomEntity.class);
        if (sShulker.get()) specificSet.add(ShulkerEntity.class);
        if (sSlime.get()) specificSet.add(SlimeEntity.class);
        if (sZoglin.get()) specificSet.add(ZoglinEntity.class);
    }

    private LivingEntity findNearestTarget() {
        World world = mc.world;
        BlockPos playerPos = mc.player.getBlockPos();
        double best = Double.MAX_VALUE;
        LivingEntity bestEntity = null;

        List<LivingEntity> list = world.getEntitiesByClass(LivingEntity.class, mc.player.getBoundingBox().expand(searchRadius.get()), e -> e != null && e.isAlive() && e != mc.player);
        for (LivingEntity e : list) {
            if (e instanceof PlayerEntity) {
                if (!targetPlayers.get()) continue;
            } else {
                if (!targetMobs.get()) continue;
                if (targetHostileOnly.get() && !(e instanceof HostileEntity)) {
                    continue;
                }
            }

            if (selectorMode.get() == TargetMode.HOSTILES) {
                if (!(e instanceof HostileEntity) && !(e instanceof PlayerEntity && targetPlayers.get())) continue;
            } else if (selectorMode.get() == TargetMode.ESPECIFICOS) {
                boolean matched = matchesSpecific(e);
                if (!matched) continue;
            }

            double d = mc.player.squaredDistanceTo(e);
            if (d < best) {
                best = d;
                bestEntity = e;
            }
        }

        return bestEntity;
    }

    private boolean matchesSpecific(Entity e) {
        Class<?> c = e.getClass();
        for (Class<? extends Entity> cls : specificSet) {
            if (cls.isAssignableFrom(c)) return true;
        }
        return false;
    }

    private void goToMaintainDistance(Entity target, double distance) {
        IBaritone b = getBaritone();
        if (b == null) return;

        Vec3d tp = target.getPos();
        double tx = tp.x;
        double tz = tp.z;
        double px = mc.player.getX();
        double pz = mc.player.getZ();

        double dx = px - tx;
        double dz = pz - tz;
        double len = Math.sqrt(dx * dx + dz * dz);
        if (len < 0.001) {
            double ang = Math.random() * Math.PI * 2;
            dx = Math.cos(ang);
            dz = Math.sin(ang);
            len = 1.0;
        }

        double nx = dx / len;
        double nz = dz / len;

        double gx = tx + nx * distance;
        double gz = tz + nz * distance;
        double gy = target.getY();

        String cmd = "goto " + (int)Math.floor(gx) + " " + (int)Math.floor(gy) + " " + (int)Math.floor(gz);
        b.getCommandManager().execute(cmd);
    }

    private Vec3d retreatPosFrom(Entity target, double distance) {
        Vec3d tp = target.getPos();
        double tx = tp.x;
        double tz = tp.z;
        double px = mc.player.getX();
        double pz = mc.player.getZ();

        double dx = px - tx;
        double dz = pz - tz;
        double len = Math.sqrt(dx * dx + dz * dz);
        if (len < 0.001) {
            double ang = Math.random() * Math.PI * 2;
            dx = Math.cos(ang);
            dz = Math.sin(ang);
            len = 1.0;
        }

        double nx = dx / len;
        double nz = dz / len;

        double gx = tx + nx * distance;
        double gz = tz + nz * distance;
        double gy = mc.player.getY();
        return new Vec3d(gx, gy, gz);
    }

    private void switchToBestWeapon() {
        Item[] priority = new Item[]{
                Items.NETHERITE_SWORD, Items.DIAMOND_SWORD, Items.IRON_SWORD, Items.GOLDEN_SWORD, Items.STONE_SWORD, Items.WOODEN_SWORD,
                Items.NETHERITE_AXE, Items.DIAMOND_AXE, Items.IRON_AXE, Items.STONE_AXE, Items.WOODEN_AXE
        };

        for (int p = 0; p < priority.length; p++) {
            for (int i = 0; i < 9; i++) {
                if (mc.player.getInventory().getStack(i).getItem() == priority[p]) {
                    if (mc.player.getInventory().selectedSlot != i) {
                        mc.player.getInventory().selectedSlot = i;
                    }
                    return;
                }
            }
        }
    }

    private IBaritone getBaritone() {
        try {
            if (BaritoneAPI.getProvider() == null) return null;
            return BaritoneAPI.getProvider().getPrimaryBaritone();
        } catch (Throwable t) {
            return null;
        }
    }

    private String posString(BlockPos p) {
        return "(" + p.getX() + ", " + p.getY() + ", " + p.getZ() + ")";
    }
}
