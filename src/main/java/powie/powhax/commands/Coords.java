package powie.powhax.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.core.BlockPos;

public class Coords extends Command {
    public Coords() {
        super("coordinates", "Gets your coordinates", "coords", "position", "pos");
    }

    @Override
    public void build(LiteralArgumentBuilder<ClientSuggestionProvider> builder) {
        builder.executes(_ -> copyPos());
        builder.then(literal("copy").executes(_ -> copyPos()));
        builder.then(literal("print").executes(_ -> {
            info(getPos());
            return SINGLE_SUCCESS;
        }));
        builder.then(literal("share-in-chat").executes(_ -> {
            ChatUtils.sendPlayerMsg(getPos());
            return SINGLE_SUCCESS;
        }));
    }

    private String getPos() {
        BlockPos pos = mc.player.blockPosition();
        return pos.getX() + ", " + pos.getY() + ", " + pos.getZ();
    }

    private int copyPos() {
        mc.keyboardHandler.setClipboard(getPos());
        info("Coordinates were copied to your clipboard");
        return SINGLE_SUCCESS;
    }
}
