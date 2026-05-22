package com.example;

import com.anthropic.client.AnthropicClient;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RootController {

  private final AnthropicClient anthropicClient;
  public RootController(AnthropicClient anthropicClient) {
    this.anthropicClient = anthropicClient;
  }

  @GetMapping("/")
  Message get() {
    MessageCreateParams params = MessageCreateParams.builder()
        .maxTokens(1024L)
        .addUserMessage("Hello, Claude")
        .model(Model.CLAUDE_OPUS_4_7)
        .build();

    Message message = anthropicClient.messages().create(params);

    return message;
  }

}
