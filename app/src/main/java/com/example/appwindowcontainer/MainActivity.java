package com.example.appwindowcontainer;

import android.app.ActivityOptions;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private static final String PREF = "apps";
    private LinearLayout shortcuts;
    private TextView info;
    private SharedPreferences prefs;
    private String selectedPkg;

    // 6480 x 960 超长屏的三个测试区域
    private final Rect LEFT   = new Rect(0, 0, 2032, 960);
    private final Rect CENTER = new Rect(2032, 0, 4064, 960);
    private final Rect RIGHT  = new Rect(4064, 0, 6480, 960);

    private int dp(int n) {
        return (int)(n * getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences(PREF, MODE_PRIVATE);
        build();
    }

    private TextView tv(String s, int size) {
        TextView v = new TextView(this);
        v.setText(s);
        v.setTextColor(Color.WHITE);
        v.setTextSize(size);
        v.setGravity(Gravity.CENTER);
        return v;
    }

    private Button btn(String s) {
        Button b = new Button(this);
        b.setText(s);
        b.setTextColor(Color.WHITE);
        b.setTextSize(14);
        b.setAllCaps(false);
        b.setBackgroundResource(R.drawable.btn);
        return b;
    }

    private void build() {
        Window w = getWindow();
        w.setStatusBarColor(Color.rgb(16, 18, 22));
        w.setNavigationBarColor(Color.rgb(16, 18, 22));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(10), dp(8), dp(10), dp(8));
        root.setBackgroundColor(Color.rgb(16, 18, 22));

        LinearLayout head = new LinearLayout(this);
        head.setGravity(Gravity.CENTER_VERTICAL);

        TextView title = tv("APP窗口容器 · 三区域诊断", 18);
        title.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);

        head.addView(title, new LinearLayout.LayoutParams(0, dp(48), 1));

        Button add = btn("＋ 添加 APP");
        add.setOnClickListener(v -> chooseApp());

        head.addView(add, new LinearLayout.LayoutParams(dp(130), dp(46)));
        root.addView(head);

        LinearLayout test = new LinearLayout(this);
        test.setGravity(Gravity.CENTER_VERTICAL);

        Button left = btn("左区域\n2032×960");
        Button center = btn("中区域\n2032×960");
        Button right = btn("右区域\n2416×960");

        left.setOnClickListener(v -> launchSelected(LEFT, "左"));
        center.setOnClickListener(v -> launchSelected(CENTER, "中"));
        right.setOnClickListener(v -> launchSelected(RIGHT, "右"));

        test.addView(left, new LinearLayout.LayoutParams(0, dp(58), 1));
        test.addView(center, new LinearLayout.LayoutParams(0, dp(58), 1));
        test.addView(right, new LinearLayout.LayoutParams(0, dp(58), 1));

        root.addView(test);

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(14), dp(10), dp(14), dp(10));
        card.setBackgroundResource(R.drawable.bg);

        info = tv("", 14);
        info.setGravity(Gravity.LEFT | Gravity.TOP);
        card.addView(info, new LinearLayout.LayoutParams(-1, -1));

        root.addView(card, new LinearLayout.LayoutParams(-1, 0, 1));

        shortcuts = new LinearLayout(this);
        shortcuts.setGravity(Gravity.CENTER_VERTICAL);

        root.addView(shortcuts, new LinearLayout.LayoutParams(-1, dp(62)));

        setContentView(root);

        updateInfo("请选择一个 APP，然后测试左 / 中 / 右区域。");
        rebuildShortcuts();
    }

    private void updateInfo(String extra) {
        DisplayMetrics m = getResources().getDisplayMetrics();

        StringBuilder s = new StringBuilder();
        s.append("屏幕诊断\n");
        s.append("DisplayMetrics：")
                .append(m.widthPixels)
                .append(" × ")
                .append(m.heightPixels)
                .append("\n");
        s.append("density=")
                .append(m.density)
                .append("，densityDpi=")
                .append(m.densityDpi)
                .append("\n");
        s.append("容器 TaskId：")
                .append(getTaskId())
                .append("\n");
        s.append("容器 Package：")
                .append(getPackageName())
                .append("\n\n");
        s.append(extra);

        info.setText(s.toString());
    }

    private void rebuildShortcuts() {
        shortcuts.removeAllViews();

        Set<String> saved = prefs.getStringSet(PREF, new HashSet<>());
        for (String pkg : saved) {
            try {
                String name = getPackageManager()
                        .getApplicationLabel(
                                getPackageManager().getApplicationInfo(pkg, 0))
                        .toString();

                Button b = btn(name);

                b.setOnClickListener(v -> {
                    selectedPkg = pkg;
                    updateInfo(
                            "已选择：" + name +
                            "\n\n请点击左 / 中 / 右区域按钮测试。"
                    );
                });

                shortcuts.addView(
                        b,
                        new LinearLayout.LayoutParams(0, dp(50), 1)
                );

            } catch (Exception ignored) {
            }
        }

        Button plus = btn("＋");
        plus.setOnClickListener(v -> chooseApp());

        shortcuts.addView(
                plus,
                new LinearLayout.LayoutParams(0, dp(50), 1)
        );
    }

    private void chooseApp() {
        PackageManager pm = getPackageManager();

        List<ApplicationInfo> apps = new ArrayList<>();

        for (ApplicationInfo a :
                pm.getInstalledApplications(PackageManager.GET_META_DATA)) {

            if (!a.packageName.equals(getPackageName())
                    && pm.getLaunchIntentForPackage(a.packageName) != null) {

                apps.add(a);
            }
        }

        Collections.sort(
                apps,
                (a, b) -> pm.getApplicationLabel(a)
                        .toString()
                        .compareToIgnoreCase(
                                pm.getApplicationLabel(b).toString()
                        )
        );

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);

        EditText search = new EditText(this);
        search.setHint("搜索 APP");
        search.setSingleLine(true);

        box.addView(
                search,
                new LinearLayout.LayoutParams(-1, dp(52))
        );

        LinearLayout rows = new LinearLayout(this);
        rows.setOrientation(LinearLayout.VERTICAL);

        ScrollView scroll = new ScrollView(this);
        scroll.addView(rows);

        box.addView(
                scroll,
                new LinearLayout.LayoutParams(-1, dp(430))
        );

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("选择 APP")
                .setView(box)
                .setNegativeButton("取消", null)
                .create();

        Runnable refresh = () -> {
            rows.removeAllViews();

            String q = search.getText()
                    .toString()
                    .trim()
                    .toLowerCase();

            int count = 0;

            for (ApplicationInfo a : apps) {

                String name = pm.getApplicationLabel(a).toString();

                if (!q.isEmpty()
                        && !name.toLowerCase().contains(q)) {
                    continue;
                }

                Button b = btn(name);
                b.setGravity(
                        Gravity.LEFT |
                        Gravity.CENTER_VERTICAL
                );

                b.setOnClickListener(v -> {

                    selectedPkg = a.packageName;

                    Set<String> set =
                            new HashSet<>(
                                    prefs.getStringSet(
                                            PREF,
                                            new HashSet<>()
                                    )
                            );

                    set.add(a.packageName);

                    prefs.edit()
                            .putStringSet(PREF, set)
                            .apply();

                    rebuildShortcuts();
                    dialog.dismiss();

                    updateInfo(
                            "已选择：" + name +
                            "\n\n请点击左 / 中 / 右区域测试。"
                    );
                });

                rows.addView(
                        b,
                        new LinearLayout.LayoutParams(
                                -1,
                                dp(48)
                        )
                );

                if (++count >= 20) {
                    break;
                }
            }
        };

        search.addTextChangedListener(
                new android.text.TextWatcher() {

                    public void beforeTextChanged(
                            CharSequence s,
                            int start,
                            int count,
                            int after) {
                    }

                    public void onTextChanged(
                            CharSequence s,
                            int start,
                            int before,
                            int count) {
                        refresh.run();
                    }

                    public void afterTextChanged(
                            android.text.Editable e) {
                    }
                }
        );

        refresh.run();
        dialog.show();
    }

    private void launchSelected(Rect bounds, String region) {

        if (selectedPkg == null) {
            Toast.makeText(
                    this,
                    "请先点击“＋ 添加 APP”选择一个 APP",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        Intent intent =
                getPackageManager()
                        .getLaunchIntentForPackage(selectedPkg);

        if (intent == null) {
            Toast.makeText(
                    this,
                    "无法启动 APP",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        ActivityOptions options =
                ActivityOptions.makeBasic();

        options.setLaunchBounds(bounds);

        String msg =
                "测试区域：" + region +
                "\nBounds：" +
                bounds.left + "," +
                bounds.top + " - " +
                bounds.right + "," +
                bounds.bottom +
                "\n目标 Package：" +
                selectedPkg +
                "\n\n" +
                "已经向 Android WindowManager 提交 launchBounds。" +
                "\n请观察 APP 实际出现的位置。" +
                "\n\n如果 APP 仍然全屏或进入其它区域，" +
                "说明车机没有通过普通 Activity launchBounds " +
                "限制第三方 APP。";

        updateInfo(msg);

        try {
            startActivity(
                    intent,
                    options.toBundle()
            );
        } catch (Exception e) {

            updateInfo(
                    msg +
                    "\n\n启动异常：" +
                    e.getClass().getName() +
                    "\n" +
                    e.getMessage()
            );

            try {
                startActivity(intent);
            } catch (Exception ignored) {
            }
        }
    }
}
