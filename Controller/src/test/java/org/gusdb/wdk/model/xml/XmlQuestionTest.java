package org.gusdb.wdk.model.xml;

import org.gusdb.wdk.model.UnitTestHelper;
import org.gusdb.wdk.model.WdkModel;
import org.gusdb.wdk.model.WdkModelException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Jerric
 */
public class XmlQuestionTest {

  private final WdkModel wdkModel;

  public XmlQuestionTest() throws Exception {
    wdkModel = UnitTestHelper.getModel();
  }

  /**
   * test reading questions from the model
   */
  @Test
  public void testGetAllXmlQuestions() {
    // validate the references to questions
    for (XmlQuestionSet questionSet : wdkModel.getXmlQuestionSets()) {
      String setName = questionSet.getName();
      assertTrue(setName.trim().length() > 0, "set name");

      XmlQuestion[] questions = questionSet.getQuestions();
      assertTrue(questions.length > 0, "question count > 0");
      for (XmlQuestion question : questions) {
        String qName = question.getName();
        assertTrue(qName.trim().length() > 0, "name");

        // the question must have reference to record class
        assertNotNull(question.getRecordClass(), "record class");
        assertNotNull(question.getDisplayName(), "display name");
        assertEquals(setName, question.getQuestionSet().getName(),
          "question set");
        String fullName = question.getFullName();
        assertTrue(fullName.startsWith(setName), "fullName starts with");
        assertTrue(fullName.endsWith(qName), "fullName ends with");
        assertNotNull(question.getXmlDataURL(), "data url");
      }
    }
  }

  /**
   * The question set belongs to toy db, not sample db; although it is defined
   * in the same model
   */
  @Test
  public void testGetInvalidXmlQuestionSet() {
    String qsetName = "NonexistXmlQuestions";
    assertThrows(WdkModelException.class,
      () -> wdkModel.getXmlQuestionSet(qsetName));
  }

  /**
   * get a known question, and verify its description
   */
  @Test
  public void testGetXmlQuestionSet() throws WdkModelException {
    for (XmlQuestionSet questionSet : wdkModel.getXmlQuestionSets()) {
      String qsetName = questionSet.getName();
      XmlQuestionSet qset = wdkModel.getXmlQuestionSet(qsetName);
      assertEquals(qsetName, qset.getName());
    }
  }

  /**
   * Test getting question and its properties
   */
  @Test
  public void testGetXmlQuestion() throws WdkModelException {
    for (XmlQuestionSet questionSet : wdkModel.getXmlQuestionSets()) {
      for (XmlQuestion question : questionSet.getQuestions()) {
        String name = question.getName();
        XmlQuestion q = questionSet.getQuestion(name);
        assertEquals(name, q.getName(), "by name");

        String fullName = question.getFullName();
        q = (XmlQuestion) wdkModel.resolveReference(fullName);
        assertEquals(fullName, q.getFullName(), "by fullName");
      }
    }
  }

  /**
   * the question is excluded from the Sample DB
   */
  @Test
  public void testGetInvalidXmlQuestion() {
    String qName = "NonexistXmlQuestion";
    assertThrows(WdkModelException.class, () -> {
      for (XmlQuestionSet questionSet : wdkModel.getXmlQuestionSets()) {
        questionSet.getQuestion(qName);
      }
    });
  }

  /**
   * the question is excluded from the Sample DB
   */
  @Test
  public void testGetInvalidXmlQuestionByFull() {
    String fullName = "NonexistXmlQuestionSet.NonexistXmlQuestion";
    assertThrows(WdkModelException.class,
      () -> wdkModel.resolveReference(fullName));
  }

}
