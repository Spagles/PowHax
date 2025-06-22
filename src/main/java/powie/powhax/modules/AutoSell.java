package powie.powhax.modules;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.StringSetting;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.screen.slot.SlotActionType;
import powie.powhax.Powhax;

public class AutoSell extends Module {
    private final SettingGroup sgGeneral = this.settings.getDefaultGroup();

    private final Setting<Integer> slotDelay = sgGeneral.add(new IntSetting.Builder()
        .name("slot-click-delay")
        .description("The delay before clicking the slot in minecraft ticks")
        .defaultValue(5)
        .min(0)
        .sliderMin(0)
        .max(2400)
        .sliderMax(2400)
        .build()
    );

    private final Setting<String> containerName = sgGeneral.add(new StringSetting.Builder()
        .name("container-name")
        .description("Only sells if the name of the container matches")
        .defaultValue("Tradeview")
        .build()
    );

    public AutoSell() {
        super(Powhax.CATEGORY, "auto-sell", "Automatically sells a stack in Tradeview");
    }

    int timer = 0;

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.currentScreen == null) return;
        ClientPlayerEntity player = mc.player;
        if (player == null || player.currentScreenHandler == null) return;
        if (!mc.currentScreen.getTitle().getString().equals(containerName.get()) || player.currentScreenHandler.slots.size() != 54) return; // 63 // 54
        if (timer <= slotDelay.get()) {
            timer++;
            return;
        }

        mc.interactionManager.clickSlot(
            player.currentScreenHandler.syncId,
            8,
            0,
            SlotActionType.PICKUP,
            player
        );

        timer = 0;
    }
}
