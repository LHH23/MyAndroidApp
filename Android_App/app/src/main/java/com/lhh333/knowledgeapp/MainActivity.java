package com.lhh2333.knowledgeapp;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    // 后端地址——此处修改为你的后端IP或域名
    private static final String BASE_URL = "12383938jokz4.vicp.fun";
    private static final long COOLDOWN_MS = 10 * 60 * 1000; // 10分钟
    private static final long AUTO_REFRESH_INTERVAL = 30 * 1000;

    private TextView tvStatus, tvNotice, tvSmallDisclaimer;
    private EditText etContent, etAuthor;
    private Button btnPublish, btnRefresh, btnSponsor;
    private RecyclerView rvKnowledge;
    private KnowledgeAdapter adapter;
    private List<KnowledgeItem> itemList = new ArrayList<>();
    private Gson gson = new Gson();
    private OkHttpClient httpClient = new OkHttpClient();
    private Handler handler = new Handler(Looper.getMainLooper());
    private CacheManager cacheManager;
    private boolean isBackendConnected = false;

    private String deviceId;
    private SharedPreferences prefs;
    private Animation fadeIn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 过渡动画
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);

        initViews();
        prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        cacheManager = new CacheManager(this);
        deviceId = getDeviceId();
        fadeIn = AnimationUtils.loadAnimation(this, android.R.anim.fade_in);

        setupRecyclerView();
        loadCachedData();

        // 自动刷新1次
        fetchKnowledgeList();

        // 定时30秒刷新
        startAutoRefresh();

        // 发布按钮
        btnPublish.setOnClickListener(v -> attemptPublish());
        btnRefresh.setOnClickListener(v -> {
            Toast.makeText(this, "正在刷新…", Toast.LENGTH_SHORT).show();
            fetchKnowledgeList();
        });
        btnSponsor.setOnClickListener(v -> showSponsorDialog());
    }

    private void initViews() {
        tvStatus = findViewById(R.id.tv_status);
        tvNotice = findViewById(R.id.tv_notice);
        tvSmallDisclaimer = findViewById(R.id.tv_small_disclaimer);
        etContent = findViewById(R.id.et_content);
        etAuthor = findViewById(R.id.et_author);
        btnPublish = findViewById(R.id.btn_publish);
        btnRefresh = findViewById(R.id.btn_refresh);
        btnSponsor = findViewById(R.id.btn_sponsor);
        rvKnowledge = findViewById(R.id.rv_knowledge);

        // 固定文案
        String notice = "①软件由个人开发，当后端服务器关闭时，无法收到更新，且无法发布信息。" +
                "\n②制作信息：Made By LHH_2333" +
                "\n③联系方式：如果有任何建议，请发送至该邮箱：2552662159@qq.com";
        tvNotice.setText(notice);
        tvSmallDisclaimer.setText("免责声明：个人制作，后端随时会永久关闭，届时程序将停用");

        // 点击按钮动效(缩放到95%再恢复)
        View.OnTouchListener touchListener = (v, event) -> {
            if (event.getAction() == android.view.MotionEvent.ACTION_DOWN) {
                v.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100).start();
            } else if (event.getAction() == android.view.MotionEvent.ACTION_UP || event.getAction() == android.view.MotionEvent.ACTION_CANCEL) {
                v.animate().scaleX(1f).scaleY(1f).setDuration(100).start();
            }
            return false;
        };
        btnPublish.setOnTouchListener(touchListener);
        btnRefresh.setOnTouchListener(touchListener);
        btnSponsor.setOnTouchListener(touchListener);
    }

    private void setupRecyclerView() {
        rvKnowledge.setLayoutManager(new LinearLayoutManager(this));
        adapter = new KnowledgeAdapter(itemList);
        rvKnowledge.setAdapter(adapter);
    }

    private void loadCachedData() {
        List<KnowledgeItem> cached = cacheManager.loadCache();
        if (cached != null && !cached.isEmpty()) {
            itemList.clear();
            itemList.addAll(cached);
            adapter.notifyDataSetChanged();
            animateRecyclerView();
        }
    }

    private void fetchKnowledgeList() {
        if (!isNetworkAvailable()) {
            updateStatus(false);
            return;
        }

        Request request = new Request.Builder().url(BASE_URL + "/api/knowledge").build();
        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> updateStatus(false));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    runOnUiThread(() -> updateStatus(false));
                    return;
                }
                String json = response.body().string();
                Type listType = new TypeToken<ApiResponse>(){}.getType();
                ApiResponse apiResp = gson.fromJson(json, ApiResponse.class);
                if (apiResp != null && apiResp.entries != null) {
                    cacheManager.saveCache(apiResp.entries);
                    runOnUiThread(() -> {
                        itemList.clear();
                        itemList.addAll(apiResp.entries);
                        adapter.notifyDataSetChanged();
                        animateRecyclerView();
                        updateStatus(true);
                    });
                } else {
                    runOnUiThread(() -> updateStatus(false));
                }
            }
        });
    }

    private void attemptPublish() {
        String content = etContent.getText().toString().trim();
        String author = etAuthor.getText().toString().trim();

        if (TextUtils.isEmpty(content)) {
            Toast.makeText(this, "内容不能为空", Toast.LENGTH_SHORT).show();
            return;
        }
        if (content.length() > 100) {
            Toast.makeText(this, "内容不能超过100字", Toast.LENGTH_SHORT).show();
            return;
        }
        if (author.length() > 7) {
            Toast.makeText(this, "昵称不能超过7个字", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!isBackendConnected) {
            Toast.makeText(this, "后端已关闭，你的信息无法发送", Toast.LENGTH_SHORT).show();
            return;
        }

        // 本地冷却检查
        long lastPublish = prefs.getLong("last_publish_time", 0);
        if (System.currentTimeMillis() - lastPublish < COOLDOWN_MS) {
            Toast.makeText(this, "10分钟后再试试吧", Toast.LENGTH_SHORT).show();
            return;
        }

        // 准备发布
        PublishData data = new PublishData();
        data.content = content;
        data.author = author.isEmpty() ? "匿名" : author;
        data.device_id = deviceId;

        String json = gson.toJson(data);
        RequestBody body = RequestBody.create(MediaType.parse("application/json"), json);
        Request request = new Request.Builder().url(BASE_URL + "/api/knowledge").post(body).build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "发布失败，请检查网络", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.code() == 201) {
                    runOnUiThread(() -> {
                        Toast.makeText(MainActivity.this, "发布成功", Toast.LENGTH_SHORT).show();
                        etContent.setText("");
                        etAuthor.setText("");
                        prefs.edit().putLong("last_publish_time", System.currentTimeMillis()).apply();
                        fetchKnowledgeList();
                    });
                } else if (response.code() == 429) {
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "10分钟后再试试吧", Toast.LENGTH_SHORT).show());
                } else {
                    String errorBody = response.body() != null ? response.body().string() : "";
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "发布失败: " + errorBody, Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    private void updateStatus(boolean connected) {
        isBackendConnected = connected;
        tvStatus.setText(connected ? "后端连接成功，可发送" : "后端已关闭，你的信息无法发送");
        tvStatus.setTextColor(connected ? 0xFF4CAF50 : 0xFFFF5252);
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnected();
    }

    private void startAutoRefresh() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isNetworkAvailable()) {
                    fetchKnowledgeList();
                }
                handler.postDelayed(this, AUTO_REFRESH_INTERVAL);
            }
        }, AUTO_REFRESH_INTERVAL);
    }

    private void animateRecyclerView() {
        rvKnowledge.setAlpha(0f);
        rvKnowledge.animate().alpha(1f).setDuration(300).start();
        for (int i = 0; i < rvKnowledge.getChildCount(); i++) {
            View item = rvKnowledge.getChildAt(i);
            item.setAlpha(0f);
            item.setTranslationY(20f);
            item.animate().alpha(1f).translationY(0f).setDuration(300).setStartDelay(i * 50).start();
        }
    }

    private String getDeviceId() {
        String id = prefs.getString("device_id", null);
        if (id == null) {
            id = java.util.UUID.randomUUID().toString();
            prefs.edit().putString("device_id", id).apply();
        }
        return id;
    }

    private void showSponsorDialog() {
        Dialog dialog = new Dialog(this, R.style.SponsorDialogTheme);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_sponsor, null);
        dialog.setContentView(view);

        Button btnCandy = view.findViewById(R.id.btn_candy);
        Button btnCoffee = view.findViewById(R.id.btn_coffee);
        Button btnDinner = view.findViewById(R.id.btn_dinner);

        btnCandy.setOnClickListener(v -> showSponsorImage(1));
        btnCoffee.setOnClickListener(v -> showSponsorImage(6));
        btnDinner.setOnClickListener(v -> showSponsorImage(12));

        dialog.show();
        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout((int)(getResources().getDisplayMetrics().widthPixels * 0.8),
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
            window.setBackgroundDrawableResource(android.R.color.transparent);
        }
    }

    private void showSponsorImage(int amount) {
        Dialog imgDialog = new Dialog(this, R.style.SponsorDialogTheme);
        ImageView imageView = new ImageView(this);
        int resId;
        switch (amount) {
            case 1: resId = R.drawable.sponsor_1; break;
            case 6: resId = R.drawable.sponsor_6; break;
            case 12: resId = R.drawable.sponsor_12; break;
            default: resId = R.drawable.sponsor_1;
        }
        imageView.setImageResource(resId);
        imageView.setAdjustViewBounds(true);
        imgDialog.setContentView(imageView);
        imgDialog.show();
        Window window = imgDialog.getWindow();
        if (window != null) {
            window.setLayout((int)(getResources().getDisplayMetrics().widthPixels * 0.8),
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
            window.setBackgroundDrawableResource(android.R.color.transparent);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
    }

    // 数据类
    static class ApiResponse {
        List<KnowledgeItem> entries;
    }

    static class KnowledgeItem {
        int id;
        String content;
        String author;
        String created_at;
    }

    static class PublishData {
        String content;
        String author;
        String device_id;
    }
}