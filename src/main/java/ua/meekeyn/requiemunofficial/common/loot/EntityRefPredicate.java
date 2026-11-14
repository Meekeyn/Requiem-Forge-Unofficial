package ua.meekeyn.requiemunofficial.common.loot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.critereon.DistancePredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.LocationPredicate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * Порт beta.16 {@code EntityRefPredicate}: предикат над посиланням на сутність
 * (позиція + сама сутність, якщо вона завантажена). JSON-поля: {@code distance}, {@code location}, {@code entity}.
 */
public record EntityRefPredicate(Optional<DistancePredicate> distance, Optional<LocationPredicate> location,
                                 Optional<EntityPredicate> entity) {
    public static final Codec<EntityRefPredicate> CODEC = RecordCodecBuilder.create(i -> i.group(
            DistancePredicate.CODEC.optionalFieldOf("distance").forGetter(EntityRefPredicate::distance),
            LocationPredicate.CODEC.optionalFieldOf("location").forGetter(EntityRefPredicate::location),
            EntityPredicate.CODEC.optionalFieldOf("entity").forGetter(EntityRefPredicate::entity)
    ).apply(i, EntityRefPredicate::new));

    public static final EntityRefPredicate ANY = new EntityRefPredicate(Optional.empty(), Optional.empty(), Optional.empty());

    /**
     * Посилання на сутність. {@code pos} може бути null, якщо сутність не завантажена:
     * у beta.16 EntityPointer зберігав останню позицію, у порту для оболонки її немає.
     */
    public record Ref(@Nullable ServerLevel level, @Nullable Vec3 pos, @Nullable Entity resolved) {
    }

    public boolean isAny() {
        return distance.isEmpty() && location.isEmpty() && entity.isEmpty();
    }

    public boolean test(ServerLevel world, @Nullable Vec3 origin, @Nullable Ref ref) {
        if (isAny()) {
            return true;
        } else if (ref == null) {
            return false;
        }

        Vec3 pos = ref.pos();
        if (pos == null) {
            // Позиція невідома (оболонка не завантажена) — просторові перевірки неможливі.
            if (this.distance.isPresent() || this.location.isPresent()) {
                return false;
            }
        } else {
            if (this.distance.isPresent()) {
                if (origin == null
                        || !this.distance.get().matches(origin.x, origin.y, origin.z, pos.x, pos.y, pos.z)) {
                    return false;
                }
            }
            ServerLevel refLevel = ref.level() != null ? ref.level() : world;
            if (this.location.isPresent() && !this.location.get().matches(refLevel, pos.x, pos.y, pos.z)) {
                return false;
            }
        }

        return this.entity.isEmpty() || this.entity.get().matches(world, origin, ref.resolved());
    }
}
