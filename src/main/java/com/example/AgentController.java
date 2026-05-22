package com.example;

import com.anthropic.client.AnthropicClient;
import com.anthropic.models.beta.agents.AgentCreateParams;
import com.anthropic.models.beta.agents.AgentUpdateParams;
import com.anthropic.models.beta.agents.BetaManagedAgentsAgent;
import com.anthropic.models.beta.agents.BetaManagedAgentsAgentToolset20260401Params;
import com.anthropic.models.beta.agents.BetaManagedAgentsModel;
import java.util.ArrayList;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AgentController {
  private final AnthropicClient anthropicClient;
  private BetaManagedAgentsAgent agent;

  public AgentController(AnthropicClient anthropicClient) {
    this.anthropicClient = anthropicClient;
  }

  @GetMapping("/agent/create")
  public BetaManagedAgentsAgent createAgent() {
    this.agent = anthropicClient.beta().agents().create(
        AgentCreateParams.builder()
            .name("Coding Assistant")
            .model(BetaManagedAgentsModel.CLAUDE_OPUS_4_7)
            .system("You are a helpful coding agent.")
            .addTool(
                BetaManagedAgentsAgentToolset20260401Params.builder()
                    .type(BetaManagedAgentsAgentToolset20260401Params.Type.AGENT_TOOLSET_20260401)
                    .build()
            )
            .build()
    );

    return agent;
  }

  @GetMapping("/agent/update")
  public BetaManagedAgentsAgent updateAgent() {
    this.agent = anthropicClient.beta().agents().update(
        agent.id(),
        AgentUpdateParams.builder()
            .version(agent.version())
            .system("You are a helpful coding agent. Always write tests.")
            .build()
    );

    return agent;
  }

  @GetMapping("/agent")
  public  BetaManagedAgentsAgent getAgent() {
    return agent;
  }


  @GetMapping("/agent/versions")
  public List<String> getAgentVersions() {
    var result = new ArrayList<String>();
    for (var version : anthropicClient.beta().agents().versions().list(agent.id()).autoPager()) {
      result.add("Version " + version.version() + ": " + version.updatedAt());
    }

    return result;
  }


  @GetMapping("/agent/archive")
  public String archiveAgent() {
    var archived = anthropicClient.beta().agents().archive(agent.id());
    return "Archived at: " + archived.archivedAt().orElseThrow();
  }


}
