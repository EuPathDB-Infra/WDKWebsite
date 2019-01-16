package org.gusdb.wdk.model.query.param;

import java.util.LinkedHashMap;
import java.util.Map;

import org.gusdb.wdk.model.UnitTestHelper;
import org.gusdb.wdk.model.WdkModel;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.WdkUserException;
import org.gusdb.wdk.model.answer.AnswerValue;
import org.gusdb.wdk.model.question.Question;
import org.gusdb.wdk.model.question.QuestionSet;
import org.gusdb.wdk.model.user.Step;
import org.gusdb.wdk.model.user.User;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author xingao
 * 
 */
public class AnswerParamTest {

  private User _user;
  Question _question;

  public AnswerParamTest() throws WdkModelException, WdkUserException {
    _user = UnitTestHelper.getRegisteredUser();
    WdkModel wdkModel = UnitTestHelper.getModel();
    for (QuestionSet questionSet : wdkModel.getAllQuestionSets()) {
      for (Question question : questionSet.getQuestions()) {
        if (question.getQuery().isTransform()) {
          this._question = question;
          return;
        }
      }
    }
  }

  @Test
  public void testGetAnswerValue() throws WdkModelException, WdkUserException {
    User user = UnitTestHelper.getRegisteredUser();
    Step step = UnitTestHelper.createNormalStep(user);
    String paramValue = Long.toString(step.getStepId());

    for (Param param : _question.getParams()) {
      if (param instanceof AnswerParam) {
        AnswerParam answerParam = (AnswerParam) param;
        Step step1 = (Step) answerParam.getRawValue(user, paramValue);

        Assert.assertEquals("raw value", step.getStepId(), step1.getStepId());
      }
    }
  }

  @Test
  public void testUseAnswerParam() throws Exception {
    Map<String, String> paramValues = new LinkedHashMap<String, String>();
    for (Param param : _question.getParams()) {
      String paramValue;
      if (param instanceof AnswerParam) {
        Step step = UnitTestHelper.createNormalStep(_user);
        paramValue = Long.toString(step.getStepId());
      }
      else
        paramValue = param.getDefault();
      paramValues.put(param.getName(), paramValue);
    }
    AnswerValue answerValue = _question.makeAnswerValue(_user, paramValues, true, 0);

    Assert.assertTrue("result size", answerValue.getResultSizeFactory().getResultSize() >= 0);
  }
}
