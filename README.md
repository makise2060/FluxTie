<p align="center">
  <img src="docs/images/logo.png" width="160" alt="FluxTie Logo" />
</p>

<h1 align="center">FluxTie</h1>

<p align="center">
  一款 Material 3 风格的<strong>非官方第三方贴吧客户端</strong> — 基于 TiebaLite 深度重构，全新外观体系 · 真悬浮底栏 · MD3 动态配色 · 细腻动效
</p>

<p align="center">
  <a href="https://github.com/makise2060/FluxTie/releases"><img src="https://img.shields.io/github/v/release/makise2060/FluxTie?color=2563eb&label=Release&include_prereleases" alt="Release" /></a>
  <a href="https://developer.android.com/about/versions/16"><img src="https://img.shields.io/badge/Android-7.0%2B_(API_23%2B)-3ddc84?logo=android&logoColor=white" alt="Min API" /></a>
  <a href="https://kotlinlang.org"><img src="https://img.shields.io/badge/Kotlin-2.3-7f52ff?logo=kotlin&logoColor=white" alt="Kotlin" /></a>
  <a href="https://developer.android.com/jetpack/compose"><img src="https://img.shields.io/badge/Jetpack%20Compose-2026.05-4285f4?logo=jetpackcompose&logoColor=white" alt="Compose" /></a>
  <a href="https://developer.android.com/build/releases/gradle-plugin"><img src="https://img.shields.io/badge/AGP-9.4-006400?logo=gradle&logoColor=white" alt="AGP" /></a>
  <a href="https://github.com/makise2060/FluxTie/blob/main/LICENSE"><img src="https://img.shields.io/badge/License-GPL--3.0-red?logo=gnu&logoColor=white" alt="License" /></a>
</p>

---

> [!WARNING]
> **本软件及源码仅供学习交流使用，严禁用于商业用途。**

## 📸 预览

| 外观设置 | 配色风格 | 主题色彩 |
|:---:|:---:|:---:|
| <img src="docs/images/preview_appearance.png" width="240" /> | <img src="docs/images/preview_variants.png" width="240" /> | <img src="docs/images/preview_seed.png" width="240" /> |
| **悬浮底栏** | **动态页** | **应用日志** |
| <img src="docs/images/preview_nav.png" width="240" /> | <img src="docs/images/preview_explore.png" width="240" /> | <img src="docs/images/preview_logs.png" width="240" /> |

## ✨ 特性

- 🎨 **MD3 动态配色** — 种子色 × 9 种配色风格（柔和色调/鲜明/高保真…）实时生成整套配色，14 款精选预置色板 + 自定义取色，全部即点即生效
- 🌗 **深浅模式** — 浅色 / 深色 / 跟随系统三卡预览；深色样式支持深邃灰与 AMOLED 纯黑
- 🎈 **真悬浮胶囊底栏** — 内容 edge-to-edge 从半透明胶囊后透出，选中胶囊动画 + 触感反馈，Tab 切换交叉淡化过渡
- 🎬 **M3 动效体系** — Shared Axis 页面转场、液态 blob 启动屏、骨架屏加载、预测性返回手势预览
- 💗 **点赞微动效** — 心跳脉冲 + 光环扩散 + 粒子爆发 + 数字滚动，全站统一点赞体验
- 📜 **顶栏滚动收起** — 动态页内容上滑自动收起标题栏，双击快速回顶
- 🗂️ **分组卡片式设置** — 对齐 Android 13+ 系统设置观感的分组卡片体系，全局 Material 3 开关组件
- 📋 **应用日志** — 内置日志查看器，级别过滤 / 一键复制 / 分享，问题反馈更高效
- 💬 **完整贴吧能力** — 首页 / 动态（关注·推荐·热榜）/ 通知 / 我的，看贴回贴、一键签到等原版功能全部保留

## 🚀 相对原版的升级

