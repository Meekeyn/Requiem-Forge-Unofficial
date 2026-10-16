package ua.meekeyn.requiemunofficial.common.transformation;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.ZombieVillager;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffers;
import ua.meekeyn.requiemunofficial.common.util.ModTags;
import ua.meekeyn.requiemunofficial.common.possession.PossessionManager;
import ua.meekeyn.requiemunofficial.common.init.ModEntities;
import ua.meekeyn.requiemunofficial.common.advancement.ModCriteria;

import java.util.UUID;

public final class TransformationManager {

    private static final String ORIGINAL_BODY_TYPE = "RequiemOriginalBodyType";

    private TransformationManager() {
    }

    public static Mob skeletonize(LivingEntity target) {
        if (!(target.level() instanceof ServerLevel level)
                || !(target instanceof Mob mob)
                || !target.getType().is(ModTags.SKELETONIZABLE)) {
            return null;
        }

        if (target.getType().is(ModTags.HUMANOID_SKELETONS)) {
            return null;
        }

        return replace(level, mob, getSkeletonType(target.getType()), false);
    }

    public static Mob decayToZombie(LivingEntity target) {
        if (!(target.level() instanceof ServerLevel level)
                || !(target instanceof Mob mob)
                || !target.getType().is(ModTags.HUMANOID_SKELETONS)) {
            return null;
        }
        return replace(level, mob, getZombieType(target.getType()), false);
    }

    public static Mob decayToSkeleton(LivingEntity target) {
        if (!(target.level() instanceof ServerLevel level)
                || !(target instanceof Mob mob)
                || !target.getType().is(ModTags.HUMANOID_ZOMBIES)) {
            return null;
        }
        return replace(level, mob, getSkeletonType(target.getType()), false);
    }

    public static Mob cureUndead(LivingEntity target) {
        if (!(target.level() instanceof ServerLevel level) || !(target instanceof Mob mob)) {
            return null;
        }

        EntityType<? extends Mob> curedType;
        if (target.getType() == EntityType.ZOMBIE_VILLAGER) {
            curedType = ModEntities.CURED_VILLAGER.get();
        } else if (target.getType() == EntityType.ZOMBIFIED_PIGLIN) {
            curedType = target.getPersistentData().getString(ORIGINAL_BODY_TYPE)
                    .equals("minecraft:piglin_brute")
                    ? ModEntities.CURED_PIGLIN_BRUTE.get()
                    : ModEntities.CURED_PIGLIN.get();
        } else {
            return null;
        }
        return replace(level, mob, curedType, true);
    }

    private static EntityType<? extends Mob> getSkeletonType(EntityType<?> sourceType) {
        if (sourceType == EntityType.DROWNED) {
            return EntityType.STRAY;
        }
        if (sourceType == EntityType.ZOMBIFIED_PIGLIN || sourceType.is(ModTags.PIGLINS)) {
            return EntityType.WITHER_SKELETON;
        }
        return EntityType.SKELETON;
    }

    private static EntityType<? extends Mob> getZombieType(EntityType<?> sourceType) {
        if (sourceType == EntityType.STRAY) {
            return EntityType.DROWNED;
        }
        if (sourceType == EntityType.WITHER_SKELETON) {
            return EntityType.ZOMBIFIED_PIGLIN;
        }
        return EntityType.ZOMBIE;
    }

