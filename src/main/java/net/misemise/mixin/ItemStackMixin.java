package net.misemise.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.misemise.OreBreaker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin {
    @Inject(
            method = "hurtAndBreak(ILnet/minecraft/world/entity/LivingEntity;"
                    + "Lnet/minecraft/world/entity/EquipmentSlot;)V",
            at = @At("HEAD"),
            cancellable = true)
    private void omniminer$skipAdditionalBlockDurability(
            int amount,
            LivingEntity entity,
            EquipmentSlot slot,
            CallbackInfo callback) {
        if (entity instanceof ServerPlayer player
                && OreBreaker.isProcessingAdditionalBreak(player)) {
            callback.cancel();
        }
    }
}
