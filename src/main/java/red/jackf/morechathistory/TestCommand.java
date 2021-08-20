package red.jackf.morechathistory;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.fabric.api.client.command.v1.ClientCommandManager;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.LiteralText;

public abstract class TestCommand {
    static void setupSpamChatCommand() {
        if (FabricLoader.getInstance().isDevelopmentEnvironment()) {
            ClientCommandManager.DISPATCHER.register(
                ClientCommandManager.literal("spamchat").then(
                    ClientCommandManager.argument("length", IntegerArgumentType.integer(1)).executes(source -> {
                        var hud = MinecraftClient.getInstance().inGameHud;
                        if (hud != null && hud.getChatHud() != null) {
                            var length = source.getArgument("length", Integer.class);
                            for (int i = 0; i < length; i++)
                                hud.getChatHud().addMessage(new LiteralText("" + i));
                        }
                        return 0;
                    })
                )
            );
        }
    }
}
