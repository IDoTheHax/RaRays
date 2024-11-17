package net.idothehax.rarays;


import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.idothehax.rarays.config.Config;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

public class RaCommands {
    public static void register() {
        CommandRegistrationCallback.EVENT.register(RaCommands::registerCommand);
    }

    private static void registerCommand(CommandDispatcher<ServerCommandSource> dispatcher,
                                        CommandRegistryAccess registryAccess,
                                        CommandManager.RegistrationEnvironment environment) {

        dispatcher.register(CommandManager.literal("rarays")
                .requires(source -> source.hasPermissionLevel(2)) // Requires operator permission
                .then(CommandManager.literal("cooldown")
                        .then(CommandManager.argument("ticks", IntegerArgumentType.integer(0, 72000)) // Max 1 hour
                                .executes(context -> {
                                    int newCooldown = IntegerArgumentType.getInteger(context, "ticks");
                                    Config.getInstance().setRaRaysCooldown(newCooldown);
                                    context.getSource().sendFeedback(
                                            () -> Text.literal("Set RaRays cooldown to " + newCooldown + " ticks (" + (newCooldown/20.0) + " seconds)"),
                                            true
                                    );
                                    return 1;
                                }))
                        .executes(context -> {
                            int currentCooldown = Config.getInstance().getRaRaysCooldown();
                            context.getSource().sendFeedback(
                                    () -> Text.literal("Current RaRays cooldown is " + currentCooldown + " ticks (" + (currentCooldown/20.0) + " seconds)"),
                                    false
                            );
                            return 1;
                        })));
    }
}
