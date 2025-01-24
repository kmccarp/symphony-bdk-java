package com.symphony.bdk.core.service.version.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AgentVersionTest {

  @Test
  public void testHigherVersion() {
    AgentVersion agent209 = new AgentVersion(20,9);
    AgentVersion agent2412 = new AgentVersion(24,12);
    AgentVersion agent2501 = new AgentVersion(25,1);


    assertTrue(agent2412.isHigher(agent209));
    assertTrue(agent2501.isHigher(agent209));
    assertTrue(agent2501.isHigher(agent2412));
  }

  @Test
  public void testConstants() {
    assertEquals(24, AgentVersion.AGENT_24_12.getMajor());
    assertEquals(12, AgentVersion.AGENT_24_12.getMinor());
  }
}
