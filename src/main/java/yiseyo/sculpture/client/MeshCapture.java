package yiseyo.sculpture.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.*;

/**
 * Utility for grabbing an entity's baked mesh at runtime.
 * <p>
 * The capturer works by rendering the entity into a fake {@link VertexConsumer}
 * that simply records every vertex it receives, grouped by texture (RenderType layer).
 * After the render pass finishes we return a {@link CaptureResult} containing
 * all vertices organised per texture, ready for compression or direct rendering.
 */
public class MeshCapture
{

    /* ------------------------------------------------------------ */
    /* === Immutable vertex record – keeps every attribute we need === */
    /* ------------------------------------------------------------ */
    public record Vertex(
            float x, float y, float z,
            float u, float v,
            int colorARGB,
            int lightPacked,
            int overlayPacked,
            float nx, float ny, float nz)
    {
    }

    /* ------------------------------------------------------------ */
    /* === Captured result wrapper ================================= */
    /* ------------------------------------------------------------ */
    public static class CaptureResult
    {
        private final Map<ResourceLocation, List<Vertex>> meshByTexture;

        public CaptureResult(Map<ResourceLocation, List<Vertex>> meshByTexture)
        {
            this.meshByTexture = Map.copyOf(meshByTexture);
        }

        public Map<ResourceLocation, List<Vertex>> meshByTexture()
        {
            return meshByTexture;
        }

        public boolean isEmpty()
        {
            return meshByTexture.isEmpty();
        }
    }

    /* ------------------------------------------------------------ */
    /* === Capturing VertexConsumer implementation ================= */
    /* ------------------------------------------------------------ */
    public static class CapturingConsumer implements VertexConsumer
    {
        private final ResourceLocation texture;
        private final List<Vertex> out = new ArrayList<>();

        // Working vars – VertexConsumer builds a vertex attribute-by-attribute then finalises with endVertex().
        private float x, y, z;
        private float u, v;
        private int colorARGB = 0xFFFFFFFF;
        private int lightPacked;
        private int overlayPacked;
        private float nx = 0, ny = 1, nz = 0;

        public CapturingConsumer(ResourceLocation texture)
        {
            this.texture = texture;
        }

        public ResourceLocation texture()
        {
            return texture;
        }

        public List<Vertex> vertices()
        {
            return out;
        }

        // ===== VertexConsumer implementation =====
        @Override
        public VertexConsumer vertex(double x, double y, double z)
        {
            // convert to float now – Minecraft mostly uses float precision for positions in vertex buffers
            this.x = (float) x;
            this.y = (float) y;
            this.z = (float) z;
            return this;
        }

        @Override
        public VertexConsumer color(int r, int g, int b, int a)
        {
            this.colorARGB = FastColor.ARGB32.color(a, r, g, b);
            return this;
        }

        @Override
        public VertexConsumer uv(float u, float v)
        {
            this.u = u;
            this.v = v;
            return this;
        }

        @Override
        public VertexConsumer overlayCoords(int u, int v)
        {
            // packed as V << 16 | U (same as LightTexture)
            this.overlayPacked = (v << 16) | (u & 0xFFFF);
            return this;
        }

        @Override
        public VertexConsumer uv2(int u, int v)
        {
            this.lightPacked = (v << 16) | (u & 0xFFFF);
            return this;
        }

        @Override
        public VertexConsumer normal(float nx, float ny, float nz)
        {
            this.nx = nx;
            this.ny = ny;
            this.nz = nz;
            return this;
        }

        @Override
        public void endVertex()
        {
            out.add(new Vertex(x, y, z, u, v, colorARGB, lightPacked, overlayPacked, nx, ny, nz));
        }

        /* -------- Methods not needed for capture but required by the interface -------- */
        @Override
        public void defaultColor(int r, int g, int b, int a)
        {
        }

        @Override
        public void unsetDefaultColor()
        {
        }
    }

    /* ------------------------------------------------------------ */
    /* === MAIN API – capture() ==================================== */
    /* ------------------------------------------------------------ */

