package net.daporkchop.pepsimod.module.impl.render;

import net.daporkchop.pepsimod.PepsiMod;
import net.daporkchop.pepsimod.module.api.CustomOption;
import net.daporkchop.pepsimod.module.api.Module;
import net.daporkchop.pepsimod.module.api.ModuleOption;
import net.daporkchop.pepsimod.util.PepsiUtils;
import net.daporkchop.pepsimod.util.XrayUtils;
import net.minecraft.block.Block;
import net.minecraft.util.ResourceLocation;

public class XrayMod extends Module {
    public static XrayMod INSTANCE;

    public XrayMod(boolean isEnabled, int key, boolean hide) {
        super(isEnabled, "Xray", key, hide);
    }

    @Override
    public void onEnable() {
        try {
            PepsiMod.INSTANCE.mc.renderGlobal.loadRenderers();
        } catch (NullPointerException e) {
            //we don't care, mc isn't initialized yet
        }
    }

    @Override
    public void onDisable() {
        try {
            PepsiMod.INSTANCE.mc.renderGlobal.loadRenderers();
        } catch (NullPointerException e) {
            //we don't care, mc isn't initialized yet
        }
    }

    @Override
    public void tick() {

    }

    @Override
    public void init() {
        INSTANCE = this;
    }

    @Override
    public ModuleOption[] getDefaultOptions() {
        return new ModuleOption[]{
                new CustomOption<>(0, "add", new String[0], (value) -> {
                }, () -> {
                    return 0;
                }),
                new CustomOption<>(0, "remove", new String[0], (value) -> {
                }, () -> {
                    return 0;
                })
        };
    }

    @Override
    public String getSuggestion(String cmd, String[] args) {
        if (args.length == 2 && args[1].equals("add")) {
            return cmd + " " + Block.REGISTRY.getObjectById(7).getRegistryName().toString();
        } else if (args.length == 3 && args[1].equals("add")) {
            if (args[2].isEmpty()) {
                return cmd + Block.REGISTRY.getObjectById(7).getRegistryName().toString();
            } else {
                String arg = args[2];
                for (Block b : Block.REGISTRY) {
                    String s = b.getRegistryName().toString();
                    if (s.startsWith(arg)) {
                        return args[0] + " " + args[1] + " " + s;
                    }
                }

                return "";
            }
        } else if (args.length == 2 && args[1].equals("remove")) {
            return cmd + " " + Block.REGISTRY.getObjectById(XrayUtils.target_blocks.get(0)).getRegistryName().toString();
        } else if (args.length == 3 && args[1].equals("remove")) {
            if (args[2].isEmpty()) {
                return cmd + Block.REGISTRY.getObjectById(XrayUtils.target_blocks.get(0)).getRegistryName().toString();
            } else {
                String arg = args[2];
                for (Integer i : XrayUtils.target_blocks) {
                    String s = Block.REGISTRY.getObjectById(i).getRegistryName().toString();
                    if (s.startsWith(arg)) {
                        return args[0] + " " + args[1] + " " + s;
                    }
                }

                return "";
            }
        }

        return super.getSuggestion(cmd, args);
    }

    @Override
    public void execute(String cmd, String[] args) {
        if (args.length == 3 && !args[2].isEmpty() && cmd.startsWith(".xray add ")) {
            String s = args[2].toLowerCase();
            try {
                int id = Integer.parseInt(s);
                Block block = Block.REGISTRY.getObjectById(id);
                if (block == null) {
                    clientMessage("Not a valid block ID: " + PepsiUtils.COLOR_ESCAPE + "o" + args[2]);
                } else {
                    XrayUtils.target_blocks.add(id);
                    clientMessage("Added " + PepsiUtils.COLOR_ESCAPE + "o" + block.getRegistryName().toString() + PepsiUtils.COLOR_ESCAPE + "r to the Xray list");
                    if (this.isEnabled) {
                        PepsiMod.INSTANCE.mc.renderGlobal.loadRenderers();
                    }
                }
            } catch (NumberFormatException e) {
                if (s.contains(":") && !s.endsWith(":") && !s.startsWith(":")) {
                    String[] split = s.split(":");
                    Block block = Block.REGISTRY.getObject(new ResourceLocation(split[0], split[1]));
                    if (block == null) {
                        clientMessage("Invalid id: " + PepsiUtils.COLOR_ESCAPE + "o" + s);
                    } else {
                        XrayUtils.target_blocks.add(PepsiUtils.getBlockId(block));
                        clientMessage("Added " + PepsiUtils.COLOR_ESCAPE + "o" + block.getRegistryName().toString() + PepsiUtils.COLOR_ESCAPE + "r to the Xray list");
                        if (this.isEnabled) {
                            PepsiMod.INSTANCE.mc.renderGlobal.loadRenderers();
                        }
                    }
                } else {
                    clientMessage("Invalid id: " + PepsiUtils.COLOR_ESCAPE + "o" + s);
                }
            }
            return;
        } else if (args.length == 3 && !args[2].isEmpty() && cmd.startsWith(".xray remove ")) {
            String s = args[2].toLowerCase();
            try {
                int id = Integer.parseInt(s);
                if (XrayUtils.target_blocks.contains(id)) {
                    XrayUtils.target_blocks.remove((Integer) id);
                    clientMessage("Removed " + PepsiUtils.COLOR_ESCAPE + "o" + id + PepsiUtils.COLOR_ESCAPE + "r from the Xray list");
                    if (this.isEnabled) {
                        PepsiMod.INSTANCE.mc.renderGlobal.loadRenderers();
                    }
                } else {
                    clientMessage("Block ID " + PepsiUtils.COLOR_ESCAPE + "o" + args[2] + PepsiUtils.COLOR_ESCAPE + "r is not on the Xray list!");
                }
            } catch (NumberFormatException e) {
                if (s.contains(":") && !s.endsWith(":") && !s.startsWith(":")) {
                    String[] split = s.split(":");
                    Block block = Block.REGISTRY.getObject(new ResourceLocation(split[0], split[1]));
                    if (block == null) {
                        clientMessage("Invalid id: " + PepsiUtils.COLOR_ESCAPE + "o" + s);
                    } else {
                        int id = PepsiUtils.getBlockId(block);
                        if (XrayUtils.target_blocks.contains(id)) {
                            XrayUtils.target_blocks.remove((Integer) id);
                            clientMessage("Removed " + PepsiUtils.COLOR_ESCAPE + "o" + s + PepsiUtils.COLOR_ESCAPE + "r from the Xray list");
                            if (this.isEnabled) {
                                PepsiMod.INSTANCE.mc.renderGlobal.loadRenderers();
                            }
                        } else {
                            clientMessage("Block ID " + PepsiUtils.COLOR_ESCAPE + "o" + s + PepsiUtils.COLOR_ESCAPE + "r is not on the Xray list!");
                        }
                    }
                } else {
                    clientMessage("Invalid id: " + PepsiUtils.COLOR_ESCAPE + "o" + s);
                }
            }
            return;
        }

        super.execute(cmd, args);
    }
}