package powie.powhax.modules;

import meteordevelopment.meteorclient.events.entity.player.DoAttackEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.protocol.common.ServerboundClientInformationPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.world.entity.HumanoidArm;
import powie.powhax.Powhax;

public class HandDerp extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    // TODO: when turned on, switch to the original hand.
    private final Setting<Boolean> silent = sgGeneral.add(new BoolSetting.Builder()
        .name("Silent")
        .description("Hides hand switching animations on the client side")
        .defaultValue(true)
        .build()
    );

    private final Setting<switchMode> mode = sgGeneral.add(new EnumSetting.Builder<switchMode>()
        .name("mode")
        .description("Choose the trigger method for hand switching")
        .defaultValue(switchMode.TickDelay)
        .build()
    );

    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
        .name("Delay")
        .description("Delay of switching hands in ticks")
        .defaultValue(100) // 5 seconds
        .min(1)
        .sliderMin(1)
        .sliderMax(1200) // 1 minutes
        .visible(() -> mode.get() == switchMode.TickDelay)
        .build()
    );

    private int delayCounter = 0;
    private HumanoidArm originalHand = mc.options.mainHand().get();
    private HumanoidArm currentHand = mc.options.mainHand().get();

    public HandDerp() {
        super(Powhax.CATEGORY, "hand-derp", "Automatically switches between left and right main hand.");
    }

    @Override
    public void onActivate() {
        originalHand = mc.options.mainHand().get();
    }

    @Override
    public void onDeactivate() {
        mc.options.mainHand().set(originalHand);
        ClientInformation updatedInfo = createClientInformationWithHand(originalHand);
        mc.player.connection.send(new ServerboundClientInformationPacket(updatedInfo));
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mode.get() != switchMode.TickDelay) return;
        if (delayCounter <= delay.get()) {
            delayCounter++;
            return;
        }
        switchHand();
        delayCounter = 0;
    }

    @EventHandler
    private void onAttack(DoAttackEvent event) {
        if (mode.get() != switchMode.OnAttack) return;
        switchHand();
    }

    // data id 15 is hand thing
    @EventHandler
    private void onReceivePacket(PacketEvent.Receive event) {
        if (!silent.get()) return;
        if (!(event.packet instanceof ClientboundSetEntityDataPacket packet)) return;

        for (var data : packet.packedItems()) {
            if (data.id() != 15) continue;
            event.cancel();
            return;
        }
    }

    private void switchHand() {
        currentHand = currentHand == HumanoidArm.LEFT ? HumanoidArm.RIGHT : HumanoidArm.LEFT;
        ClientInformation updatedInfo = createClientInformationWithHand(currentHand);
        mc.player.connection.send(new ServerboundClientInformationPacket(updatedInfo));
    }

    // usually I'll repeat this code because its only 2 usages but Claude said so.....
    private ClientInformation createClientInformationWithHand(HumanoidArm hand) {
        ClientInformation currentInfo = mc.options.buildPlayerInformation();
        return new ClientInformation(
            currentInfo.language(),
            currentInfo.viewDistance(),
            currentInfo.chatVisibility(),
            currentInfo.chatColors(),
            currentInfo.modelCustomisation(),
            hand,
            currentInfo.textFilteringEnabled(),
            currentInfo.allowsListing(),
            currentInfo.particleStatus()
        );
    }

    private enum switchMode {
        TickDelay,
        OnAttack
    }
}
