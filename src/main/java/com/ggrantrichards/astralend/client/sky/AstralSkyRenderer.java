package com.ggrantrichards.astralend.client.sky;

import com.ggrantrichards.astralend.maze.Constellations;
import com.ggrantrichards.astralend.AstralEnd;
import com.ggrantrichards.astralend.flare.ClientFlares;
import com.ggrantrichards.astralend.flare.FlarePhase;
import com.ggrantrichards.astralend.flare.FlareState;
import com.ggrantrichards.astralend.world.ModBiomes;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.vertex.VertexFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.SkyRenderer;
import net.minecraft.client.renderer.state.level.SkyRenderState;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.model.sprite.AtlasManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

// The End's sky. Draws vanilla's End sky first, then on top of it, far away like the sun and moon:
//
//   - a starfield in two layers: one fixed, one turning so slowly (once in 40 minutes) you only notice
//     after a while;
//   - auroras: slow curtains of light, strongest over the Ashen Barrens (green and gold), faint violet
//     over the Nebula Fields; none in the Hollow Isles;
//   - the Remnant, the cinder of the collapsed star, hanging over the nearest Corona (the server tells us
//     where; it's a real structure, and close up the real one takes over). It brightens before a
//     Sunstorm and blazes during one;
//   - in the Corona during a flare: the sky turns gold and sheets of light roll across it on the wind;
//   - now and then a far-off comet streaks past, and very rarely something huge - a whale, a ring - drifts
//     across the far sky.
//
// Nothing here is an entity or touches the world; it's all a few hundred quads a frame.
public class AstralSkyRenderer extends SkyRenderer {
    private static final VertexFormat FORMAT = DefaultVertexFormat.POSITION_TEX_COLOR;
    private static final float DISTANCE = 100.0F;
    private static final int MAX_DYNAMIC_QUADS = 640;
    private static final Identifier STAR = texture("star");
    private static final Identifier REMNANT = texture("remnant");
    private static final Identifier REMNANT_FLARE = texture("remnant_flare");
    private static final Identifier AURORA = texture("aurora");
    private static final Identifier FLARE_SHEET = texture("flare_sheet");
    private static final Identifier COMET = texture("comet");
    private static final Identifier WHALE = texture("silhouette_whale");
    private static final Identifier RING = texture("silhouette_ring");

    private final RenderTarget target;
    private final TextureManager textures;
    // Loaded up front: a texture can't be loaded (uploaded) while a render pass is open.
    private final Map<Identifier, AbstractTexture> loaded = new HashMap<>();
    private final GpuBuffer stars;
    private final int fixedStarQuads;
    private final int turningStarQuads;
    private final GpuBuffer dynamic;
    private final RenderSystem.AutoStorageIndexBuffer indices = RenderSystem.getSequentialBuffer(PrimitiveTopology.QUADS);

    // Read from the level each frame (extractRenderState), used in render().
    private boolean inEnd;
    private double time;
    private float partialTick;
    private Vec3 camera = Vec3.ZERO;
    private boolean inCorona;
    private float corona;     // 0..1: how much of the view round the camera is the Corona (eased)
    private float auroraStrength;
    private final float[] auroraTarget = new float[7];   // strength, then two colours (r, g, b)
    private final float[] aurora = new float[7];

    public AstralSkyRenderer(TextureManager textures, AtlasManager atlases, RenderTarget target) {
        super(textures, atlases, target);
        this.target = target;
        this.textures = textures;
        for (Identifier id : List.of(STAR, REMNANT, REMNANT_FLARE, AURORA, FLARE_SHEET, COMET, WHALE, RING)) {
            this.loaded.put(id, textures.getTexture(id));
        }
        int[] counts = new int[2];
        this.stars = buildStars(counts);
        this.fixedStarQuads = counts[0];
        this.turningStarQuads = counts[1];
        this.dynamic = RenderSystem.getDevice().createBuffer(() -> "Astral End sky", 40, (long) MAX_DYNAMIC_QUADS * 4 * FORMAT.getVertexSize());
    }

    private static Identifier texture(String name) {
        return Identifier.fromNamespaceAndPath(AstralEnd.MODID, "textures/environment/" + name + ".png");
    }

