package org.gusdb.wdk.model;

import org.gusdb.fgputil.validation.ValidObjectFactory.RunnableObj;
import org.gusdb.wdk.model.answer.AnswerFilterInstance;
import org.gusdb.wdk.model.answer.AnswerValue;
import org.gusdb.wdk.model.answer.factory.AnswerValueFactory;
import org.gusdb.wdk.model.record.attribute.AttributeField;
import org.gusdb.wdk.model.user.Step;
import org.gusdb.wdk.model.user.User;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author xingao
 */
public class AnswerValueTest {

  private final User user;

  public AnswerValueTest() throws Exception {
    this.user = UnitTestHelper.getRegisteredUser();
  }

  @Test
  public void testGetSummaryAttributes() throws Exception {
    RunnableObj<Step> step = UnitTestHelper.createNormalStep(user);
    AnswerValue answerValue = AnswerValueFactory.makeAnswer(step);

    Map<String, AttributeField> displayFields = answerValue.getAttributes()
      .getDisplayableAttributeMap();
    Map<String, AttributeField> summaryFields = answerValue.getAttributes()
      .getSummaryAttributeFieldMap();

    // no display fields should appear in summary fields
    assertTrue(displayFields.size() > summaryFields.size(),
      "display attributes is fewer than summary attributes");

    // summary fields should be included in displayable fields
    for (String name : summaryFields.keySet()) {
      assertTrue(displayFields.containsKey(name),
        "Summary atribute [" + name
          + "] doesn't exist in displayable attributes.");
    }
  }

  @Test
  public void testAddSummaryAttribute() throws Exception {
    RunnableObj<Step> step = UnitTestHelper.createNormalStep(user);
    AnswerValue answerValue = AnswerValueFactory.makeAnswer(step);

    Map<String, AttributeField> displayFields = answerValue.getAttributes()
      .getDisplayableAttributeMap();
    Map<String, AttributeField> summaryFields = answerValue.getAttributes()
      .getSummaryAttributeFieldMap();

    for (AttributeField field : displayFields.values()) {
      // skip the fields that are already in the summary
      if (summaryFields.containsKey(field.getName()))
        continue;

      // prepare the summary list
      List<String> list = new ArrayList<>();
      for (AttributeField f : summaryFields.values()) {
        list.add(f.getName());
      }
      list.add(field.getName());
      String[] summaryList = new String[list.size()];
      list.toArray(summaryList);

      summaryFields = answerValue.getAttributes().getSummaryAttributeFieldMap();

      for (String name : summaryList) {
        assertTrue(summaryFields.containsKey(name));
      }
    }
  }

  @Test
  public void testDeleteSummaryAttibute() throws Exception {
    RunnableObj<Step> step = UnitTestHelper.createNormalStep(user);
    AnswerValue answerValue = AnswerValueFactory.makeAnswer(step);

    var displayFields = answerValue.getAttributes()
      .getDisplayableAttributeMap();
    var summaryFields = answerValue.getAttributes()
      .getSummaryAttributeFieldMap();

    // skip the primary key field
    var it = summaryFields.values().iterator();
    it.next();
    var field = it.next();

    // prepare the summary list
    List<String> list = new ArrayList<>();
    for (AttributeField f : summaryFields.values()) {
      if (f.equals(field))
        continue;
      list.add(f.getName());
    }
    String[] summaryList = new String[list.size()];
    list.toArray(summaryList);

    displayFields = answerValue.getAttributes().getDisplayableAttributeMap();
    summaryFields = answerValue.getAttributes().getSummaryAttributeFieldMap();

    assertTrue(displayFields.containsKey(field.getName()));
    assertFalse(summaryFields.containsKey(field.getName()));
    for (String name : summaryList) {
      assertTrue(summaryFields.containsKey(name));
    }
  }

  @Test
  public void testGetFilterSizes() throws Exception {
    RunnableObj<Step> step = UnitTestHelper.createNormalStep(user);
    AnswerValue answerValue = AnswerValueFactory.makeAnswer(step);
    Optional<AnswerFilterInstance> currentFilter = answerValue.getAnswerSpec()
      .getLegacyFilter();
    int size = answerValue.getResultSizeFactory().getResultSize();

    AnswerFilterInstance[] filters = answerValue.getAnswerSpec()
      .getQuestion()
      .getRecordClass()
      .getFilterInstances();
    for (AnswerFilterInstance filter : filters) {
      int filterSize = answerValue.getResultSizeFactory()
        .getFilterSize(filter.getName());
      if (currentFilter.isPresent() && filter.getName()
        .equals(currentFilter.get().getName())) {
        assertEquals(size, filterSize);
      }
      else {
        assertTrue(filterSize >= 0);
      }
    }
  }
}
