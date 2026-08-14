# APP窗口容器 修复版

版本：1.1

## 本次修复

- 修复 Kotlin stdlib / kotlin-stdlib-jdk7 / jdk8 重复类导致的构建失败
- Android Gradle Plugin 更新到 8.6.1
- compileSdk 35
- Java 17
- Gradle 8.7
- 加入 GitHub Actions `build.yml`
- 自动生成 `APP窗口容器.apk`

## GitHub 打包

把整个工程上传到 GitHub。

然后：

Actions
→ 构建 APP窗口容器 APK
→ Run workflow

完成后：

Actions
→ 对应运行记录
→ Artifacts
→ APP窗口容器

## 注意

`ActivityOptions.setLaunchBounds()` 能否真正限制第三方 APP 的窗口，
取决于车机 Android WindowManager 是否允许自由窗口/多窗口。

如果普通 Android 12 环境忽略这个区域，下一步需要针对车机的系统权限、
TaskView/ActivityView 或厂商窗口接口进行适配。
