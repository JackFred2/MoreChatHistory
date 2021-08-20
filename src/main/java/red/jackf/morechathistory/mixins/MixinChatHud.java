package red.jackf.morechathistory.mixins;

import net.minecraft.client.gui.hud.ChatHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import red.jackf.morechathistory.MoreChatHistory;

@Mixin(ChatHud.class)
public class MixinChatHud {

    @ModifyConstant(method = "addMessage(Lnet/minecraft/text/Text;IIZ)V", constant = @Constant(intValue = 100), expect = 2)
    public int morechathistory_changeMaxHistory(int original) {
        return MoreChatHistory.getMaxLength();
    }
}
