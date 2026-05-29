package dev.sixik.assemblytable.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.sixik.assemblytable.block.LaserBlock;
import dev.sixik.assemblytable.blockentity.LaserBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public class LaserBlockEntityRenderer implements BlockEntityRenderer<LaserBlockEntity> {

    private static final ResourceLocation LASER_TEXTURE = ResourceLocation.withDefaultNamespace("textures/entity/beacon_beam.png");

    @Override
    public void render(LaserBlockEntity entity, float partialTicks, PoseStack poseStack, MultiBufferSource bufferSource, int combinedLight, int combinedOverlay) {
        BlockPos targetPos = entity.getTargetPos();
        if (targetPos == null || !entity.hasEnergy()) return;

        Direction facing = entity.getBlockState().getValue(LaserBlock.FACING);
        LaserBlock laserBlock = (LaserBlock) entity.getBlockState().getBlock();
        Vec3 beamColor = entity.getBeamColor();

        double startX = 0.5 + facing.getStepX() * 0.25;
        double startY = 0.5 + facing.getStepY() * 0.25;
        double startZ = 0.5 + facing.getStepZ() * 0.25;
        Vec3 start = new Vec3(startX, startY, startZ);

        Vec3 endWorld;
        if (entity.laserRenderOffset != null) {
            endWorld = entity.laserRenderOffset;
        } else {
            endWorld = new Vec3(targetPos.getX() + 0.5, targetPos.getY() + 0.5625, targetPos.getZ() + 0.5);
        }

        Vec3 end = endWorld.subtract(entity.getBlockPos().getX(), entity.getBlockPos().getY(), entity.getBlockPos().getZ());

        Vec3 diff = start.subtract(end);
        float distance = (float) diff.length();

        float yaw = (float) (Mth.atan2(diff.x, diff.z) * Mth.RAD_TO_DEG);
        float distXZ = (float) Math.sqrt(diff.x * diff.x + diff.z * diff.z);
        float pitch = (float) (Mth.atan2(distXZ, diff.y) * Mth.RAD_TO_DEG);

        poseStack.pushPose();

        poseStack.translate(end.x, end.y, end.z);
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(pitch));

        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityTranslucentCull(LASER_TEXTURE));
        long time = entity.getLevel().getGameTime();
        float vOffset = (time + partialTicks) * -0.05F;
        float width = 0.03125f;

        renderBeam(
                poseStack,
                consumer,
                width,
                distance,
                vOffset,
                (int) Math.round(beamColor.x * 255.0),
                (int) Math.round(beamColor.y * 255.0),
                (int) Math.round(beamColor.z * 255.0),
                laserBlock.colorAlpha
        );

        poseStack.popPose();
    }

    private void renderBeam(PoseStack poseStack, VertexConsumer consumer, float width, float length, float vOffset, int r, int g, int b, int a) {
        poseStack.pushPose();

        int light = 15728880;
        for (int i = 0; i < 4; i++) {
            PoseStack.Pose pos = poseStack.last();
            Matrix4f pose = pos.pose();
            consumer.addVertex(pose, -width, 0, width).setColor(r, g, b, a).setUv(0, length + vOffset).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pos, 0, 0, 1);
            consumer.addVertex(pose, width, 0, width).setColor(r, g, b, a).setUv(1, length + vOffset).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pos, 0, 0, 1);
            consumer.addVertex(pose, width, length, width).setColor(r, g, b, a).setUv(1, 0 + vOffset).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pos, 0, 0, 1);
            consumer.addVertex(pose, -width, length, width).setColor(r, g, b, a).setUv(0, 0 + vOffset).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pos, 0, 0, 1);

            poseStack.mulPose(Axis.YP.rotationDegrees(90));
        }

        PoseStack.Pose pos = poseStack.last();
        Matrix4f pose = pos.pose();

        consumer.addVertex(pose, -width, 0, -width).setColor(r, g, b, a).setUv(0, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pos, 0, -1, 0);
        consumer.addVertex(pose, width, 0, -width).setColor(r, g, b, a).setUv(1, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pos, 0, -1, 0);
        consumer.addVertex(pose, width, 0, width).setColor(r, g, b, a).setUv(1, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pos, 0, -1, 0);
        consumer.addVertex(pose, -width, 0, width).setColor(r, g, b, a).setUv(0, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pos, 0, -1, 0);

        poseStack.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen(LaserBlockEntity pBlockEntity) {
        return true;
    }
}
