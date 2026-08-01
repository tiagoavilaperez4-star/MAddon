package com.tiagoavilaperez4.addon;

import com.mojang.logging.LogUtils;
import meteordevelopment.meteorclient.addons.GithubRepo;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.commands.Commands;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.Category;
import org.slf4j.Logger;

public class AddonMain extends MeteorAddon {
    public static final Logger LOG = LogUtils.getLogger();
    public static final Category CATEGORY = new Category("MAddon");

    @Override
    public void onInitialize() {
        LOG.info("Initializing MAddon");

        // Register modules
        Modules.get().add(new com.tiagoavilaperez4.addon.modules.WalkNearCave());

        // Register commands here when added
    }

    @Override
    public void onRegisterCategories() {
        Modules.registerCategory(CATEGORY);
    }

    @Override
    public String getPackage() {
        return "com.tiagoavilaperez4.addon";
    }

    @Override
    public GithubRepo getRepo() {
        return new GithubRepo("tiagoavilaperez4-star", "MAddon");
    }
}
