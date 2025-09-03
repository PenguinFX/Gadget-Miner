# Gadget-Miner 🛠️

**中文** | **[English](https://github.com/PenguinFX/Gadget-Miner/blob/master/README.md)**

`Gadget-Miner` 是一款针对 Java 反序列化漏洞利用链的深度分析工具，它是对项目 [Gadget Inspector](https://github.com/JackOfMostTrades/gadgetinspector) 的增强和改进。
本项目的工作建立在原始 `Gadget Inspector` 及其衍生版本 ([threedr3am](https://github.com/threedr3am/gadgetinspector), [5wimming](https://github.com/5wimming/gadgetinspector)) 的基础之上。


## ✨ 主要改进

### 1\. 扫描覆盖面优化😀 (Enhanced Scan Coverage)

静态分析工具常常因无法理解 Java 的一些动态特性而漏报关键的利用链。`Gadget-Miner` 在以下方面做出了核心改进：

- **`transient` 字段**：静态分析工具通常会忽略 `transient` 字段，导致污点分析在开发者通过自定义 `readObject` 方法恢复这些字段时中断。`Gadget-Miner` 能够智能识别此类情况，并允许污点在这些看似“非序列化”的字段上继续传递。

- **动态代理 (InvocationHandler)**：`InvocationHandler` 在诸如 Commons-Collections 等复杂利用链中扮演着核心角色。`Gadget-Miner` 不再将其简单视为 Source 点，而是从**优化污点传递**、**增强 Source 点**和**新增 Sink 点**三个维度对其进行深度建模。

### 2\. 扩展的 Sink 点集合😄 (Expanded Sink Collection)

`Gadget-Miner` 大幅扩展了其 Sink 点（攻击终点）的知识库，现在能够识别包括但不限于以下的漏洞类型：

- **LDAP 注入**: 如 `javax.naming.ldap.LdapContext.search()`
- **模板注入**: 如 `freemarker.template.Template.process()`
- **任意文件写**: 如 `java.io.FileWriter.<init>()`, `java.nio.channels.FileChannel.write()`
- **高危原生方法**: 如 `sun.misc.Unsafe.defineClass()`
- **SpEL 表达式注入**: 如 `org.springframework.expression.Expression.getValue()`

### 3\. 智能误报过滤🤖 (Intelligent False Positive Reduction)

静态分析的“理论”链条往往与实际可利用性存在差距。`Gadget-Miner` 引入了双重过滤机制来解决这个问题：

- **模式过滤**: 内置了一套精炼的规则，自动过滤掉已知的、因静态分析局限性而产生的常见误报模式。
- **AI 研判 (可选)**: 通过集成大型语言模型（Gemini），`Gadget-Miner` 可以将发现的利用链提交给 AI 进行语义层面的可利用性分析。

## 🔧 安装与配置

### 安装步骤

1.  **下载项目:**
    ```bash
    git clone https://github.com/PenguinFX/Gadget-Miner.git
    cd Gadget-Miner
    ```
2.  **构建项目:** 使用 Gradle 构建可执行的 `jar` 文件。
    ```bash
    ./gradlew shadowJar
    ```
    构建成功后，您将在 `build/libs/` 目录下找到 `Gadget-Miner.jar`。

## 🚀 使用指南

`Gadget-Miner` 的使用方式与原版 `Gadget Inspector` 完全兼容。启用 AI 研判功能使用 `--api-key` 参数传入您的 `Gemini API Key`。
```bash
java -jar build/libs/Gadget-Miner.jar --config jserial --api-key YOUR_GEMINI_API_KEY /path/to/your.jar
```
分析完成后，AI 的研判结果将以 JSON 格式保存在 `gadget-result/AI-gadget-chains-analysis-*.json` 文件中。

## 📜 开源许可

本项目采用 [MIT License](https://github.com/PenguinFX/Gadget-Miner/blob/master/LICENSE) 开源许可。

-----

**免责声明：** 本工具仅供授权的安全研究和教育目的使用。使用者应对其行为负全部责任。

-----
