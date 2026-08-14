# APP窗口容器 三区域诊断版

针对 6480×960 超长车机屏幕。

本版本预设：

左区域：
X=0
Y=0
W=2032
H=960

中区域：
X=2032
Y=0
W=2032
H=960

右区域：
X=4064
Y=0
W=2416
H=960

注意：三个区域中的前两个按照你提供的 2032×960 设置，右侧使用剩余的 2416 像素。

## 测试

1. 安装 APK。
2. 点击“＋ 添加 APP”。
3. 选择一个普通可启动 APP。
4. 点击快捷栏里的 APP，确认已经选中。
5. 点击“左区域 / 中区域 / 右区域”。
6. 观察目标 APP 实际位置。
7. 把实际结果告诉我。

## 目的

本版本重点测试车机 WindowManager 是否真正执行：

ActivityOptions.setLaunchBounds()

如果三个区域按钮都不能把目标 APP 限制在指定矩形内，就不能继续依赖普通 launchBounds。

下一步需要根据实际车机情况研究：

- TaskView
- ActivityView
- 多 Display
- OEM WindowManager
- 系统签名权限
- 车机厂商自己的多区域 API

本工程已经包含 GitHub Actions：

.github/workflows/build.yml

GitHub：

Actions → 构建 APP窗口容器 三区域诊断版 → Run workflow

完成后下载 Artifact：

APP窗口容器_三区域诊断版.apk
