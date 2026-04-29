package powie.powhax;

import com.mojang.logging.LogUtils;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.addons.GithubRepo;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.commands.Commands;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.fabricmc.loader.api.metadata.CustomValue;
import org.slf4j.Logger;
import powie.powhax.commands.ClearChat;
import powie.powhax.commands.Coords;
import powie.powhax.commands.Xp;
import powie.powhax.modules.*;

import static meteordevelopment.meteorclient.MeteorClient.MOD_META;

public class Powhax extends MeteorAddon {
    public static final Logger LOG = LogUtils.getLogger();
    public static final Category CATEGORY = new Category("PowHax");
//    public static final HudGroup HUD_GROUP = new HudGroup("PowHax");

    @Override
    public void onInitialize() {
        LOG.info("Initializing PowHax");

        // Modules
        Modules.get().add(new ArmorBuster());
        Modules.get().add(new AutoEndorse());
        Modules.get().add(new AutoLogin());
        Modules.get().add(new AutoSell());
        Modules.get().add(new BedrockPrinter());
        Modules.get().add(new BedrockPrinter());
        Modules.get().add(new BlazeFarm());
        Modules.get().add(new DeathCommands());
        Modules.get().add(new FlySpeed());
        Modules.get().add(new HandDerp());
        Modules.get().add(new SmiteAura());
        Modules.get().add(new TrajectoriesPlus());

        // Commands
        Commands.add(new ClearChat());
        Commands.add(new Coords());
        Commands.add(new Xp());

        // HUD
    }

    @Override
    public void onRegisterCategories() {
        Modules.registerCategory(CATEGORY);
    }

    @Override
    public String getWebsite() {
        return "https://github.com/Powie69/PowHax";
    }

    @Override
    public GithubRepo getRepo() {
        return new GithubRepo("Powie69", "Powhax", "master", null);
    }

    @Override
    public String getPackage() {
        return "powie.powhax";
    }

    @Override
    public String getCommit() {
        CustomValue commit = MOD_META.getCustomValue(MeteorClient.MOD_ID + ":commit");
        String commitStr = commit == null ? "" : commit.getAsString();
        return commitStr.isEmpty() ? null : commitStr;
    }
}
