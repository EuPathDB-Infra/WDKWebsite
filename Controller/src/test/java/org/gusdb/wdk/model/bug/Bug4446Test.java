package org.gusdb.wdk.model.bug;

import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.log4j.Logger;
import org.gusdb.wdk.model.UnitTestHelper;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.query.BooleanOperator;
import org.gusdb.wdk.model.query.param.AnswerParam;
import org.gusdb.wdk.model.query.param.Param;
import org.gusdb.wdk.model.query.param.ParamValuesSet;
import org.gusdb.wdk.model.question.Question;
import org.gusdb.wdk.model.record.RecordClass;
import org.gusdb.wdk.model.test.ParamValuesFactory;
import org.gusdb.wdk.model.user.Step;
import org.gusdb.wdk.model.user.StepUtilities;
import org.gusdb.wdk.model.user.Strategy;
import org.gusdb.wdk.model.user.User;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author jerric
 * 
 *         revising search upstream of named search reverts name.
 * 
 *         at least in toxodb when the renamed search is an ortholog transform. Don't know if this is only
 *         when the immediate upstream step is revised or whether it just affects the ortholog transform.
 */
public class Bug4446Test {

  private static final String CUSTOM_NAME = "My Custom Transform Step";
  private static final Logger logger = Logger.getLogger(Bug4446Test.class);

  private User user;
  private Random random = UnitTestHelper.getRandom();

  public Bug4446Test() throws Exception {
    user = UnitTestHelper.getRegisteredUser();
  }

  @Test
  public void testRevise() throws Exception {
    // create a boolean step
    Step booleanStep = createBooleanStep();

    // use the boolean as the input of a transform
    Step transformStep = createTransformStep(booleanStep);
    if (transformStep == null)
      return; // no transform exists, skip the test.

    // set the custom name
    transformStep.setCustomName(CUSTOM_NAME);
    transformStep.update(false);
    Strategy strategy = StepUtilities.createStrategy(transformStep, false);
    logger.debug("name before revising: " + strategy.getLatestStep().getCustomName());

    // now revise the previous step of the boolean
    Step previousStep = booleanStep.getPreviousStep();
    // TODO - change the param values of the previousStep
    previousStep.saveParamFilters();

    // check the custom name, see if it is preserved
    Step newTransformStep = strategy.getLatestStep();

    Assert.assertEquals(CUSTOM_NAME, newTransformStep.getCustomName());
  }

  private Step createBooleanStep() throws Exception {
    Step leftOperand = UnitTestHelper.createNormalStep(user);
    Step rightOperand = UnitTestHelper.createNormalStep(user);

    return StepUtilities.createBooleanStep(user, leftOperand.getStrategyId(), leftOperand, rightOperand,
        BooleanOperator.UNION, null);
  }

  private Step createTransformStep(Step inputStep) throws WdkModelException {
    // look for a transform question
    RecordClass recordClass = inputStep.getQuestion().getRecordClass();
    Question[] questions = recordClass.getTransformQuestions(true);
    if (questions.length == 0)
      return null;

    Question question = questions[random.nextInt(questions.length)];

    // create a step from the question, using the default values
    List<ParamValuesSet> paramValueSets = ParamValuesFactory.getParamValuesSets(user, question.getQuery());
    ParamValuesSet paramValueSet = paramValueSets.get(random.nextInt(paramValueSets.size()));
    Map<String, String> values = paramValueSet.getParamValues();

    // set the input of answerParam using the inputStep
    Param[] params = question.getParams();
    for (Param param : params) {
      if (param instanceof AnswerParam) {
        String inputValue = Long.toString(inputStep.getStepId());
        values.put(param.getName(), inputValue);
      }
    }

    return StepUtilities.createStep(user, inputStep.getStrategyId(), question, values, (String) null, false, false, 0);
  }
}
