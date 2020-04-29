package org.gusdb.wdk.model;

import org.gusdb.wdk.model.config.ModelConfigParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Jerric
 *
 */
public class ModelConfigParserTest {

  private String projectId;
  private String gusHome;

  /**
   * get and validate the input
   */
  @BeforeEach
  public void getInput() throws WdkModelException {
    // get input from the system environment
    projectId = System.getProperty(Utilities.ARGUMENT_PROJECT_ID);
    gusHome = System.getProperty(Utilities.SYSTEM_PROPERTY_GUS_HOME);

    // GUS_HOME is required
    if (gusHome == null || gusHome.length() == 0)
      throw new WdkModelException("Required system property "
        + Utilities.SYSTEM_PROPERTY_GUS_HOME
        + " is missing.");

    if (projectId == null || projectId.length() == 0)
      throw new WdkModelException("Required system property "
        + Utilities.ARGUMENT_PROJECT_ID
        + " is missing.");
  }

  /**
   * test parsing a valid config file
   */
  @Test
  public void testParseConfig()
  throws SAXException, IOException, WdkModelException {
    var parser = new ModelConfigParser(gusHome);
    var config = parser.parseConfig(projectId).build();
    assertNotNull(config);
  }
}
