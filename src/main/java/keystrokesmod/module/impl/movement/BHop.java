package keystrokesmod.module.impl.movement;

import keystrokesmod.Raven;
import keystrokesmod.event.ReceivePacketEvent;
import keystrokesmod.module.Module;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.Utils;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Keyboard;

public class BHop extends Module {
    public static SliderSetting mode;
    public static SliderSetting speed;
    private final ButtonSetting waterDisable;
    private final ButtonSetting sneakDisable;
    private final ButtonSetting stopMotion;
    private final String[] modes = new String[]{"Strafe", "Ground", "Hypixel"};

    // Hypixel
    boolean strafe, cooldown;
    int airTicks, cooldownticks;

    public BHop() {
        super("Bhop", Module.category.movement);
        this.registerSetting(mode = new SliderSetting("Mode", modes, 0));
        this.registerSetting(speed = new SliderSetting("Speed", 2.0, 0.5, 8.0, 0.1));
        this.registerSetting(waterDisable = new ButtonSetting("Disable in water", true));
        this.registerSetting(sneakDisable = new ButtonSetting("Disable while sneaking", true));
        this.registerSetting(stopMotion = new ButtonSetting("Stop motion", false));
    }

    @Override
    public String getInfo() {
        return modes[(int) mode.getInput()];
    }

    public void onUpdate() {
        if ((mc.thePlayer.isInWater() && waterDisable.isToggled()) || (mc.thePlayer.isSneaking() && sneakDisable.isToggled())) {
            return;
        }
        switch ((int) mode.getInput()) {
            case 0:
                if (Utils.isStrafing()) {
                    if (mc.thePlayer.onGround) {
                        mc.thePlayer.jump();
                    }
                    mc.thePlayer.setSprinting(true);
                    Utils.setSpeed(Utils.getHorizontalSpeed() + 0.005 * speed.getInput());
                    break;
                }
                break;
            case 1:
                if (!Keyboard.isKeyDown(mc.gameSettings.keyBindJump.getKeyCode()) && Utils.isStrafing() && mc.currentScreen == null) {
                    if (!mc.thePlayer.onGround) {
                        break;
                    }
                    mc.thePlayer.jump();
                    mc.thePlayer.setSprinting(true);
                    double horizontalSpeed = Utils.getHorizontalSpeed();
                    double additionalSpeed = 0.4847 * ((speed.getInput() - 1.0) / 3.0 + 1.0);
                    if (horizontalSpeed < additionalSpeed) {
                        horizontalSpeed = additionalSpeed;
                    }
                    Utils.setSpeed(horizontalSpeed);
                }
                break;
            case 2:
                if (mc.thePlayer.motionX != 0 && mc.thePlayer.motionZ != 0 && mc.thePlayer.onGround) {
                    mc.thePlayer.jump();
                    if (!strafe) {
                        setSpeed(0.42);
                    }
                }
                if (mc.thePlayer.hurtTime == 9 && !mc.thePlayer.onGround && !cooldown && mc.thePlayer.motionX != 0 && mc.thePlayer.motionZ != 0) {
                    strafe = true;
                    setSpeed(Math.hypot(mc.thePlayer.motionX, mc.thePlayer.motionZ) * 1.2);
                    cooldown = true;
                } else {
                    strafe = false;
                }
                if (cooldown) {
                    cooldownticks++;
                }
                if (cooldownticks == 10) {
                    cooldown = false;
                    cooldownticks = 0;
                }

                if (!mc.thePlayer.onGround) {
                    airTicks++;
                } else {
                    airTicks = 0;
                }
                break;
        }
    }

    @SubscribeEvent
    public void onReceivePacket(ReceivePacketEvent e) {
        Packet packet = e.getPacket();
        if (mode.getInput() == 2) {
            if (packet instanceof S12PacketEntityVelocity) {
                S12PacketEntityVelocity s12 = (S12PacketEntityVelocity) packet;
                if (s12.getEntityID() == mc.thePlayer.getEntityId()) {
                    if (mc.thePlayer.hurtTime <= 1 && s12.getMotionY() > 0d) {
                        mc.thePlayer.setVelocity(-(s12.getMotionX()) / 8000d, s12.getMotionY() / 8000d, -(s12.getMotionZ()) / 8000d);
                    } else {
                        mc.thePlayer.setVelocity(mc.thePlayer.motionX, s12.getMotionY() / 8000d, mc.thePlayer.motionZ);
                    }
                }
            }
        }
    }

    boolean wasEnabledVelocity;
    @Override
    public void onEnable() {
        if (Raven.getModuleManager().getModule("AntiKnockback").isEnabled()) {
            wasEnabledVelocity = true;
            Raven.getModuleManager().getModule("AntiKnockback").disable();
        } else {
            wasEnabledVelocity = false;
        }
        cooldownticks = 0;
        cooldown = false;
        airTicks = 0;
        strafe = false;
        super.onEnable();
    }

    @Override
    public void onDisable() {
        if (wasEnabledVelocity) {
            Raven.getModuleManager().getModule("AntiKnockback").enable();
        }
        if (stopMotion.isToggled()) {
            mc.thePlayer.motionZ = 0;
            mc.thePlayer.motionY = 0;
            mc.thePlayer.motionX = 0;
        }
        super.onDisable();
    }

    public static void setSpeed(double moveSpeed, float yaw, double strafe, double forward) {
        if (forward != 0.0D) {
            if (strafe > 0.0D) {
                yaw += ((forward > 0.0D) ? -45 : 45);
            } else if (strafe < 0.0D) {
                yaw += ((forward > 0.0D) ? 45 : -45);
            }
            strafe = 0.0D;
            if (forward > 0.0D) {
                forward = 1.0D;
            } else if (forward < 0.0D) {
                forward = -1.0D;
            }
        }
        if (strafe > 0.0D) {
            strafe = 1.0D;
        } else if (strafe < 0.0D) {
            strafe = -1.0D;
        }
        double mx = Math.cos(Math.toRadians((yaw + 90.0F)));
        double mz = Math.sin(Math.toRadians((yaw + 90.0F)));
        mc.thePlayer.motionX = forward * moveSpeed * mx + strafe * moveSpeed * mz;
        mc.thePlayer.motionZ = forward * moveSpeed * mz - strafe * moveSpeed * mx;
    }

    public static void setSpeed(double moveSpeed) {
        setSpeed(moveSpeed, mc.thePlayer.rotationYaw, mc.thePlayer.movementInput.moveStrafe, mc.thePlayer.movementInput.moveForward);
    }
}
