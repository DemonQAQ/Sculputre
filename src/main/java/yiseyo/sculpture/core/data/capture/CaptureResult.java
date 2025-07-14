package yiseyo.sculpture.core.data.capture;

import net.minecraft.client.renderer.RenderType;

import java.util.List;
import java.util.Map;

public class CaptureResult
{
    private final Map<RenderType, List<Vertex>> byRenderType;

    public CaptureResult(Map<RenderType, List<Vertex>> map)
    {
        this.byRenderType = Map.copyOf(map);
    }

    public Map<RenderType, List<Vertex>> mesh()
    {
        return byRenderType;
    }

    public boolean isEmpty()
    {
        return byRenderType.isEmpty();
    }
}