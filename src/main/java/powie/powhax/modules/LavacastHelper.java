package powie.powhax.modules;

import meteordevelopment.meteorclient.events.entity.player.InteractBlockEvent;
import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WVerticalList;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.renderer.text.TextRenderer;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.meteorclient.utils.render.NametagUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import org.joml.Vector3d;
import powie.powhax.Powhax;

import java.util.Arrays;

public class LavacastHelper extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgCorners = settings.createGroup("Corners");

    private final Setting<castShape> shapeMode = sgGeneral.add(new EnumSetting.Builder<castShape>()
        .name("mode")
        .description("get the bedrock from floor or ceiling?")
        .defaultValue(castShape.Wedge)
        .build()
    );

    private final Setting<Integer> height = sgGeneral.add(new IntSetting.Builder()
        .name("height")
        .description("The Y level of the top of the lavacast")
        .defaultValue(200)
        .range(-64, 320)
        .sliderRange(100, 320)
        .build()
    );

    private final Setting<Keybind> calcKeybind = sgGeneral.add(new KeybindSetting.Builder()
        .name("Calculate keybind")
        .description("keybind to calculate the lavacast corners")
        .action(this::calc)
        .build()
    );

    private final Setting<Keybind> quickSelectKeybind = sgGeneral.add(new KeybindSetting.Builder()
        .name("Quick Select keybind")
        .description("keybind to calculate the lavacast corners")
        .action(this::startQuickSelecting)
        .build()
    );

    private final Setting<BlockPos> corner1 = sgCorners.add(new BlockPosSetting.Builder()
        .name("corner-1")
        .description("The first corner of the lavacast")
        .build()
    );

    private final Setting<BlockPos> corner2 = sgCorners.add(new BlockPosSetting.Builder()
        .name("corner-2")
        .description("The second corner of the lavacast")
        .build()
    );

    private final Setting<BlockPos> corner3 = sgCorners.add(new BlockPosSetting.Builder()
        .name("corner-3")
        .description("The third corner of the lavacast")
        .build()
    );

    private final Setting<BlockPos> corner4 = sgCorners.add(new BlockPosSetting.Builder()
        .name("corner-4")
        .description("The fourth corner of the lavacast")
        .build()
    );

    private BlockPos corner5 = new BlockPos(0, 0, 0);
    private boolean shouldRender5thPoint, isQuickSelecting;
    private boolean isFirstCorner = true;

    public LavacastHelper() {
        super(Powhax.CATEGORY, "lavacast-helper", "Helping tools for making lavacast");
    }

    @Override
    public void onDeactivate() {
        shouldRender5thPoint = false;
    }

    @Override
    public WWidget getWidget(GuiTheme theme) {
        WVerticalList l = theme.verticalList();
        WButton CalcButton = l.add(theme.button("Calculate")).expandX().widget();
        CalcButton.action = this::calc;
        WButton quickSelectButton = l.add(theme.button("Quick Select")).expandX().widget();
        quickSelectButton.action = this::startQuickSelecting;
        return l;
    }

    @EventHandler
    private void onRender3d(Render3DEvent event) {
        event.renderer.box(new AABB(corner1.get()), Color.BLUE, Color.RED, ShapeMode.Lines, 0);
        event.renderer.box(new AABB(corner2.get()), Color.BLUE, Color.GREEN, ShapeMode.Lines, 0);
        event.renderer.box(new AABB(corner3.get()), Color.BLUE, Color.YELLOW, ShapeMode.Lines, 0);
        event.renderer.box(new AABB(corner4.get()), Color.BLUE, Color.BLUE, ShapeMode.Lines, 0);
        if (shouldRender5thPoint) event.renderer.box(new AABB(corner5), Color.BLUE, Color.WHITE, ShapeMode.Lines, 0);
    }

    @EventHandler
    private void onRender2d(Render2DEvent event) {
        renderCornerLabels(corner1.get(), "1");
        renderCornerLabels(corner2.get(), "2");
        renderCornerLabels(corner3.get(), "3");
        renderCornerLabels(corner4.get(), "4");
        if (shouldRender5thPoint) renderCornerLabels(corner5, "5");
    }

    @EventHandler
    private void onInteractBlock(InteractBlockEvent event) {
        if (!isQuickSelecting) return;
        if (event.result.getType() == HitResult.Type.MISS) return;

        if (isFirstCorner) {
            isFirstCorner = false;
            corner1.set(event.result.getBlockPos());
            info("Select second corner");
            event.cancel();
            return;
        }
        corner3.set(event.result.getBlockPos());

        corner2.set(new BlockPos(corner1.get().getX(), 0, corner3.get().getZ()));
        corner4.set(new BlockPos(corner3.get().getX(), 0, corner1.get().getZ()));

        calc();

        info("done!");

        event.cancel();
        isFirstCorner = true;
        isQuickSelecting = false;
    }

    private void startQuickSelecting() {
        info("Select first corner");
        isQuickSelecting = true;
        mc.setScreen(null);
    }

    // calc is short for calculator btw im just using slang
    private void calc() {
        shouldRender5thPoint = false;
        if (!isRectangle()) {
            error("Corners don't make a square nor a rectangle");
            return;
        }

        corner1.set(new BlockPos(corner1.get().getX(), height.get(), corner1.get().getZ()));

        corner2.set(adjustCorner(corner1.get(), corner2.get()));
        corner3.set(adjustCorner(corner2.get(), corner3.get()));
        if (shapeMode.get() == castShape.Spiral) {
            corner4.set(adjustCorner(corner3.get(), corner4.get()));

            BlockPos c1 = corner1.get();
            BlockPos c5 = adjustCorner(corner4.get(), corner1.get());
            corner5 = new BlockPos(c1.getX(), c5.getY(), c1.getZ());
            shouldRender5thPoint = true;
        } else {
            corner4.set(adjustCorner(corner1.get(), corner4.get()));
        }
    }

    private BlockPos adjustCorner(BlockPos from, BlockPos to) {
        long dx = from.getX() - to.getX();
        long dz = from.getZ() - to.getZ();
        double dist = Math.sqrt(dx * dx + dz * dz);
        int newY = (int) Math.round(from.getY() - dist);

        return new BlockPos(to.getX(), newY, to.getZ());
    }

    private boolean isRectangle() {
        BlockPos[] points = {
            corner1.get(),
            corner2.get(),
            corner3.get(),
            corner4.get()
        };

        long[] distances = new long[6];
        int k = 0;

        for (int i = 0; i < 4; i++) {
            for (int j = i + 1; j < 4; j++) {
                distances[k++] = distanceSquared(points[i], points[j]);
            }
        }

        Arrays.sort(distances);

        // Duplicate points
        if (distances[0] == 0)
            return false;

        // Diagonals must match
        if (distances[4] != distances[5])
            return false;

        // Square
        if (distances[0] == distances[3])
            return distances[4] == 2 * distances[0];

        // Rectangle
        return distances[0] == distances[1]
            && distances[2] == distances[3]
            && distances[4] == distances[0] + distances[2];
    }

    private long distanceSquared(BlockPos p1, BlockPos p2) {
        long dz = p1.getZ() - p2.getZ();
        long dx = p1.getX() - p2.getX();
        return dx * dx + dz * dz;
    }

    private void renderCornerLabels(BlockPos corner, String label) {
        Vector3d blockPosToVec3 = new Vector3d(corner.getX() + .5, corner.getY() + .5, corner.getZ() + .5);
        if (NametagUtils.to2D(blockPosToVec3, 1.5, true)) {
            NametagUtils.begin(blockPosToVec3);
            TextRenderer.get().begin(1, false, true);
            TextRenderer.get().render(label, -TextRenderer.get().getWidth(label) / 2, 0, Color.WHITE, true);
            TextRenderer.get().end();
            NametagUtils.end();
        }
    }

    private enum castShape {
        Wedge,
        Spiral,
    }
}
