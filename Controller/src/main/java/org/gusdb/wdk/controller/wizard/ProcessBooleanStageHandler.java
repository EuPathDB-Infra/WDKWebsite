package org.gusdb.wdk.controller.wizard;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.apache.struts.action.ActionServlet;
import org.gusdb.wdk.controller.CConstants;
import org.gusdb.wdk.controller.action.ProcessBooleanAction;
import org.gusdb.wdk.controller.action.ProcessQuestionAction;
import org.gusdb.wdk.controller.actionutil.ActionUtility;
import org.gusdb.wdk.controller.form.QuestionForm;
import org.gusdb.wdk.controller.form.WizardForm;
import org.gusdb.wdk.model.Utilities;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.WdkUserException;
import org.gusdb.wdk.model.jspwrap.QuestionBean;
import org.gusdb.wdk.model.jspwrap.StepBean;
import org.gusdb.wdk.model.jspwrap.StrategyBean;
import org.gusdb.wdk.model.jspwrap.UserBean;
import org.gusdb.wdk.model.jspwrap.WdkModelBean;
import org.gusdb.wdk.model.user.StepUtilities;

public class ProcessBooleanStageHandler implements StageHandler {

  public static final String PARAM_QUESTION = "questionFullName";
  public static final String PARAM_CUSTOM_NAME = "customName";
  public static final String PARAM_STRATEGY = "strategy";
  public static final String PARAM_STEP = "step";
  public static final String PARAM_IMPORT_STRATEGY = "importStrategy";

  public static final String ATTR_IMPORT_STEP = ProcessBooleanAction.PARAM_IMPORT_STEP;

  private static final Logger logger = Logger.getLogger(ProcessBooleanStageHandler.class);

  @Override
  public Map<String, Object> execute(ActionServlet servlet, HttpServletRequest request,
      HttpServletResponse response, WizardForm wizardForm) throws Exception {
    logger.debug("Entering BooleanStageHandler...");

    Map<String, Object> attributes = new HashMap<String, Object>();

    UserBean user = ActionUtility.getUser(request);
    WdkModelBean wdkModel = ActionUtility.getWdkModel(servlet);

    String strStratId = request.getParameter(PARAM_STRATEGY);
    if (strStratId == null || strStratId.isEmpty())
      throw new WdkUserException("Required " + PARAM_STRATEGY + " param is missing.");

    String[] pieces = strStratId.split("_", 2);
    long strategyId = Long.valueOf(pieces[0]);
    Long branchId = (pieces.length == 1) ? null : Long.valueOf(pieces[1]);
    StrategyBean strategy = new StrategyBean(user, StepUtilities.getStrategy(user.getUser(), strategyId));

    String strStepId = request.getParameter(PARAM_STEP);
    long stepId = (strStepId == null || strStepId.isEmpty()) ? 0 : Long.valueOf(strStepId);

    logger.debug("Strategy: id=" + strategy.getStrategyId() + ", saved=" + strategy.getIsSaved());
    if (strategy.getIsSaved()) {
      Map<Long, Long> stepIdMap = new HashMap<>();
      strategy = user.copyStrategy(strategy, stepIdMap, strategy.getName());

      // make sure to also change the strategy key in the wizard form, so the new unsaved strategy can be
      // carried over the next stages.
      String strategyKey = Long.toString(strategy.getStrategyId());
      if (branchId != null)
        strategyKey += "_" + stepIdMap.get(branchId);
      wizardForm.setStrategy(strategyKey);
      attributes.put(PARAM_STRATEGY, strategyKey);

      // also replace the saved strategy with the new unsaved copy in the view
      user.replaceActiveStrategy(strategyId, strategy.getStrategyId(), stepIdMap);

      if (stepId != 0) {
        stepId = stepIdMap.get(stepId);
        attributes.put(PARAM_STEP, Long.toString(stepId));
      }
    }

    StepBean childStep = null;

    // unify between question and strategy
    String questionName = request.getParameter(PARAM_QUESTION);
    String importStrategyId = request.getParameter(PARAM_IMPORT_STRATEGY);
    if (questionName != null && questionName.length() > 0) {
      // a question name specified, either create a step from it, or revise a current step
      String action = request.getParameter(ProcessBooleanAction.PARAM_ACTION);
      if (action.equals(WizardForm.ACTION_REVISE)) {
        childStep = updateStepWithQuestion(servlet, request, wizardForm, strategy, questionName, user, stepId);
      }
      else {
        childStep = createStepFromQuestion(servlet, request, wizardForm, strategy, questionName, user,
            wdkModel);
      }
    }
    else if (importStrategyId != null && importStrategyId.length() > 0) {
      // a step specified, it must come from an insert strategy. make a
      // copy of it, and mark it as collapsable.
      childStep = createStepFromStrategy(user, strategy, Long.valueOf(importStrategyId));
    }

    String customName = request.getParameter(PARAM_CUSTOM_NAME);
    if (childStep != null && customName != childStep.getCustomName()) {
      childStep.setCustomName(customName);
      childStep.update(false);
    }

    // the childStep might not be created, in which case user just revises
    // the boolean operator.
    logger.debug("child step: " + childStep);
    if (childStep != null) {
      attributes.put(ATTR_IMPORT_STEP, childStep.getStepId());
    }
    return attributes;
  }

