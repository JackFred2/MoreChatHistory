package red.jackf.morechathistory.mixins;

import net.minecraft.client.gui.components.ChatComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(ChatComponent.class)
public class ChatComponentMixin {

    // todo move to mixin extras ModifyExpressionValue when it's likely most have changed to floader 0.15
    @ModifyConstant(method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;ILnet/minecraft/client/GuiMessageTag;Z)V",
            constant = @Constant(intValue = 100),
            expect = 2)
    public int morechathistory_changeMaxHistory(int original) {
        return 16384;
    }
}
