# Gadget-Miner 🛠️

**[中文](https://github.com/PenguinFX/Gadget-Miner/blob/master/README_ch.md)** | **English**

`Gadget-Miner` is a deep analysis tool for Java deserialization vulnerability exploit chains, serving as an enhancement and improvement upon the original [Gadget Inspector](https://github.com/JackOfMostTrades/gadgetinspector) project.

The work of this project is built upon the original `Gadget Inspector` and its derivative versions ([threedr3am](https://github.com/threedr3am/gadgetinspector), [5wimming](https://github.com/5wimming/gadgetinspector)).

## ✨ Key Improvements

### 1\. Enhanced Scan Coverage 😀

Static analysis tools often miss critical gadget chains because they cannot understand some of Java's dynamic features. `Gadget-Miner` makes core improvements in the following areas:

- **`transient` Field**: Static analysis tools typically ignore `transient` fields, causing taint analysis to break when developers restore these fields via a custom `readObject` method. `Gadget-Miner` can intelligently identify such cases and allow taints to propagate through these seemingly "non-serialized" fields.

- **Dynamic Proxies (InvocationHandler)**: `InvocationHandler` plays a central role in complex exploit chains like those in Commons-Collections. `Gadget-Miner` no longer treats it merely as a source but performs deep modeling from three perspectives: **optimizing taint propagation**, **enhancing sources**, and **adding new sinks**.

### 2\. Expanded Sink Collection 😄

`Gadget-Miner` expands its knowledge base of sinks and can now identify, but is not limited to, the following vulnerability types:

- **LDAP Injection**: e.g., `javax.naming.ldap.LdapContext.search()`
- **Template Injection**: e.g., `freemarker.template.Template.process()`
- **Arbitrary File Write**: e.g., `java.io.FileWriter.<init>()`, `java.nio.channels.FileChannel.write()`
- **High-Risk Native Methods**: e.g., `sun.misc.Unsafe.defineClass()`
- **SpEL Injection**: e.g., `org.springframework.expression.Expression.getValue()`

### 3\. Intelligent False Positive Reduction 🤖

A gap often exists between "theoretical" chains from static analysis and actual exploitability. `Gadget-Miner` introduces a dual-filter mechanism to address this:

- **Pattern-based Filtering**: A set of refined rules is built-in to automatically filter out common false positive patterns known to arise from the limitations of static analysis.
- **AI-Powered Analysis (Optional)**: By integrating with a large language model, `Gadget-Miner` can submit discovered gadget chains to the AI for a semantic-level analysis of their exploitability.

## 🔧 Installation and Configuration

### Installation Steps

1.  **Download the project:**
    ```bash
    git clone https://github.com/PenguinFX/Gadget-Miner.git
    cd Gadget-Miner
    ```
2.  **Build the project:** Use Gradle to build the executable `jar` file.
    ```bash
    ./gradlew shadowJar
    ```
    After a successful build, you will find `Gadget-Miner.jar` in the `build/libs/` directory.

## 🚀 Usage Guide

`Gadget-Miner` is fully compatible with the original `Gadget Inspector`'s command-line usage.

**To enable the AI analysis feature:** Pass your `API Key` using the `--api-key` argument(Currently only Gemini models are supported).

```bash
java -jar build/libs/Gadget-Miner.jar --config jserial --api-key YOUR_API_KEY /path/to/your.jar
```

After the analysis is complete, the AI's judgment results will be saved in JSON format in the `gadget-result/AI-gadget-chains-analysis-*.json` file.

## 📜 License

This project is licensed under the [MIT License](https://github.com/PenguinFX/Gadget-Miner/blob/master/LICENSE).

-----

**Disclaimer:** This tool is intended for authorized security research and educational purposes only. The user is solely responsible for their actions.

-----