  public static StepBean updateStepWithQuestion(ActionServlet servlet, HttpServletRequest request,
      WizardForm wizardForm, StrategyBean strategy, String questionName, UserBean user,
      long stepId) throws WdkUserException, WdkModelException {
    logger.debug("updating step with question: " + questionName);

    // get the assigned weight
    String strWeight = request.getParameter(CConstants.WDK_ASSIGNED_WEIGHT_KEY);
    int weight = Utilities.DEFAULT_WEIGHT;
    if (strWeight != null && strWeight.length() > 0) {
      if (!strWeight.matches("[\\-\\+]?\\d+"))
        throw new WdkUserException("Invalid weight value: '" + strWeight +
            "'. Only integer numbers are allowed.");
      if (strWeight.length() > 9)
        throw new WdkUserException("Weight number is too big: " + strWeight);
      weight = Integer.parseInt(strWeight);
    }

    // get params
    QuestionForm questionForm = new QuestionForm();
    questionForm.setServlet(servlet);
    questionForm.setQuestionFullName(questionName);
    questionForm.copyFrom(wizardForm);
    Map<String, String> params = ProcessQuestionAction.prepareParams(user, request, questionForm);

    // get the boolean/span step, then get child from the boolean
    if (stepId == 0)
      throw new WdkUserException("The required param \"" + ProcessBooleanAction.PARAM_STEP + "\" is missing.");

    StepBean booleanStep = strategy.getStepById(stepId);
    StepBean childStep = booleanStep.getChildStep();

    // before changing step, need to check if strategy is saved, if yes, make a copy.
    if (strategy.getIsSaved())
      strategy.update(false);

    // revise on the child step
    childStep.setQuestionName(questionName);
    childStep.setParamValues(params);
    childStep.setAssignedWeight(weight);
    childStep.saveParamFilters();

    logger.info("step#" + childStep.getStepId() + " - " + params);

    return childStep;
  }

  private StepBean createStepFromQuestion(ActionServlet servlet, HttpServletRequest request,
      WizardForm wizardForm, StrategyBean strategy, String questionName, UserBean user, WdkModelBean wdkModel)
      throws WdkUserException, WdkModelException {
    logger.debug("creating step from question: " + questionName);

    // get the assigned weight
    String strWeight = request.getParameter(CConstants.WDK_ASSIGNED_WEIGHT_KEY);
    int weight = Utilities.DEFAULT_WEIGHT;
    if (strWeight != null && strWeight.length() > 0) {
      if (!strWeight.matches("[\\-\\+]?\\d+"))
        throw new WdkUserException("Invalid weight value: '" + strWeight +
            "'. Only integer numbers are allowed.");
      if (strWeight.length() > 9)
        throw new WdkUserException("Weight number is too big: " + strWeight);
      weight = Integer.parseInt(strWeight);
    }

    // get params
    QuestionForm questionForm = new QuestionForm();
    questionForm.setServlet(servlet);
    questionForm.setQuestionFullName(questionName);
    questionForm.copyFrom(wizardForm);
    Map<String, String> params = ProcessQuestionAction.prepareParams(user, request, questionForm);

    // create child step
    QuestionBean question = wdkModel.getQuestion(questionName);
    return user.createStep(strategy.getStrategyId(), question, params, null, false, true, weight);
  }

  public static StepBean createStepFromStrategy(UserBean user, StrategyBean newStrategy, long importStrategyId)
      throws WdkModelException, WdkUserException {
    logger.debug("creating step from strategy: " + importStrategyId);
    StrategyBean importStrategy = user.getStrategy(importStrategyId);
    StepBean step = importStrategy.getLatestStep();
    StepBean childStep = step.deepClone(newStrategy.getStrategyId(), new HashMap<Long, Long>());
    childStep.setIsCollapsible(true);
    childStep.setCollapsedName("Copy of " + importStrategy.getName());
    childStep.update(false);
    return childStep;
  }
}
