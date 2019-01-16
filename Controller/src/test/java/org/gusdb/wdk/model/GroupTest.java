package org.gusdb.wdk.model;

import java.util.Map;

import org.gusdb.wdk.model.query.param.Param;
import org.gusdb.wdk.model.question.Question;
import org.gusdb.wdk.model.question.QuestionSet;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Jerric
 * 
 */
public class GroupTest {

    private WdkModel wdkModel;

    public GroupTest() throws Exception {
        wdkModel = UnitTestHelper.getModel();
    }

    /**
     * test getting all groups from model
     */
    @Test
    public void testGetGroups() {
        for (GroupSet groupSet : wdkModel.getAllGroupSets()) {
            Assert.assertNotNull(groupSet);
            String setName = groupSet.getName();
            Assert.assertTrue(setName.trim().length() > 0);

            // validate each group
            Group[] groups = groupSet.getGroups();
            Assert.assertTrue("group count", groups.length > 0);
            for (Group group : groups) {
                String gName = group.getName();
                Assert.assertTrue("name", gName.trim().length() > 0);

                Assert.assertEquals("group set", setName,
                        group.getGroupSet().getName());

                String fullName = group.getFullName();
                Assert.assertTrue("fullName starts with",
                        fullName.startsWith(setName));
                Assert.assertTrue("fullName ends with",
                        fullName.endsWith(gName));

                Assert.assertTrue(group.getDisplayName().trim().length() > 0);
            }
        }
    }

    @Test(expected = WdkModelException.class)
    public void testGetInvalidGroupSet() throws WdkModelException {
        String gsetName = "NonexistGroupSet";
        wdkModel.getGroupSet(gsetName);
    }

    @Test
    public void testGetGroup() throws WdkModelException {
        for (GroupSet groupSet : wdkModel.getAllGroupSets()) {
            for (Group group : groupSet.getGroups()) {
                String name = group.getName();
                Group g = groupSet.getGroup(name);

                Assert.assertEquals("by name", name, g.getName());
                
                String fullName = group.getFullName();
                g = (Group)wdkModel.resolveReference(fullName);
                Assert.assertEquals("by full name", fullName, g.getFullName());
            }
        }
    }

    @Test(expected = WdkModelException.class)
    public void testGetInvalidGroup() throws WdkModelException {
        String gName = "NonexistGroup";
        GroupSet[] groupSets = wdkModel.getAllGroupSets();
        if (groupSets.length == 0)
            throw new WdkModelException("Exception is expected.");
        for (GroupSet groupSet : groupSets) {
            groupSet.getGroup(gName);
        }
    }

    @Test(expected = WdkModelException.class)
    public void testGetInvalidGroupByFullName() throws WdkModelException {
        String fullName = "NonexistGroupSet.NonexistGroup";
        wdkModel.resolveReference(fullName);
    }

    @Test
    public void testGetParamGroups() throws Exception {
        for (QuestionSet questionSet : wdkModel.getQuestionSetsMap().values()) {
            for(Question question : questionSet.getQuestions()) {
                Map<String, Param> params = question.getParamMap();
                if (params.size() == 0) continue;
                
                Map<Group, Map<String, Param>> groups = question.getParamMapByGroups();
                Assert.assertTrue(groups.size() > 0); 
            }
        }
    }

}
