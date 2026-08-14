# APP窗口容器

这是一个 Android 12+ 车机方向的第一版 APP 窗口容器工程。

## 功能

- 横屏运行
- 中间 APP 显示区域
- 右下角“返回 / 关闭”
- 底部 APP 快捷键
- “＋ 添加 APP”
- 自动读取可启动的已安装 APP
- 搜索并添加 APP
- 长按快捷键可以删除
- 显示区域可以设置上下左右百分比
- 使用 `ActivityOptions.setLaunchBounds()` 尝试把外部 APP 启动到指定矩形

## 重要限制

普通 Android 第三方 APP 没有权限把另一个独立 APP 的 Activity 真正嵌入自己的 View 中。

本工程采用 Android 的 `launchBounds` 机制作为第一版窗口定位方案。
是否真正生效取决于车机 WindowManager 是否支持自由窗口 / 多窗口 / OEM 任务窗口。

如果目标车机是定制 Android 12，并且允许系统级 TaskView/ActivityView 或厂商窗口 API，
可以在这个工程基础上继续改成真正的“APP嵌入区域”。

## 编译

Android Studio 打开本目录即可。

建议：
- JDK 17
- Android Gradle Plugin 8.5.2
- Gradle 8.7+
- compileSdk 35
- minSdk 26

## GitHub Actions

可以直接用 Android Studio 生成 Gradle wrapper 后提交到 GitHub。
如果仓库已有 wrapper，可执行：

`./gradlew assembleDebug`

生成：

`app/build/outputs/apk/debug/app-debug.apk`
