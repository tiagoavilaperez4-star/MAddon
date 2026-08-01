package com.tiagoavilaperez4.addon.modules;

import com.tiagoavilaperez4.addon.AddonMain;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;

public class WalkNearCave extends Module {
    private final SettingGroup sgGeneral = this.settings.getDefaultGroup();

    private final Setting<Integer> radioBusqueda = sgGeneral.add(new IntSetting.Builder()
        .name("radio-busqueda")
        .description("Radio para buscar entradas de cuevas")
        .defaultValue(30)
        .range(10, 100)
        .build()
    );

    private final Setting<Integer> distanciaCaminar = sgGeneral.add(new IntSetting.Builder()
        .name("distancia-caminar")
        .description("Distancia a la que caminar desde la entrada")
        .defaultValue(3)
        .range(1, 10)
        .build()
    );

    private final Setting<Integer> tiempoEspera = sgGeneral.add(new IntSetting.Builder()
        .name("tiempo-espera")
        .description("Ticks de espera en cada cueva")
        .defaultValue(40)
        .range(10, 200)
        .build()
    );

    public WalkNearCave() {
        super(AddonMain.CATEGORY, "walk-near-cave", "Busca entradas de cuevas cercanas y camina hacia ellas.");
    }

    // TODO: Implement cave-finding and walking behavior using the Minecraft client & pathfinding utilities.
}
