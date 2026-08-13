<div align="center">

<img src="app/src/main/res/drawable/ic_banner.webp" style="border-radius: 24px; margin-top: 32px;"/>

# SBV

[![Android Sdk Require](https://img.shields.io/badge/Android-7.0%2B-informational?logo=android)](https://apilevels.com/)
[![GitHub](https://img.shields.io/github/license/sunls24/bv)](https://github.com/sunls24/bv)

第三方哔哩哔哩 Android TV 客户端，使用 Jetpack Compose 开发，支持 Android 7.0 及以上版本。

**SBV 不支持在中国大陆地区内使用，如有相关使用需求请使用 [云视听小电视](https://app.bilibili.com)。**

</div>

## 功能

- 面向电视遥控器设计的哔哩哔哩内容浏览和播放体验。
- 支持推荐、热门、动态、分区、搜索、收藏、历史、稍后再看和追番。
- 支持普通视频、分 P、合集、番剧和相关视频播放。
- 支持弹幕、字幕、画质、编码、音轨和播放行为设置。
- 支持扫码登录，业务数据优先使用 Web API。

## 项目结构

- `app`：TV 界面、业务状态和播放器控制。
- `bili-api`：Web API、认证、业务仓库和播放数据转换。
- `bili-api-grpc`：扫码登录与播放兜底所需的协议定义。
- `akdanmaku`：`libs/AkDanmaku` 的 Android 封装。

## 开发准备

```shell
git submodule update --init --recursive
mise install
```

JDK 版本由 `mise.toml` 固定，构建和测试使用项目自带的 `./gradlew`。

## License

[MIT](LICENSE) © sunls24
