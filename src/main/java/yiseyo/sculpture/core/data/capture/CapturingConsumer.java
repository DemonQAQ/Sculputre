package yiseyo.sculpture.core.data.capture;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.FastColor;

import java.util.ArrayList;
import java.util.List;

public class CapturingConsumer implements VertexConsumer
{
    private final RenderType renderType;
    private final List<Vertex> out = new ArrayList<>();
    // working vars
    private float x, y, z, u, v;
    private int colorARGB = 0xFFFFFFFF, lightPacked, overlayPacked;
    private float nx = 0, ny = 1, nz = 0;

    public CapturingConsumer(RenderType rt)
    {
        this.renderType = rt;
    }

    public RenderType renderType()
    {
        return renderType;
    }

    public List<Vertex> vertices()
    {
        return out;
    }

    @Override
    public VertexConsumer vertex(double x, double y, double z)
    {
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

    @Override
    public void defaultColor(int r, int g, int b, int a)
    {
    }

    @Override
    public void unsetDefaultColor()
    {
    }
}