package yiseyo.sculpture.client;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.network.PacketDistributor;

public final class StatueBlockEntity extends BlockEntity
{

    /* -------- “原料” -------- */
    private CompoundTag entityNbt;
    private Pose pose;
    private float bodyYaw, headYaw;

    /* -------- Mesh 数据 -------- */
    private byte[] meshBytes;
    private boolean meshReady = false;

    public StatueBlockEntity(BlockPos pos, BlockState state)
    {
        super(ModBlocks.STATUE_BE.get(), pos, state);
    }

    /* ===== 存取器 ===== */
    public void setEntityData(CompoundTag tag, Pose p, float bYaw, float hYaw)
    {
        this.entityNbt = tag;
        this.pose = p;
        this.bodyYaw = bYaw;
        this.headYaw = hYaw;
    }

    public CompoundTag entityNbt()
    {
        return entityNbt;
    }

    public Pose pose()
    {
        return pose;
    }

    public float bodyYaw()
    {
        return bodyYaw;
    }

    public float headYaw()
    {
        return headYaw;
    }

    /* 客户端回传 Mesh 时调用 */
    public void acceptMesh(byte[] bytes)
    {
        this.meshBytes = bytes;
        this.meshReady = true;
        setChanged();

        if (!level.isClientSide) {
            // 同步网格数据给观看该区块的玩家
            ModNet.CHANNEL.send(PacketDistributor.TRACKING_CHUNK.with(
                            () -> ((ServerLevel) level).getChunkAt(worldPosition)),
                    new S2CSyncMesh(worldPosition, bytes));
            // TODO: Persist the captured mesh data for future use (e.g., save to disk or item)
        }
    }

    public boolean hasMesh()
    {
        return meshReady;
    }

    public byte[] meshBytes()
    {
        return meshBytes;
    }

    /* -------- NBT 持久化 -------- */
    @Override
    protected void saveAdditional(CompoundTag tag)
    {
        super.saveAdditional(tag);
        if (entityNbt != null) tag.put("Entity", entityNbt);
        if (pose != null) tag.putString("Pose", pose.name());
        tag.putFloat("BodyYaw", bodyYaw);
        tag.putFloat("HeadYaw", headYaw);
        if (meshReady) tag.putByteArray("Mesh", meshBytes);
    }

    @Override
    public void load(CompoundTag tag)
    {
        super.load(tag);
        if (tag.contains("Entity")) entityNbt = tag.getCompound("Entity");
        if (tag.contains("Pose")) pose = Pose.valueOf(tag.getString("Pose"));
        bodyYaw = tag.getFloat("BodyYaw");
        headYaw = tag.getFloat("HeadYaw");
        if (tag.contains("Mesh"))
        {
            meshBytes = tag.getByteArray("Mesh");
            meshReady = true;
        }
    }

    @Override
    public CompoundTag getUpdateTag()
    {
        return saveWithFullMetadata();
    }

    @Override
    public void handleUpdateTag(CompoundTag tag)
    {
        load(tag);
    }
}