package keystrokesmod.module.impl.movement;

import keystrokesmod.event.PreMotionEvent;
import keystrokesmod.module.Module;
import keystrokesmod.utility.*;
import net.minecraft.util.*;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class BMCFly extends Module {

    private float moveSpeed;
    private boolean started;

    public BMCFly() {
        super("BMCFly", category.movement, 0);
    }

    public void onEnable() {
        started = false;
    }

    public void onDisable() {
        MovementUtils.stop();
    }

    @SubscribeEvent
    public void onPreMotion(PreMotionEvent event) {
        final AxisAlignedBB bb = mc.thePlayer.getEntityBoundingBox().offset(0, 1, 0);

        if(started) {
            mc.thePlayer.motionY += 0.025;
            MovementUtils.strafe(moveSpeed *= 0.935F);

            if(mc.thePlayer.motionY < -0.5 && !PlayerUtils.isBlockUnder()) {
                Utils.sendDebugMessage("not block under");
                disable();
            }
        }

        if(!mc.theWorld.getCollidingBoundingBoxes(mc.thePlayer, bb).isEmpty() && !started) {
            started = true;
            mc.thePlayer.jump();
            MovementUtils.strafe(moveSpeed = 9);
        }

        Utils.sendDebugMessage(started+ "   bb " +mc.theWorld.getCollidingBoundingBoxes(mc.thePlayer, bb).isEmpty());
    }


}