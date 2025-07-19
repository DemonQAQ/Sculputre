package yiseyo.sculpture.core.world;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;
import yiseyo.sculpture.core.net.ModNet;
import yiseyo.sculpture.core.net.packet.S2CSyncMesh;
import yiseyo.sculpture.common.ModBlocks;

public final class StatueBlockEntity extends BlockEntity
{
    private CompoundTag entityNbt;
    private Pose pose;
    private float bodyYaw, headYaw;

    private byte[] meshBytes;
    private boolean meshReady = false;

    public StatueBlockEntity(BlockPos pos, BlockState state)
    {
        super(ModBlocks.STATUE_BE.get(), pos, state);
    }

    public void setEntityData(CompoundTag tag, Pose p, float bYaw, float hYaw)
    {
        if (!tag.contains("id", Tag.TAG_STRING)) {
            tag.putString("id",
                    ForgeRegistries.ENTITY_TYPES.getKey(
                            EntityType.byString(tag.getString("id")).orElseThrow()
                    ).toString());
        }

        this.entityNbt = tag;
        this.pose = p;
        this.bodyYaw = bYaw;
        this.headYaw = hYaw;

        setChanged();   // 存档
        if (!level.isClientSide)
            level.sendBlockUpdated(worldPosition, getBlockState(),
                    getBlockState(), Block.UPDATE_CLIENTS); // 重新发包
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

    public void acceptMesh(byte[] bytes)
    {
        this.meshBytes = bytes;
        this.meshReady = true;
        setChanged();

        if (!level.isClientSide)
        {
            // 同步网格数据给观看该区块的玩家
            ModNet.CHANNEL.send(PacketDistributor.TRACKING_CHUNK.with(
                            () -> ((ServerLevel) level).getChunkAt(worldPosition)),
                    new S2CSyncMesh(worldPosition, bytes));
            // TODO: 持久化雕像数据
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

    @Override
    protected void saveAdditional(CompoundTag tag)
    {
        super.saveAdditional(tag);
        if (entityNbt != null) tag.put("Entity", entityNbt);
        tag.putString("Pose", pose.name());
        tag.putFloat("BodyYaw", bodyYaw);
        tag.putFloat("HeadYaw", headYaw);
        if (meshReady) tag.putByteArray("Mesh", meshBytes);
    }

    @Override
    public void load(CompoundTag tag)
    {
        super.load(tag);
        if (tag.contains("Entity")) entityNbt = tag.getCompound("Entity");
        if (tag.contains("Pose", Tag.TAG_STRING))          // 8
            pose = Pose.valueOf(tag.getString("Pose"));
        else
            pose = Pose.STANDING;                          // 兜底
        bodyYaw = tag.getFloat("BodyYaw");
        headYaw = tag.getFloat("HeadYaw");
        if (tag.contains("Mesh"))
        {
            meshBytes = tag.getByteArray("Mesh");
            meshReady = true;
        }
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket()
    {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag()
    {
        CompoundTag tag = super.getUpdateTag();
        if (entityNbt != null) tag.put("Entity", entityNbt.copy());
        tag.putInt("Pose", pose.ordinal());
        tag.putFloat("BodyYaw", bodyYaw);
        tag.putFloat("HeadYaw", headYaw);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag)
    {
        if (tag.contains("Entity"))
            this.entityNbt = tag.getCompound("Entity");
        this.pose = Pose.values()[tag.getInt("Pose")];
        this.bodyYaw = tag.getFloat("BodyYaw");
        this.headYaw = tag.getFloat("HeadYaw");
    }

}