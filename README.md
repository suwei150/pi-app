# Pocket Pi

> 一个 APK 装完即完整世界 — Agent 原语原生系统

## 版本历程

| 版本 | 版本号 | 大小 | 关键特性 | GH Actions |
|------|--------|------|----------|------------|
| M1 桩版 | 0.1.0 | 8.6KB | WebView 壳 + JS-Native 桥(version/echo) | success |
| M1.5 | 0.2.0-m1.5 | 12KB | WebView 调试模式 + shell/sysinfo/toast/clip/execJS + 三标签页UI | success |

## 架构 M1.5

```
WebView (Chrome debug mode)
  JS <-> Native Bridge (JavascriptInterface)
    version() / echo() / shell(cmd) / sysinfo() / toast(msg) / clip(text) / execJS(code)
```

## 路线图

- [x] M1: 桩版 APK (WebView 壳 + JS 桥)
- [x] M1.5: 增强版 (调试模式 + shell + 三标签页)
- [ ] M2: Termux 集成
- [ ] M3: WebADB 操控层
- [ ] M4: Pi Agent 集成
- [ ] M5: 生成式 UI

## License

MIT
