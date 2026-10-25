package net.zennin.mushoku.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

/** Teclas del mod. Por defecto igual que el original: Shift + Ctrl para conjurar, WASD flechas, Tab para lanzar. */
public final class ModKeys {
    private static final String CAT = "key.categories.remushoku";
    public static final KeyMapping CAST_1 = reg("key.remushoku.cast_key", GLFW.GLFW_KEY_LEFT_SHIFT);
    public static final KeyMapping CAST_2 = reg("key.remushoku.cast_key_2", GLFW.GLFW_KEY_LEFT_CONTROL);
    public static final KeyMapping UP = reg("key.remushoku.up_arrow", GLFW.GLFW_KEY_W);
    public static final KeyMapping DOWN = reg("key.remushoku.down_arrow", GLFW.GLFW_KEY_S);
    public static final KeyMapping LEFT = reg("key.remushoku.left_arrow", GLFW.GLFW_KEY_A);
    public static final KeyMapping RIGHT = reg("key.remushoku.right_arrow", GLFW.GLFW_KEY_D);
    public static final KeyMapping SHOOT = reg("key.remushoku.spell_shoot", GLFW.GLFW_KEY_TAB);

    private ModKeys() {}

    private static KeyMapping reg(String name, int key) {
        return KeyBindingHelper.registerKeyBinding(new KeyMapping(name, InputConstants.Type.KEYSYM, key, CAT));
    }

    /**
     * Lee el estado real de la tecla. Varias teclas del mod comparten tecla con el juego (Shift, Ctrl, WASD, Tab),
     * y Minecraft solo le avisa a una de ellas, asi que se consulta el teclado directamente.
     */
    public static boolean isDown(KeyMapping mapping) {
        InputConstants.Key key = KeyBindingHelper.getBoundKeyOf(mapping);
        if (key.getType() == InputConstants.Type.KEYSYM && key.getValue() != InputConstants.UNKNOWN.getValue())
            return InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), key.getValue());
        return mapping.isDown();
    }

    public static void init() {}
}
