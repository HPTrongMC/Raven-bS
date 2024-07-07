package keystrokesmod.module.impl.render;

import keystrokesmod.module.Module;
import keystrokesmod.module.impl.player.Freecam;
import keystrokesmod.module.impl.world.AntiBot;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.DescriptionSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.RenderUtils;
import keystrokesmod.utility.Utils;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.*;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.*;
import net.minecraft.util.MathHelper;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.GLU;

import java.awt.*;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.Map;

public class Nametags extends Module {
    private SliderSetting scale;
    private ButtonSetting autoScale;
    private ButtonSetting drawBackground;
    private ButtonSetting dropShadow;
    private ButtonSetting showDistance;
    private ButtonSetting showHealth;
    private ButtonSetting showHitsToKill;
    private ButtonSetting showInvis;
    private ButtonSetting removeTags;
    private ButtonSetting renderSelf;
    private ButtonSetting showArmor;
    private ButtonSetting showEnchants;
    private ButtonSetting showDurability;
    private ButtonSetting showStackSize;
    private Map<EntityPlayer, double[]> entityPositions = new HashMap();
    private int backGroundColor = new Color(0, 0, 0, 65).getRGB();
    private int friendColor = new Color(0, 255, 0, 255).getRGB();
    private int enemyColor = new Color(255, 0, 0, 255).getRGB();
    public Nametags() {
        super("Nametags", category.render, 0);
        this.registerSetting(scale = new SliderSetting("Scale", 1.0, 0.5, 5.0, 0.1));
        this.registerSetting(autoScale = new ButtonSetting("Auto-scale", true));
        this.registerSetting(drawBackground = new ButtonSetting("Draw background", true));
        this.registerSetting(renderSelf = new ButtonSetting("Render self", false));
        this.registerSetting(dropShadow = new ButtonSetting("Drop shadow", true));
        this.registerSetting(showDistance = new ButtonSetting("Show distance", false));
        this.registerSetting(showHealth = new ButtonSetting("Show health", true));
        this.registerSetting(showHitsToKill = new ButtonSetting("Show hits to kill", false));
        this.registerSetting(showInvis = new ButtonSetting("Show invis", true));
        this.registerSetting(removeTags = new ButtonSetting("Remove tags", false));
        this.registerSetting(new DescriptionSetting("Armor settings"));
        this.registerSetting(showArmor = new ButtonSetting("Show armor", false));
        this.registerSetting(showEnchants = new ButtonSetting("Show enchants", true));
        this.registerSetting(showDurability = new ButtonSetting("Show durability", true));
        this.registerSetting(showStackSize = new ButtonSetting("Show stack size", true));
    }

