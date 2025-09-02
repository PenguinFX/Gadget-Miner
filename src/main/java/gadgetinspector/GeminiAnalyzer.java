package gadgetinspector;

import org.apache.http.HttpHost;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import gadgetinspector.GadgetChainDiscovery.GadgetChain;

public class GeminiAnalyzer {

    private static final Logger LOGGER = LoggerFactory.getLogger(GeminiAnalyzer.class);
    private static final String API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent";
    private static final int BATCH_SIZE = 50;

    private final String apiKey;

    public GeminiAnalyzer(String apiKey) {
        this.apiKey = apiKey;
        if (this.apiKey == null || this.apiKey.isEmpty()) {
            LOGGER.warn("Gemini API Key not provided. AI analysis will be skipped.");
        }
    }

    /**
     * 将发现的利用链分批发送给Gemini进行可用性分析，并将所有结果合并到一个文件中。
     * @param gadgetChains 发现的利用链集合
     */
    public void analyzeChains(Collection<GadgetChain> gadgetChains) {
        if (apiKey == null || apiKey.isEmpty()) {
            return;
        }

        if (gadgetChains.isEmpty()) {
            LOGGER.info("No gadget chains found to analyze.");
            return;
        }
        List<GadgetChain> chainsList = new ArrayList<>(gadgetChains);
        int totalChains = chainsList.size();
        int totalBatches = (int) Math.ceil((double) totalChains / BATCH_SIZE);
        JSONArray allAnalysisResults = new JSONArray();

        LOGGER.info("Starting analysis of " + totalChains + " gadget chains in " + totalBatches + " batches of up to " + BATCH_SIZE + " each.");

        for (int i = 0; i < totalChains; i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, totalChains);
            List<GadgetChain> batch = chainsList.subList(i, end);
            int currentBatchNum = (i / BATCH_SIZE) + 1;

            LOGGER.info("Analyzing batch " + currentBatchNum + "/" + totalBatches + " (" + batch.size() + " chains)...");

            String chainsAsText = formatChainsToText(batch);
            String prompt = buildPrompt(chainsAsText);

            String responseJsonString = null;
            String cleanedJson = null;
            try {
                responseJsonString = callGeminiApi(prompt);
                cleanedJson = cleanJsonString(responseJsonString);
                JSONObject batchResult = new JSONObject(cleanedJson);
                JSONArray batchAnalysis = batchResult.getJSONArray("analysisResults");
                for (int j = 0; j < batchAnalysis.length(); j++) {
                    allAnalysisResults.put(batchAnalysis.get(j));
                }
                LOGGER.info("Successfully processed batch " + currentBatchNum + ".");

            } catch (IOException e) {
                System.out.print(cleanedJson);
                LOGGER.error("Failed to call Gemini API for batch " + currentBatchNum + ".", e);
            } catch (JSONException e) {
                System.out.print(cleanedJson);
                LOGGER.error("Failed to parse JSON response for batch " + currentBatchNum + ". The model may have returned an invalid format.", e);
            }
        }

