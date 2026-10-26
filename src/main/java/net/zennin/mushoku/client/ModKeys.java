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
    // Espadachin: igual que el original, V abre el menu y Z / X / C usan las habilidades asignadas.
    public static final KeyMapping MENU = reg("key.remushoku.swordsman_menu_key", GLFW.GLFW_KEY_V);
    public static final KeyMapping SKILL_1 = reg("key.remushoku.skill_one_key", GLFW.GLFW_KEY_Z);
    public static final KeyMapping SKILL_2 = reg("key.remushoku.skill_two_key", GLFW.GLFW_KEY_X);
    public static final KeyMapping SKILL_3 = reg("key.remushoku.skill_three_key", GLFW.GLFW_KEY_C);
    // Ojos demoniacos: G y H como el original
    public static final KeyMapping EYE_1 = reg("key.remushoku.key_eye_one", GLFW.GLFW_KEY_G);
    public static final KeyMapping EYE_2 = reg("key.remushoku.key_eye_twoh", GLFW.GLFW_KEY_H);
    // Addon: N abre el Menu del Aventurero, R usa la habilidad de raza (como el addon original)
    public static final KeyMapping ADVENTURER = reg("key.remushoku.adventurer_menu", GLFW.GLFW_KEY_N);
    public static final KeyMapping RACE_SKILL = reg("key.remushoku.race_skill", GLFW.GLFW_KEY_R);
    /** Cargar la flecha en los arcos del mod. Comparte la R con la habilidad de raza: con un arco del mod en la mano, R carga. */
    public static final KeyMapping BOW_RELOAD = reg("key.remushoku.bow_reload", GLFW.GLFW_KEY_R);
    public static final KeyMapping COMBINE = reg("key.remushoku.spell_combine", GLFW.GLFW_KEY_KP_ADD);

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
