package com.goodbird.mindofthecolony.utils;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import java.util.Map;

public class Utils {
    public static String replacePlaceholders(String input, Map<String, String> replacements) {
        for (Map.Entry<String, String> entry : replacements.entrySet()) {
            String placeholder = "\\{\\{" + entry.getKey() + "}}";
            input = input.replaceAll(placeholder, entry.getValue());
        }
        return input;
    }

    public static String getStringJsonSafely(JsonObject input, String fieldName) {
        return (input.has(fieldName) && !input.get(fieldName).isJsonNull()) ?
                input.get(fieldName).getAsString() :
                null;
    }

    public static JsonObject parseCleanedJson(String content) throws JsonSyntaxException {
        content = content.replaceAll("^```json\\s*", "").replaceAll("\\s*```$", "").trim();
        return JsonParser.parseString(content).getAsJsonObject();
    }

    public static JsonObject deepCopy(JsonObject original) {
        return JsonParser.parseString(original.toString()).getAsJsonObject();
    }
}