        if (allAnalysisResults.length() > 0) {
            JSONObject finalReport = new JSONObject();
            finalReport.put("analysisResults", allAnalysisResults);

            try {
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd-HH-mm");
                String timestamp = dateFormat.format(new Date());
                String fileName = "AI-gadget-chains-analysis-" + timestamp + ".json";

                Path outputDir = Paths.get("gadget-result");
                Files.createDirectories(outputDir);
                Path filePath = outputDir.resolve(fileName);

                Files.write(filePath, finalReport.toString(4).getBytes(StandardCharsets.UTF_8));
                LOGGER.info("Consolidated AI analysis for " + allAnalysisResults.length() + " chains saved to: " + filePath.toAbsolutePath());

            } catch (IOException e) {
                LOGGER.error("Failed to write final AI analysis report to file.", e);
            }
        } else {
            LOGGER.warn("No analysis results were generated from any batch.");
        }
    }

    private String callGeminiApi(String prompt) throws IOException {
        HttpHost proxy = new HttpHost("127.0.0.1", 7890);
        try (CloseableHttpClient httpClient = HttpClients.custom().setProxy(proxy).build()) {
            HttpPost httpPost = new HttpPost(API_URL + "?key=" + this.apiKey);

            JSONObject part = new JSONObject();
            part.put("text", prompt);

            JSONArray parts = new JSONArray();
            parts.put(part);

            JSONObject content = new JSONObject();
            content.put("parts", parts);

            JSONArray contents = new JSONArray();
            contents.put(content);

            JSONObject requestBody = new JSONObject();
            requestBody.put("contents", contents);

            StringEntity entity = new StringEntity(requestBody.toString(), StandardCharsets.UTF_8);
            httpPost.setEntity(entity);
            httpPost.setHeader("Content-Type", "application/json");

            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                try {
                    String responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                    try {
                        JSONObject root = new JSONObject(responseBody);
                        return root.getJSONArray("candidates")
                                .getJSONObject(0)
                                .getJSONObject("content")
                                .getJSONArray("parts")
                                .getJSONObject(0)
                                .getString("text");
                    } catch (JSONException e) {
                        return responseBody;
                    }
                } finally {

                }
            }
        }
    }

    private String formatChainsToText(Collection<GadgetChain> gadgetChains) {
        StringBuilder sb = new StringBuilder();
        int chainCount = 1;
        for (GadgetChain chain : gadgetChains) {
            sb.append("--- Chain #").append(chainCount++).append(" ---\n");
            sb.append(chain.toString());
            sb.append("\n");
        }
        return sb.toString();
    }

    private String cleanJsonString(String text) {
        if (text == null) {
            return "";
        }
        String output = text.trim().replace("```json", "").replace("```", "").trim();
        return output;
    }

    private String buildPrompt(String chainsAsText) {
        String promptTemplate = "You are a world-class expert security researcher with deep knowledge of the Java JDK, popular libraries, and deserialization exploitation techniques. You will function as a JSON API to analyze gadget chains.\n"
                + "Your task is to distinguish between genuinely exploitable gadget chains and common false positives generated by static analysis tools. Your analysis must be based on the semantic intent of the methods and the plausibility of attacker-controlled data flow, not just the existence of a call path.\n\n"
                + "## Core Analysis Principles:\n"
                + "1.  **Semantic Intent vs. Capability**: Critically assess the primary purpose of each method in the chain (e.g., `equals()` is for comparison, `toString()` is for representation). A chain is likely a false positive if exploiting it requires the method to behave in a way that contradicts its fundamental design intent.\n"
                + "2.  **Taint Flow Plausibility**: Do not assume data flow just because a method is called. For each step, question whether attacker-controlled data from a source (e.g., a parameter) can realistically and meaningfully influence the subsequent call. The numbers in parentheses like `(0)` or `(1)` indicate the tainted argument index; pay close attention to how this index changes between calls.\n"
                + "3.  **Context and Type Constraints**: Consider implicit constraints. For example, when a chain involves `ObjectInputStream.readObject()`, is it for deserializing an arbitrary, attacker-controlled object, or is it likely constrained by the calling context to a specific, non-dangerous type (e.g., reading a `Level` object in a logging framework), which would likely result in a `ClassCastException` if exploited?\n"
                + "4.  **Skepticism of Common False Positives**: Be highly skeptical of chains starting with `equals()`, `hashCode()`, or `toString()`. While valid entry points exist, they are statistically more likely to be false positives. The burden of proof for their exploitability is high.\n\n"
                + "## Output Schema:\n"
                + "Respond **only with a single, valid JSON object**. Do not include any introductory text, explanations, or markdown formatting outside of the JSON structure.\n"
                + "The root object must contain a single key `analysisResults`, which is an array of objects.\n"

                + "Each object in the array represents a single chain and must contain the following keys:\n"
                + "- `chain` (string): The full gadget chain string being analyzed.\n"
                + "- `isExploitable` (boolean): `true` only if the chain is practically exploitable, otherwise `false`.\n"
                + "- `exploitability` (string): Your assessment, one of: \"High\", \"Medium\", \"Low\", or \"Not Exploitable\".\n"
                + "- `justification` (string): Your detailed reasoning, **explicitly applying the Core Analysis Principles**. Explain *why* the taint flow is plausible or not, and why the chain's intent suggests it is malicious or a benign artifact of static analysis.\n"
                + "- `chainAnalysis` (string | null): A step-by-step analysis of the **plausible data flow** if exploitable. Detail what attacker-controlled object/data is needed at each step. If not exploitable, this must be `null`.\n"
                + "- `poc` (string | null): Complete, working Java Proof-of-Concept code as a single string (use '\\n' for newlines) if exploitable, otherwise `null`.\n\n"
                + "## Examples:\n"
                + "{\n"
                + "  \"analysisResults\": [\n"
                + "    {\n"
                + "      \"chain\": \"TransientHolder.readObject(Ljava/io/ObjectInputStream;)V (1) -> VulnerableAction.executeCommand(Ljava/lang/String;)V (1) -> java/lang/Runtime.exec(Ljava/lang/String;)Ljava/lang/Process; (1)\",\n"
                + "      \"isExploitable\": true,\n"
                + "      \"exploitability\": \"High\",\n"
                + "      \"justification\": \"This is a classic and direct deserialization gadget. The entry point is a standard `readObject` method which passes a deserialized, attacker-controlled String directly to a dangerous sink (`Runtime.exec`). The taint flow is direct and plausible.\",\n"
                + "      \"chainAnalysis\": \"1. An attacker provides a serialized `TransientHolder` object. 2. During deserialization, `readObject` is called. 3. Inside `readObject`, the `VulnerableAction.executeCommand` method is called with a controlled string. 4. `executeCommand` passes this string directly to `Runtime.exec`, leading to command execution.\",\n"
                + "      \"poc\": \"import java.io.*;\\n// ... Complete POC code ...\"\n"
                + "    },\n"
                + "    {\n"
                + "      \"chain\": \"com/sun/org/apache/xpath/internal/objects/XString.equals(Ljava/lang/Object;)Z (1) -> java/security/PermissionCollection.toString()Ljava/lang/String; (0) -> java/net/URL.openConnection()Ljava/net/URLConnection; (0)\",\n"
                + "      \"isExploitable\": false,\n"
                + "      \"exploitability\": \"Not Exploitable\",\n"
                + "      \"justification\": \"False positive based on Semantic Intent. The purpose of `equals()` and `toString()` is comparison and representation, not initiating attacker-controlled network connections. The call to `openConnection` is likely for an internal, non-attacker-controlled resource (e.g., loading an XML schema). The taint flow from the `equals` parameter to the `URL` is highly implausible.\",\n"
                + "      \"chainAnalysis\": null,\n"
                + "      \"poc\": null\n"
                + "    }\n"
                + "  ]\n"
                + "}\n\n"
                + "## Analyze Gadget Chains:\n"
                + "Analyze the following gadget chains based on all the rules and principles above:\n\n"
                + "%s";

        return String.format(promptTemplate, chainsAsText);
    }
}