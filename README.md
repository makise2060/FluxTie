<p align="center">
  <img src="docs/images/logo.png?v=1.1.0" width="128" alt="FluxTie Logo" />
</p>

<h1 align="center">FluxTie</h1>

<p align="center">
  <strong>为兴趣而生</strong> —— 基于 TiebaLite 深度重构的非官方第三方贴吧客户端
</p>

<p align="center">
  <a href="https://github.com/makise2060/FluxTie/releases"><img src="https://img.shields.io/github/v/release/makise2060/FluxTie?color=1e88e5&label=Release" alt="Release" /></a>
  <a href="https://developer.android.com/studio/releases/platforms#6.0"><img src="https://img.shields.io/badge/Android-6.0%2B%20(API%2023%2B)-3ddc84?logo=android&logoColor=white" alt="Android" /></a>
  <a href="https://kotlinlang.org"><img src="https://img.shields.io/badge/Kotlin-2.3-7f52ff?logo=kotlin&logoColor=white" alt="Kotlin" /></a>
  <a href="https://developer.android.com/jetpack/compose"><img src="https://img.shields.io/badge/Jetpack%20Compose-MaterialKolor-4285f4?logo=jetpackcompose&logoColor=white" alt="Jetpack Compose" /></a>
  <a href="https://github.com/makise2060/FluxTie/blob/main/LICENSE"><img src="https://img.shields.io/badge/License-GPL--3.0-red?logo=gnu&logoColor=white" alt="License" /></a>
</p>

> [!WARNING]
> 本项目仅供学习交流，不用于商业用途；它不是百度或贴吧的官方客户端，相关商标与内容权利归其权利人所有。

<p align="center">
  <img src="docs/images/showcase_hero.png?v=1.1.0" alt="FluxTie 主视觉" />
</p>

## 项目简介