    @SubscribeEvent
    public void onRenderTick(RenderGameOverlayEvent.Post ev) {
        if (!Utils.nullCheck()) {
            return;
        }
        if (ev.type != RenderGameOverlayEvent.ElementType.ALL) {
            return;
        }
        GlStateManager.pushMatrix();
        ScaledResolution scaledRes = new ScaledResolution(mc);
        double twoDScale = scaledRes.getScaleFactor() / Math.pow(scaledRes.getScaleFactor(), 2.0D);
        GlStateManager.scale(twoDScale, twoDScale, twoDScale);
        for (EntityPlayer entityPlayer : entityPositions.keySet()) {
            GlStateManager.pushMatrix();
            String name;
            if (removeTags.isToggled()) {
                name = entityPlayer.getName();
            }
            else {
                name = entityPlayer.getDisplayName().getFormattedText();
            }
            if (showHealth.isToggled()) {
                name = name + " " + Utils.getHealthStr(entityPlayer);
            }
            if (showHitsToKill.isToggled()) {
                name = name + " " + Utils.getHitsToKill(entityPlayer, mc.thePlayer.getCurrentEquippedItem());
            }
            if (showDistance.isToggled()) {
                int distance = Math.round(mc.thePlayer.getDistanceToEntity(entityPlayer));
                String color = "§";
                if (distance <= 8) {
                    color += "c";
                }
                else if (distance <= 15) {
                    color += "6";
                }
                else if (distance <= 25) {
                    color += "e";
                }
                else {
                    color = "";
                }
                name = color + distance + "m§r " + name;
            }
            double[] renderPositions = entityPositions.get(entityPlayer);
            if ((renderPositions[3] < 0) || (renderPositions[3] >= 1)) {
                GlStateManager.popMatrix();
                continue;
            }
            GlStateManager.translate(renderPositions[0], renderPositions[1], 0);
            int strWidth = mc.fontRendererObj.getStringWidth(name) / 2;
            GlStateManager.color(0.0F, 0.0F, 0.0F);
            double rawScaleSetting = scale.getInput();
            double scaleSetting = rawScaleSetting * 10;
            double nameTagScale = twoDScale * scaleSetting;
            final float renderPartialTicks = Utils.getTimer().renderPartialTicks;
            final EntityPlayer player = (Freecam.freeEntity == null) ? mc.thePlayer : Freecam.freeEntity;
            final double deltaX = player.lastTickPosX + (player.posX - player.lastTickPosX) * renderPartialTicks - (entityPlayer.lastTickPosX + (entityPlayer.posX - entityPlayer.lastTickPosX) * renderPartialTicks);
            final double deltaY = player.lastTickPosY + (player.posY - player.lastTickPosY) * renderPartialTicks - (entityPlayer.lastTickPosY + (entityPlayer.posY - entityPlayer.lastTickPosY) * renderPartialTicks);
            final double deltaZ = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * renderPartialTicks - (entityPlayer.lastTickPosZ + (entityPlayer.posZ - entityPlayer.lastTickPosZ) * renderPartialTicks);
            double distance = MathHelper.sqrt_double(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);
            if (!autoScale.isToggled()) {
                if (renderSelf.isToggled() && entityPlayer == mc.thePlayer) {
                    distance = 3;
                }
                nameTagScale = rawScaleSetting / (Math.max(distance, 3) / 10);
            }
            else {
                distance = Math.min(1, Math.max(0.7, 1 - (0.012 * Math.max(distance, 1))));
                nameTagScale *= distance;
            }
            GlStateManager.scale(nameTagScale, nameTagScale, nameTagScale);
            int x1 = -strWidth - 1;
            int y1 = -10;
            int x2 = strWidth + 1;
            int y2 = 8 - 9;
            if (drawBackground.isToggled()) {
                RenderUtils.drawRect(x1, y1, x2, y2, backGroundColor);
            }
            if (Utils.isFriended(entityPlayer)) {
                RenderUtils.drawOutline(x1, y1, x2, y2, 2, friendColor);
            }
            else if (Utils.isEnemy(entityPlayer)) {
                RenderUtils.drawOutline(x1, y1, x2, y2, 2, enemyColor);
            }
            mc.fontRendererObj.drawString(name, -strWidth, -9, -1, dropShadow.isToggled());
            if (showArmor.isToggled()) {
                renderArmor(entityPlayer);
            }
            GlStateManager.color(1.0F, 1.0F, 1.0F);
            GlStateManager.popMatrix();
        }
        GlStateManager.popMatrix();
    }

    @SubscribeEvent
    public void onRenderWorld(RenderWorldLastEvent renderWorldLastEvent) {
        if (!Utils.nullCheck()) {
            return;
        }
        updatePositions();
    }

    @SubscribeEvent
    public void onRenderLiving(RenderLivingEvent.Specials.Pre e) {
        if (e.entity instanceof EntityPlayer && (e.entity != mc.thePlayer || renderSelf.isToggled()) && e.entity.deathTime == 0) {
            final EntityPlayer entityPlayer = (EntityPlayer) e.entity;
            if (!showInvis.isToggled() && entityPlayer.isInvisible()) {
                return;
            }
            if (entityPlayer.getDisplayNameString().isEmpty() || (entityPlayer != mc.thePlayer && AntiBot.isBot(entityPlayer))) {
                return;
            }
            e.setCanceled(true);
        }
    }

