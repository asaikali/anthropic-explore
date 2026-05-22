package com.example;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class AnthropicExploreApplication {

  @Bean
  AnthropicClient anthropicClient() {
    return AnthropicOkHttpClient.fromEnv();
  }

  public static void main(String[] args) {
    SpringApplication.run(AnthropicExploreApplication.class, args);
  }

}
