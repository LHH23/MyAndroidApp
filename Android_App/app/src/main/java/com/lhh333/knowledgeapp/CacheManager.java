package com.lhh2333.knowledgeapp;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class CacheManager {
    private static final String PREFS_NAME = "knowledge_cache";
    private static final String KEY_DATA = "cached_value";
    private SharedPreferences prefs;
    private Gson gson;

    public CacheManager(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
    }

    public void saveCache(List<MainActivity.KnowledgeItem> items) {
        String json = gson.toJson(items);
        prefs.edit().putString(KEY_DATA, json).apply();
    }

    public List<MainActivity.KnowledgeItem> loadCache() {
        String json = prefs.getString(KEY_DATA, null);
        if (json == null) return new ArrayList<>();
        Type listType = new TypeToken<List<MainActivity.KnowledgeItem>>(){}.getType();
        return gson.fromJson(json, listType);
    }

    public void clearCache() {
        prefs.edit().remove(KEY_DATA).apply();
    }
}