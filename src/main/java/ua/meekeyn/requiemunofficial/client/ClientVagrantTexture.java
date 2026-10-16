package ua.meekeyn.requiemunofficial.client;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import ua.meekeyn.requiemunofficial.RequiemUnofficial;

public final class ClientVagrantTexture {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(RequiemUnofficial.MODID, "dynamic/vagrant_gray");
    public static final float RED = 1.0F;
    public static final float GREEN = 1.0F;
    public static final float BLUE = 1.0F;
    public static final float ALPHA = 1.0F;
    private static final int COLOR = 0x00F0F2F5;
    private static final int MAX_ALPHA = 77;
    private static boolean registered;

    private ClientVagrantTexture() {
    }

    public static ResourceLocation location() {
        if (!registered) {
            NativeImage image = new NativeImage(64, 64, false);
            fillRect(image, 0, 0, 64, 64);
            Minecraft.getInstance().getTextureManager().register(TEXTURE, new DynamicTexture(image));
            registered = true;
        }
        return TEXTURE;
    }

    private static void fillRect(NativeImage image, int left, int top, int width, int height) {
        for (int y = top; y < top + height; y++) {
            for (int x = left; x < left + width; x++) {
                image.setPixelRGBA(x, y, (MAX_ALPHA << 24) | COLOR);
            }
        }
    }
}
