package com.example.appwindowcontainer;

import android.app.ActivityOptions;
import android.content.*;
import android.content.pm.*;
import android.graphics.*;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.appcompat.app.*;
import org.json.*;
import java.util.*;

public class MainActivity extends AppCompatActivity {
    static final String PREF="app_window_manager", APPS="apps", PRESETS="presets";
    SharedPreferences sp; LinearLayout presetList, appList; TextView status;
    String selectedPkg, selectedName; int topSafe,bottomSafe;
    ArrayList<Preset> presets=new ArrayList<>(); ArrayList<AppItem> apps=new ArrayList<>();

    static class Preset {
        String name; int x,y,w,h,dpi;
        Preset(String n,int x,int y,int w,int h,int dpi){this.name=n;this.x=x;this.y=y;this.w=w;this.h=h;this.dpi=dpi;}
        Rect bounds(){return new Rect(x,y,x+w,y+h);}
        String summary(){return "X "+x+"  Y "+y+"   "+w+"×"+h+"   DPI "+dpi;}
    }
    static class AppItem {String pkg,name; AppItem(String p,String n){pkg=p;name=n;}}

    int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
    TextView tv(String s,float z){TextView t=new TextView(this);t.setText(s);t.setTextColor(Color.WHITE);t.setTextSize(z);t.setGravity(Gravity.CENTER_VERTICAL);return t;}
    Button bt(String s){Button b=new Button(this);b.setText(s);b.setTextColor(Color.WHITE);b.setTextSize(14);b.setAllCaps(false);b.setBackgroundResource(com.example.appwindowcontainer.R.drawable.btn);return b;}
    EditText field(String hint,String val){EditText e=new EditText(this);e.setHint(hint);e.setText(val);e.setTextColor(Color.WHITE);e.setHintTextColor(Color.GRAY);e.setSingleLine(true);e.setInputType(2);return e;}
    int num(EditText e,int d){try{return Integer.parseInt(e.getText().toString().trim());}catch(Exception x){return d;}}

    @Override public void onCreate(Bundle b){super.onCreate(b);sp=getSharedPreferences(PREF,0);load();ui();}

    void load(){
        topSafe=sp.getInt("topSafe",0);bottomSafe=sp.getInt("bottomSafe",0);
        try{JSONArray a=new JSONArray(sp.getString(APPS,"[]"));PackageManager pm=getPackageManager();
            for(int i=0;i<a.length();i++){String p=a.getString(i);try{ApplicationInfo x=pm.getApplicationInfo(p,0);apps.add(new AppItem(p,pm.getApplicationLabel(x).toString()));}catch(Exception ignored){}}}catch(Exception ignored){}
        try{JSONArray a=new JSONArray(sp.getString(PRESETS,"[]"));for(int i=0;i<a.length();i++){JSONObject o=a.getJSONObject(i);presets.add(new Preset(o.getString("name"),o.getInt("x"),o.getInt("y"),o.getInt("w"),o.getInt("h"),o.optInt("dpi",160)));}}catch(Exception ignored){}
        if(presets.isEmpty()){presets.add(new Preset("默认",0,topSafe,2032,Math.max(1,960-topSafe-bottomSafe),160));savePresets();}
    }
    void saveApps(){JSONArray a=new JSONArray();for(AppItem x:apps)a.put(x.pkg);sp.edit().putString(APPS,a.toString()).apply();}
    void savePresets(){JSONArray a=new JSONArray();try{for(Preset p:presets){JSONObject o=new JSONObject();o.put("name",p.name);o.put("x",p.x);o.put("y",p.y);o.put("w",p.w);o.put("h",p.h);o.put("dpi",p.dpi);a.put(o);}}catch(Exception ignored){}sp.edit().putString(PRESETS,a.toString()).apply();}

