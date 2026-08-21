package thaumicenergistics.mixin.ae2;

import appeng.container.slot.SlotRestrictedInput;
import appeng.container.slot.SlotRestrictedInput.PlacableItemType;

import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import thaumicenergistics.annotation.LateMixin;
import thaumicenergistics.api.ThEApi;

/**
 * Counts the Arcane Charging Card as a real AE2 card letting it be carried by the network tool.
 * This also means you can put it in any AE2 machine, but it will do nothing.
 */
@Mixin(SlotRestrictedInput.class)
@LateMixin
public abstract class MixinSlotRestrictedInput {

    @Shadow(remap = false)
    @Final
    private PlacableItemType which;

    @Inject(method = "isItemValid", at = @At("HEAD"), cancellable = true)
    private void thaumicenergistics$acceptArcaneChargingCard(
            ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (this.which == PlacableItemType.UPGRADES
                && !stack.isEmpty()
                && ThEApi.instance().items().upgradeArcane().isSameAs(stack)) {
            cir.setReturnValue(true);
        }
    }
}
