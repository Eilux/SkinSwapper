package dev.bodner.jack.skin_switcher.mixin;

import dev.bodner.jack.skin_switcher.client.SkinSwitcherClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.PlayerSkinTexture;
import net.minecraft.client.texture.ResourceTexture;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.CompletableFuture;

@Mixin(PlayerSkinTexture.class)
public abstract class SkinOverride extends ResourceTexture {

    @Shadow private static @Final Logger LOGGER;
    @Mutable @Shadow private @Final File cacheFile;
    @Mutable @Shadow private @Final String url;
    @Shadow private @Final boolean convertLegacy;
    @Shadow private @Final Runnable loadedCallback;
    @Shadow private CompletableFuture<?> loader;
    @Shadow private boolean loaded;

    @Shadow private NativeImage loadTexture(InputStream stream){return null;}
    @Shadow private void onTextureLoaded(NativeImage image) {}

//    Path skinLocation = FabricLoader.getInstance().getModContainer("who_is_spencer").get().getPath("assets").resolve("who_is_spencer").resolve("skins").resolve("NJ_Devil30.png");
//    File thing = new File(String.valueOf(skinLocation));

    public SkinOverride(Identifier location) {
        super(location);
    }

    /**
     * @author Eilux
     * @reason Can't figure out who spencer is
     */
    @Overwrite
    public void load(ResourceManager manager) throws IOException {
        boolean isLocal = false;
        if (SkinSwitcherClient.skins.contains(url)) {
            this.url = SkinSwitcherClient.filePaths.get(SkinSwitcherClient.skins.indexOf(url));
            this.cacheFile = null;
            isLocal = true;
        }

        MinecraftClient.getInstance().execute(() -> {
            if (!this.loaded) {
                try {
                    super.load(manager);
                } catch (IOException var3) {
                    LOGGER.warn((String)"Failed to load texture: {} : {}", (Object)this.location, (Object)var3);
                }

                this.loaded = true;
            }

        });
        if (this.loader == null) {
            NativeImage nativeImage2;

            if (this.cacheFile != null && this.cacheFile.isFile()) {
                LOGGER.debug((String)"Loading http texture from local cache ({})", (Object)this.cacheFile);
                FileInputStream fileInputStream = new FileInputStream(this.cacheFile);
                nativeImage2 = this.loadTexture(fileInputStream);
            } else {
                nativeImage2 = null;
            }

            if (nativeImage2 != null) {
                this.onTextureLoaded(nativeImage2);
            } else {
                boolean finalIsLocal = isLocal;
                this.loader = CompletableFuture.runAsync(() -> {
                    HttpURLConnection httpURLConnection = null;
                    LOGGER.debug((String)"Downloading http texture from {} to {}", (Object)this.url, (Object)this.cacheFile);

                    if (finalIsLocal){
                        try {
//                            File path = new File(this.url);
                            File path = new File(url);
                            if (!path.exists()){
                                LOGGER.error("the path is null");
                            }
                            InputStream inputStream;
                            if (this.cacheFile != null){
                                FileUtils.copyFile(path, this.cacheFile);
                                inputStream = new FileInputStream(this.cacheFile);

                            }
                            else {
                                inputStream = new FileInputStream(path);
                            }
                            MinecraftClient.getInstance().execute(() -> {
                                NativeImage nativeImage = this.loadTexture(inputStream);
                                if (nativeImage != null) {
                                    this.onTextureLoaded(nativeImage);
                                }

                            });

                        }
                        catch (Exception e){
                            LOGGER.error((String)"Couldn't download http texture", (Throwable)e);
                        }

                    }
                    else {
                        try {
                            httpURLConnection = (HttpURLConnection)(new URL(this.url)).openConnection(MinecraftClient.getInstance().getNetworkProxy());
                            httpURLConnection.setDoInput(true);
                            httpURLConnection.setDoOutput(false);
                            httpURLConnection.connect();
                            if (httpURLConnection.getResponseCode() / 100 == 2) {
                                InputStream inputStream2;
                                if (this.cacheFile != null) {
                                    FileUtils.copyInputStreamToFile(httpURLConnection.getInputStream(), this.cacheFile);
                                    inputStream2 = new FileInputStream(this.cacheFile);
                                } else {
                                    inputStream2 = httpURLConnection.getInputStream();
                                }

                                MinecraftClient.getInstance().execute(() -> {
                                    NativeImage nativeImage = this.loadTexture(inputStream2);
                                    if (nativeImage != null) {
                                        this.onTextureLoaded(nativeImage);
                                    }

                                });
                            }
                        } catch (Exception var6) {
                            LOGGER.error((String)"Couldn't download http texture", (Throwable)var6);
                        } finally {
                            if (httpURLConnection != null) {
                                httpURLConnection.disconnect();
                            }

                        }
                    }

                }, Util.getMainWorkerExecutor());
            }
        }
    }

}