    private void updatePositions() {
        entityPositions.clear();
        final float pTicks = Utils.getTimer().renderPartialTicks;
        for (EntityPlayer entityPlayer : mc.theWorld.playerEntities) {
            if (!showInvis.isToggled() && entityPlayer.isInvisible()) {
                continue;
            }
            if (entityPlayer == mc.thePlayer && (!renderSelf.isToggled() || mc.gameSettings.thirdPersonView == 0)) {
                continue;
            }
            if (entityPlayer.getDisplayNameString().isEmpty() || (entityPlayer != mc.thePlayer && AntiBot.isBot(entityPlayer))) {
                continue;
            }

            double interpolatedX = entityPlayer.lastTickPosX + (entityPlayer.posX - entityPlayer.lastTickPosX) * pTicks - mc.getRenderManager().viewerPosX;
            double interpolatedY = entityPlayer.lastTickPosY + (entityPlayer.posY - entityPlayer.lastTickPosY) * pTicks - mc.getRenderManager().viewerPosY;
            double interpolatedZ = entityPlayer.lastTickPosZ + (entityPlayer.posZ - entityPlayer.lastTickPosZ) * pTicks - mc.getRenderManager().viewerPosZ;

            interpolatedY += entityPlayer.isSneaking() ? entityPlayer.height - 0.05 : entityPlayer.height + 0.27;

            double[] convertedPosition = convertTo2D(interpolatedX, interpolatedY, interpolatedZ);
            if (convertedPosition == null) {
                continue;
            }
            if (convertedPosition[2] >= 0.0D && convertedPosition[2] < 1.0D) {
                double[] headConvertedPosition = convertTo2D(interpolatedX, interpolatedY + 1.0D, interpolatedZ);
                double height = Math.abs(headConvertedPosition[1] - convertedPosition[1]);
                entityPositions.put(entityPlayer, new double[]{convertedPosition[0], convertedPosition[1], height, convertedPosition[2]});
            }
        }
    }

    private double[] convertTo2D(double x, double y, double z) {
        FloatBuffer screenCoords = BufferUtils.createFloatBuffer(3);
        IntBuffer viewport = BufferUtils.createIntBuffer(16);
        FloatBuffer modelView = BufferUtils.createFloatBuffer(16);
        FloatBuffer projection = BufferUtils.createFloatBuffer(16);
        GL11.glGetFloat(2982, modelView);
        GL11.glGetFloat(2983, projection);
        GL11.glGetInteger(2978, viewport);
        boolean result = GLU.gluProject((float)x, (float)y, (float)z, modelView, projection, viewport, screenCoords);
        if (result) {
            return new double[] { screenCoords.get(0), org.lwjgl.opengl.Display.getHeight() - screenCoords.get(1), screenCoords.get(2) };
        }
        return null;
    }

    private void renderArmor(EntityPlayer e) {
        int pos = 0;
        for (ItemStack is : e.inventory.armorInventory) {
            if (is != null) {
                pos -= 8;
            }
        }
        if (e.getHeldItem() != null) {
            pos -= 8;
            ItemStack item = e.getHeldItem().copy();
            if (item.hasEffect() && (item.getItem() instanceof ItemTool || item.getItem() instanceof ItemArmor)) {
                item.stackSize = 1;
            }
            renderItemStack(item, pos, -20);
            pos += 16;
        }
        for (int i = 3; i >= 0; --i) {
            ItemStack stack = e.inventory.armorInventory[i];
            if (stack != null) {
                renderItemStack(stack, pos, -20);
                pos += 16;
            }
        }
    }

    private void renderItemStack(ItemStack stack, int xPos, int yPos) {
        GlStateManager.pushMatrix();
        mc.getRenderItem().zLevel = -150.0F;
        GlStateManager.enableDepth();
        RenderHelper.enableGUIStandardItemLighting();
        mc.getRenderItem().renderItemAndEffectIntoGUI(stack, xPos, yPos - 8);
        RenderHelper.disableStandardItemLighting();
        mc.getRenderItem().zLevel = 0.0F;
        GlStateManager.disableDepth();
        GlStateManager.scale(0.5, 0.5, 0.5);
        GlStateManager.translate(0, -10, 0);
        renderText(stack, xPos, yPos);
        GlStateManager.enableDepth();
        GlStateManager.scale(2, 2, 2);
        GlStateManager.popMatrix();
    }

