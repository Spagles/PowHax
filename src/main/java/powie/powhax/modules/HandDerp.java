package powie.powhax.modules;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.util.Arm;
import powie.powhax.Powhax;

public class HandDerp extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> silent = sgGeneral.add(new BoolSetting.Builder()
            .name("Silent")
            .description("Whether to show hand switching client")
            .defaultValue(true)
            .build()
    );

    // TODO: pick modes. tick delay or on hand swing/attack

//    private final Setting<enum>

    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
            .name("Delay")
            .description("Delay of switching hands in ticks")
            .defaultValue(100) // 5 seconds
                    .min(1)
                    .sliderMin(1)
                    .sliderMax(1200) // 1 minutes
            .build()
    );

    private int delayCounter = 0;
    private Arm originalHand = mc.options.getMainArm().getValue();
    private Arm currentHand = mc.options.getMainArm().getValue();

    public HandDerp() {
        super(Powhax.CATEGORY, "hand-derp", "Switches your main hand.");
    }

    @Override
    public void onActivate() {
        originalHand = mc.options.getMainArm().getValue();
    }

    @Override
    public void onDeactivate() {
        mc.options.getMainArm().setValue(originalHand);
        mc.options.sendClientSettings();
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (delayCounter <= delay.get()) {
            delayCounter++;
            return;
        }
        switchHand();
        info(String.valueOf(currentHand));
        delayCounter = 0;
    }

    private void switchHand() {
        currentHand = currentHand == Arm.LEFT ? Arm.RIGHT : Arm.LEFT;
        mc.options.getMainArm().setValue(currentHand);
        mc.options.sendClientSettings();
    }

//    private enum mode
}
