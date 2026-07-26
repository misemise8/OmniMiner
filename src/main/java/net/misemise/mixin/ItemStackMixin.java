package net.misemise.mixin;

import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.misemise.OreBreaker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin {
    @Inject(
            method = "damage(ILnet/minecraft/entity/LivingEntity;"
                    + "Lnet/minecraft/entity/EquipmentSlot;)V",
            at = @At("HEAD"),
            cancellable = true)
    private void omniminer$skipAdditionalBlockDurability(
            int amount,
            LivingEntity entity,
            EquipmentSlot slot,
            CallbackInfo callback) {
        if (entity instanceof ServerPlayerEntity player
                && OreBreaker.isProcessingAdditionalBreak(player)) {
            callback.cancel();
        }
    }
}