    void ui(){
        getWindow().setStatusBarColor(Color.rgb(16,18,22));getWindow().setNavigationBarColor(Color.rgb(16,18,22));
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(10),dp(8),dp(10),dp(8));root.setBackgroundColor(Color.rgb(16,18,22));
        LinearLayout head=new LinearLayout(this);head.setGravity(Gravity.CENTER_VERTICAL);
        TextView title=tv("APP窗口容器 · 自定义窗口预设",19);title.setTypeface(null,1);head.addView(title,new LinearLayout.LayoutParams(0,dp(48),1));
        Button set=bt("⚙ 设置");set.setOnClickListener(v->settings());head.addView(set,new LinearLayout.LayoutParams(dp(78),dp(46)));
        Button add=bt("＋ 添加 APP");add.setOnClickListener(v->chooseApp());head.addView(add,new LinearLayout.LayoutParams(dp(112),dp(46)));root.addView(head);
        LinearLayout safe=new LinearLayout(this);safe.setPadding(dp(4),dp(4),dp(4),dp(4));safe.setBackgroundResource(R.drawable.bg);
        TextView st=tv("上方空白 "+topSafe+" px   ·   下方空白 "+bottomSafe+" px",13);safe.addView(st,new LinearLayout.LayoutParams(0,dp(42),1));
        Button se=bt("修改");se.setOnClickListener(v->settings());safe.addView(se,new LinearLayout.LayoutParams(dp(70),dp(42)));root.addView(safe);
        TextView tip=tv("先点击 APP，再点击上方预设即可按该预设启动。预设长按可编辑/删除。",13);tip.setPadding(dp(8),dp(8),dp(8),dp(8));root.addView(tip);
        TextView pt=tv("窗口预设",16);pt.setTypeface(null,1);root.addView(pt,new LinearLayout.LayoutParams(-1,dp(34)));
        ScrollView ps=new ScrollView(this);presetList=new LinearLayout(this);presetList.setOrientation(LinearLayout.HORIZONTAL);ps.addView(presetList);root.addView(ps,new LinearLayout.LayoutParams(-1,dp(82)));
        TextView at=tv("已添加 APP",16);at.setTypeface(null,1);root.addView(at,new LinearLayout.LayoutParams(-1,dp(34)));
        ScrollView as=new ScrollView(this);appList=new LinearLayout(this);appList.setOrientation(LinearLayout.VERTICAL);as.addView(appList);root.addView(as,new LinearLayout.LayoutParams(-1,0,1));
        status=tv("",13);status.setPadding(dp(8),dp(5),dp(8),dp(5));status.setBackgroundResource(R.drawable.bg);root.addView(status,new LinearLayout.LayoutParams(-1,dp(72)));
        setContentView(root);refresh();
    }

    void refresh(){refreshPresets();refreshApps();status.setText(selectedPkg==null?"未选择 APP：先选择 APP，再点击窗口预设。":"当前 APP："+selectedName+"\n请点击窗口预设启动。");}
    void refreshPresets(){
        presetList.removeAllViews();
        for(int i=0;i<presets.size();i++){final int ix=i;Preset p=presets.get(i);Button b=bt(p.name+"\n"+p.summary());
            b.setOnClickListener(v->{if(selectedPkg==null){Toast.makeText(this,"请先选择 APP",0).show();return;}launch(p);});
            b.setOnLongClickListener(v->{presetMenu(ix);return true;});presetList.addView(b,new LinearLayout.LayoutParams(dp(270),dp(68)));}
        Button add=bt("＋\n新建预设");add.setOnClickListener(v->editPreset(-1));presetList.addView(add,new LinearLayout.LayoutParams(dp(120),dp(68)));
    }
    void refreshApps(){
        appList.removeAllViews();
        if(apps.isEmpty()){TextView e=tv("还没有添加 APP\n点击右上角“＋ 添加 APP”",15);e.setGravity(17);appList.addView(e,new LinearLayout.LayoutParams(-1,dp(100)));return;}
        for(AppItem a:apps){Button b=bt((a.pkg.equals(selectedPkg)?"✓ ":"")+a.name+"\n"+a.pkg);b.setGravity(Gravity.LEFT|Gravity.CENTER_VERTICAL);
            b.setOnClickListener(v->{selectedPkg=a.pkg;selectedName=a.name;refreshApps();status.setText("已选择："+a.name+"\n现在点击窗口预设启动。");});
            b.setOnLongClickListener(v->{new AlertDialog.Builder(this).setTitle(a.name).setMessage("长按删除此 APP 快捷方式。").setNegativeButton("取消",null).setPositiveButton("删除",(d,w)->{apps.remove(a);if(a.pkg.equals(selectedPkg)){selectedPkg=null;selectedName=null;}saveApps();refresh();}).show();return true;});
            appList.addView(b,new LinearLayout.LayoutParams(-1,dp(62)));}
    }

    void chooseApp(){
        PackageManager pm=getPackageManager();List<ApplicationInfo> list=new ArrayList<>();
        for(ApplicationInfo a:pm.getInstalledApplications(128))if(!a.packageName.equals(getPackageName())&&pm.getLaunchIntentForPackage(a.packageName)!=null)list.add(a);
        Collections.sort(list,(a,b)->pm.getApplicationLabel(a).toString().compareToIgnoreCase(pm.getApplicationLabel(b).toString()));
        LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);EditText search=field("搜索 APP","");box.addView(search,new LinearLayout.LayoutParams(-1,dp(52)));
        LinearLayout rows=new LinearLayout(this);rows.setOrientation(LinearLayout.VERTICAL);ScrollView sv=new ScrollView(this);sv.addView(rows);box.addView(sv,new LinearLayout.LayoutParams(-1,dp(430)));
        AlertDialog d=new AlertDialog.Builder(this).setTitle("添加 APP").setView(box).setNegativeButton("关闭",null).create();
        Runnable refresh=()->{rows.removeAllViews();String q=search.getText().toString().toLowerCase();int n=0;for(ApplicationInfo a:list){String name=pm.getApplicationLabel(a).toString();if(!q.isEmpty()&&!name.toLowerCase().contains(q))continue;Button b=bt(name);b.setGravity(Gravity.LEFT|Gravity.CENTER_VERTICAL);b.setOnClickListener(v->{boolean exists=false;for(AppItem x:apps)if(x.pkg.equals(a.packageName))exists=true;if(!exists)apps.add(new AppItem(a.packageName,name));saveApps();selectedPkg=a.packageName;selectedName=name;refresh();d.dismiss();});rows.addView(b,new LinearLayout.LayoutParams(-1,dp(48)));if(++n>=30)break;}};
        search.addTextChangedListener(new android.text.TextWatcher(){public void beforeTextChanged(CharSequence s,int a,int b,int c){}public void onTextChanged(CharSequence s,int a,int b,int c){refresh.run();}public void afterTextChanged(android.text.Editable e){}});refresh.run();d.show();
    }

    void settings(){
        LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);EditText top=field("顶部空白 px",String.valueOf(topSafe)),bot=field("底部空白 px",String.valueOf(bottomSafe));box.addView(top);box.addView(bot);
        new AlertDialog.Builder(this).setTitle("车机上下安全区域").setMessage("用于避开顶部状态栏和底部系统按钮。").setView(box).setNegativeButton("取消",null).setPositiveButton("保存",(d,w)->{topSafe=Math.max(0,num(top,0));bottomSafe=Math.max(0,num(bot,0));sp.edit().putInt("topSafe",topSafe).putInt("bottomSafe",bottomSafe).apply();refresh();}).show();
    }

    void presetMenu(int i){new AlertDialog.Builder(this).setTitle(presets.get(i).name).setItems(new String[]{"编辑预设","删除预设"},(d,w)->{if(w==0)editPreset(i);else{if(presets.size()==1){Toast.makeText(this,"至少保留一个预设",0).show();return;}presets.remove(i);savePresets();refresh();}}).show();}
    void editPreset(int i){
        Preset old=i>=0?presets.get(i):new Preset("",0,topSafe,2032,Math.max(1,960-topSafe-bottomSafe),160);
        LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);
        EditText name=field("预设名称",old.name),x=field("X 左上",String.valueOf(old.x)),y=field("Y 左上",String.valueOf(old.y)),ww=field("窗口宽度",String.valueOf(old.w)),hh=field("窗口高度",String.valueOf(old.h)),dpi=field("APP DPI",String.valueOf(old.dpi));
        box.addView(name);box.addView(x);box.addView(y);box.addView(ww);box.addView(hh);box.addView(dpi);
        new AlertDialog.Builder(this).setTitle(i>=0?"编辑窗口预设":"新建窗口预设").setView(box).setNegativeButton("取消",null).setPositiveButton("保存",(d,w)->{
            String n=name.getText().toString().trim();if(n.isEmpty()){Toast.makeText(this,"请输入预设名称",0).show();return;}
            Preset p=new Preset(n,num(x,0),num(y,topSafe),Math.max(1,num(ww,2032)),Math.max(1,num(hh,860)),Math.max(1,num(dpi,160)));
            if(i>=0)presets.set(i,p);else presets.add(p);savePresets();refresh();
        }).show();
    }

    void launch(Preset p){
        Intent in=getPackageManager().getLaunchIntentForPackage(selectedPkg);if(in==null){Toast.makeText(this,"无法启动 APP",0).show();return;}
        ActivityOptions o=ActivityOptions.makeBasic();o.setLaunchBounds(p.bounds());
        status.setText("启动："+selectedName+"\n预设："+p.name+"\nX="+p.x+" Y="+p.y+"\n"+p.w+"×"+p.h+"   DPI="+p.dpi);
        try{startActivity(in,o.toBundle());}catch(Exception e){status.setText(status.getText()+"\n启动异常："+e.getMessage());try{startActivity(in);}catch(Exception ignored){}}
    }
}
