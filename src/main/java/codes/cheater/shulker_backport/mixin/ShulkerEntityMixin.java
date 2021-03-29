package codes.cheater.shulker_backport.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.mob.ShulkerEntity;
import net.minecraft.entity.passive.GolemEntity;
import net.minecraft.util.DyeColor;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ShulkerEntity.class)
public abstract class ShulkerEntityMixin extends GolemEntity implements Monster {
    @Shadow
    protected abstract boolean isClosed();

    @Shadow
    protected abstract boolean tryTeleport();

    @Shadow @Nullable
    public abstract DyeColor getColor();

    @Shadow @Final
    protected static TrackedData<Byte> COLOR;

    @Unique
    private boolean didTryTeleport = false;

    protected ShulkerEntityMixin(EntityType<? extends GolemEntity> entityType, World world) {
        super(entityType, world);
    }

    @Inject(method = "damage", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/mob/ShulkerEntity;tryTeleport()Z"))
    private void onTryTeleport(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        didTryTeleport = true;
    }

    @Inject(method = "damage", at = @At("RETURN"))
    private void onReturn(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if(cir.getReturnValue() && !didTryTeleport && source.isProjectile()) {
            Entity entity = source.getSource();
            if (entity != null && entity.getType() == EntityType.SHULKER_BULLET) {
                this.spawnNewShulker();
            }
        }
        didTryTeleport = false;
    }

    private void spawnNewShulker() {
        Vec3d vec3d = this.getPos();
        Box box = this.getBoundingBox();
        if (!this.isClosed() && this.tryTeleport()) {
            int i = this.world.getEntitiesByType(EntityType.SHULKER, box.expand(8.0D), Entity::isAlive).size();
            float f = (float)(i - 1) / 5.0F;
            if (this.world.random.nextFloat() >= f) {
                ShulkerEntity shulkerEntity = EntityType.SHULKER.create(this.world);
                DyeColor dyeColor = this.getColor();
                if (dyeColor != null) {
                    shulkerEntity.getDataTracker().set(COLOR, (byte)dyeColor.getId());
                }

                shulkerEntity.refreshPositionAfterTeleport(vec3d);
                this.world.spawnEntity(shulkerEntity);
            }
        }
    }
}
