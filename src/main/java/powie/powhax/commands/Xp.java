package powie.powhax.commands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;

/**
 * The Meteor Client command API uses the <a href="https://github.com/Mojang/brigadier">same command system as Minecraft does</a>.
 */

public class Xp extends Command {
    public Xp() {
        super("xp", "Adds client sided XP", "add-xp", "experience", "xp");
    }

    @Override
    public void build(LiteralArgumentBuilder<ClientSuggestionProvider> builder) {
        builder.then(argument("amount", IntegerArgumentType.integer(Integer.MIN_VALUE, Integer.MAX_VALUE)).executes(context -> {
            mc.player.giveExperiencePoints(context.getArgument("amount", Integer.class));
            return SINGLE_SUCCESS;
        }));
    }
}
