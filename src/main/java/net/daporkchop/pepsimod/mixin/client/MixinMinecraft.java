/*
 * Adapted from the Wizardry License
 *
 * Copyright (c) 2017-2019 DaPorkchop_
 *
 * Permission is hereby granted to any persons and/or organizations using this software to copy, modify, merge, publish, and distribute it.
 * Said persons and/or organizations are not allowed to use the software or any derivatives of the work for commercial use or any other means to generate income, nor are they allowed to claim this software as their own.
 *
 * The persons and/or organizations are also disallowed from sub-licensing and/or trademarking this software without explicit permission from DaPorkchop_.
 *
 * Any persons and/or organizations using this software must disclose their source code and have it publicly available, include this license, provide sufficient credit to the original author of the project (IE: DaPorkchop_), as well as provide a link to the original project.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON INFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */

package net.daporkchop.pepsimod.mixin.client;

import net.daporkchop.pepsimod.Pepsimod;
import net.daporkchop.pepsimod.PepsimodMixinLoader;
import net.daporkchop.pepsimod.module.ModuleManager;
import net.daporkchop.pepsimod.module.api.Module;
import net.daporkchop.pepsimod.module.impl.render.UnfocusedCPUMod;
import net.daporkchop.pepsimod.module.impl.render.ZoomMod;
import net.daporkchop.pepsimod.util.config.impl.CpuLimitTranslator;
import net.daporkchop.pepsimod.util.config.impl.FriendsTranslator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.fml.common.FMLLog;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import static net.daporkchop.pepsimod.util.PepsiConstants.mc;
import static net.daporkchop.pepsimod.util.PepsiConstants.mcStartedSuccessfully;
import static net.daporkchop.pepsimod.util.PepsiConstants.pepsimod;

@Mixin(Minecraft.class)
public abstract class MixinMinecraft {
    @Shadow
    public EntityPlayerSP player;

    @Shadow
    private ByteBuffer readImageToBuffer(InputStream imageStream) throws IOException {
        return null;
    }

    @Inject(
            method = "Lnet/minecraft/client/Minecraft;shutdown()V",
            at = @At("HEAD")
    )
    public void saveSettingsOnShutdown(CallbackInfo ci) {
        if (mcStartedSuccessfully) {
            System.out.println("[PEPSIMOD] Saving config...");
            pepsimod.saveConfig();
            System.out.println("[PEPSIMOD] Saved.");

            if (ZoomMod.INSTANCE.state.enabled) {
                ModuleManager.disableModule(ZoomMod.INSTANCE);
                mc.gameSettings.fovSetting = ZoomMod.INSTANCE.fov;
            }
        }
    }

    @Inject(
            method = "Lnet/minecraft/client/Minecraft;runGameLoop()V",
            at = @At("RETURN")
    )
    public void postOnClientPreTick(CallbackInfo callbackInfo) {
        if (mcStartedSuccessfully && mc.player != null && mc.player.movementInput != null) { // is ingame
            for (Module module : ModuleManager.AVALIBLE_MODULES) {
                if (module.shouldTick()) {
                    module.tick();
                }
            }
        }
    }

    @Inject(
            method = "Lnet/minecraft/client/Minecraft;runTickMouse()V",
            at = @At(
                    value = "INVOKE_ASSIGN",
                    target = "Lorg/lwjgl/input/Mouse;getEventButton()I"
            ))
    public void onMouseClick(CallbackInfo ci) {
        try {
            if (Mouse.getEventButtonState()) {
                int buttonID = Mouse.getEventButton();
                switch (buttonID) {
                    case 2:
                        if (Minecraft.getMinecraft().objectMouseOver != null) {
                            RayTraceResult result = Minecraft.getMinecraft().objectMouseOver;
                            if (result.typeOfHit == RayTraceResult.Type.ENTITY && result.entityHit instanceof EntityPlayer) {
                                if (FriendsTranslator.INSTANCE.isFriend(result.entityHit)) {
                                    this.player.sendMessage(new TextComponentString(Pepsimod.CHAT_PREFIX + "Removed \u00A7c" + result.entityHit.getName() + "\u00A7r as a friend"));
                                    FriendsTranslator.INSTANCE.friends.remove(result.entityHit.getUniqueID());
                                } else {
                                    this.player.sendMessage(new TextComponentString(Pepsimod.CHAT_PREFIX + "Added \u00A79" + result.entityHit.getName() + "\u00A7r as a friend"));
                                    FriendsTranslator.INSTANCE.friends.add(result.entityHit.getUniqueID());
                                }
                            }
                            FMLLog.log.info(result.entityHit.getClass().getCanonicalName());
                        }
                        break;
                }
            }
        } catch (NullPointerException e) {
            //wtf who cares
        }
    }