    private static Mob replace(ServerLevel level, Mob source, EntityType<? extends Mob> replacementType,
                               boolean cure) {
        Mob replacement = replacementType.create(level);
        if (replacement == null) {
            return null;
        }

        replacement.moveTo(source.getX(), source.getY(), source.getZ(), source.getYRot(), source.getXRot());
        replacement.setDeltaMovement(source.getDeltaMovement());
        replacement.setCustomName(source.getCustomName());
        replacement.setCustomNameVisible(source.isCustomNameVisible());
        replacement.setNoAi(source.isNoAi());
        replacement.setLeftHanded(source.isLeftHanded());
        replacement.setCanPickUpLoot(source.canPickUpLoot());
        replacement.setAggressive(source.isAggressive());
        replacement.setSilent(source.isSilent());
        replacement.setNoGravity(source.isNoGravity());
        replacement.setInvulnerable(source.isInvulnerable());
        replacement.setGlowingTag(source.hasGlowingTag());
        replacement.setRemainingFireTicks(source.getRemainingFireTicks());
        replacement.setPortalCooldown(source.getPortalCooldown());
        replacement.setArrowCount(source.getArrowCount());
        replacement.setAbsorptionAmount(source.getAbsorptionAmount());
        replacement.invulnerableTime = source.invulnerableTime;
        replacement.fallDistance = source.fallDistance;

        if (source.isPersistenceRequired()) {
            replacement.setPersistenceRequired();
        }

        CompoundTag sourceData = source.saveWithoutId(new CompoundTag());
        copyVillagerData(source, replacement, sourceData);
        ListTag handDropChances = sourceData.getList("HandDropChances", Tag.TAG_FLOAT);
        ListTag armorDropChances = sourceData.getList("ArmorDropChances", Tag.TAG_FLOAT);
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack stack = source.getItemBySlot(slot);
            if (!stack.isEmpty()) {
                replacement.setItemSlot(slot, stack.copy());
            }
            replacement.setDropChance(slot, getDropChance(slot, handDropChances, armorDropChances));
        }

        for (MobEffectInstance effect : source.getActiveEffects()) {
            replacement.addEffect(new MobEffectInstance(effect));
        }

        String originalBodyType = source.getPersistentData().getString(ORIGINAL_BODY_TYPE);
        if (originalBodyType.isEmpty() && source.getType() == EntityType.PIGLIN_BRUTE) {
            originalBodyType = "minecraft:piglin_brute";
        }
        if (!originalBodyType.isEmpty()) {
            replacement.getPersistentData().putString(ORIGINAL_BODY_TYPE, originalBodyType);
        }

        float healthRatio = source.getHealth() / source.getMaxHealth();
        replacement.setHealth(Math.max(1.0F, replacement.getMaxHealth() * healthRatio));

        if (!level.addFreshEntity(replacement)) {
            return null;
        }

        UUID possessorId = PossessionManager.getPossessorId(source);
        if (possessorId != null) {
            ServerPlayer possessor = PossessionManager.getActivePossessor(level, possessorId);
            if (possessor != null && !PossessionManager.transferPossession(possessor, source, replacement)) {
                replacement.discard();
                return null;
            }
            if (possessor != null) {
                ModCriteria.TRANSFORMED_POSSESSED_ENTITY.trigger(
                        possessor,
                        source,
                        replacement,
                        cure
                );
            }
        }

        source.discard();
        return replacement;
    }

    private static void copyVillagerData(Mob source, Mob replacement, CompoundTag sourceData) {
        if (!(source instanceof ZombieVillager zombieVillager)
                || !(replacement instanceof Villager villager)) {
            return;
        }

        villager.setVillagerData(zombieVillager.getVillagerData());
        villager.setVillagerXp(zombieVillager.getVillagerXp());
        if (sourceData.contains("Gossips", Tag.TAG_LIST)) {
            villager.setGossips(sourceData.get("Gossips"));
        }
        if (sourceData.contains("Offers", Tag.TAG_COMPOUND)) {
            villager.setOffers(new MerchantOffers(sourceData.getCompound("Offers")));
        }
    }

    private static float getDropChance(EquipmentSlot slot, ListTag handChances, ListTag armorChances) {
        return switch (slot.getType()) {
            case HAND -> slot.getIndex() < handChances.size()
                    ? handChances.getFloat(slot.getIndex())
                    : 0.085F;
            case ARMOR -> slot.getIndex() < armorChances.size()
                    ? armorChances.getFloat(slot.getIndex())
                    : 0.085F;
        };
    }
}
