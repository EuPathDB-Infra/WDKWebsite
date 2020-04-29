package org.gusdb.wdk.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Jerric
 */
public class GroupTest {

  private final WdkModel wdkModel;

  public GroupTest() throws Exception {
    wdkModel = UnitTestHelper.getModel();
  }

  /**
   * test getting all groups from model
   */
  @Test
  public void testGetGroups() {
    for (var groupSet : wdkModel.getAllGroupSets()) {
      assertNotNull(groupSet);
      var setName = groupSet.getName();
      assertTrue(setName.trim().length() > 0);

      // validate each group
      var groups = groupSet.getGroups();
      assertTrue(groups.length > 0, "group count");
      for (var group : groups) {
        var gName = group.getName();
        assertTrue(gName.trim().length() > 0, "name");
        assertEquals(setName, group.getGroupSet().getName(), "group set");

        var fullName = group.getFullName();

        assertTrue(fullName.startsWith(setName), "fullName starts with");
        assertTrue(fullName.endsWith(gName), "fullName ends with");
        assertTrue(group.getDisplayName().trim().length() > 0);
      }
    }
  }

  @Test
  public void testGetInvalidGroupSet() {
    var gsetName = "NonexistGroupSet";
    assertThrows(WdkModelException.class, () -> wdkModel.getGroupSet(gsetName));
  }

  @Test
  public void testGetGroup() throws WdkModelException {
    for (var groupSet : wdkModel.getAllGroupSets()) {
      for (var group : groupSet.getGroups()) {
        var name = group.getName();
        var g = groupSet.getGroup(name);

        assertEquals(name, g.getName(), "by name");

        var fullName = group.getFullName();
        g = (Group) wdkModel.resolveReference(fullName);
        assertEquals(fullName, g.getFullName(), "by full name");
      }
    }
  }

  @Test
  public void testGetInvalidGroup() {
    var gName = "NonexistGroup";
    var groupSets = wdkModel.getAllGroupSets();
    assertThrows(WdkModelException.class, () -> {
      if (groupSets.length == 0)
        throw new WdkModelException("Exception is expected.");
      for (var groupSet : groupSets) {
        groupSet.getGroup(gName);
      }
    });
  }

  @Test
  public void testGetInvalidGroupByFullName() {
    String fullName = "NonexistGroupSet.NonexistGroup";
    assertThrows(WdkModelException.class,
      () -> wdkModel.resolveReference(fullName));
  }

  @Test
  public void testGetParamGroups() {
    for (var questionSet : wdkModel.getQuestionSetsMap().values()) {
      for (var question : questionSet.getQuestions()) {
        var params = question.getParamMap();
        if (params.size() == 0)
          continue;

        var groups = question.getQuery().getParamMapByGroups();
        assertTrue(groups.size() > 0);
      }
    }
  }

}
