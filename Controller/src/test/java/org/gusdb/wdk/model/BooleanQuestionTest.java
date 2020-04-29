package org.gusdb.wdk.model;

import org.gusdb.fgputil.db.pool.DatabaseInstance;
import org.gusdb.fgputil.validation.ValidObjectFactory.RunnableObj;
import org.gusdb.wdk.model.answer.AnswerValue;
import org.gusdb.wdk.model.answer.factory.AnswerValueFactory;
import org.gusdb.wdk.model.answer.spec.AnswerSpec;
import org.gusdb.wdk.model.query.BooleanOperator;
import org.gusdb.wdk.model.query.BooleanQuery;
import org.gusdb.wdk.model.question.BooleanQuestion;
import org.gusdb.wdk.model.record.RecordClass;
import org.gusdb.wdk.model.user.Step;
import org.gusdb.wdk.model.user.StepContainer;
import org.gusdb.wdk.model.user.StepContainer.ListStepContainer;
import org.gusdb.wdk.model.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author xingao
 */
public class BooleanQuestionTest {

    private final WdkModel wdkModel;
    private final User user;
    private final DatabaseInstance appDb;

    private RecordClass recordClass;
    private AnswerValue leftAnswerValue;
    private AnswerValue rightAnswerValue;
    private RunnableObj<Step> leftStep;
    private RunnableObj<Step> rightStep;
    private StepContainer stepContainer;

    public BooleanQuestionTest() throws Exception {
        // load the model
        wdkModel = UnitTestHelper.getModel();
        //user = UnitTestHelper.getRegisteredUser();
        user = UnitTestHelper.getGuest();
        appDb = wdkModel.getAppDb();
    }

    @BeforeEach
    public void createOperands() throws Exception {
        var regUser = UnitTestHelper.getRegisteredUser();
        leftStep = UnitTestHelper.createNormalStep(regUser);
        rightStep = UnitTestHelper.createNormalStep(regUser);

        leftAnswerValue = AnswerValueFactory.makeAnswer(leftStep);
        rightAnswerValue = AnswerValueFactory.makeAnswer(rightStep);

        recordClass = leftStep.get().getAnswerSpec().getQuestion().getRecordClass();

        var container = new ListStepContainer();
        container.add(leftStep.get());
        container.add(rightStep.get());
        stepContainer = container;
    }

    private int getBooleanResultSizeWithOperator(BooleanOperator operator) throws WdkModelException {

      var booleanQuestion = new BooleanQuestion(recordClass);
      var booleanQuery = (BooleanQuery) booleanQuestion.getQuery();
      var paramValues = new LinkedHashMap<String, String>();

      var leftParam = booleanQuery.getLeftOperandParam();
      // calling answer info to make sure the answer is saved first
      paramValues.put(leftParam.getName(), String.valueOf(leftStep.get().getStepId()));

      var rightParam = booleanQuery.getRightOperandParam();
      paramValues.put(rightParam.getName(), String.valueOf(rightStep.get().getStepId()));

      var operatorParam = booleanQuery.getOperatorParam();
      paramValues.put(operatorParam.getName(), operator.getOperator(appDb.getPlatform()));

      var answerValue = AnswerValueFactory.makeAnswer(user, AnswerSpec
          .builder(wdkModel)
          .setQuestionFullName(booleanQuestion.getFullName())
          .setParamValues(paramValues)
          .buildRunnable(user, stepContainer));

      return answerValue.getResultSizeFactory().getResultSize();
    }

    @Test
    public void testOrOperator() throws WdkModelException {
      int size = getBooleanResultSizeWithOperator(BooleanOperator.UNION);
      assertTrue(size >= leftAnswerValue.getResultSizeFactory().getResultSize(),
          "bigger than left");
      assertTrue(size >= rightAnswerValue.getResultSizeFactory().getResultSize(),
          "bigger than right");
    }

    @Test
    public void testAndOperator() throws WdkModelException {
      int size = getBooleanResultSizeWithOperator(BooleanOperator.INTERSECT);
      assertTrue(size <= leftAnswerValue.getResultSizeFactory().getResultSize(),
          "smaller than left");
      assertTrue(size <= rightAnswerValue.getResultSizeFactory().getResultSize(),
          "smaller than right");
    }

    @Test
    public void testLeftMinusOperator() throws WdkModelException {
      int size = getBooleanResultSizeWithOperator(BooleanOperator.LEFT_MINUS);
      assertTrue(size <= leftAnswerValue.getResultSizeFactory().getResultSize(),
          "smaller than left");
    }

    @Test
    public void testRightMinusOperator() throws WdkModelException {
      int size = getBooleanResultSizeWithOperator(BooleanOperator.RIGHT_MINUS);
      assertTrue(size <= rightAnswerValue.getResultSizeFactory().getResultSize(),
          "smaller than right");
    }

    @Test
    public void testLeftOnlyOperator() throws WdkModelException {
      int size = getBooleanResultSizeWithOperator(BooleanOperator.LEFT_ONLY);
      assertEquals(size, leftAnswerValue.getResultSizeFactory().getResultSize(),
        "equal to left");
    }

    @Test
    public void testRightOnlyOperator() throws WdkModelException {
      int size = getBooleanResultSizeWithOperator(BooleanOperator.RIGHT_ONLY);
      assertEquals(size,
        rightAnswerValue.getResultSizeFactory().getResultSize(),
        "equal to right");
    }
}
