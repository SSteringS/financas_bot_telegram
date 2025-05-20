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
    return largestPhoto.getString("file_id");
  }

  public JSONArray getPhotoArray(JSONObject json) {
    return json.getJSONObject("message").optJSONArray("photo");
  }

  public String getMediaGroupId(JSONObject json) {
    return json.getJSONObject("message").optString("media_group_id", null);
  }

  public String getMessage(JSONObject json) {
    JSONObject messageObj = json.optJSONObject("message");
    return (messageObj != null && messageObj.has("caption")) ? messageObj.getString("caption")
        : null;
  }

  public static String extractCategoria(String message) {
    return extractField(message, "categoria");
  }

  public static String extractOrigem(String message) {
    return extractField(message, "origem");
  }

  private static String extractField(String message, String field) {
    if (message == null) {
      return null;
    }
    var matcher = java.util.regex.Pattern
        .compile(field + "\\s*:\\s*([^\\n\\r]+)", java.util.regex.Pattern.CASE_INSENSITIVE)
        .matcher(message);
    return matcher.find() ? matcher.group(1).trim() : null;
  }

}
