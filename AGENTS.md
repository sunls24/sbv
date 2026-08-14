# AGENTS.md

- `libs/AkDanmaku` 是 Git 子模块；缺失时运行 `git submodule update --init --recursive`。
- JDK 版本由 `mise.toml` 固定；环境缺失时运行 `mise install`。构建和测试使用项目自带的 `./gradlew`。
- 依赖版本统一维护在 `gradle/*.versions.toml`，不要在模块脚本中直接散落版本号。
- 当前按单账号 Android TV 客户端的最小实现设计，优先直接、清晰的逻辑；未明确要求时不做迁移、旧数据或旧版本兼容、回滚及多版本共存，也不为低概率场景引入复杂抽象。
- UI 改动需检查遥控器焦点、返回键和电视端性能；播放器、焦点或设备兼容性改动需说明需要真机验证。
- 业务数据优先使用 Web API；App token 仅用于扫码登录、刷新和播放器最小 gRPC 兜底，不扩展 App 业务接口，除非任务明确要求。
- 列表优先服务端按需分页，不为排序或计数全量拉取数据。
- 持续参考 BewlyCat 的 Bilibili API 设计，仅对照端点、参数、响应、鉴权、签名和错误码；不照搬其浏览器扩展通信、前端状态或维护架构。
- 默认不执行 APK 打包、安装或发布构建；日常修改只做必要的轻量验证，非必要不运行完整 `lintRelease`。用户明确要求时，才执行指定的构建或设备操作。
- TCL 电视更新 APK 直接执行 `mise exec -- ./scripts/update-tv.sh`。
