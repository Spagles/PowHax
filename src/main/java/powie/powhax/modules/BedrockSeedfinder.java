package powie.powhax.modules;

import meteordevelopment.meteorclient.events.world.ChunkDataEvent;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.widgets.WLabel;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WHorizontalList;
import meteordevelopment.meteorclient.gui.widgets.containers.WVerticalList;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Util;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import org.lwjgl.util.tinyfd.TinyFileDialogs;
import powie.powhax.Powhax;

import java.io.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BedrockSeedfinder extends Module {
    SettingGroup sgDefault = settings.getDefaultGroup();

    private final Setting<yLevel> searchY = sgDefault.add(new EnumSetting.Builder<BedrockSeedfinder.yLevel>()
        .name("y-level")
        .description("get the bedrock from floor or ceiling?")
        .defaultValue(yLevel.Floor)
        .build());

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private static File saveFile = new File("D:/br.txt");
    private BufferedWriter writer;

    /**
     * Thanks to <a href="https://github.com/Nippaku-Zanmu/">Nippaku Zanmu</a> for initial code snippet
     */
    public BedrockSeedfinder() {
        super(Powhax.CATEGORY,
            "bedrock-seedfinder",
            "Writes the position of bedrock in the nether in a file. meant for seed cracking.",
            "Seedcracker");
    }


    @Override
    public WWidget getWidget(GuiTheme theme) {
        WVerticalList l = theme.verticalList();

        WLabel pathLabel = l.add(theme.label(getFilePath())).widget();

        WHorizontalList horizontalList = l.add(theme.horizontalList()).widget();

        WButton changeBtn = horizontalList.add(theme.button("Change save file")).widget();
        WButton resetBtn = horizontalList.add(theme.confirmedButton(GuiRenderer.RESET)).right().widget();

        resetBtn.action = () -> {
            saveFile = new File("D:/br.txt");
            pathLabel.set(getFilePath());
        };

        changeBtn.action = () -> {
            String file = TinyFileDialogs.tinyfd_openFileDialog(
                "Select profile to import",
                null,
                null,
                null,
                false
            );
            if (file == null) return;
            saveFile = new File(file);
            pathLabel.set(getFilePath());
        };

        l.add(theme.horizontalSeparator()).expandX();

        l.add(theme.label("This module is meant to be used for Nether Bedrock Cracker"));
        l.add(theme.label("https://github.com/19MisterX98/Nether_Bedrock_Cracker/"));
        WButton button = l.add(theme.button("open github repo")).widget();
        button.action = () -> Util.getPlatform().openUri("https://github.com/19MisterX98/Nether_Bedrock_Cracker/");
        return l;
    }

    @EventHandler
    private void onChunkData(ChunkDataEvent event) {
        // implementation note is only a suggestion right?
        executor.execute(() -> {
            ChunkAccess c = event.chunk();
            for (int x = c.getPos().getMinBlockX(); x <= c.getPos().getMaxBlockX(); x++) {
                for (int z = c.getPos().getMinBlockZ(); z <= c.getPos().getMaxBlockZ(); z++) {
                    BlockPos sPos = new BlockPos(x, searchY.get().getValue(), z);
                    if (!c.getBlockState(sPos).getBlock().equals(Blocks.BEDROCK)) continue;
                    try {
                        String s = sPos.getX() + " " + sPos.getY() + " " + sPos.getZ() + " Bedrock";
                        writer.write(s);
                        writer.newLine();
                    } catch (IOException e) {
                        info(e.getMessage());
                    }
                }
            }
        });
    }

    @Override
    public void onActivate() {
        if (isInvalidFile(saveFile)) {
            toggle();
            return;
        }
        if (!mc.level.dimensionTypeRegistration().is(BuiltinDimensionTypes.NETHER))
            warning("This module is only meant to be used in the nether");
        try {
            writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(saveFile)));
        } catch (FileNotFoundException e) {
            error("Failed to open file for writing: " + e.getMessage());
            toggle();
        }
    }

    @Override
    public void onDeactivate() {
        try {
            if (writer != null) {
                writer.flush();
                writer.close();
                info("Done writing to file");
            }
        } catch (IOException e) {
            info(e.getMessage());
        }
    }

    private boolean isInvalidFile(File file) {
        if (!file.exists()) {
            error("File does not exist");
            return true;
        }
        if (!file.isFile()) {
            error("The specified path is a directory, not a file");
            return true;
        }
        if (!file.canWrite()) {
            error("File is not writable");
            return true;
        }
        return false;
    }

    private String getFilePath() {
        return "Save File path: " + saveFile.getPath();
    }

    public enum yLevel {
        Floor(4),
        Ceiling(123);

        private final int value;

        yLevel(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }
}
