package com.gugas749.abysscore.Client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

/**
 * Registers 9 keybind slots in the Controls
 * Assign keys in Options → Controls → AbyssCore.
 */
public class KeyBindings {
    public static final String CATEGORY = "key.categories.abysscore";
    public static final int SLOT_COUNT  = 9;

    public static final KeyMapping[] SLOTS = new KeyMapping[SLOT_COUNT];

    static {
        for (int i = 0; i < SLOT_COUNT; i++) {
            SLOTS[i] = new KeyMapping(
                    "key.abysscore.bind_" + (i + 1),          // translation key
                    InputConstants.Type.KEYSYM,
                    GLFW.GLFW_KEY_UNKNOWN,                      // unbound by default
                    CATEGORY
            );
        }
    }

    public static void register(RegisterKeyMappingsEvent event) {
        for (KeyMapping slot : SLOTS) {
            event.register(slot);
        }
    }
}
