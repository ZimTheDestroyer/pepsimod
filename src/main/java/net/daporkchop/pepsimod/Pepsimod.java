/*
 * Adapted from the Wizardry License
 *
 * Copyright (c) 2016-2019 DaPorkchop_
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

package net.daporkchop.pepsimod;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.experimental.UtilityClass;
import net.daporkchop.pepsimod.asm.PepsimodMixinLoader;
import net.daporkchop.pepsimod.module.Module;
import net.daporkchop.pepsimod.util.PepsiConstants;
import net.daporkchop.pepsimod.util.PepsiUtil;
import net.daporkchop.pepsimod.util.config.ConfigManager;
import net.daporkchop.pepsimod.util.event.impl.Event;
import net.daporkchop.pepsimod.util.render.text.RainbowTextRenderer;
import net.daporkchop.pepsimod.util.render.text.TextRenderer;
import net.daporkchop.pepsimod.util.resources.Resources;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.fml.common.event.FMLConstructionEvent;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.relauncher.Side;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The main class of pepsimod's Forge mod.
 *
 * @author DaPorkchop_
 */
@Mod(
        modid = PepsiConstants.MOD_ID,
        useMetadata = true
)
@Getter
public final class Pepsimod implements PepsiConstants {
    @Getter(onMethod_ = {@Mod.InstanceFactory})
    private static final Pepsimod INSTANCE = new Pepsimod();
    protected Resources resources;

    private Pepsimod() {
        if (INSTANCE != null) {
            throw new IllegalStateException("pepsimod instance already created!");
        }

        Minecraft.memoryReserve = null; //literally no reason to have this exist lol
    }

    @Mod.EventHandler
    public void construction(FMLConstructionEvent event) {
        if (event.getSide() != Side.CLIENT) {
            throw new IllegalStateException("pepsimod can only be loaded on a client!");
        }

        String version;
        if (PepsimodMixinLoader.OBFUSCATED) {
            ModContainer container = FMLCommonHandler.instance().findContainerFor(this);
            version = container.getVersion();
            if ("${version}".equals(version)) {
                version = "unknown";
            } else {
                Matcher matcher = Pattern.compile("([.0-9]+)-([.0-9]+)").matcher(VERSION);
                if (!matcher.find()) {
                    throw new IllegalStateException(String.format("Unparseable version: \"%s\"", VERSION));
                }
                version = matcher.group(1);
                if (!MinecraftForge.MC_VERSION.equals(matcher.group(2))) {
                    throw new IllegalStateException(String.format("pepsimod expected Minecraft %s, but found %s!", MinecraftForge.MC_VERSION, matcher.group(2)));
                }
            }
        } else {
            version = "dev";
        }

        PepsiUtil.putStaticFinalField(Minecraft.getMinecraft(), PepsiConstants.class, "mc");
        PepsiUtil.putStaticFinalField(version, PepsiConstants.class, "VERSION");
        PepsiUtil.putStaticFinalField(String.format("%s-%s", VERSION, MinecraftForge.MC_VERSION), PepsiConstants.class, "VERSION_FULL");
        PepsiUtil.putStaticFinalField(new ScaledResolution(mc), PepsiConstants.class, "RESOLUTION");
    }

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        log.info("Loading pepsimod %s...\n", VERSION_FULL);

        ConfigManager.init(event.getAsmData()); //initialize config manager

        this.resources = new Resources();
        PepsiUtil.setTextRenderer(TextRenderer.Type.RAINBOW.renderer());
        //MinecraftForge.EVENT_BUS.register(new KeyRegistry());

        PepsiUtil.registerStandardEvents();
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        this.resources.tryLoad();
    }

    @Config(modid = Pepsimod.MOD_ID)
    @UtilityClass
    public static class SystemConfig    {
        @Config.Comment({
                "pepsimod's resources system.",
                "This loads various data from the network, such as capes, icons and special players."
        })
        public ResourcesConfig resources = new ResourcesConfig();

        @NoArgsConstructor(access = AccessLevel.PRIVATE)
        public static class ResourcesConfig    {
            @Config.Comment({
                    "Toggles the resources system.",
                    "There's generally no reason to disable this unless you're worried about GitHub stealing your IP address, which is stupid."
            })
            @Config.RequiresMcRestart
            public boolean enable = true;

            @Config.Comment({
                    "Whether or not to cache resources locally.",
                    "Disabling this is generally safe, but may cause issues if your internet connection is bad."
            })
            public boolean useCache = true;

            @Config.Comment({
                    "The base url to fetch resources from.",
                    "Use this if you want to use your own resources server!"
            })
            public String baseUrl = "https://raw.githubusercontent.com/Team-Pepsi/pepsimod/master/resources/resources.json";
        }
    }
}