本项目 Fork 自 [TiebaLite](https://github.com/zzc10086/TiebaLite)，在原版基础上完成了一次系统的现代化改造：

### 🎨 主题引擎重做
| 原版 | FluxTie |
|---|---|
| 固定主题名列表（清新蓝/少女粉…） | **MD3 种子配色**：种子色 × 配色风格动态生成整套色板 |
| Material You 动态取色（仅 Android 12+） | MaterialKolor 引擎，全版本可用 |
| 选色后部分效果需重启 | 全部即时生效，设置页所见即所得 |

### 🛠️ 工具链升级
| 组件 | 原版 | FluxTie |
|---|---|---|
| Android Gradle Plugin | 8.13.2 | **9.4.0**（内置 Kotlin 支持） |
| Gradle | 8.14.5 | **9.7.1** |
| compileSdk / targetSdk | 36 | **37** |
| Hilt | 2.58 | **2.60** |
| 配色引擎 | — | **MaterialKolor 5.0.1** |

### 🧹 细节打磨
- 启动屏重做：应用图标与系统启动屏同源同位无缝衔接，标语 + 液态 blob 动画，Roboto 字体
- 卡片化列表：动态流 / 吧页 / 帖子楼层统一 12dp 留白与发丝线描边，告别生硬分隔线
- 点赞微动效：心跳双脉冲 + 旋转摆动 + 光环粒子爆发，全站统一点赞组件
- 骨架屏体系：帖子页 / 楼中楼 / 吧页 / 个人页加载骨架，Shimmer 高光扫过
- 真悬浮底栏：列表呼吸位统一，末项可完整滚出胶囊（开关可回退传统贴底样式）
- 预测性返回：返回手势实时预览页面退场动画（Android 13+，外观设置可关闭）
- 权限弹窗统一新图标；状态栏字体颜色随明暗模式正确切换
- KSP 全链路构建，Java 17 目标，`FluxTie-v{版本}-{渠道}.apk` 规范命名

## 📥 下载

前往 [**Releases**](https://github.com/makise2060/FluxTie/releases) 下载最新 APK，或自行构建（见下）。

> [!NOTE]
> 本包名（`com.makise.fluxtie`）与原版 TiebaLite 不同，可作为独立应用安装，互不影响。

## 🛠️ 技术栈

| 层 | 技术 |
|---|---|
| 语言 | **Kotlin 2.3** · Java 17 |
| UI | **Jetpack Compose**（Material 组件 + 自研 M3 风格组件库） |
| 主题 | **MaterialKolor 5.0**（MD3 DynamicSchemeVariant 配色引擎） |
| 架构 | 手写 MVI（Intent → PartialChange → State → Event） |
| 依赖注入 | Hilt 2.60（全 KSP，无 kapt） |
| 网络 | Retrofit + OkHttp + Wire protobuf（贴吧 c/tiebac 接口） |
| 持久化 | Room 40（账号/历史/草稿）+ DataStore（偏好） |
| 导航 | compose-destinations 1.11（单 Activity + 底部抽屉目的地） |
| 图片 | Sketch 3.3（GIF/HEIF 支持）+ Glide（遗留通道） |
| 构建 | AGP 9.4 · Gradle 9.7.1 · KSP |

## 🏗️ 构建

```bash
# 克隆
git clone https://github.com/makise2060/FluxTie.git
cd FluxTie

# 需要 JDK 17+，Android SDK 37；local.properties 配置 sdk.dir
./gradlew assembleDebug
# 产物：app/build/outputs/apk/debug/FluxTie-v{版本}-debug.apk
```

## 🧬 Fork 谱系与致谢

```text
HuanCheng65/TiebaLite（原版）
  └── zzc10086/TiebaLite（fork）
        └── makise2060/FluxTie（本项目）
```

- [**HuanCheng65/TiebaLite**](https://github.com/HuanCheng65/TiebaLite) — 原始项目，感谢原作者及所有贡献者
- [**zzc10086/TiebaLite**](https://github.com/zzc10086/TiebaLite) — fork 自原版，本项目基于其 4.0-dev 分支
- [**Lingyan000/fluxdo**](https://github.com/Lingyan000/fluxdo) — 外观设置体系的灵感与设计参考
- [**appshubcc/Bettbox**](https://github.com/appshubcc/Bettbox) — 设置页与底栏交互的设计参考
- [Starry-OvO/aiotieba](https://github.com/Starry-OvO/aiotieba) · [n0099/tbclient.protobuf](https://github.com/n0099/tbclient.protobuf) — 贴吧协议研究

## 📄 License

[GPL-3.0](LICENSE) © 原作者及 FluxTie 贡献者

本程序为自由软件，在 GPL-3.0 协议下你可以自由使用、学习、修改与分发。
