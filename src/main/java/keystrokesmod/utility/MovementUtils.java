package keystrokesmod.utility;

import net.minecraft.util.MathHelper;

import static keystrokesmod.Raven.mc;

public class MovementUtils {

    /**
     * Checks if the player is moving
     *
     * @return player moving
     */
    public static boolean isMoving() {
        return mc.thePlayer.moveForward != 0 || mc.thePlayer.moveStrafing != 0;
    }

    /**
     * Makes the player strafe
     */
    public void strafe() {
        strafe(speed());
    }

    /**
     * Makes the player strafe at the specified speed
     */
    public static void strafe(final double speed) {
        if (!isMoving()) {
            return;
        }

        final double yaw = direction();
        mc.thePlayer.motionX = -MathHelper.sin((float) yaw) * speed;
        mc.thePlayer.motionZ = MathHelper.cos((float) yaw) * speed;
    }

    public void strafe(final double speed, float yaw) {
        if (!isMoving()) {
            return;
        }

        yaw = (float) Math.toRadians(yaw);
        mc.thePlayer.motionX = -MathHelper.sin(yaw) * speed;
        mc.thePlayer.motionZ = MathHelper.cos(yaw) * speed;
    }

    /**
     * Stops the player from moving
     */
    public static void stop() {
        mc.thePlayer.motionX = 0;
        mc.thePlayer.motionZ = 0;
    }

    /**
     * Gets the players' movement yaw
     */
    public static double direction() {
        float rotationYaw = mc.thePlayer.rotationYaw;

        if (mc.thePlayer.moveForward < 0) {
            rotationYaw += 180;
        }

        float forward = 1;

        if (mc.thePlayer.moveForward < 0) {
            forward = -0.5F;
        } else if (mc.thePlayer.moveForward > 0) {
            forward = 0.5F;
        }

        if (mc.thePlayer.moveStrafing > 0) {
            rotationYaw -= 70 * forward;
        }

        if (mc.thePlayer.moveStrafing < 0) {
            rotationYaw += 70 * forward;
        }

        return Math.toRadians(rotationYaw);
    }

    /**
     * Used to get the players speed
     */
    public double speed() {
        return Math.hypot(mc.thePlayer.motionX, mc.thePlayer.motionZ);
    }
}