    private void renderText(ItemStack stack, int xPos, int yPos) {
        int newYPos = yPos - 24;
        int remainingDurability = stack.getMaxDamage() - stack.getItemDamage();
        if (showDurability.isToggled() && stack.getItem() instanceof ItemArmor) {
            mc.fontRendererObj.drawString(String.valueOf(remainingDurability), (float) (xPos * 2), (float) yPos, 16777215, dropShadow.isToggled());
        }
        if (stack.getEnchantmentTagList() != null && stack.getEnchantmentTagList().tagCount() < 6 && showEnchants.isToggled()) {
            if (stack.getItem() instanceof ItemArmor) {
                int protection = EnchantmentHelper.getEnchantmentLevel(Enchantment.protection.effectId, stack);
                int projectileProtection = EnchantmentHelper.getEnchantmentLevel(Enchantment.projectileProtection.effectId, stack);
                int blastProtectionLvL = EnchantmentHelper.getEnchantmentLevel(Enchantment.blastProtection.effectId, stack);
                int fireProtection = EnchantmentHelper.getEnchantmentLevel(Enchantment.fireProtection.effectId, stack);
                int thornsLvl = EnchantmentHelper.getEnchantmentLevel(Enchantment.thorns.effectId, stack);
                int unbreakingLvl = EnchantmentHelper.getEnchantmentLevel(Enchantment.unbreaking.effectId, stack);

                if (protection > 0) {
                    mc.fontRendererObj.drawString("prot" + protection, (float) (xPos * 2), (float) newYPos, -1, dropShadow.isToggled());
                    newYPos += 8;
                }

                if (projectileProtection > 0) {
                    mc.fontRendererObj.drawString("proj" + projectileProtection, (float) (xPos * 2), (float) newYPos, -1, dropShadow.isToggled());
                    newYPos += 8;
                }

                if (blastProtectionLvL > 0) {
                    mc.fontRendererObj.drawString("bp" + blastProtectionLvL, (float) (xPos * 2), (float) newYPos, -1, dropShadow.isToggled());
                    newYPos += 8;
                }

                if (fireProtection > 0) {
                    mc.fontRendererObj.drawString("frp" + fireProtection, (float) (xPos * 2), (float) newYPos, -1, dropShadow.isToggled());
                    newYPos += 8;
                }

                if (thornsLvl > 0) {
                    mc.fontRendererObj.drawString("th" + thornsLvl, (float) (xPos * 2), (float) newYPos, -1, dropShadow.isToggled());
                    newYPos += 8;
                }

                if (unbreakingLvl > 0) {
                    mc.fontRendererObj.drawString("ub" + unbreakingLvl, (float) (xPos * 2), (float) newYPos, -1, dropShadow.isToggled());
                }
            }
            else if (stack.getItem() instanceof ItemBow) {
                int powerLvl = EnchantmentHelper.getEnchantmentLevel(Enchantment.power.effectId, stack);
                int punchLvl = EnchantmentHelper.getEnchantmentLevel(Enchantment.punch.effectId, stack);
                int flameLvl = EnchantmentHelper.getEnchantmentLevel(Enchantment.flame.effectId, stack);
                int unbreakingLvl = EnchantmentHelper.getEnchantmentLevel(Enchantment.unbreaking.effectId, stack);
                if (powerLvl > 0) {
                    mc.fontRendererObj.drawString("pow" + powerLvl, (float) (xPos * 2), (float) newYPos, -1, dropShadow.isToggled());
                    newYPos += 8;
                }

                if (punchLvl > 0) {
                    mc.fontRendererObj.drawString("pun" + punchLvl, (float) (xPos * 2), (float) newYPos, -1, dropShadow.isToggled());
                    newYPos += 8;
                }

                if (flameLvl > 0) {
                    mc.fontRendererObj.drawString("flame" + flameLvl, (float) (xPos * 2), (float) newYPos, -1, dropShadow.isToggled());
                    newYPos += 8;
                }

                if (unbreakingLvl > 0) {
                    mc.fontRendererObj.drawString("ub" + unbreakingLvl, (float) (xPos * 2), (float) newYPos, -1, dropShadow.isToggled());
                }
            }
            else if (stack.getItem() instanceof ItemSword) {
                int sharpnessLvl = EnchantmentHelper.getEnchantmentLevel(Enchantment.sharpness.effectId, stack);
                int knockbackLvl = EnchantmentHelper.getEnchantmentLevel(Enchantment.knockback.effectId, stack);
                int fireAspectLvl = EnchantmentHelper.getEnchantmentLevel(Enchantment.fireAspect.effectId, stack);
                int unbreakingLvl = EnchantmentHelper.getEnchantmentLevel(Enchantment.unbreaking.effectId, stack);
                if (sharpnessLvl > 0) {
                    mc.fontRendererObj.drawString("sh" + sharpnessLvl, (float) (xPos * 2), (float) newYPos, -1, dropShadow.isToggled());
                    newYPos += 8;
                }

                if (knockbackLvl > 0) {
                    mc.fontRendererObj.drawString("kb" + knockbackLvl, (float) (xPos * 2), (float) newYPos, -1, dropShadow.isToggled());
                    newYPos += 8;
                }

                if (fireAspectLvl > 0) {
                    mc.fontRendererObj.drawString("fire" + fireAspectLvl, (float) (xPos * 2), (float) newYPos, -1, dropShadow.isToggled());
                    newYPos += 8;
                }

                if (unbreakingLvl > 0) {
                    mc.fontRendererObj.drawString("ub" + unbreakingLvl, (float) (xPos * 2), (float) newYPos, -1, dropShadow.isToggled());
                }
            }
            else if (stack.getItem() instanceof ItemTool) {
                int unbreakingLvl = EnchantmentHelper.getEnchantmentLevel(Enchantment.unbreaking.effectId, stack);
                int efficiencyLvl = EnchantmentHelper.getEnchantmentLevel(Enchantment.efficiency.effectId, stack);
                int fortuneLvl = EnchantmentHelper.getEnchantmentLevel(Enchantment.fortune.effectId, stack);
                int silkTouchLvl = EnchantmentHelper.getEnchantmentLevel(Enchantment.silkTouch.effectId, stack);
                if (efficiencyLvl > 0) {
                    mc.fontRendererObj.drawString("eff" + efficiencyLvl, (float) (xPos * 2), (float) newYPos, -1, dropShadow.isToggled());
                    newYPos += 8;
                }

                if (fortuneLvl > 0) {
                    mc.fontRendererObj.drawString("fo" + fortuneLvl, (float) (xPos * 2), (float) newYPos, -1, dropShadow.isToggled());
                    newYPos += 8;
                }

                if (silkTouchLvl > 0) {
                    mc.fontRendererObj.drawString("silk" + silkTouchLvl, (float) (xPos * 2), (float) newYPos, -1, dropShadow.isToggled());
                    newYPos += 8;
                }

                if (unbreakingLvl > 0) {
                    mc.fontRendererObj.drawString("ub" + unbreakingLvl, (float) (xPos * 2), (float) newYPos, -1, dropShadow.isToggled());
                }
            }
        }
        if (showStackSize.isToggled() && !(stack.getItem() instanceof ItemSword) && !(stack.getItem() instanceof ItemBow) && !(stack.getItem() instanceof ItemTool) && !(stack.getItem() instanceof ItemArmor)) {
            mc.fontRendererObj.drawString(stack.stackSize + "x", (float) (xPos * 2), (float) yPos, -1, dropShadow.isToggled());
        }
    }
}
