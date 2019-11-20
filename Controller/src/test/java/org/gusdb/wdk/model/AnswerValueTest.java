/**
 * 
 */
package org.gusdb.wdk.model;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.gusdb.fgputil.validation.ValidObjectFactory.RunnableObj;
import org.gusdb.wdk.model.answer.AnswerFilterInstance;
import org.gusdb.wdk.model.answer.AnswerValue;
import org.gusdb.wdk.model.answer.factory.AnswerValueFactory;
import org.gusdb.wdk.model.record.attribute.AttributeField;
import org.gusdb.wdk.model.user.Step;
import org.gusdb.wdk.model.user.User;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author xingao
 * 
 */
public class AnswerValueTest {

    private User user;

    public AnswerValueTest() throws Exception {
        this.user = UnitTestHelper.getRegisteredUser();
    }

    @Test
    public void testGetSummaryAttributes() throws Exception {
        RunnableObj<Step> step = UnitTestHelper.createNormalStep(user);
        AnswerValue answerValue = AnswerValueFactory.makeAnswer(step);

        Map<String, AttributeField> displayFields = answerValue
            .getAttributes().getDisplayableAttributeMap();
        Map<String, AttributeField> summaryFields = answerValue
            .getAttributes().getSummaryAttributeFieldMap();

        // no display fields should appear in summary fields
        Assert.assertTrue(
                "display attributes is fewer than summary attributes",
                displayFields.size() > summaryFields.size());

        // summary fields should be included in displayable fields
        for (String name : summaryFields.keySet()) {
            Assert.assertTrue("Summary atribute [" + name
                    + "] doesn't exist in displayable attributes.",
                    displayFields.containsKey(name));
        }
    }

    @Test
    public void testAddSummaryAttibute() throws Exception {
        RunnableObj<Step> step = UnitTestHelper.createNormalStep(user);
        AnswerValue answerValue = AnswerValueFactory.makeAnswer(step);

        Map<String, AttributeField> displayFields = answerValue
            .getAttributes().getDisplayableAttributeMap();
        Map<String, AttributeField> summaryFields = answerValue
            .getAttributes().getSummaryAttributeFieldMap();

        for (AttributeField field : displayFields.values()) {
            // skip the fields that are already in the summary
            if (summaryFields.containsKey(field.getName()))
                continue;

            // prepare the summary list
            List<String> list = new ArrayList<String>();
            for (AttributeField f : summaryFields.values()) {
                list.add(f.getName());
            }
            list.add(field.getName());
            String[] summaryList = new String[list.size()];
            list.toArray(summaryList);

            summaryFields = answerValue.getAttributes().getSummaryAttributeFieldMap();

            for (String name : summaryList) {
                Assert.assertTrue(summaryFields.containsKey(name));
            }
        }
    }

    @Test
    public void testDeleteSummaryAttibute() throws Exception {
        RunnableObj<Step> step = UnitTestHelper.createNormalStep(user);
        AnswerValue answerValue = AnswerValueFactory.makeAnswer(step);

        Map<String, AttributeField> displayFields = answerValue
            .getAttributes().getDisplayableAttributeMap();
        Map<String, AttributeField> summaryFields = answerValue
            .getAttributes().getSummaryAttributeFieldMap();

        // skip the primary key field
        Iterator<AttributeField> it = summaryFields.values().iterator();
        it.next();
        AttributeField field = it.next();

        // prepare the summary list
        List<String> list = new ArrayList<String>();
        for (AttributeField f : summaryFields.values()) {
            if (f.equals(field))
                continue;
            list.add(f.getName());
        }
        String[] summaryList = new String[list.size()];
        list.toArray(summaryList);

        displayFields = answerValue.getAttributes().getDisplayableAttributeMap();
        summaryFields = answerValue.getAttributes().getSummaryAttributeFieldMap();

        Assert.assertTrue(displayFields.containsKey(field.getName()));
        Assert.assertFalse(summaryFields.containsKey(field.getName()));
        for (String name : summaryList) {
            Assert.assertTrue(summaryFields.containsKey(name));
        }
    }

    @Test
    public void testGetFilterSizes() throws Exception {
        RunnableObj<Step> step = UnitTestHelper.createNormalStep(user);
        AnswerValue answerValue = AnswerValueFactory.makeAnswer(step);
        Optional<AnswerFilterInstance> currentFilter = answerValue.getAnswerSpec().getLegacyFilter();
        int size = answerValue.getResultSizeFactory().getResultSize();

        AnswerFilterInstance[] filters = answerValue.getAnswerSpec().getQuestion()
                .getRecordClass().getFilterInstances();
        for (AnswerFilterInstance filter : filters) {
            int filterSize = answerValue.getResultSizeFactory().getFilterSize(filter.getName());
            if (currentFilter.isPresent() && filter.getName().equals(currentFilter.get().getName())) {
                Assert.assertEquals(size, filterSize);
            } else {
                Assert.assertTrue(filterSize >= 0);
            }
        }
    }
}
