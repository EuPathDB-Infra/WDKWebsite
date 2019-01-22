package org.gusdb.wdk.model.user;

import java.util.Arrays;
import java.util.Collection;

import org.gusdb.wdk.model.WdkModel;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.query.param.AnswerParam;
import org.gusdb.wdk.model.query.param.MockAnswerParam;

public class MockBooleanStep extends MockStep {

  private final AnswerParam previousParam;
  private final AnswerParam childParam;

  public MockBooleanStep(WdkModel wdkModel, User user, Collection<String> previousTypes,
      Collection<String> childTypes, String outType) {
    super(wdkModel, user, outType);
    previousParam = new MockAnswerParam(previousTypes);
    childParam = new MockAnswerParam(childTypes);
  }

  public MockBooleanStep(WdkModel wdkModel, User user, Step previousStep, Step childStep, String outType)
      throws WdkModelException {
    super(wdkModel, user, outType);
    previousParam = new MockAnswerParam(Arrays.asList(previousStep.getType()));
    childParam = new MockAnswerParam(Arrays.asList(childStep.getType()));
    setPreviousStep(previousStep);
    setChildStep(childStep);
  }

  @Override
  public boolean isFirstStep() {
    return false;
  }

  @Override
  public boolean isCombined() {
    return true;
  }

  @Override
  public boolean isTransform() {
    return false;
  }

  @Override
  public AnswerParam getPrimaryInputStepParam() {
    return previousParam;
  }

  @Override
  public AnswerParam getSecondaryInputStepParam() {
    return childParam;
  }
}
