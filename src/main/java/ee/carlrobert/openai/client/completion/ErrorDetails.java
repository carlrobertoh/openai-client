package ee.carlrobert.openai.client.completion;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ErrorDetails {

  private static final String DEFAULT_ERROR_MSG = "Something went wrong. Please try again later.";

  private final String message;
  private final String type;
  private final String param;
  private final String code;

  public ErrorDetails(String message) {
    this(message, null, null, null);
  }

  @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
  public ErrorDetails(
      @JsonProperty("message") String message,
      @JsonProperty("type") String type,
      @JsonProperty("param") String param,
      @JsonProperty("code") String code) {
    this.message = message;
    this.type = type;
    this.param = param;
    this.code = code;
  }

  public static ErrorDetails DEFAULT_ERROR = new ErrorDetails(DEFAULT_ERROR_MSG);

  public String getMessage() {
    return message;
  }

  public String getType() {
    return type;
  }

  public String getParam() {
    return param;
  }

  public String getCode() {
    return code;
  }
}
