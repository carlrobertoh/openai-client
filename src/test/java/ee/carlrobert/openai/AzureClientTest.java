package ee.carlrobert.openai;

import static ee.carlrobert.openai.util.JSONUtil.e;
import static ee.carlrobert.openai.util.JSONUtil.jsonArray;
import static ee.carlrobert.openai.util.JSONUtil.jsonMap;
import static ee.carlrobert.openai.util.JSONUtil.jsonMapResponse;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import ee.carlrobert.openai.client.AzureClient;
import ee.carlrobert.openai.client.azure.AzureClientRequestParams;
import ee.carlrobert.openai.client.completion.CompletionEventListener;
import ee.carlrobert.openai.client.completion.ErrorDetails;
import ee.carlrobert.openai.client.completion.chat.ChatCompletionModel;
import ee.carlrobert.openai.client.completion.chat.request.ChatCompletionMessage;
import ee.carlrobert.openai.client.completion.chat.request.ChatCompletionRequest;
import ee.carlrobert.openai.client.completion.text.TextCompletionModel;
import ee.carlrobert.openai.client.completion.text.request.TextCompletionRequest;
import ee.carlrobert.openai.http.ResponseEntity;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AzureClientTest extends BaseTest {

  @Test
  void shouldStreamAzureChatCompletion() {
    var prompt = "TEST_PROMPT";
    var resultMessageBuilder = new StringBuilder();
    expectStreamRequest("/openai/deployments/TEST_DEPLOYMENT_ID/chat/completions", request -> {
      assertThat(request.getMethod()).isEqualTo("POST");
      assertThat(request.getHeaders().get("Authorization").get(0))
          .isEqualTo("Bearer TEST_API_KEY");
      assertThat(request.getBody())
          .extracting(
              "model",
              "temperature",
              "stream",
              "max_tokens",
              "frequency_penalty",
              "presence_penalty",
              "messages")
          .containsExactly(
              "gpt-3.5-turbo",
              0.5,
              true,
              500,
              0.1,
              0.1,
              List.of(Map.of("role", "user", "content", prompt)));
      return List.of(
          jsonMapResponse("choices", jsonArray(jsonMap("delta", jsonMap("role", "assistant")))),
          jsonMapResponse("choices", jsonArray(jsonMap("delta", jsonMap("content", "Hello")))),
          jsonMapResponse("choices", jsonArray(jsonMap("delta", jsonMap("content", "!")))));
    });

    new AzureClient.Builder("TEST_API_KEY",
        new AzureClientRequestParams("TEST_RESOURCE", "TEST_DEPLOYMENT_ID", "TEST_API_VERSION"))
        .setActiveDirectoryAuthentication(true)
        .buildChatCompletionClient()
        .stream(
            (ChatCompletionRequest) new ChatCompletionRequest.Builder(
                List.of(new ChatCompletionMessage("user", prompt)))
                .setModel(ChatCompletionModel.GPT_3_5)
                .setMaxTokens(500)
                .setTemperature(0.5)
                .setPresencePenalty(0.1)
                .setFrequencyPenalty(0.1)
                .build(),
            new CompletionEventListener() {
              @Override
              public void onMessage(String message) {
                resultMessageBuilder.append(message);
              }

              @Override
              public void onComplete(StringBuilder messageBuilder) {
                assertThat(messageBuilder.toString()).isEqualTo(resultMessageBuilder.toString());
              }
            });

    await().atMost(5, SECONDS).until(() -> "Hello!".contentEquals(resultMessageBuilder));
  }

  @Test
  void shouldStreamAzureTextCompletion() {
    var prompt = "TEST_PROMPT";
    var resultMessageBuilder = new StringBuilder();
    expectStreamRequest("/openai/deployments/TEST_DEPLOYMENT_ID/completions", request -> {
      assertThat(request.getMethod()).isEqualTo("POST");
      assertThat(request.getHeaders().get("Api-key").get(0)).isEqualTo("TEST_API_KEY");
      assertThat(request.getBody())
          .extracting(
              "model",
              "prompt",
              "stop",
              "temperature",
              "stream",
              "max_tokens",
              "frequency_penalty",
              "presence_penalty")
          .containsExactly(
              "text-davinci-003",
              prompt,
              List.of(" Human:", " AI:"),
              0.1,
              true,
              1000,
              0.2,
              0.2);
      return List.of(
          jsonMapResponse("choices", jsonArray(jsonMap("text", "He"))),
          jsonMapResponse("choices", jsonArray(jsonMap("text", "llo"))),
          jsonMapResponse("choices", jsonArray(jsonMap("text", "!"))));
    });

    new AzureClient.Builder("TEST_API_KEY",
        new AzureClientRequestParams("TEST_RESOURCE", "TEST_DEPLOYMENT_ID", "TEST_API_VERSION"))
        .buildTextCompletionClient()
        .stream(
            (TextCompletionRequest) new TextCompletionRequest.Builder(prompt)
                .setModel(TextCompletionModel.DAVINCI)
                .setStop(List.of(" Human:", " AI:"))
                .setMaxTokens(1000)
                .setTemperature(0.1)
                .setPresencePenalty(0.2)
                .setFrequencyPenalty(0.2)
                .build(),
            new CompletionEventListener() {
              @Override
              public void onMessage(String message) {
                resultMessageBuilder.append(message);
              }

              @Override
              public void onComplete(StringBuilder messageBuilder) {
                assertThat(messageBuilder.toString()).isEqualTo(resultMessageBuilder.toString());
              }
            });

    await().atMost(5, SECONDS).until(() -> "Hello!".contentEquals(resultMessageBuilder));
  }

  @Test
  void shouldListenForInvalidTokenErrorResponse() {
    var errorMessageBuilder = new StringBuilder();
    var errorResponse = jsonMapResponse(
        e("statusCode", 401),
        e("message", "Token is invalid"));
    expectRequest("/openai/deployments/TEST_DEPLOYMENT_ID/chat/completions",
        request -> new ResponseEntity(401, errorResponse));

    new AzureClient.Builder("TEST_API_KEY",
        new AzureClientRequestParams("TEST_RESOURCE", "TEST_DEPLOYMENT_ID", "TEST_API_VERSION"))
        .buildChatCompletionClient()
        .stream(
            new ChatCompletionRequest.Builder(
                List.of(new ChatCompletionMessage("user", "TEST_PROMPT")))
                .setModel(ChatCompletionModel.GPT_3_5)
                .build(),
            new CompletionEventListener() {
              @Override
              public void onError(ErrorDetails error) {
                errorMessageBuilder.append(error.getMessage());
              }
            });

    await().atMost(5, SECONDS).until(() -> "Token is invalid".contentEquals(errorMessageBuilder));
  }

  @Test
  void shouldListenForInvalidResourceErrorResponse() {
    var errorMessageBuilder = new StringBuilder();
    var errorResponse = jsonMapResponse("error", jsonMap(
        e("message", "Resource not found"),
        e("code", "404")));
    expectRequest("/openai/deployments/TEST_DEPLOYMENT_ID/chat/completions",
        request -> new ResponseEntity(404, errorResponse));

    new AzureClient.Builder("TEST_API_KEY",
        new AzureClientRequestParams("TEST_RESOURCE", "TEST_DEPLOYMENT_ID", "TEST_API_VERSION"))
        .buildChatCompletionClient()
        .stream(
            new ChatCompletionRequest.Builder(
                List.of(new ChatCompletionMessage("user", "TEST_PROMPT")))
                .setModel(ChatCompletionModel.GPT_3_5)
                .build(),
            new CompletionEventListener() {
              @Override
              public void onError(ErrorDetails error) {
                errorMessageBuilder.append(error.getMessage());
              }
            });

    await().atMost(5, SECONDS).until(() -> "Resource not found".contentEquals(errorMessageBuilder));
  }
}
