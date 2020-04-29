package org.gusdb.wdk.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Jerric
 */
public class WdkModelTest {

  private final WdkModel wdkModel;

  public WdkModelTest() throws Exception {
    wdkModel = UnitTestHelper.getModel();
  }

  /**
   * get model name, display name, and version
   */
  @Test
  public void testGetModelInfo() {
    var name = wdkModel.getProjectId();
    assertTrue(name != null && name.length() > 0, "the model name is not set");

    var displayName = wdkModel.getDisplayName();
    assertTrue(displayName != null && displayName.length() > 0,
      "the model display name is not set");

    var version = wdkModel.getVersion();
    assertTrue(version != null && version.length() > 0,
      "the model version is not set");
  }

  /**
   * test getting default property lists
   */
  @Test
  public void testGetDefaultPropertyList() {
    var propLists = wdkModel.getDefaultPropertyLists();
    for (var plName : propLists.keySet()) {
      assertNotNull(plName, "property list name should not be null");
      var values = propLists.get(plName);
      assertTrue(values.length > 0, "property list should have some values");
    }
  }
}
