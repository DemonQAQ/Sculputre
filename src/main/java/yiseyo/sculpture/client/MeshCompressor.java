package yiseyo.sculpture.client;

public final class MeshCompressor
{
    public static byte[] compress(MeshCapture.CaptureResult res)
    {
        /* 这里用你喜欢的协议：NBT、Kryo、LZ4、Snappy… */
        return res.toString().getBytes(); // 占位实现
    }

    public static MeshCapture.CaptureResult decompress(byte[] bytes)
    {
        /* 同样自己实现 */
        return null;
    }
}