    @Redirect(
            method = "Lnet/minecraft/client/Minecraft;createDisplay()V",
            at = @At(
                    value = "INVOKE",
                    target = "Lorg/lwjgl/opengl/Display;setTitle(Ljava/lang/String;)V"
            ))
    public void changeWindowTitle(String title) {
        Display.setTitle(Pepsimod.NAME_VERSION + (PepsimodMixinLoader.isObfuscatedEnvironment ? "" : " (dev environment)"));
    }

    @Inject(
            method = "Lnet/minecraft/client/Minecraft;setWindowIcon()V",
            at = @At("HEAD"),
            cancellable = true
    )
    public void preSetWindowIcon(CallbackInfo callbackInfo) {
        try {
            BufferedImage img = ImageIO.read(Pepsimod.class.getResourceAsStream("/pepsilogo.png"));
            List<ByteBuffer> sizes = new ArrayList<>();
            int w = img.getWidth();
            do  {
                BufferedImage tmp = new BufferedImage(w, w, img.getType());
                tmp.createGraphics().drawImage(img, 0, 0, w, w, null);
                sizes.add(this.convertImageToBuffer(tmp));
                w >>= 1;
            } while (w >= 8);
            Display.setIcon(sizes.toArray(new ByteBuffer[sizes.size()]));
            callbackInfo.cancel();
        } catch (IOException e) {
            e.printStackTrace();
            //thonk
        }

        //in case of exception vanilla code will run
    }

    @Inject(method = "displayGuiScreen", at = @At("HEAD"))
    public void preDisplayGuiScreen(GuiScreen guiScreen, CallbackInfo callbackInfo) {
        if (mcStartedSuccessfully && ZoomMod.INSTANCE.state.enabled) {
            ModuleManager.disableModule(ZoomMod.INSTANCE);
            mc.gameSettings.fovSetting = ZoomMod.INSTANCE.fov;
        }
    }

    @Inject(
            method = "Lnet/minecraft/client/Minecraft;getLimitFramerate()I",
            at = @At("HEAD"),
            cancellable = true
    )
    public void preGetLimitFramerate(CallbackInfoReturnable<Integer> callbackInfoReturnable) {
        try {
            if (UnfocusedCPUMod.INSTANCE.state.enabled && !Display.isActive()) {
                callbackInfoReturnable.setReturnValue(CpuLimitTranslator.INSTANCE.limit);
            }
        } catch (NullPointerException e) {
        }
    }

    @Inject(
            method = "Lnet/minecraft/client/Minecraft;processKeyBinds()V",
            at = @At("HEAD")
    )
    public void preProcessKeyBinds(CallbackInfo ci) {
        // If . is typed open GuiChat
        // Bypass the keybind system because the command prefix is not configurable
        if (mcStartedSuccessfully && mc.currentScreen == null && Keyboard.getEventCharacter() == '.') {
            mc.displayGuiScreen(new GuiChat("."));
        }
    }

    @Redirect(
            method = "Lnet/minecraft/client/Minecraft;clickMouse()V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/multiplayer/PlayerControllerMP;attackEntity(Lnet/minecraft/entity/player/EntityPlayer;Lnet/minecraft/entity/Entity;)V"
            ))
    public void preventAttackingRiddenEntity(PlayerControllerMP controller, EntityPlayer attacker, Entity attacked) {
        if (!attacked.isPassenger(attacker)) {
            controller.attackEntity(attacker, attacked);
        }
    }


    private ByteBuffer convertImageToBuffer(BufferedImage bufferedimage) throws IOException {
        int[] aint = bufferedimage.getRGB(0, 0, bufferedimage.getWidth(), bufferedimage.getHeight(), null, 0, bufferedimage.getWidth());
        ByteBuffer bytebuffer = ByteBuffer.allocate(4 * aint.length);

        for (int i : aint) {
            bytebuffer.putInt(i << 8 | i >> 24 & 255);
        }

        bytebuffer.flip();
        return bytebuffer;
    }
}
