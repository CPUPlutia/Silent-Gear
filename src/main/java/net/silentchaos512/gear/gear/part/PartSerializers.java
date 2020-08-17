package net.silentchaos512.gear.gear.part;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.JSONUtils;
import net.minecraft.util.ResourceLocation;
import net.silentchaos512.gear.SilentGear;
import net.silentchaos512.gear.api.part.IGearPart;
import net.silentchaos512.gear.api.part.IPartSerializer;
import net.silentchaos512.gear.api.part.PartType;
import net.silentchaos512.gear.config.Config;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public final class PartSerializers {
    public static final CompoundPart.Serializer COMPOUND_PART = new CompoundPart.Serializer(SilentGear.getId("compound_part"), CompoundPart::new);

    private static final Map<ResourceLocation, IPartSerializer<?>> REGISTRY = new HashMap<>();

    static {
        for (PartType type : PartType.getValues()) {
            IPartSerializer<? extends IGearPart> serializer = type.getSerializer();
            if (serializer != null) {
                register(serializer);
            }
        }
        register(COMPOUND_PART);
    }

    private PartSerializers() {}

    public static <S extends IPartSerializer<T>, T extends IGearPart> S register(S serializer) {
        if (REGISTRY.containsKey(serializer.getName())) {
            throw new IllegalArgumentException("Duplicate gear part serializer " + serializer.getName());
        }
        SilentGear.LOGGER.info(PartManager.MARKER, "Registered serializer '{}'", serializer.getName());
        REGISTRY.put(serializer.getName(), serializer);
        return serializer;
    }

    public static IGearPart deserialize(ResourceLocation id, JsonObject json) {
        String typeStr = JSONUtils.getString(json, "type");
        ResourceLocation type = SilentGear.getIdWithDefaultNamespace(typeStr);
        log(() -> "deserialize " + id + " (type " + type + ")");

        IPartSerializer<?> serializer = REGISTRY.get(type);
        if (serializer == null) {
            throw new JsonSyntaxException("Invalid or unsupported gear part type " + type);
        }
        return serializer.read(id, json);
    }

    public static IGearPart read(PacketBuffer buffer) {
        ResourceLocation id = buffer.readResourceLocation();
        ResourceLocation type = buffer.readResourceLocation();
        log(() -> "read " + id + " (type " + type + ")");
        IPartSerializer<?> serializer = REGISTRY.get(type);
        if (serializer == null) {
            throw new IllegalArgumentException("Unknown gear part serializer " + type);
        }
        return serializer.read(id, buffer);
    }

    @SuppressWarnings("unchecked")
    public static <T extends IGearPart> void write(T part, PacketBuffer buffer) {
        ResourceLocation id = part.getId();
        ResourceLocation type = part.getSerializer().getName();
        log(() -> "write " + id + " (type " + type + ")");
        buffer.writeResourceLocation(id);
        buffer.writeResourceLocation(type);
        IPartSerializer<T> serializer = (IPartSerializer<T>) part.getSerializer();
        serializer.write(buffer, part);
    }

    private static void log(Supplier<?> msg) {
        if (Config.Common.extraPartAndTraitLogging.get()) {
            SilentGear.LOGGER.info(PartManager.MARKER, msg.get());
        }
    }
}
