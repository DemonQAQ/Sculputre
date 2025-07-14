package yiseyo.sculpture.core.manager.capture;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

record PendingCapture(ServerLevel level, BlockPos pos, int ticks)
{
}