    @Override
    public void extractRenderState(ClientLevel level, float partialTicks, Camera camera, SkyRenderState state) {
        super.extractRenderState(level, partialTicks, camera, state);
        this.inEnd = state.skybox == DimensionType.Skybox.END;
        if (!this.inEnd) {
            return;
        }
        this.partialTick = partialTicks;
        this.time = level.getGameTime() + partialTicks;
        this.camera = camera.position();
        // What the sky looks like here is blended from the biomes in a 24-block square round the camera
        // (the middle counting double), then eased toward: so walking or flying across a border, the
        // auroras and the Corona's burning sky fade over 10-15 blocks, never snap.
        float[] t = this.auroraTarget;
        java.util.Arrays.fill(t, 0.0F);
        float[] one = new float[7];
        float corona = 0.0F, weights = 0.0F;
        for (int sx = -12; sx <= 12; sx += 12) {
            for (int sz = -12; sz <= 12; sz += 12) {
                float w = sx == 0 && sz == 0 ? 2.0F : 1.0F;
                Holder<Biome> biome = level.getBiome(BlockPos.containing(this.camera.x + sx, this.camera.y, this.camera.z + sz));
                auroraFor(biome, one);
                for (int i = 0; i < 7; i++) {
                    t[i] += one[i] * w;
                }
                corona += biome.is(ModBiomes.CORONA) ? w : 0.0F;
                weights += w;
            }
        }
        for (int i = 0; i < 7; i++) {
            t[i] /= weights;
            this.aurora[i] += (t[i] - this.aurora[i]) * 0.03F;
        }
        this.corona += (corona / weights - this.corona) * 0.03F;
        this.inCorona = this.corona > 0.5F;
        this.auroraStrength = this.aurora[0];
    }

    // The auroras of a biome: strength, then two colours (r, g, b each).
    private static void auroraFor(Holder<Biome> biome, float[] t) {
        if (biome.is(ModBiomes.ASHEN_BARRENS)) {
            set(t, 1.0F, 0.35F, 1.0F, 0.6F, 1.0F, 0.82F, 0.35F);
        } else if (biome.is(ModBiomes.NEBULA_FIELDS)) {
            set(t, 0.55F, 0.7F, 0.45F, 1.0F, 1.0F, 0.5F, 0.85F);
        } else if (biome.is(ModBiomes.CHORUS_THICKET)) {
            set(t, 0.45F, 0.95F, 0.5F, 1.0F, 1.0F, 0.75F, 0.95F);
        } else if (biome.is(ModBiomes.GLIMMERING_CRAGS)) {
            set(t, 0.7F, 0.25F, 1.0F, 0.85F, 0.5F, 1.0F, 0.9F);
        } else if (biome.is(ModBiomes.HOLLOW_ISLES) || biome.is(ModBiomes.CORONA)) {
            set(t, 0.0F, 0.12F, 0.65F, 0.5F, 1.0F, 0.5F, 0.8F);
        } else {
            set(t, 0.12F, 0.65F, 0.5F, 1.0F, 0.5F, 0.8F, 1.0F);
        }
    }

    private static void set(float[] a, float... values) {
        System.arraycopy(values, 0, a, 0, values.length);
    }

