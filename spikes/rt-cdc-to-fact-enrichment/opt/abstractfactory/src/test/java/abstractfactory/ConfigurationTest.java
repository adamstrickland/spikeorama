package abstractfactory;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class ConfigurationTest {
  @Test
  void testBasicConfig() {
    String fixture = "basic.yml";
    assertNotNull(Configuration.fromResource(fixture));
  }

  @Test
  void testOptionalAttribute() {
    String fixture = "withOptional.yml";
    assertNotNull(Configuration.fromResource(fixture));
  }
}
