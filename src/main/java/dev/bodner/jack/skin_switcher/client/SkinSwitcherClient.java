package dev.bodner.jack.skin_switcher.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

@Environment(EnvType.CLIENT)
public class SkinSwitcherClient implements ClientModInitializer {
    public static String spencerName = getPlayerSkinURL("HiImNJ");
    public static ArrayList<String> skins;
    public static ArrayList<String> filePaths;
    @Override
    public void onInitializeClient() {
        String workingDir = MinecraftClient.getInstance().runDirectory.getAbsolutePath();
        File skinsFolder = new File(workingDir+File.separator+"skins_to_replace");
        if (!skinsFolder.exists()){
            skinsFolder.mkdir();
        }
        skins = new ArrayList<>(Arrays.asList(skinsFolder.list())) ;
        for (int i = 0; i<=skins.size()-1; i++){
            skins.set(i,skins.get(i).replace(".png",""));
            skins.set(i,getPlayerSkinURL(skins.get(i)));
        }

        filePaths = new ArrayList<>(Arrays.asList(skinsFolder.list()));
        for (int j = 0; j<= filePaths.size()-1; j++){
            filePaths.set(j,skinsFolder.getAbsolutePath()+File.separator+filePaths.get(j));
        }

//        System.out.println(skins);
//        System.out.println(filePaths);
    }

    public static String getPlayerSkinURL(String name){

        String uuid;
        String encodedValue;
        String url = null;

        try {
            JsonParser jsonParser = new JsonParser();
            URL request = new URL("https://api.mojang.com/users/profiles/minecraft/" + name);
            HttpURLConnection connection = (HttpURLConnection) request.openConnection();
            connection.setRequestMethod("GET");
            JsonObject jsonObject = (JsonObject) jsonParser.parse(new InputStreamReader(connection.getInputStream()));
            uuid = jsonObject.get("id").getAsString();
            connection.disconnect();

            URL request1 = new URL("https://sessionserver.mojang.com/session/minecraft/profile/"+uuid);
            HttpURLConnection connection1 = (HttpURLConnection) request1.openConnection();
            connection1.setRequestMethod("GET");
            JsonObject jsonObject1 = (JsonObject) jsonParser.parse(new InputStreamReader(connection1.getInputStream()));
            encodedValue = jsonObject1.getAsJsonArray("properties").get(0).getAsJsonObject().get("value").getAsString();
            connection1.disconnect();

            Base64.Decoder decoder = Base64.getDecoder();
            byte[] bytes = decoder.decode(encodedValue);
            JsonObject jsonObject2 = (JsonObject) jsonParser.parse(new String(bytes));
            url = jsonObject2.getAsJsonObject("textures").getAsJsonObject("SKIN").get("url").getAsString();
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return url;
    }
}
