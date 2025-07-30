package powie.powhax.modules;

import meteordevelopment.meteorclient.events.game.OpenScreenEvent;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WVerticalList;
import meteordevelopment.meteorclient.mixin.AbstractSignEditScreenAccessor;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.entity.SortPriority;
import meteordevelopment.meteorclient.utils.entity.TargetUtils;
import meteordevelopment.meteorclient.utils.entity.fakeplayer.FakePlayerEntity;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.client.gui.screen.ingame.HangingSignEditScreen;
import net.minecraft.client.gui.screen.ingame.SignEditScreen;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.c2s.play.UpdateSignC2SPacket;
import powie.powhax.Powhax;

public class AutoEndorse extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgFront = settings.createGroup("Text");

    private final Setting<Target> targets = sgGeneral.add(new EnumSetting.Builder<Target>()
        .name("targets")
        .description("Who to target")
        .defaultValue(Target.Everyone)
        .build()
    );

    //  Distance to the player. Not the placed sign btw.
    private final Setting<SortPriority> targetPriority = sgGeneral.add(new EnumSetting.Builder<SortPriority>()
        .name("target-priority")
        .description("How to prioritize targets")
        .defaultValue(SortPriority.ClosestAngle)
        .build()
    );

    private final Setting<Double> targetRange = sgGeneral.add(new DoubleSetting.Builder()
        .name("target-range")
        .description("The maximum range the player can be to be targeted")
        .defaultValue(10)
        .sliderMax(20)
        .build()
    );

    // lines

    private final Setting<String> firstLine = sgFront.add(new StringSetting.Builder()
        .name("first-line-front")
        .description("The first line of the sign")
        .defaultValue("I Love using")
        .build()
    );

    private final Setting<String> secondLine = sgFront.add(new StringSetting.Builder()
        .name("second-line-front")
        .description("The second line of the sign")
        .defaultValue("Powhax it's")
        .build()
    );

    private final Setting<String> thirdLine = sgFront.add(new StringSetting.Builder()
        .name("third-line-front")
        .description("The third line of the sign")
        .defaultValue("the best")
        .build()
    );

    public AutoEndorse() {
        super(Powhax.CATEGORY, "Auto-Endorse", "Places a sign with specified text. The last line will contain the name of the player nearest to you");
    }

    @Override
    public WWidget getWidget(GuiTheme theme) {
        WVerticalList l = theme.verticalList();
        l.add(theme.label("Note: The 4th line will be the Target's name if the other lines are full"));
        return l;
    }

    @EventHandler
    private void onOpenScreen(OpenScreenEvent event) {
        if (mc.player == null || mc.world == null) return;
        if(!(event.screen instanceof SignEditScreen) && !(event.screen instanceof HangingSignEditScreen)) return;

        SignBlockEntity sign = ((AbstractSignEditScreenAccessor) event.screen).meteor$getSign();

        Entity target = getTarget(targetRange.get(), targetPriority.get());
        if (target == null) {
            warning("no target found");
            return;
        }

        String[] lines = composeLines();

        mc.player.networkHandler.sendPacket(new UpdateSignC2SPacket(
                sign.getPos(),
                true,
                lines[0],
                lines[1],
                lines[2],
                lines[3]
        ));

        event.cancel();
    }

    private String[] composeLines() {
        String[] lines = {firstLine.get(), secondLine.get(), thirdLine.get(), ""};

        // Find first empty line with no non-empty content after it
        for (int i = 0; i < lines.length; i++) {
            if (!lines[i].trim().isEmpty()) continue;
            // Check if all remaining lines are empty
            boolean allEmptyAfter = true;
            for (int j = i + 1; j < lines.length; j++) {
                if (!lines[j].trim().isEmpty()) {
                    allEmptyAfter = false;
                    break;
                }
            }
            if (allEmptyAfter) {
                lines[i] = "-" + getTarget(targetRange.get(), targetPriority.get()).getName().getString();
                break;
            }

        }

        return lines;
    }

    private PlayerEntity getTarget(double range, SortPriority priority) {
        if (!Utils.canUpdate()) return null;
        return (PlayerEntity) TargetUtils.get(entity -> {
            if (!(entity instanceof PlayerEntity) || entity == mc.player) return false;
            if (((PlayerEntity) entity).isDead() || ((PlayerEntity) entity).getHealth() <= 0) return false;
            if (!PlayerUtils.isWithin(entity, range)) return false;
            if (targets.get() == Target.Friends && !Friends.get().isFriend((PlayerEntity) entity)) return false;
            if (targets.get() == Target.NonFriends && Friends.get().isFriend((PlayerEntity) entity)) return false;
            return entity instanceof FakePlayerEntity;
        }, priority);
    }

    public enum Target {
        Everyone,
        Friends,
        NonFriends
    }
}
