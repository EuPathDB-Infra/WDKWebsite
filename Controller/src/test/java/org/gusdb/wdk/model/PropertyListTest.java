package org.gusdb.wdk.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author xingao
 */
public class PropertyListTest {

  private static WdkModel wdkModel;

  public PropertyListTest() throws Exception {
    wdkModel = UnitTestHelper.getModel();
  }

  @Test
  public void testGetPropertyList() {
    var defaultPropertyList = wdkModel.getDefaultPropertyLists();

    for (var questionSet : wdkModel.getAllQuestionSets()) {
      for (var question : questionSet.getQuestions()) {
        var propertyMap = question.getPropertyLists();
        for (var propName : propertyMap.keySet()) {
          var properties = propertyMap.get(propName);
          assertTrue(properties.length > 0,
            "property list is empty: " + propName);
        }

        // default properties should appear in the property list too
        for (var propName : defaultPropertyList.keySet()) {
          assertTrue(propertyMap.containsKey(propName),
            "default prop doesn't exist: " + propName);
        }
      }
    }
  }
}
