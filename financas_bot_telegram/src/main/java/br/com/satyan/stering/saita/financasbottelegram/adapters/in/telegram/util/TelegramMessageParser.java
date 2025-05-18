package br.com.satyan.stering.saita.financasbottelegram.adapters.in.telegram.util;

import org.hibernate.annotations.Comment;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Component;

@Component
public class TelegramMessageParser {

    public String getFileId(JSONObject json) {
        JSONArray photos = json.getJSONObject("message").getJSONArray("photo");
        JSONObject largestPhoto = photos.getJSONObject(photos.length() - 1);
        String fileId = largestPhoto.getString("file_id");
        return fileId;
    }

}
