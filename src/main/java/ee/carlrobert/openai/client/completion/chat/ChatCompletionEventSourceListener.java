package ee.carlrobert.openai.client.completion.chat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import ee.carlrobert.openai.client.completion.ApiResponseError;
import ee.carlrobert.openai.client.completion.CompletionEventListener;
import ee.carlrobert.openai.client.completion.CompletionEventSourceListener;
import ee.carlrobert.openai.client.completion.ErrorDetails;
import ee.carlrobert.openai.client.completion.chat.response.ChatCompletionResponse;
import java.util.function.Consumer;

public class ChatCompletionEventSourceListener extends CompletionEventSourceListener {

  public ChatCompletionEventSourceListener(CompletionEventListener listeners, boolean retryOnReadTimeout, Consumer<String> onRetry) {
    super(listeners, retryOnReadTimeout, onRetry);
  }

  protected String getMessage(String data) throws JsonProcessingException {
    var choice = new ObjectMapper()
        .readValue(data, ChatCompletionResponse.class)
        .getChoices()
        .get(0);
    if (choice != null) {
      var delta = choice.getDelta();
      if (delta != null) {
        return delta.getContent();
      }
    }
    return "";
  }

  @Override
  protected ErrorDetails getErrorDetails(String data) throws JsonProcessingException {
    return new ObjectMapper().readValue(data, ApiResponseError.class).getError();
  }
}