FluxTie fork 自 [HuanCheng65/TiebaLite](https://github.com/HuanCheng65/TiebaLite)，并以 [zzc10086/TiebaLite](https://github.com/zzc10086/TiebaLite) 的 4.0-dev 分支为基础继续深度重构。项目保留贴吧客户端的核心能力，同时以 Jetpack Compose、MaterialKolor 与现代 Android 工具链重新打磨浏览、主题、动效和更新体验。

- 无自建内容后端，帖子、图片等贴吧业务数据直连官方接口。
- 应用更新检查使用 GitHub Releases，不后台静默下载。
- 应用 ID 为 <code>com.makise.fluxtie</code>，可与官方贴吧及原版 TiebaLite 共存。
- 包名不同，因此不会接管原版数据；从原版迁移需重新登录并按需调整设置。

## 核心特性

<p align="center">
  <img src="docs/images/feature_showcase.png?v=1.1.0" alt="FluxTie 核心特性" />
</p>

- **双阶段流体启动**：系统启动屏与 Compose 启动覆盖层衔接，双水滴、贴字与星石构成完整启动动效。
- **官方贴字新标**：参考贴吧 12.0 MEUX 字形重构“贴”字图标，并同步替换应用内主要图标资源。
- **MaterialKolor 种子配色**：14 个预置种子色、自定义取色、9 种配色风格、浅色/深色/AMOLED 纯黑与 Android 12+ 系统取色。
- **真悬浮底栏**：毛玻璃 Dock 悬浮于内容之上，支持按住拖拽、跟手预览、就近切换与触感反馈；低版本系统自动降级。
- **个人表情包**：收藏与 DIY 表情支持云端同步，可在编辑器、帖子和评论中直接渲染。
- **应用内更新**：检查、更新日志、下载进度、取消、系统安装与“忽略当前版本”一站式完成。
- **现代图片链路**：以 Sketch 为主要图片加载框架，大图预览使用 ZoomImage，并保留缩略图占位与加载状态反馈。

## 界面预览

<div align="center">

| 启动屏 | 动态流 | 帖子页 |
| :---: | :---: | :---: |
| <img src="docs/images/preview_splash.png?v=1.1.0" width="240" alt="启动屏" /> | <img src="docs/images/preview_explore.png?v=1.1.0" width="240" alt="动态流" /> | <img src="docs/images/preview_thread.png?v=1.1.0" width="240" alt="帖子页" /> |
| **吧页信息流** | **外观设置** | **设置主页** |
| <img src="docs/images/preview_forum.png?v=1.1.0" width="240" alt="吧页信息流" /> | <img src="docs/images/preview_appearance.png?v=1.1.0" width="240" alt="外观设置" /> | <img src="docs/images/preview_settings.png?v=1.1.0" width="240" alt="设置主页" /> |

</div>

<p align="center">
  <img src="docs/images/bento_grid_showcase.png?v=1.1.0" alt="FluxTie 设计看板" />
</p>

## 相对原版的升级

| 维度 | 原版 TiebaLite | FluxTie |
| --- | --- | --- |
| 界面方向 | 以既有 View/XML 界面为主 | 新 UI 以 Jetpack Compose 为主，逐步统一页面与组件 |
| 主题系统 | 固定主题列表，部分调整不够即时 | MaterialKolor 种子色引擎，14 个预置色、9 种风格与实时预览 |
| 深色体验 | 深色模式能力较基础 | 浅色、深色、深邃灰、AMOLED 纯黑及 Android 12+ 系统取色 |
| 底部导航 | 贴底导航栏 | 可开关的悬浮毛玻璃 Dock，支持拖拽切换与动效反馈 |
| 启动体验 | 静态图标与传统淡入 | 系统启动屏加 Compose 覆盖层的双阶段流体动效 |
| 图片体验 | 传统图片加载与查看 | Sketch 加载、缩略图占位、crossfade 与 ZoomImage 大图预览 |
| 表情能力 | 基础表情与收藏 | 收藏/DIY 云端同步、编辑器渲染、长按预览与帖子评论展示 |
| 应用更新 | 以手动下载安装为主 | GitHub Releases 检查、下载、安装与版本忽略 |
| 工具链 | AGP 8.13、Gradle 8.14、compileSdk 36 | AGP 9.4、Gradle 9.7、Kotlin 2.3、compileSdk/targetSdk 37 |
| 应用共存 | <code>com.huanchengfly.tieba.post</code> | <code>com.makise.fluxtie</code>，可与原版和官方客户端共存 |

签到、多账号、发帖回帖、收藏、屏蔽、浏览记录、内置日志查看器等上游能力继续保留，并在新界面中逐步统一交互与视觉。

## 下载与安装

1. 在 [Releases](https://github.com/makise2060/FluxTie/releases) 下载最新 APK。
2. 最低支持 **Android 6.0（API 23）**。
3. v1.0.3 起发布签名已统一，后续版本可正常覆盖升级；**v1.0.2 及更早版本升级前需先卸载**。
4. FluxTie 与官方贴吧、原版 TiebaLite 包名不同，可直接共存安装。

CI 会基于 <code>v*</code> tag 自动构建 APK 并上传 Release；Release 正文与截图以发布页为准。

## 构建

要求 **JDK 17+**、Android SDK 与可执行 Gradle Wrapper 的环境。

~~~bash
git clone https://github.com/makise2060/FluxTie.git
cd FluxTie
./gradlew assembleDebug
~~~

Debug APK 输出位置：

~~~text
app/build/outputs/apk/debug/FluxTie-v{version}.apk
~~~

本地 SDK 路径通过 <code>local.properties</code> 的 <code>sdk.dir</code> 配置；Release 构建会启用 R8 与资源收缩。

## 技术栈

- Kotlin 2.3
- Jetpack Compose（Material 2 组件基座 + MaterialKolor 动态配色）
- Hilt、KSP、手写 MVI
- Retrofit、OkHttp、Wire
- Room、DataStore、kotlinx.serialization
- Sketch、ZoomImage
- AGP 9.4、Gradle 9.7、compileSdk/targetSdk 37

## 开源边界与安全说明

- 本仓库不提供贴吧内容镜像，也不收集帖子数据；登录与业务请求由客户端直连相应服务。
- 应用更新、发布说明与安装包来自 GitHub Releases。
- 请勿将本项目用于冒充官方客户端、抓取滥用、骚扰他人或其他违反服务条款与法律法规的用途。
- 源码按 GPL-3.0 发布；使用、修改与分发时仍需遵守上游协议、第三方资源许可及本项目的学习交流声明。

## Linux.do 致谢

<p align="center">
  <img src="docs/images/linuxdo_thanks.png?v=1.1.0" alt="感谢 Linux.do 社区" />
</p>

FluxTie 的开发与迭代离不开 [Linux.do](https://linux.do/) 社区。感谢分享搞机经验、讨论设计方向、试用开发版并反馈问题的每一位朋友；你们的真诚、友善、团结与专业，让这个项目得以持续完善。

## 致谢

- [HuanCheng65/TiebaLite](https://github.com/HuanCheng65/TiebaLite)：原作。
- [zzc10086/TiebaLite](https://github.com/zzc10086/TiebaLite)：本项目所基于的 4.0-dev 分支。
- [Lingyan000/fluxdo](https://github.com/Lingyan000/fluxdo)：外观与界面设计参考。
- [aiotieba](https://github.com/Starry-OvO/aiotieba) 与 [tbclient.protobuf](https://github.com/n0099/tbclient.protobuf)：贴吧协议研究参考。

## Star History

<p align="center">
  <a href="https://star-history.com/#makise2060/FluxTie&Date">
    <picture>
      <source media="(prefers-color-scheme: dark)" srcset="https://api.star-history.com/svg?repos=makise2060/FluxTie&type=Date&theme=dark" />
      <source media="(prefers-color-scheme: light)" srcset="https://api.star-history.com/svg?repos=makise2060/FluxTie&type=Date" />
      <img alt="Star History Chart" src="https://api.star-history.com/svg?repos=makise2060/FluxTie&type=Date" />
    </picture>
  </a>
</p>

## License

[GPL-3.0](LICENSE) © 原作者及 FluxTie 贡献者。
