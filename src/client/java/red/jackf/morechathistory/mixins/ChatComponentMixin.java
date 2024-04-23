package red.jackf.morechathistory.mixins;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.gui.components.ChatComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ChatComponent.class)
public class ChatComponentMixin {

    @ModifyExpressionValue(
            method = {"addMessageToDisplayQueue", "addMessageToQueue", "addRecentChat"},
            at = @At(value = "CONSTANT", args = "intValue=100")
    )
    public int morechathistory_changeMaxHistory(int original) {
        return original + 16284;
    }
}