    /**
     * Render the entity described by {@code entityNbt} into an off‑screen {@link MultiBufferSource}
     * that records every vertex, returning the full mesh split by texture.
     *
     * @param entityNbt the entity data previously stored via {@link Entity#saveWithoutId}
     * @param level     client level reference (should be Minecraft.getInstance().level)
     * @param pose      pose to force the entity into when capturing
     * @param bodyYaw   the body rotation (degrees)
     * @param headYaw   the head rotation (degrees)
     */
    public static CaptureResult capture(CompoundTag entityNbt,
                                        ClientLevel level,
                                        Pose pose,
                                        float bodyYaw,
                                        float headYaw)
    {
        // 1. Re‑create entity from NBT -------------------------------------------------------------------
        Entity entity = EntityTypeFromNbt.loadEntity(entityNbt, level);
        if (entity == null)
        {
            return new CaptureResult(Collections.emptyMap());
        }

        entity.moveTo(Vec3.ZERO);
        entity.setPose(pose);
        entity.setYRot(bodyYaw);
        entity.setXRot(0);

// If the captured entity is a LivingEntity we can also synchronise body & head rotations
        if (entity instanceof net.minecraft.world.entity.LivingEntity living)
        {
            living.yBodyRot = bodyYaw;
            living.yBodyRotO = bodyYaw;
            living.setYHeadRot(headYaw);
            living.yHeadRotO = headYaw;
        }

        // 2. Prepare a dummy buffer source that records vertices ----------------------------------------
        MeshBufferSource recorder = new MeshBufferSource();

        // 3. Render entity using vanilla RenderDispatcher ----------------------------------------------
        PoseStack stack = new PoseStack();
        stack.translate(0.5, 0, 0.5); // centre on block origin
        Minecraft.getInstance().getEntityRenderDispatcher().render(
                entity,
                0,                // render x offset (already in stack)
                0,                // render y offset
                0,
                0,                // yaw not required (we set entity yaw above)
                Minecraft.getInstance().getFrameTime(),
                stack,
                recorder,
                0xF000F0);

        // 4. Finish buffer (no‑op here) & build result --------------------------------------------------
        return new CaptureResult(recorder.asImmutable());
    }

    /* ------------------------------------------------------------ */
    /* === Helper classes ========================================= */
    /* ------------------------------------------------------------ */

    /**
     * A MultiBufferSource implementation that provides {@link CapturingConsumer}s for every texture layer.
     */
    private static class MeshBufferSource implements MultiBufferSource
    {
        private final Map<ResourceLocation, CapturingConsumer> consumers = new HashMap<>();

        @SuppressWarnings("removal")
        @Override
        public VertexConsumer getBuffer(RenderType type)
        {
            ResourceLocation tex = extractTexture(type);
            if (tex == null)
            {
                // Fallback: use RenderType name as pseudo texture to avoid NPE later
                tex = new ResourceLocation("dummy", type.toString().replace(':', '/'));
            }
            return consumers.computeIfAbsent(tex, CapturingConsumer::new);
        }

        public Map<ResourceLocation, List<Vertex>> asImmutable()
        {
            Map<ResourceLocation, List<Vertex>> out = new HashMap<>();
            consumers.forEach((tex, cc) -> out.put(tex, List.copyOf(cc.vertices())));
            return out;
        }
    }

    /*
     * Try to pull the texture location out of a RenderType via reflection.
     * Implementation details change across MC versions so we keep it lenient.
     */
    @Nullable
    private static ResourceLocation extractTexture(RenderType type)
    {
        try
        {
            // In 1.20 the CompositeRenderType holds a private final CompositeState "state"
            Field stateField = ObfuscationReflectionHelper.findField(RenderType.class, "f_289963_"); // "state" SRG name
            Object compositeState = stateField.get(type);
            Field texStateField = ObfuscationReflectionHelper.findField(compositeState.getClass(), "f_173254_"); // "textureState" SRG name
            Object textureStateShard = texStateField.get(compositeState);
            Field locField = ObfuscationReflectionHelper.findField(textureStateShard.getClass(), "f_78917_"); // "location" SRG name
            return (ResourceLocation) locField.get(textureStateShard);
        } catch (Exception ignored)
        {
            return null;
        }
    }

    /*
     * Helper to spawn an entity from NBT without adding it to the world permanently.
     */
    private static class EntityTypeFromNbt
    {
        @Nullable
        static Entity loadEntity(CompoundTag nbt, ClientLevel level)
        {
            try
            {
                return EntityType.loadEntityRecursive(nbt, level, entity -> entity);
            } catch (Exception e)
            {
                return null;
            }
        }
    }

    /*
     * Utility method for external callers – create or retrieve the CapturingConsumer for a specific texture
     * in a map keyed by ResourceLocation. (Not used internally anymore but kept for API parity.)
     */
    public static CapturingConsumer consumerFor(Map<ResourceLocation, CapturingConsumer> map, ResourceLocation tex)
    {
        return map.computeIfAbsent(tex, CapturingConsumer::new);
    }
}