    @Override
    public void render(GpuBufferSlice skyFog, SkyRenderState state) {
        super.render(skyFog, state);
        if (!this.inEnd) {
            return;
        }
        FlareState flare = ClientFlares.state();
        float intensity = ClientFlares.intensity(this.partialTick);
        float coronaFlare = this.corona * intensity;

        // ---- everything that moves is rebuilt each frame into one buffer, in drawing order
        Quads quads = new Quads();
        if (coronaFlare > 0.01F) {
            quads.group(FLARE_SHEET);
            this.skyTint(quads, coronaFlare);
        }
        int behindStars = quads.groups.size();
        this.silhouette(quads);
        this.constellations(quads, Mth.clamp(1.0F - 1.4F * coronaFlare, 0.0F, 1.0F));
        if (this.auroraStrength > 0.01F) {
            quads.group(AURORA);
            this.auroras(quads);
        }
        if (flare.hasRemnant()) {
            this.remnant(quads, flare, intensity);
        }
        this.comet(quads);
        if (coronaFlare > 0.01F && (flare.phase() == FlarePhase.FLARE || flare.phase() == FlarePhase.WARNING)) {
            quads.group(FLARE_SHEET);
            this.flareSheets(quads, flare, coronaFlare);
        }
        quads.upload(this.dynamic);

        Matrix4f view = RenderSystem.getModelViewMatrixCopy();
        try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(() -> "Astral End sky",
                this.target.getColorTextureView(), Optional.empty(), this.target.getDepthTextureView(), OptionalDouble.empty())) {
            RenderSystem.bindDefaultUniforms(pass);
            Vector4f white = new Vector4f(1.0F);
            for (int i = 0; i < behindStars; i++) {
                this.draw(pass, quads.groups.get(i), view, white);
            }
            // The stars fade while the Corona's sky burns.
            float starAlpha = Mth.clamp(1.0F - 1.4F * coronaFlare, 0.0F, 1.0F);
            this.drawStars(pass, view, 0, this.fixedStarQuads, new Vector4f(1.0F, 1.0F, 1.0F, 0.75F * starAlpha));
            Matrix4f turning = new Matrix4f(view)
                .rotate((float) Math.toRadians(23.0), 1.0F, 0.0F, 0.0F)
                .rotate((float) (this.time / (20.0 * 60.0 * 40.0) * Math.PI * 2.0), 0.0F, 1.0F, 0.0F);
            this.drawStars(pass, turning, this.fixedStarQuads, this.turningStarQuads, new Vector4f(1.0F, 1.0F, 1.0F, starAlpha));
            for (int i = behindStars; i < quads.groups.size(); i++) {
                this.draw(pass, quads.groups.get(i), view, white);
            }
        }
    }

    @Override
    public void close() {
        super.close();
        this.stars.close();
        this.dynamic.close();
    }

    // ------------------------------------------------------------------ drawing

    private void draw(RenderPass pass, Group group, Matrix4f matrix, Vector4f colour) {
        if (group.count == 0) {
            return;
        }
        this.drawRange(pass, this.dynamic, group.texture, group.first, group.count, matrix, colour);
    }

    private void drawStars(RenderPass pass, Matrix4f matrix, int first, int count, Vector4f colour) {
        if (count > 0) {
            this.drawRange(pass, this.stars, STAR, first, count, matrix, colour);
        }
    }

    private void drawRange(RenderPass pass, GpuBuffer buffer, Identifier texture, int firstQuad, int quadCount, Matrix4f matrix, Vector4f colour) {
        AbstractTexture tex = this.loaded.get(texture);
        GpuBufferSlice transforms = RenderSystem.getDynamicUniforms().writeTransform(matrix, colour);
        pass.setPipeline(RenderSystem.getCompiledPipeline(RenderPipelines.END_SKY));
        pass.setUniform("DynamicTransforms", transforms);
        pass.setUniform("Sampler0", tex.getTextureView(), tex.getSampler());
        pass.setVertexBuffer(0, buffer.slice());
        pass.setIndexBuffer(this.indices.getBuffer(quadCount * 6), this.indices.type());
        pass.drawIndexed(quadCount * 6, 1, 0, firstQuad * 4, 0);
    }

    // ------------------------------------------------------------------ the starfield (built once)

    private static GpuBuffer buildStars(int[] counts) {
        RandomSource random = RandomSource.createThreadLocalInstance(5791L);
        int[] tints = {0xFFE8F0FF, 0xFFE8F0FF, 0xFFFFFFFF, 0xFFC9B6FF, 0xFFFFE2B0, 0xFFB8F4FF};
        int total = 1100;
        try (ByteBufferBuilder bytes = ByteBufferBuilder.exactlySized(total * 2 * 4 * FORMAT.getVertexSize())) {
            BufferBuilder builder = new BufferBuilder(bytes, PrimitiveTopology.QUADS, FORMAT);
            int[] made = new int[2];
            for (int layer = 0; layer < 2; layer++) {
                int n = layer == 0 ? 400 : 700;
                while (made[layer] < n * 2) {
                    Vector3f dir = new Vector3f(random.nextFloat() * 2 - 1, random.nextFloat() * 2 - 1, random.nextFloat() * 2 - 1);
                    float len = dir.length();
                    if (len < 0.1F || len > 1.0F) {
                        continue;
                    }
                    dir.normalize();
                    float size = 0.22F + random.nextFloat() * random.nextFloat() * 0.45F;   // mostly small, a few bright
                    int alpha = (int) ((0.45F + random.nextFloat() * 0.55F) * 255);
                    int colour = (tints[random.nextInt(tints.length)] & 0x00FFFFFF) | (alpha << 24);
                    made[layer] += sprite(builder, dir, size, size, random.nextFloat() * Mth.TWO_PI, 0, 0, 1, 1, colour);
                }
            }
            counts[0] = made[0];
            counts[1] = made[1];
            try (MeshData mesh = builder.buildOrThrow()) {
                return RenderSystem.getDevice().createBuffer(() -> "Astral End stars", 40, mesh.vertexBuffer());
            }
        }
    }

    // ------------------------------------------------------------------ sky pieces (rebuilt each frame)

    // The Corona's sky turns gold during a flare: a box round the camera, drawn faintly.
    private void skyTint(Quads quads, float strength) {
        int colour = argb(0.42F * strength, 1.0F, 0.74F, 0.42F);
        float d = DISTANCE * 0.9F;
        Vector3f[] c = {
            new Vector3f(-d, -d, -d), new Vector3f(d, -d, -d), new Vector3f(d, d, -d), new Vector3f(-d, d, -d),
            new Vector3f(-d, -d, d), new Vector3f(d, -d, d), new Vector3f(d, d, d), new Vector3f(-d, d, d)};
        int[][] faces = {{0, 1, 2, 3}, {5, 4, 7, 6}, {4, 0, 3, 7}, {1, 5, 6, 2}, {3, 2, 6, 7}, {4, 5, 1, 0}};
        for (int[] f : faces) {
            // the bright middle row of the sheet texture: a flat wash of colour
            quads.quad(c[f[0]], c[f[1]], c[f[2]], c[f[3]], 0.2F, 0.66F, 0.3F, 0.67F, colour);
        }
    }

    // The four constellations of the Constellation Mazes (maze/Constellations), fixed in the sky: their
    // stars a little brighter than the rest, joined by faint lines, so you can learn to find them.
    private void constellations(Quads quads, float alpha) {
        if (alpha < 0.02F) {
            return;
        }
        quads.group(FLARE_SHEET);
        int line = argb(0.2F * alpha, 0.72F, 0.82F, 1.0F);
        for (Constellations.Constellation c : Constellations.ALL) {
            for (int[] l : c.lines()) {
                Vec3 a = Constellations.star(c, c.stars()[l[0]][0], c.stars()[l[0]][1]);
                Vec3 b = Constellations.star(c, c.stars()[l[1]][0], c.stars()[l[1]][1]);
                Vector3f pa = new Vector3f((float) a.x, (float) a.y, (float) a.z).mul(DISTANCE);
                Vector3f pb = new Vector3f((float) b.x, (float) b.y, (float) b.z).mul(DISTANCE);
                Vector3f across = new Vector3f(pb).sub(pa).cross(new Vector3f(pa).add(pb)).normalize().mul(0.12F);
                // the bright middle row of the sheet texture: a flat wash of colour
                quads.quad(new Vector3f(pa).add(across), new Vector3f(pa).sub(across), new Vector3f(pb).sub(across),
                    new Vector3f(pb).add(across), 0.2F, 0.66F, 0.3F, 0.67F, line);
            }
        }
        quads.group(STAR);
        for (Constellations.Constellation c : Constellations.ALL) {
            for (float[] star : c.stars()) {
                Vec3 d = Constellations.star(c, star[0], star[1]);
                float twinkle = 0.85F + 0.15F * Mth.sin((float) this.time * 0.05F + star[0] * 1.7F + star[1]);
                quads.sprite(new Vector3f((float) d.x, (float) d.y, (float) d.z), 0.95F, 0.95F, 0.0F, 0, 0, 1, 1,
                    argb(alpha * twinkle, 0.9F, 0.94F, 1.0F));
            }
        }
    }

    // Three slow curtains of aurora around the sky, rippling.
    private void auroras(Quads quads) {
        int segments = 40;
        for (int k = 0; k < 3; k++) {
            float base = (float) Math.toRadians(k * 120.0 + this.time * 0.0008);
            float r = k == 1 ? this.aurora[4] : this.aurora[1];
            float g = k == 1 ? this.aurora[5] : this.aurora[2];
            float b = k == 1 ? this.aurora[6] : this.aurora[3];
            Vector3f[] bottom = new Vector3f[segments + 1];
            Vector3f[] top = new Vector3f[segments + 1];
            int[] colours = new int[segments + 1];
            for (int j = 0; j <= segments; j++) {
                float a = base + (float) Math.toRadians((j / (float) segments - 0.5F) * 75.0F);
                float radius = 92.0F + 7.0F * Mth.sin(j * 0.55F + (float) this.time * 0.012F + k * 2.0F);
                float y = 16.0F + 4.0F * Mth.sin(j * 0.3F + (float) this.time * 0.008F + k);
                bottom[j] = new Vector3f(Mth.cos(a) * radius, y, Mth.sin(a) * radius);
                top[j] = new Vector3f(Mth.cos(a) * radius * 0.92F, y + 42.0F, Mth.sin(a) * radius * 0.92F);
                float fade = Mth.sin(Mth.PI * j / segments);
                float shimmer = 0.7F + 0.3F * Mth.sin(j * 0.9F + (float) this.time * 0.04F + k * 3.0F);
                colours[j] = argb(Math.min(1.0F, this.auroraStrength * fade * shimmer), r, g, b);
            }
            for (int j = 0; j < segments; j++) {
                float u0 = j / 10.0F;
                float u1 = (j + 1) / 10.0F;
                quads.quad(top[j], bottom[j], bottom[j + 1], top[j + 1], u0, 0.0F, u1, 1.0F,
                    colours[j], colours[j], colours[j + 1], colours[j + 1]);
            }
        }
    }

    // The Remnant, where it really hangs over the nearest Corona: low on the horizon from far away,
    // high overhead beneath it. Far off it's drawn larger than it is (as the moon is); close enough for
    // the real thing to be in view, the picture of it fades away and leaves the real one.
    private void remnant(Quads quads, FlareState flare, float intensity) {
        double dx = flare.remnantX() + 0.5 - this.camera.x;
        double dy = flare.remnantY() + 0.5 - this.camera.y;
        double dz = flare.remnantZ() + 0.5 - this.camera.z;
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        float view = Minecraft.getInstance().options.getEffectiveRenderDistance() * 16.0F;
        float fade = (float) Mth.clamp((distance - view * 0.75) / (view * 0.45), 0.0, 1.0);
        if (fade <= 0.0F || distance < 1.0) {
            return;
        }
        Vector3f dir = new Vector3f((float) (dx / distance), (float) (dy / distance), (float) (dz / distance));
        float closeness = (float) Mth.clamp(1.0 - (distance - 300.0) / 3000.0, 0.0, 1.0);
        float pulse = 1.0F + 0.05F * Mth.sin((float) this.time * 0.02F);
        float real = (float) (DISTANCE * 22.0 / distance);
        float size = Math.max(real, 7.0F + 9.0F * closeness) * pulse * (1.0F + 0.25F * intensity);
        float glow = (0.3F + 0.7F * intensity) * fade;
        quads.group(REMNANT_FLARE);
        float haloSize = size * (1.7F + 1.1F * intensity);
        quads.sprite(dir, haloSize, haloSize, 0.0F, 0, 0, 1, 1,
            argb(Math.min(1.0F, glow), 1.0F, 0.75F, 0.45F));
        float bright = 0.75F + 0.25F * intensity;
        quads.group(REMNANT);
        quads.sprite(dir, size, size, 0.0F, 0, 0, 1, 1, argb(fade, bright, 0.55F + 0.45F * intensity, 0.5F + 0.5F * intensity));
    }

    // Sheets of flare light rolling across the Corona's sky with the wind.
    private void flareSheets(Quads quads, FlareState flare, float strength) {
        float yaw = flare.windYaw() * Mth.DEG_TO_RAD;
        Vector3f wind = new Vector3f(-Mth.sin(yaw), 0.0F, Mth.cos(yaw));
        Vector3f across = new Vector3f(Mth.cos(yaw), 0.0F, Mth.sin(yaw));
        for (int k = 0; k < 3; k++) {
            float s = (float) ((this.time * 0.0035 + k / 3.0) % 1.0) * 2.0F - 1.0F;
            float alpha = strength * 0.6F * (1.0F - s * s);
            Vector3f centre = new Vector3f(wind).mul(s * 80.0F).add(0.0F, 50.0F + 8.0F * k, 0.0F);
            Vector3f side = new Vector3f(across).mul(140.0F);
            Vector3f depth = new Vector3f(wind).mul(22.0F).add(0.0F, -6.0F, 0.0F);
            Vector3f a = new Vector3f(centre).sub(side).sub(depth);
            Vector3f b = new Vector3f(centre).sub(side).add(depth);
            Vector3f c = new Vector3f(centre).add(side).add(depth);
            Vector3f d = new Vector3f(centre).add(side).sub(depth);
            int colour = argb(alpha, 1.0F, 0.8F, 0.45F);
            quads.quad(a, b, c, d, 0.0F, 0.0F, 4.0F, 1.0F, colour);
        }
    }

    // About once every 45 seconds (now and then), a comet streaks across the far sky.
    private void comet(Quads quads) {
        long window = (long) (this.time / 900.0);
        RandomSource random = RandomSource.createThreadLocalInstance(window * 341873128712L + 132897987541L);
        if (random.nextInt(5) >= 3) {
            return;
        }
        double start = window * 900.0 + random.nextInt(700);
        double p = (this.time - start) / 80.0;
        if (p < 0.0 || p > 1.0) {
            return;
        }
        float yaw0 = random.nextFloat() * Mth.TWO_PI;
        float elev0 = (float) Math.toRadians(25.0 + random.nextFloat() * 35.0);
        float sweep = (float) Math.toRadians(30.0 + random.nextFloat() * 20.0) * (random.nextBoolean() ? 1 : -1);
        Vector3f now = direction(yaw0 + sweep * (float) p, elev0 - 0.15F * (float) p);
        Vector3f before = direction(yaw0 + sweep * (float) (p - 0.08), elev0 - 0.15F * (float) (p - 0.08));
        Vector3f head = new Vector3f(now).mul(DISTANCE);
        Vector3f tail = new Vector3f(before).mul(DISTANCE).sub(head).normalize().mul(16.0F);
        Vector3f side = new Vector3f(now).cross(tail).normalize().mul(0.9F);
        float alpha = Mth.sin((float) p * Mth.PI);
        int colour = argb(alpha, 0.85F, 0.95F, 1.0F);
        quads.group(COMET);
        quads.quad(new Vector3f(head).sub(side), new Vector3f(head).add(side), new Vector3f(head).add(tail).add(side),
            new Vector3f(head).add(tail).sub(side), 0.0F, 0.0F, 1.0F, 1.0F, colour);
    }

    // Very rarely (some of the 20-minute stretches), a vast shape drifts across the far sky.
    private void silhouette(Quads quads) {
        long window = (long) (this.time / 24000.0);
        RandomSource random = RandomSource.createThreadLocalInstance(window * 9781237L + 412391L);
        if (random.nextInt(2) == 0) {
            return;
        }
        double start = window * 24000.0 + 4000 + random.nextInt(16000);
        double p = (this.time - start) / 2400.0;
        if (p < 0.0 || p > 1.0) {
            return;
        }
        boolean whale = random.nextInt(3) != 0;
        quads.group(whale ? WHALE : RING);
        float yaw = random.nextFloat() * Mth.TWO_PI + (float) Math.toRadians(80.0) * (float) p;
        float elevation = (float) Math.toRadians(20.0 + 6.0 * Math.sin(p * Math.PI));
        float alpha = (float) Math.min(1.0, Math.sin(p * Math.PI) * 1.6) * 0.85F;
        Vector3f dir = direction(yaw, elevation);
        if (whale) {
            quads.sprite(dir, 30.0F, 9.5F, 0.0F, 0, 0, 1, 1, argb(alpha, 1, 1, 1));
        } else {
            quads.sprite(dir, 20.0F, 20.0F, 0.15F, 0, 0, 1, 1, argb(alpha, 1, 1, 1));
        }
    }

    private static Vector3f direction(float yaw, float elevation) {
        return new Vector3f(Mth.cos(yaw) * Mth.cos(elevation), Mth.sin(elevation), Mth.sin(yaw) * Mth.cos(elevation));
    }

    private static int argb(float a, float r, float g, float b) {
        return ((int) (Mth.clamp(a, 0, 1) * 255) << 24) | ((int) (Mth.clamp(r, 0, 1) * 255) << 16)
            | ((int) (Mth.clamp(g, 0, 1) * 255) << 8) | (int) (Mth.clamp(b, 0, 1) * 255);
    }

    // A quad facing the camera at `dir` (unit), half-size w x h, rolled by `roll`. Written twice (both
    // windings), so it shows whichever way the pipeline culls. Returns the number of quads written.
    private static int sprite(BufferBuilder builder, Vector3f dir, float w, float h, float roll, float u0, float v0, float u1, float v1,
                              int colour) {
        Vector3f centre = new Vector3f(dir).mul(DISTANCE);
        Vector3f right = new Vector3f(dir).cross(0.0F, 1.0F, 0.0F);
        if (right.lengthSquared() < 1.0E-4F) {
            right.set(1.0F, 0.0F, 0.0F);
        }
        right.normalize();
        Vector3f up = new Vector3f(right).cross(dir).normalize();
        Vector3f r = new Vector3f(right).mul(Mth.cos(roll)).add(new Vector3f(up).mul(Mth.sin(roll))).mul(w);
        Vector3f u = new Vector3f(up).mul(Mth.cos(roll)).sub(new Vector3f(right).mul(Mth.sin(roll))).mul(h);
        Vector3f a = new Vector3f(centre).sub(r).add(u);
        Vector3f b = new Vector3f(centre).sub(r).sub(u);
        Vector3f c = new Vector3f(centre).add(r).sub(u);
        Vector3f d = new Vector3f(centre).add(r).add(u);
        quadBothSides(builder, a, b, c, d, u0, v0, u1, v1, colour, colour, colour, colour);
        return 2;
    }

    // a = (u0, v0), b = (u0, v1), c = (u1, v1), d = (u1, v0)
    private static void quadBothSides(BufferBuilder builder, Vector3f a, Vector3f b, Vector3f c, Vector3f d,
                                      float u0, float v0, float u1, float v1, int ca, int cb, int cc, int cd) {
        builder.addVertex(a.x, a.y, a.z).setUv(u0, v0).setColor(ca);
        builder.addVertex(b.x, b.y, b.z).setUv(u0, v1).setColor(cb);
        builder.addVertex(c.x, c.y, c.z).setUv(u1, v1).setColor(cc);
        builder.addVertex(d.x, d.y, d.z).setUv(u1, v0).setColor(cd);
        builder.addVertex(a.x, a.y, a.z).setUv(u0, v0).setColor(ca);
        builder.addVertex(d.x, d.y, d.z).setUv(u1, v0).setColor(cd);
        builder.addVertex(c.x, c.y, c.z).setUv(u1, v1).setColor(cc);
        builder.addVertex(b.x, b.y, b.z).setUv(u0, v1).setColor(cb);
    }

    // A run of quads in the per-frame buffer that share one texture. Quads always go into the newest group.
    private static final class Group {
        final Identifier texture;
        int first;
        int count;

        Group(Identifier texture, int first) {
            this.texture = texture;
            this.first = first;
        }
    }

    // Collects the frame's quads into one vertex buffer, in groups.
    private static final class Quads {
        private final ByteBufferBuilder bytes = new ByteBufferBuilder(MAX_DYNAMIC_QUADS * 4 * FORMAT.getVertexSize());
        private final BufferBuilder builder = new BufferBuilder(this.bytes, PrimitiveTopology.QUADS, FORMAT);
        private final List<Group> groups = new ArrayList<>();
        private int total;

        Group group(Identifier texture) {
            Group group = new Group(texture, this.total);
            this.groups.add(group);
            return group;
        }

        private Group current() {
            return this.groups.get(this.groups.size() - 1);
        }

        private boolean full() {
            return this.total + 2 > MAX_DYNAMIC_QUADS;
        }

        void quad(Vector3f a, Vector3f b, Vector3f c, Vector3f d, float u0, float v0, float u1, float v1, int colour) {
            this.quad(a, b, c, d, u0, v0, u1, v1, colour, colour, colour, colour);
        }

        void quad(Vector3f a, Vector3f b, Vector3f c, Vector3f d, float u0, float v0, float u1, float v1, int ca, int cb, int cc, int cd) {
            if (this.full()) {
                return;
            }
            quadBothSides(this.builder, a, b, c, d, u0, v0, u1, v1, ca, cb, cc, cd);
            this.total += 2;
            this.current().count += 2;
        }

        void sprite(Vector3f dir, float w, float h, float roll, float u0, float v0, float u1, float v1, int colour) {
            if (this.full()) {
                return;
            }
            int n = AstralSkyRenderer.sprite(this.builder, dir, w, h, roll, u0, v0, u1, v1, colour);
            this.total += n;
            this.current().count += n;
        }

        void upload(GpuBuffer buffer) {
            try (MeshData mesh = this.builder.build()) {
                if (mesh != null) {
                    RenderSystem.getDevice().createCommandEncoder().writeToBuffer(buffer.slice(), mesh.vertexBuffer());
                }
            } finally {
                this.bytes.close();
            }
        }
    }
}
