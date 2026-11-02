package ua.meekeyn.requiemunofficial.common.loot;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import net.minecraft.advancements.critereon.DistancePredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.LocationPredicate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Порт beta.16 {@code EntityRefPredicate}: предикат над посиланням на сутність
 * (позиція + сама сутність, якщо вона завантажена). JSON-поля: {@code distance}, {@code location}, {@code entity}.
 */
public record EntityRefPredicate(DistancePredicate distance, LocationPredicate location, EntityPredicate entity) {
    public static final EntityRefPredicate ANY = new EntityRefPredicate(
            DistancePredicate.ANY,
            LocationPredicate.ANY,
            EntityPredicate.ANY
    );

    /**
     * Посилання на сутність. {@code pos} може бути null, якщо сутність не завантажена:
     * у beta.16 EntityPointer зберігав останню позицію, у порту для оболонки її немає.
     */
    public record Ref(@Nullable ServerLevel level, @Nullable Vec3 pos, @Nullable Entity resolved) {
    }

    public boolean test(ServerLevel world, @Nullable Vec3 origin, @Nullable Ref ref) {
        if (this == ANY) {
            return true;
        } else if (ref == null) {
            return false;
        }

        Vec3 location = ref.pos();
        if (location == null) {
            // Позиція невідома (оболонка не завантажена) — просторові перевірки неможливі.
            if (this.distance != DistancePredicate.ANY || this.location != LocationPredicate.ANY) {
                return false;
            }
        } else {
            if (origin == null) {
                if (this.distance != DistancePredicate.ANY) {
                    return false;
                }
            } else if (!this.distance.matches(origin.x, origin.y, origin.z, location.x, location.y, location.z)) {
                return false;
            }

            ServerLevel refLevel = ref.level() != null ? ref.level() : world;
            if (!this.location.matches(refLevel, location.x, location.y, location.z)) {
                return false;
            }
        }

        return this.entity.matches(world, origin, ref.resolved());
    }

    public JsonElement toJson() {
        if (this == ANY) {
            return JsonNull.INSTANCE;
        }
        JsonObject json = new JsonObject();
        json.add("distance", this.distance.serializeToJson());
        json.add("location", this.location.serializeToJson());
        json.add("entity", this.entity.serializeToJson());
        return json;
    }

    public static EntityRefPredicate fromJson(@Nullable JsonElement json) {
        if (json == null || json.isJsonNull()) {
            return ANY;
        }
        JsonObject jsonObject = GsonHelper.convertToJsonObject(json, "entity_ref");
        return new EntityRefPredicate(
                DistancePredicate.fromJson(jsonObject.get("distance")),
                LocationPredicate.fromJson(jsonObject.get("location")),
                EntityPredicate.fromJson(jsonObject.get("entity"))
        );
    }
}
