package org.gusdb.wdk.model;

import java.util.LinkedHashMap;
import java.util.Map;

import org.gusdb.fgputil.db.pool.DatabaseInstance;
import org.gusdb.wdk.model.answer.AnswerValue;
import org.gusdb.wdk.model.query.BooleanOperator;
import org.gusdb.wdk.model.query.BooleanQuery;
import org.gusdb.wdk.model.query.param.AnswerParam;
import org.gusdb.wdk.model.query.param.StringParam;
import org.gusdb.wdk.model.question.Question;
import org.gusdb.wdk.model.record.RecordClass;
import org.gusdb.wdk.model.user.Step;
import org.gusdb.wdk.model.user.User;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * @author xingao
 * 
 */
public class BooleanQuestionTest {

    private WdkModel wdkModel;
    private User user;
    private DatabaseInstance appDb;

    private RecordClass recordClass;
    private AnswerValue leftAnswerValue;
    private AnswerValue rightAnswerValue;
    private String leftStepId;
    private String rightStepId;

    public BooleanQuestionTest() throws Exception {
        // load the model
        wdkModel = UnitTestHelper.getModel();
        //user = UnitTestHelper.getRegisteredUser();
        user = UnitTestHelper.getGuest();
        appDb = wdkModel.getAppDb();
    }

    @Before
    public void createOperands() throws Exception {
        User regUser = UnitTestHelper.getRegisteredUser();
        Step left = UnitTestHelper.createNormalStep(regUser);
        Step right = UnitTestHelper.createNormalStep(regUser);

        leftStepId = Long.toString(left.getStepId());
        rightStepId = Long.toString(right.getStepId());
        leftAnswerValue = left.getAnswerValue();
        rightAnswerValue = right.getAnswerValue();
        recordClass = left.getQuestion().getRecordClass();
    }

    @Test
    public void testOrOperator() throws WdkModelException, WdkUserException {
        Question booleanQuestion = wdkModel.getBooleanQuestion(recordClass);
        BooleanQuery booleanQuery = (BooleanQuery) booleanQuestion.getQuery();
        Map<String, String> paramValues = new LinkedHashMap<String, String>();

        AnswerParam leftParam = booleanQuery.getLeftOperandParam();
        // calling answer info to make sure the answer is saved first
        paramValues.put(leftParam.getName(), leftStepId);

        AnswerParam rightParam = booleanQuery.getRightOperandParam();
        paramValues.put(rightParam.getName(), rightStepId);

        StringParam operator = booleanQuery.getOperatorParam();
        paramValues.put(operator.getName(),
                BooleanOperator.UNION.getOperator(appDb.getPlatform()));

        AnswerValue answerValue = booleanQuestion.makeAnswerValue(user,
                paramValues, true, 0);
        int size = answerValue.getResultSizeFactory().getResultSize();

        Assert.assertTrue("bigger than left",
                size >= leftAnswerValue.getResultSizeFactory().getResultSize());
        Assert.assertTrue("bigger than right",
                size >= rightAnswerValue.getResultSizeFactory().getResultSize());
    }

    @Test
    public void testAndOperator() throws WdkModelException, WdkUserException {
        Question booleanQuestion = wdkModel.getBooleanQuestion(recordClass);
        BooleanQuery booleanQuery = (BooleanQuery) booleanQuestion.getQuery();
        Map<String, String> paramValues = new LinkedHashMap<String, String>();

        AnswerParam leftParam = booleanQuery.getLeftOperandParam();
        // calling answer info to make sure the answer is saved first
        paramValues.put(leftParam.getName(), leftStepId);

        AnswerParam rightParam = booleanQuery.getRightOperandParam();
        paramValues.put(rightParam.getName(), rightStepId);

        StringParam operator = booleanQuery.getOperatorParam();
        paramValues.put(operator.getName(),
                BooleanOperator.INTERSECT.getOperator(appDb.getPlatform()));

        AnswerValue answerValue = booleanQuestion.makeAnswerValue(user,
                paramValues, true, 0);
        int size = answerValue.getResultSizeFactory().getResultSize();

        Assert.assertTrue("smaller than left",
                size <= leftAnswerValue.getResultSizeFactory().getResultSize());
        Assert.assertTrue("smaller than right",
                size <= rightAnswerValue.getResultSizeFactory().getResultSize());
    }

    @Test
    public void testLeftMinusOperator() throws WdkModelException, WdkUserException {
        Question booleanQuestion = wdkModel.getBooleanQuestion(recordClass);
        BooleanQuery booleanQuery = (BooleanQuery) booleanQuestion.getQuery();
        Map<String, String> paramValues = new LinkedHashMap<String, String>();

        AnswerParam leftParam = booleanQuery.getLeftOperandParam();
        // calling answer info to make sure the answer is saved first
        paramValues.put(leftParam.getName(), leftStepId);

        AnswerParam rightParam = booleanQuery.getRightOperandParam();
        paramValues.put(rightParam.getName(), rightStepId);

        StringParam operator = booleanQuery.getOperatorParam();
        paramValues.put(operator.getName(),
                BooleanOperator.LEFT_MINUS.getOperator(appDb.getPlatform()));

        AnswerValue answerValue = booleanQuestion.makeAnswerValue(user,
                paramValues, true, 0);
        int size = answerValue.getResultSizeFactory().getResultSize();

        Assert.assertTrue("smaller than left",
                size <= leftAnswerValue.getResultSizeFactory().getResultSize());
    }

    @Test
    public void testRightMinueOperator() throws WdkModelException, WdkUserException {
        Question booleanQuestion = wdkModel.getBooleanQuestion(recordClass);
        BooleanQuery booleanQuery = (BooleanQuery) booleanQuestion.getQuery();
        Map<String, String> paramValues = new LinkedHashMap<String, String>();

        AnswerParam leftParam = booleanQuery.getLeftOperandParam();
        // calling answer info to make sure the answer is saved first
        paramValues.put(leftParam.getName(), leftStepId);

        AnswerParam rightParam = booleanQuery.getRightOperandParam();
        paramValues.put(rightParam.getName(), rightStepId);

        StringParam operator = booleanQuery.getOperatorParam();
        paramValues.put(operator.getName(),
                BooleanOperator.INTERSECT.getOperator(appDb.getPlatform()));

        AnswerValue answerValue = booleanQuestion.makeAnswerValue(user,
                paramValues, true, 0);
        int size = answerValue.getResultSizeFactory().getResultSize();

        Assert.assertTrue("smaller than right",
                size <= rightAnswerValue.getResultSizeFactory().getResultSize());
    }
}
