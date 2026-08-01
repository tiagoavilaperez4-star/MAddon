package com.example.addon.modules;

import baritone.api.BaritoneAPI;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.SettingGroups;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Module;
import net.minecraft.block.BlockState;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * WalkNearCave - busca entradas de cuevas en superficie y las visita sin entrar.
 */
public class WalkNearCave extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> searchRadius = sgGeneral.add(new IntSetting.Builder()
            .name("search-radius")
            .description("Radio de búsqueda alrededor del jugador para encontrar entradas de cuevas.")
            .defaultValue(30)
            .min(10)
            .max(100)
            .build()
    );

    private final Setting<Integer> walkDistance = sgGeneral.add(new IntSetting.Builder()
            .name("walk-distance")
            .description("Distancia a la que pararse de la entrada (no entrar).")
            .defaultValue(3)
            .min(1)
            .max(10)
            .build()
    );

    private final Setting<Integer> waitTime = sgGeneral.add(new IntSetting.Builder()
            .name("wait-time")
            .description("Ticks a esperar en cada cueva antes de buscar la siguiente.")
            .defaultValue(40)
            .min(10)
            .max(200)
            .build()
    );

    private final List<BlockPos> visited = new ArrayList<>();
    private BlockPos currentCave = null;
    private BlockPos currentTarget = null;
    private int waitTicks = 0;
    private final Random random = new Random();

    public WalkNearCave() {
        super(Category.Movement, "walk-near-cave", "Busca entradas de cuevas en superficie y camina cerca sin entrar.");
    }

    @Override
    public void onActivate() {
        visited.clear();
        currentCave = null;
        currentTarget = null;
        waitTicks = 0;
        info("WalkNearCave activado");
    }

    @Override
    public void onDeactivate() {
        try {
            if (BaritoneAPI.getProvider() != null && BaritoneAPI.getProvider().getPrimaryBaritone() != null) {
                BaritoneAPI.getProvider().getPrimaryBaritone().getCommandManager().execute("stop");
            }
        } catch (Throwable ignored) {}
        currentCave = null;
        currentTarget = null;
        waitTicks = 0;
        info("WalkNearCave desactivado");
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.world == null) return;

        if (waitTicks > 0) {
            waitTicks--;
            if (waitTicks == 0) {
                if (currentCave != null) {
                    visited.add(currentCave);
                    info("Marcada cueva como visitada: " + posString(currentCave));
                }
                currentCave = null;
                currentTarget = null;
            }
            return;
        }

        if (currentTarget != null) {
            double dx = mc.player.getX() - (currentTarget.getX() + 0.5);
            double dy = mc.player.getY() - (currentTarget.getY());
            double dz = mc.player.getZ() - (currentTarget.getZ() + 0.5);
            double distSq = dx * dx + dy * dy + dz * dz;
            if (distSq <= 4.0) {
                waitTicks = waitTime.get();
                info("Llegado a la cueva cercana: " + posString(currentCave) + " — esperando " + waitTicks + " ticks");
            }
            return;
        }

        BlockPos playerPos = mc.player.getBlockPos();
        BlockPos foundCave = findNearestCave(mc.world, playerPos, searchRadius.get());
        if (foundCave != null) {
            if (wasVisited(foundCave)) {
                // continue
            } else {
                BlockPos approach = computeApproachPosition(mc.world, mc.player, foundCave, walkDistance.get());
                if (approach != null) {
                    currentCave = foundCave;
                    currentTarget = approach;
                    try {
                        if (BaritoneAPI.getProvider() != null && BaritoneAPI.getProvider().getPrimaryBaritone() != null) {
                            String cmd = "goto " + approach.getX() + " " + approach.getY() + " " + approach.getZ();
                            BaritoneAPI.getProvider().getPrimaryBaritone().getCommandManager().execute(cmd);
                            info("Cambiando a cueva: " + posString(foundCave) + " — yendo a " + posString(approach));
                            return;
                        }
                    } catch (Throwable t) {}
                }
            }
        }

        BlockPos randomTarget = computeRandomTarget(mc.world, playerPos, searchRadius.get());
        if (randomTarget != null) {
            currentCave = null;
            currentTarget = randomTarget;
            try {
                if (BaritoneAPI.getProvider() != null && BaritoneAPI.getProvider().getPrimaryBaritone() != null) {
                    String cmd = "goto " + randomTarget.getX() + " " + randomTarget.getY() + " " + randomTarget.getZ();
                    BaritoneAPI.getProvider().getPrimaryBaritone().getCommandManager().execute(cmd);
                    info("No se encontró cueva, moviéndose a posición aleatoria: " + posString(randomTarget));
                }
            } catch (Throwable ignored) {
                currentTarget = null;
            }
        }
    }

    private BlockPos findNearestCave(World world, BlockPos origin, int radius) {
        BlockPos best = null;
        double bestDistSq = Double.MAX_VALUE;
        int ox = origin.getX();
        int oy = origin.getY();
        int oz = origin.getZ();

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                int x = ox + dx;
                int z = oz + dz;
                double planarDistSq = dx * dx + dz * dz;
                if (planarDistSq > radius * radius) continue;

                for (int y = oy + 6; y >= Math.max(0, oy - 20); y--) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockPos below = pos.down();

                    BlockState stateAtPos = world.getBlockState(pos);
                    BlockState stateBelow = world.getBlockState(below);

                    boolean atPosSolid = !stateAtPos.isAir();
                    boolean belowIsAir = stateBelow.isAir();

                    if (atPosSolid && belowIsAir) {
                        double dSq = origin.getSquaredDistance(x + 0.5, y + 0.5, z + 0.5);
                        if (dSq < bestDistSq) {
                            bestDistSq = dSq;
                            best = pos;
                        }
                        break;
                    }
                }
            }
        }

        return best;
    }

    private boolean wasVisited(BlockPos pos) {
        for (BlockPos p : visited) {
            if (p.equals(pos)) return true;
        }
        return false;
    }

    private BlockPos computeApproachPosition(World world, ClientPlayerEntity player, BlockPos cavePos, int walkDist) {
        double px = player.getX();
        double pz = player.getZ();
        double cx = cavePos.getX() + 0.5;
        double cz = cavePos.getZ() + 0.5;

        double dx = px - cx;
        double dz = pz - cz;
        double len = Math.sqrt(dx * dx + dz * dz);
        if (len <= 0.001) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            dx = Math.cos(angle);
            dz = Math.sin(angle);
            len = 1.0;
        }

        double nx = dx / len;
        double nz = dz / len;

        double tx = cx + nx * walkDist;
        double tz = cz + nz * walkDist;

        int ix = MathHelper.floor(tx);
        int iz = MathHelper.floor(tz);

        int startY = MathHelper.floor(player.getY()) + 5;
        int minY = Math.max(0, startY - 40);

        for (int y = startY; y >= minY; y--) {
            BlockPos feet = new BlockPos(ix, y, iz);
            BlockPos below = feet.down();
            BlockState stateBelow = world.getBlockState(below);
            BlockState stateFeet = world.getBlockState(feet);

            if (!stateBelow.isAir() && stateFeet.isAir()) {
                if (below.equals(cavePos) || below.equals(cavePos.down())) continue;
                return feet;
            }
        }

        int playerY = MathHelper.floor(player.getY());
        BlockPos candidate = new BlockPos(ix, playerY, iz);
        if (!world.getBlockState(candidate.down()).isAir()) return candidate;

        return null;
    }

    private BlockPos computeRandomTarget(World world, BlockPos origin, int radius) {
        int ox = origin.getX();
        int oz = origin.getZ();
        int attempts = 16;
        while (attempts-- > 0) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double dist = 4 + random.nextDouble() * (radius - 4);
            int tx = ox + MathHelper.floor(Math.cos(angle) * dist);
            int tz = oz + MathHelper.floor(Math.sin(angle) * dist);
            int startY = origin.getY() + 10;
            for (int y = startY; y >= Math.max(1, startY - 60); y--) {
                BlockPos feet = new BlockPos(tx, y, tz);
                if (!world.getBlockState(feet.down()).isAir() && world.getBlockState(feet).isAir()) {
                    return feet;
                }
            }
        }
        return null;
    }

    private String posString(BlockPos p) {
        return "(" + p.getX() + ", " + p.getY() + ", " + p.getZ() + ")";
    }
}
