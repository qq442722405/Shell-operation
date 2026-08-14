package com.example.appwindowcontainer;

import android.app.Activity;
import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private static final String PREF = "container_settings";
    private static final String KEY_APPS = "apps";

    private LinearLayout shortcutBar;
    private TextView emptyText;
    private SharedPreferences prefs;
    private String currentPackage = null;

    // 显示区域相对于当前 Activity 内容区的比例。
    // 如果车机支持 freeform/launchBounds，外部 APP 会尝试使用这个矩形启动。
    private float topRatio = 0.08f;
    private float bottomRatio = 0.82f;
    private float leftRatio = 0.02f;
    private float rightRatio = 0.98f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences(PREF, MODE_PRIVATE);
        buildUi();
    }

    private int dp(float v) {
        return (int)(v * getResources().getDisplayMetrics().density + 0.5f);
    }

    private TextView text(String s, float size) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextColor(Color.WHITE);
        t.setTextSize(size);
        t.setGravity(Gravity.CENTER);
        return t;
    }

    private void buildUi() {
        Window w = getWindow();
        w.setStatusBarColor(Color.rgb(15,17,20));
        w.setNavigationBarColor(Color.rgb(15,17,20));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(15,17,20));

        // 顶部
        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setPadding(dp(18), dp(8), dp(18), dp(8));

        TextView title = text("APP 窗口容器", 18);
        title.setGravity(Gravity.CENTER_VERTICAL);
        top.addView(title, new LinearLayout.LayoutParams(0, dp(48), 1));

        Button area = button("显示区域");
        area.setOnClickListener(v -> showAreaDialog());
        top.addView(area, new LinearLayout.LayoutParams(dp(120), dp(46)));

        root.addView(top);

        // 中间显示区域
        LinearLayout center = new LinearLayout(this);
        center.setGravity(Gravity.CENTER);
        center.setOrientation(LinearLayout.VERTICAL);
        center.setPadding(dp(18), dp(8), dp(18), dp(8));

        LinearLayout frame = new LinearLayout(this);
        frame.setGravity(Gravity.CENTER);
        frame.setOrientation(LinearLayout.VERTICAL);
        frame.setBackgroundResource(com.example.appwindowcontainer.R.drawable.bg_card);

        emptyText = text("＋\n点击“＋ 添加 APP”\n启动应用", 22);
        emptyText.setOnClickListener(v -> showAppList());
        frame.addView(emptyText, new LinearLayout.LayoutParams(-1, -1));

        center.addView(frame, new LinearLayout.LayoutParams(-1, 0, 1));

        // 右下角控制按钮
        LinearLayout controls = new LinearLayout(this);
        controls.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        controls.setPadding(0, dp(6), dp(2), 0);

        Button back = button("↩ 返回");
        back.setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_MAIN);
            i.addCategory(Intent.CATEGORY_HOME);
            try {
                startActivity(i);
            } catch (Exception e) {
                Toast.makeText(this, "返回失败", Toast.LENGTH_SHORT).show();
            }
        });

        Button close = button("■ 关闭");
        close.setOnClickListener(v -> closeCurrent());

        controls.addView(back, new LinearLayout.LayoutParams(dp(100), dp(46)));
        controls.addView(close, new LinearLayout.LayoutParams(dp(100), dp(46)));
        center.addView(controls, new LinearLayout.LayoutParams(-1, dp(54)));

        root.addView(center, new LinearLayout.LayoutParams(-1, 0, 1));

        // 底部快捷栏
        shortcutBar = new LinearLayout(this);
        shortcutBar.setGravity(Gravity.CENTER_VERTICAL);
        shortcutBar.setPadding(dp(10), dp(8), dp(10), dp(8));
        root.addView(shortcutBar, new LinearLayout.LayoutParams(-1, dp(70)));

        setContentView(root);
        rebuildShortcuts();
    }

    private Button button(String s) {
        Button b = new Button(this);
        b.setText(s);
        b.setTextColor(Color.WHITE);
        b.setTextSize(14);
        b.setAllCaps(false);
        b.setBackgroundResource(com.example.appwindowcontainer.R.drawable.bg_button);
        b.setPadding(dp(6), 0, dp(6), 0);
        return b;
    }

    private Set<String> getSavedPackages() {
        return new HashSet<>(prefs.getStringSet(KEY_APPS, new HashSet<>()));
    }

    private void savePackages(Set<String> set) {
        prefs.edit().putStringSet(KEY_APPS, new HashSet<>(set)).apply();
    }

    private void rebuildShortcuts() {
        shortcutBar.removeAllViews();

        List<String> pkgs = new ArrayList<>(getSavedPackages());
        PackageManager pm = getPackageManager();

        for (String pkg : pkgs) {
            try {
                ApplicationInfo ai = pm.getApplicationInfo(pkg, 0);
                String label = pm.getApplicationLabel(ai).toString();

                Button b = button(label);
                b.setOnClickListener(v -> launchPackage(pkg));
                b.setOnLongClickListener(v -> {
                    removeShortcut(pkg);
                    return true;
                });

                shortcutBar.addView(b, new LinearLayout.LayoutParams(0, dp(52), 1));
            } catch (Exception ignored) {
            }
        }

        Button add = button("＋ 添加 APP");
        add.setOnClickListener(v -> showAppList());
        shortcutBar.addView(add, new LinearLayout.LayoutParams(0, dp(52), 1));
    }

    private void showAppList() {
        PackageManager pm = getPackageManager();
        List<ApplicationInfo> apps = pm.getInstalledApplications(PackageManager.GET_META_DATA);

        List<ApplicationInfo> launchable = new ArrayList<>();
        for (ApplicationInfo ai : apps) {
            Intent launch = pm.getLaunchIntentForPackage(ai.packageName);
            if (launch != null && !ai.packageName.equals(getPackageName())) {
                launchable.add(ai);
            }
        }

        Collections.sort(launchable, new Comparator<ApplicationInfo>() {
            @Override
            public int compare(ApplicationInfo a, ApplicationInfo b) {
                String aa = pm.getApplicationLabel(a).toString();
                String bb = pm.getApplicationLabel(b).toString();
                return aa.compareToIgnoreCase(bb);
            }
        });

        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(dp(12), dp(8), dp(12), dp(8));

        EditText search = new EditText(this);
        search.setHint("搜索应用");
        search.setSingleLine(true);
        list.addView(search, new LinearLayout.LayoutParams(-1, dp(52)));

        LinearLayout items = new LinearLayout(this);
        items.setOrientation(LinearLayout.VERTICAL);
        list.addView(items, new LinearLayout.LayoutParams(-1, dp(430)));

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("选择 APP")
                .setView(list)
                .setNegativeButton("取消", null)
                .create();

        Runnable refresh = () -> {
            items.removeAllViews();
            String q = search.getText().toString().trim().toLowerCase();

            int count = 0;
            for (ApplicationInfo ai : launchable) {
                String label = pm.getApplicationLabel(ai).toString();
                if (!q.isEmpty() && !label.toLowerCase().contains(q)) continue;

                Button b = button(label);
                b.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
                b.setOnClickListener(v -> {
                    addShortcut(ai.packageName);
                    dialog.dismiss();
                    launchPackage(ai.packageName);
                });
                items.addView(b, new LinearLayout.LayoutParams(-1, dp(48)));
                count++;
                if (count >= 12) break;
            }
        };

        search.addTextChangedListener(new SimpleTextWatcher() {
            @Override public void afterTextChanged(android.text.Editable s) { refresh.run(); }
        });

        refresh.run();
        dialog.show();
    }

    private void addShortcut(String pkg) {
        Set<String> set = getSavedPackages();
        set.add(pkg);
        savePackages(set);
        rebuildShortcuts();
    }

    private void removeShortcut(String pkg) {
        Set<String> set = getSavedPackages();
        set.remove(pkg);
        savePackages(set);
        rebuildShortcuts();
        Toast.makeText(this, "已移除快捷键", Toast.LENGTH_SHORT).show();
    }

    private void launchPackage(String pkg) {
        PackageManager pm = getPackageManager();
        Intent intent = pm.getLaunchIntentForPackage(pkg);

        if (intent == null) {
            Toast.makeText(this, "无法启动该 APP", Toast.LENGTH_SHORT).show();
            return;
        }

        currentPackage = pkg;
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        /*
         * 核心：
         * Android 允许通过 ActivityOptions.setLaunchBounds() 提供任务初始矩形。
         * 在支持 freeform / OEM 车机窗口管理的系统上，这可以让目标 APP
         * 在指定区域打开。
         *
         * 普通 Android 12 手机/部分车机可能忽略 launchBounds，
         * 这是系统 WindowManager 的限制，并非普通 APP 能强制绕过。
         */
        int sw = getResources().getDisplayMetrics().widthPixels;
        int sh = getResources().getDisplayMetrics().heightPixels;

        int l = (int)(sw * leftRatio);
        int t = (int)(sh * topRatio);
        int r = (int)(sw * rightRatio);
        int b = (int)(sh * bottomRatio);

        Rect bounds = new Rect(l, t, r, b);
        ActivityOptions options = ActivityOptions.makeBasic();
        options.setLaunchBounds(bounds);

        try {
            startActivity(intent, options.toBundle());
            emptyText.setText("正在运行：\n" + getAppName(pkg) +
                    "\n\n如果目标 APP 没有进入框内，请检查车机是否支持自由窗口/任务窗口。");
        } catch (Exception e) {
            try {
                startActivity(intent);
            } catch (Exception ex) {
                Toast.makeText(this, "启动失败：" + ex.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }

    private String getAppName(String pkg) {
        try {
            return getPackageManager()
                    .getApplicationLabel(getPackageManager().getApplicationInfo(pkg, 0))
                    .toString();
        } catch (Exception e) {
            return pkg;
        }
    }

    private void closeCurrent() {
        if (currentPackage == null) {
            emptyText.setText("＋\n点击“＋ 添加 APP”\n启动应用");
            return;
        }

        /*
         * 普通第三方 APP 无法被另一个普通 APP 强制 finish。
         * 这里通过返回任务主页，让容器重新成为前台。
         * 对拥有系统权限的车机版本，可进一步替换为 ActivityTaskManager
         * 的系统级关闭实现。
         */
        try {
            Intent home = new Intent(Intent.ACTION_MAIN);
            home.addCategory(Intent.CATEGORY_HOME);
            startActivity(home);

            new android.os.Handler().postDelayed(() -> {
                try {
                    Intent self = new Intent(this, MainActivity.class);
                    self.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                    startActivity(self);
                    emptyText.setText("＋\n点击“＋ 添加 APP”\n启动应用");
                } catch (Exception ignored) {}
            }, 150);
        } catch (Exception e) {
            emptyText.setText("＋\n点击“＋ 添加 APP”\n启动应用");
        }

        currentPackage = null;
    }

    private void showAreaDialog() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(20), dp(8), dp(20), dp(8));

        EditText top = numberInput(String.valueOf((int)(topRatio * 100)));
        EditText bottom = numberInput(String.valueOf((int)(bottomRatio * 100)));
        EditText left = numberInput(String.valueOf((int)(leftRatio * 100)));
        EditText right = numberInput(String.valueOf((int)(rightRatio * 100)));

        addField(box, "上边界 %", top);
        addField(box, "下边界 %", bottom);
        addField(box, "左边界 %", left);
        addField(box, "右边界 %", right);

        new AlertDialog.Builder(this)
                .setTitle("设置 APP 显示范围")
                .setView(box)
                .setPositiveButton("保存", (d, which) -> {
                    try {
                        topRatio = clamp(Float.parseFloat(top.getText().toString()) / 100f, 0, 0.95f);
                        bottomRatio = clamp(Float.parseFloat(bottom.getText().toString()) / 100f, topRatio + 0.03f, 1);
                        leftRatio = clamp(Float.parseFloat(left.getText().toString()) / 100f, 0, 0.95f);
                        rightRatio = clamp(Float.parseFloat(right.getText().toString()) / 100f, leftRatio + 0.03f, 1);
                    } catch (Exception e) {
                        Toast.makeText(this, "参数格式错误", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private float clamp(float v, float min, float max) {
        return Math.max(min, Math.min(max, v));
    }

    private EditText numberInput(String value) {
        EditText e = new EditText(this);
        e.setText(value);
        e.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        e.setSingleLine(true);
        return e;
    }

    private void addField(LinearLayout box, String label, EditText input) {
        TextView t = text(label, 14);
        t.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
        box.addView(t, new LinearLayout.LayoutParams(-1, dp(30)));
        box.addView(input, new LinearLayout.LayoutParams(-1, dp(48)));
    }

    public static abstract class SimpleTextWatcher implements android.text.TextWatcher {
        public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
        public void onTextChanged(CharSequence s, int st, int before, int count) {}
    }
}
