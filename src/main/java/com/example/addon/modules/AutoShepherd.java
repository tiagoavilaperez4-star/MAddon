package com.example.addon.modules;

import baritone.api.BaritoneAPI;
import baritone.api.IBaritone;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Module;
import net.minecraft.entity.passive.SheepEntity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.List;
import java.util.Random;

/**
 * AutoShepherd - automatiza el esquilado de ovejas.
 */
public class AutoShepherd extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> searchRadius = sgGeneral.add(new IntSetting.Builder()
            .name("search-radius")
            .description("Radio de búsqueda de ovejas.")
            .defaultValue(30)
            .min(10)
            .max(50)
            .build()
    );

    private final Setting<Boolean> autoEquipShears = sgGeneral.add(new BoolSetting.Builder()
            .name("auto-equip-shears")
            .description("Equipar tijeras automáticamente si están en el inventario.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> autoCollect = sgGeneral.add(new BoolSetting.Builder()
            .name("auto-collect")
            .description("Recolectar la lana caída automáticamente.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> onlyAdult = sgGeneral.add(new BoolSetting.Builder()
            .name("only-adult")
            .description("Solo esquilar ovejas adultas.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Integer> shearingDelay = sgGeneral.add(new IntSetting.Builder()
            .name("shearing-delay")
            .description("Ticks de espera entre esquilados.")
            .defaultValue(10)
            .min(5)
            .max(20)
            .build()
    );

    private final Setting<Integer> maxSheepPerRun = sgGeneral.add(new IntSetting.Builder()
            .name("max-sheep-per-run")
            .description("Máximo de ovejas a esquilar en un ciclo.")
            .defaultValue(10)
            .min(1)
            .max(20)
            .build()
    );

    private final Setting<Boolean> stopWhenFull = sgGeneral.add(new BoolSetting.Builder()
            .name("stop-when-full")
            .description("Detenerse si el inventario se llena.")
            .defaultValue(true)
            .build()
    );

    private enum State {
        BUSCANDO_OVEJA,
        YENDO_OVEJA,
        ESQUILANDO,
        RECOGIENDO,
        ESPERANDO
    }

    private State state = State.BUSCANDO_OVEJA;

    private SheepEntity targetSheep = null;
    private int shearedCount = 0;
    private int actionDelay = 0;
    private final Random random = new Random();

    public AutoShepherd() {
        super(Category.Farming, "auto-shepherd", "Automatiza el esquilado de ovejas.");
    }

    @Override
    public void onActivate() {
        state = State.BUSCANDO_OVEJA;
        targetSheep = null;
        shearedCount = 0;
        actionDelay = 0;
        info("AutoShepherd activado");
    }

    @Override
    public void onDeactivate() {
        try {
            IBaritone b = getBaritone();
            if (b != null) b.getCommandManager().execute("stop");
        } catch (Throwable ignored) {}
        info("AutoShepherd desactivado");
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.world == null) return;

        if (actionDelay > 0) {
            actionDelay--;
            return;
        }

        if (maxSheepPerRun.get() > 0 && shearedCount >= maxSheepPerRun.get()) {
            info("Máximo de ovejas esquiladas en este ciclo alcanzado: " + shearedCount);
            state = State.ESPERANDO;
            actionDelay = 40;
            return;
        }

        if (stopWhenFull.get() && !hasInventorySpace()) {
            info("Inventario lleno. Deteniendo AutoShepherd.");
            toggle();
            return;
        }

        switch (state) {
            case BUSCANDO_OVEJA -> {
                SheepEntity nearest = findNearestSheep(mc.world, mc.player.getBlockPos(), searchRadius.get());
                if (nearest != null) {
                    targetSheep = nearest;
                    info("Oveja objetivo encontrada en " + posString(targetSheep.getBlockPos()));
                    IBaritone b = getBaritone();
                    if (b != null) {
                        b.getCommandManager().execute("goto " + (int)Math.floor(targetSheep.getX()) + " " + (int)Math.floor(targetSheep.getY()) + " " + (int)Math.floor(targetSheep.getZ()));
                        state = State.YENDO_OVEJA;
                    } else {
                        info("Baritone no disponible, desactivando módulo.");
                        toggle();
                    }
                } else {
                    actionDelay = 20 + random.nextInt(40);
                }
            }

            case YENDO_OVEJA -> {
                if (targetSheep == null || !targetSheep.isAlive()) {
                    targetSheep = null;
                    state = State.BUSCANDO_OVEJA;
                    return;
                }

                if (targetSheep.isSheared()) {
                    info("La oveja ya está esquilada, buscando otra.");
                    targetSheep = null;
                    state = State.BUSCANDO_OVEJA;
                    return;
                }

                if (onlyAdult.get() && targetSheep.isBaby()) {
                    info("Oveja objetivo es bebé, ignorando.");
                    targetSheep = null;
                    state = State.BUSCANDO_OVEJA;
                    return;
                }

                double distSq = mc.player.squaredDistanceTo(targetSheep);
                if (distSq <= 9.0) {
                    if (!hasShearsInMainHand()) {
                        if (autoEquipShears.get()) {
                            int slot = findShearsHotbarSlot();
                            if (slot != -1) {
                                mc.player.getInventory().selectedSlot = slot;
                                actionDelay = 2;
                            } else {
                                info("No se encontraron tijeras en el inventario.");
                                targetSheep = null;
                                state = State.BUSCANDO_OVEJA;
                                return;
                            }
                        } else {
                            info("No hay tijeras en mano y autoEquipShears está desactivado.");
                            targetSheep = null;
                            state = State.BUSCANDO_OVEJA;
                            return;
                        }
                    }

                    try {
                        mc.interactionManager.interactEntity(mc.player, targetSheep, Hand.MAIN_HAND);
                    } catch (Throwable t) {
                        mc.player.swingHand(Hand.MAIN_HAND);
                    }
                    mc.player.swingHand(Hand.MAIN_HAND);
                    info("Esquilando oveja en " + posString(targetSheep.getBlockPos()));
                    shearedCount++;
                    state = State.ESQUILANDO;
                    actionDelay = shearingDelay.get();
                } else {
                    if (mc.player.age % 40 == 0) {
                        IBaritone b = getBaritone();
                        if (b != null && targetSheep != null) {
                            b.getCommandManager().execute("goto " + (int)Math.floor(targetSheep.getX()) + " " + (int)Math.floor(targetSheep.getY()) + " " + (int)Math.floor(targetSheep.getZ()));
                        }
                    }
                }
            }

            case ESQUILANDO -> {
                if (autoCollect.get()) {
                    state = State.RECOGIENDO;
                } else {
                    state = State.BUSCANDO_OVEJA;
                    targetSheep = null;
                    actionDelay = 5;
                }
            }

            case RECOGIENDO -> {
                List<ItemEntity> items = mc.world.getEntitiesByClass(ItemEntity.class, mc.player.getBoundingBox().expand(6), ent -> {
                    if (ent == null || ent.getStack().isEmpty()) return false;
                    String name = ent.getStack().getItem().toString().toLowerCase();
                    return name.contains("wool");
                });

                if (items != null && !items.isEmpty()) {
                    ItemEntity nearest = null;
                    double best = Double.MAX_VALUE;
                    for (ItemEntity it : items) {
                        double d = mc.player.squaredDistanceTo(it);
                        if (d < best) {
                            best = d;
                            nearest = it;
                        }
                    }

                    if (nearest != null) {
                        if (best <= 2.25) {
                            state = State.BUSCANDO_OVEJA;
                            targetSheep = null;
                            actionDelay = 5;
                        } else {
                            IBaritone b = getBaritone();
                            if (b != null) {
                                BlockPos ip = nearest.getBlockPos();
                                b.getCommandManager().execute("goto " + ip.getX() + " " + ip.getY() + " " + ip.getZ());
                                actionDelay = 20;
                                state = State.ESPERANDO;
                            } else {
                                actionDelay = 20;
                                state = State.ESPERANDO;
                            }
                        }
                    } else {
                        state = State.BUSCANDO_OVEJA;
                        targetSheep = null;
                    }
                } else {
                    state = State.BUSCANDO_OVEJA;
                    targetSheep = null;
                }
            }

            case ESPERANDO -> {
                state = State.BUSCANDO_OVEJA;
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

    private SheepEntity findNearestSheep(World world, BlockPos origin, int radius) {
        SheepEntity best = null;
        double bestDist = Double.MAX_VALUE;

        List<SheepEntity> list = world.getEntitiesByClass(SheepEntity.class, mc.player.getBoundingBox().expand(radius), e -> e != null && e.isAlive());
        if (list == null || list.isEmpty()) return null;

        for (SheepEntity s : list) {
            if (s.isSheared()) continue;
            if (onlyAdult.get() && s.isBaby()) continue;

            double d = mc.player.squaredDistanceTo(s);
            if (d < bestDist) {
                bestDist = d;
                best = s;
            }
        }
        return best;
    }

    private boolean hasShearsInMainHand() {
        return mc.player.getMainHandStack() != null && mc.player.getMainHandStack().getItem() == Items.SHEARS;
    }

    private int findShearsHotbarSlot() {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).getItem() == Items.SHEARS) return i;
        }
        for (int i = 9; i < mc.player.getInventory().size(); i++) {
            if (mc.player.getInventory().getStack(i).getItem() == Items.SHEARS) {
                int sel = mc.player.getInventory().selectedSlot;
                mc.player.getInventory().swap(sel, i);
                return sel;
            }
        }
        return -1;
    }

    private boolean hasInventorySpace() {
        return getEmptySlotCount() > 0;
    }

    private int getEmptySlotCount() {
        int cnt = 0;
        for (int i = 0; i < mc.player.getInventory().size(); i++) {
            if (mc.player.getInventory().getStack(i).isEmpty()) cnt++;
        }
        return cnt;
    }

    private String posString(BlockPos p) {
        return "(" + p.getX() + ", " + p.getY() + ", " + p.getZ() + ")";
    }
}
