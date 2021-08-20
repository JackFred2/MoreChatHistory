package red.jackf.morechathistory;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.Toml4jConfigSerializer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.ActionResult;

@Environment(EnvType.CLIENT)
public class MoreChatHistory implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        if (FabricLoader.getInstance().isModLoaded("fabric-command-api-v1")) {
            TestCommand.setupSpamChatCommand();
        }

        AutoConfig.register(MoreChatHistoryConfig.class, Toml4jConfigSerializer::new).registerSaveListener((configHolder, moreChatHistoryConfig) -> {
            if (moreChatHistoryConfig.maxHistory < 100) {
                moreChatHistoryConfig.maxHistory = 100;
                configHolder.save();
            }
            return ActionResult.PASS;
        });
    }

    public static int getMaxLength() {
        return AutoConfig.getConfigHolder(MoreChatHistoryConfig.class).get().maxHistory;
    }
}
