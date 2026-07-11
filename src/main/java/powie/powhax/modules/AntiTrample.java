package powie.powhax.modules;

import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.mixin.ServerboundMovePlayerPacketAccessor;
import meteordevelopment.meteorclient.mixininterface.IServerboundMovePlayerPacket;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.level.block.Blocks;
import powie.powhax.Powhax;

public class AntiTrample extends Module {
    /**
     * "inspiration" from <a href="https://github.com/moxvallix">moxvallix</a> with minor improvements
     */
    public AntiTrample() {
        super(Powhax.CATEGORY,
            "anti-trample",
            "Prevents you from trampling farmland",
            "farmland protect");
    }

    @EventHandler
    private void onSendPacket(PacketEvent.Send event) {
        if (!(event.packet instanceof ServerboundMovePlayerPacket packet)
            || ((IServerboundMovePlayerPacket) event.packet).meteor$getTag() == 1337) return;

        BlockPos blockPos = BlockPos.containing(packet.getX(0d), packet.getY(0d), packet.getZ(0d));

        BlockPos[] posChecks = {
            blockPos.offset(0, -1, 0),
            blockPos.offset(-1, -1, 0),
            blockPos.offset(0, -1, -1),
            blockPos.offset(0, -1, 1),
            blockPos.offset(1, -1, 0),
            blockPos.offset(-1, -1, -1),
            blockPos.offset(-1, -1, 1),
            blockPos.offset(1, -1, -1),
            blockPos.offset(1, -1, 1)
        };

        for (BlockPos pos : posChecks) {
            if (mc.level.getBlockState(pos).is(Blocks.FARMLAND)) {
                ((ServerboundMovePlayerPacketAccessor) event.packet).meteor$setOnGround(true);
                break;
            }
        }
    }
}
