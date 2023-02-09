package com.pos.empressa.sunmi_pos.Nexgo.utils;

import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nexgo.oaf.apiv3.emv.AidEntity;
import com.nexgo.oaf.apiv3.emv.AidEntryModeEnum;
import com.nexgo.oaf.apiv3.emv.CapkEntity;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class EmvUtils {
    static Context context;
    public EmvUtils(Context context){
        this.context = context;
    }

    public static final String[] tags = new String[]{
            "5F20",
            "5F30",
            "9F03",
            "9F26",
            "9F27",
            "9F10",
            "9F37",
            "9F36",
            "95",
            "9A",
            "9C",
            "9F02",
            "5F2A",
            "82",
            "9F1A",
            "9F03",
            "9F33",
            "9F34",
            "9F35",
            "9F1E",
            "84",
            "9F09",
            "9F41",
            "9F63",
            "5A",
            "4F",
            "5F24",
            "5F34",
            "5F28",
            "9F12",
            "50",
            "56",
            "57",
            "9F20",
            "9F6B"
    };


    public static List<CapkEntity> getCapkList(){
        List<CapkEntity> capkEntityList = new ArrayList<>();
        Gson gson = new Gson();
        JsonParser parser = new JsonParser();

        JsonArray jsonArray;

        jsonArray = parser.parse(readAssetsTxt("emv_capk.json")).getAsJsonArray();

        if (jsonArray == null){
            return null;
        }

        for (JsonElement user : jsonArray) {
            CapkEntity userBean = gson.fromJson(user, CapkEntity.class);
            capkEntityList.add(userBean);
        }
        return capkEntityList;
    }




    public static List<AidEntity> getAidList(){
        List<AidEntity> aidEntityList = new ArrayList<>();
        Gson gson = new Gson();
        JsonParser parser = new JsonParser();
        JsonArray jsonArray;

        jsonArray = parser.parse(readAssetsTxt("emv_aid.json")).getAsJsonArray();

        if (jsonArray == null){
            return null;
        }

        for (JsonElement user : jsonArray) {
            AidEntity userBean = gson.fromJson(user, AidEntity.class);
            JsonObject jsonObject = user.getAsJsonObject();
            if(jsonObject != null){
                if(jsonObject.get("emvEntryMode") != null){
                    int emvEntryMode = jsonObject.get("emvEntryMode").getAsInt();
                    userBean.setAidEntryModeEnum(AidEntryModeEnum.values()[emvEntryMode]);
                    Log.d("nexgo", "emvEntryMode" + userBean.getAidEntryModeEnum());
                }
            }

            aidEntityList.add(userBean);
        }
        return aidEntityList;
    }


    @SuppressWarnings("ResultOfMethodCallIgnored")
    private static String readAssetsTxt(String fileName) {
        try {
            InputStream is = context.getAssets().open(fileName);

            int size = is.available();
            // Read the entire asset into a local byte buffer.
            byte[] buffer = new byte[size];
            //noinspection ResultOfMethodCallIgnored
            is.read(buffer);
            is.close();
            // Convert the buffer into a string.
            return new String(buffer, "utf-8");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;

    }


    @SuppressWarnings("ResultOfMethodCallIgnored")
    private static String readFile(String filePath) {
        try {
            InputStream is = new FileInputStream(filePath);
            int size = is.available();
            // Read the entire asset into a local byte buffer.
            byte[] buffer = new byte[size];
            //noinspection ResultOfMethodCallIgnored
            is.read(buffer);
            is.close();
            // Convert the buffer into a string.
            return new String(buffer, "utf-8");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }


    private static String getElementAttr(Element element, String attr) {
        if (element == null) return "";
        if (element.hasAttribute(attr)) {
            return element.getAttribute(attr);
        }
        return "";
    }

    private static Element[] getElementsByName(Element parent, String name) {
        ArrayList resList = new ArrayList();
        NodeList nl = parent.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            Node nd = nl.item(i);
            if (nd.getNodeName().equals(name)) {
                resList.add(nd);
            }
        }
        Element[] res = new Element[resList.size()];
        for (int i = 0; i < resList.size(); i++) {
            res[i] = (Element) resList.get(i);
        }
        return res;
    }



}