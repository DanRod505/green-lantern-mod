package com.danrod505.greenlantern.client.render.aqua;

import com.danrod505.greenlantern.GreenLantern;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.util.Mth;

/**
 * The giant seahorse: an upright, ringed body with the belly plates in front, a neck bending forward
 * into the long-snouted head with its crown (coronet), the fluttering dorsal and pectoral fins, a
 * gold-trimmed saddle behind the neck and a tail of five segments that curls forward under the
 * belly. Model space as in vanilla: facing -Z, Y pointing down. Texture (128x128) painted by
 * {@code tools/generate_atlantis_mounts.py} with the same box layout.
 */
public class SeahorseModel extends EntityModel<MountRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(GreenLantern.id("giant_seahorse"), "main");
    private static final float[] TAIL_CURL = {-0.25F, -0.45F, -0.6F, -0.75F, -0.9F};

    private final ModelPart body;
    private final ModelPart neck;
    private final ModelPart head;
    private final ModelPart dorsal;
    private final ModelPart leftFin;
    private final ModelPart rightFin;
    private final ModelPart[] tail = new ModelPart[5];

    public SeahorseModel(ModelPart root) {
        super(root, RenderTypes::entityCutoutNoCull);
        this.body = root.getChild("body");
        this.neck = body.getChild("neck");
        this.head = neck.getChild("head");
        this.dorsal = body.getChild("dorsal");
        this.leftFin = head.getChild("left_fin");
        this.rightFin = head.getChild("right_fin");
        ModelPart part = body;
        for (int i = 0; i < tail.length; i++) {
            part = part.getChild("tail" + i);
            tail[i] = part;
        }
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body",
                CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-6.0F, -10.0F, -5.0F, 12, 20, 11)
                        .texOffs(48, 0).addBox(-5.0F, -8.0F, -7.0F, 10, 16, 3)
                        .texOffs(48, 50).addBox(-4.5F, -11.5F, 0.0F, 9, 3, 8),
                PartPose.offset(0.0F, -4.0F, 0.0F));
        PartDefinition neck = body.addOrReplaceChild("neck",
                CubeListBuilder.create().texOffs(76, 0).addBox(-4.0F, -8.0F, -4.0F, 8, 8, 8),
                PartPose.offsetAndRotation(0.0F, -9.0F, -2.0F, 0.35F, 0.0F, 0.0F));
        PartDefinition head = neck.addOrReplaceChild("head",
                CubeListBuilder.create()
                        .texOffs(0, 32).addBox(-4.5F, -6.0F, -6.0F, 9, 8, 10)
                        .texOffs(40, 32).addBox(-2.0F, -3.0F, -16.0F, 4, 4, 10)
                        .texOffs(70, 32).addBox(-1.5F, -11.0F, -0.5F, 3, 5, 3),
                PartPose.offsetAndRotation(0.0F, -7.0F, 0.0F, -0.15F, 0.0F, 0.0F));
        head.addOrReplaceChild("left_fin",
                CubeListBuilder.create().texOffs(100, 32).addBox(0.0F, -2.0F, 0.0F, 1, 4, 4),
                PartPose.offsetAndRotation(4.5F, -1.0F, 1.0F, 0.0F, 0.4F, 0.0F));
        head.addOrReplaceChild("right_fin",
                CubeListBuilder.create().texOffs(112, 32).addBox(-1.0F, -2.0F, 0.0F, 1, 4, 4),
                PartPose.offsetAndRotation(-4.5F, -1.0F, 1.0F, 0.0F, -0.4F, 0.0F));
        body.addOrReplaceChild("dorsal",
                CubeListBuilder.create().texOffs(84, 32).addBox(-0.5F, -6.0F, 0.0F, 1, 12, 5),
                PartPose.offset(0.0F, 1.0F, 6.0F));
        int[][] boxes = {{9, 8, 9, 0, 52}, {7, 7, 7, 0, 70}, {5, 6, 5, 30, 70}, {4, 5, 4, 52, 70}, {3, 4, 3, 70, 70}};
        PartDefinition parent = body;
        float drop = 9.0F;
        for (int i = 0; i < boxes.length; i++) {
            int[] b = boxes[i];
            parent = parent.addOrReplaceChild("tail" + i,
                    CubeListBuilder.create().texOffs(b[3], b[4]).addBox(-b[0] / 2.0F, 0.0F, -b[2] / 2.0F, b[0], b[1], b[2]),
                    PartPose.offsetAndRotation(0.0F, drop, i == 0 ? 1.0F : 0.0F, TAIL_CURL[i], 0.0F, 0.0F));
            drop = b[1] - 0.5F;
        }
        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    public void setupAnim(MountRenderState state) {
        super.setupAnim(state);
        float t = state.ageInTicks;
        float swim = state.swimAmount;
        float phase = state.swimPhase;
        // Fast swimmers lean forward and stream the tail out behind; idle ones float upright, bobbing.
        float lean = state.outOfWater ? 0.0F : Mth.clamp((swim - 0.3F) / 0.7F, 0.0F, 1.0F);
        body.xRot = 0.55F * lean + Mth.sin(t * 0.06F) * 0.04F;
        body.y += Mth.sin(t * 0.08F) * 0.6F;
        neck.xRot = 0.35F - 0.25F * lean;
        head.xRot = -0.15F - 0.3F * lean + Mth.sin(t * 0.05F + 1.0F) * 0.05F;
        head.yRot = Mth.sin(t * 0.03F) * 0.12F;
        for (int i = 0; i < tail.length; i++) {
            float wave = Mth.sin(phase * 0.8F - i * 0.7F) * (0.08F + 0.05F * swim);
            tail[i].xRot = TAIL_CURL[i] * (1.0F - 0.75F * lean) + wave + (state.outOfWater ? -0.2F : 0.0F);
        }
        // The fins flutter very fast (that's how seahorses swim).
        dorsal.yRot = Mth.sin(t * 1.6F) * 0.35F;
        leftFin.yRot = 0.4F + Mth.sin(t * 1.4F) * 0.4F;
        rightFin.yRot = -0.4F - Mth.sin(t * 1.4F) * 0.4F;
    }
}
