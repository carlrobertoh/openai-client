package ee.carlrobert.openai.client;

public enum ClientCode {
  DASHBOARD("dashboard"),
  CHAT_COMPLETION("chat.completion"),
  TEXT_COMPLETION("text.completion"),
  AZURE_CHAT_COMPLETION("azure.chat.completion"),
  AZURE_TEXT_COMPLETION("azure.text.completion");

  private final String code;

  ClientCode(String code) {
    this.code = code;
  }

  public String getCode() {
    return code;
  }
}
