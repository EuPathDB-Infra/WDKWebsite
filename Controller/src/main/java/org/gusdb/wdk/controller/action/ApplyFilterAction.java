package org.gusdb.wdk.controller.action;

import static org.gusdb.fgputil.FormatUtil.NL;
import static org.gusdb.fgputil.FormatUtil.join;
import static org.gusdb.wdk.model.user.StepContainer.withId;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.gusdb.fgputil.Tuples.ThreeTuple;
import org.gusdb.fgputil.validation.ValidObjectFactory.RunnableObj;
import org.gusdb.fgputil.validation.ValidationLevel;
import org.gusdb.wdk.controller.CConstants;
import org.gusdb.wdk.controller.actionutil.ActionUtility;
import org.gusdb.wdk.model.WdkModel;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.WdkUserException;
import org.gusdb.wdk.model.answer.spec.AnswerSpec;
import org.gusdb.wdk.model.answer.spec.AnswerSpecBuilder;
import org.gusdb.wdk.model.answer.spec.FilterOption;
import org.gusdb.wdk.model.user.Step;
import org.gusdb.wdk.model.user.Strategy;
import org.gusdb.wdk.model.user.StrategyLoader;
import org.gusdb.wdk.model.user.User;
import org.json.JSONArray;
import org.json.JSONObject;

public class ApplyFilterAction extends Action {

  public static final String PARAM_FILTER = "filter";
  public static final String PARAM_STEP = "step";

  private static final Logger LOG = Logger.getLogger(ApplyFilterAction.class);

  @Override
  public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request,
      HttpServletResponse response) throws Exception {
    LOG.debug("Entering ApplyFilterAction...");
    try {
      ThreeTuple<Step, String, JSONObject> inputs = getInputs(request);
      Step step = inputs.getFirst();
      String filterName = inputs.getSecond();
      JSONObject filterValue = inputs.getThird();
      User user = ActionUtility.getUser(request).getUser();
      WdkModel wdkModel = ActionUtility.getWdkModel(servlet).getModel();

      // before changing step, need to check if strategy is saved, if yes, make a copy.
      Strategy strategy = step.getStrategy();
      if (strategy.isSaved()) {
        // cannot modify saved strategy directly, will need to create a copy, and change the steps of the copy instead
        Map<Long, Long> stepIdMap = new HashMap<>();
        strategy = wdkModel.getStepFactory().copyStrategy(strategy, stepIdMap);
        // map the old step to the new one
        step = strategy.findFirstStep(withId(stepIdMap.get(step.getStepId())));

        // FIXME: rrd - in this case, I think we need to replace the existing active strategy with the new
        //        one.  Unless this is handled elsewhere, the user will continue to see their (unmodified)
        //        saved strategy while their modified, unsaved strat will only be in the all tab
      }

      // Create a new answer spec containing the new filter
      RunnableObj<AnswerSpec> newSpec = AnswerSpec
          .builder(step.getAnswerSpec())
          .replaceFirstFilterOption(filterName, filter -> filter.setValue(filterValue))
          .build(user, strategy, ValidationLevel.RUNNABLE)
          .getRunnable()
          .getOrThrow(spec -> new WdkUserException("Invalid filter value.  " +
              "The following errors were found:" + NL +
              join(spec.getValidationBundle().getAllErrors(), NL)));

      step.setAnswerSpec(newSpec.getObject());
      step.writeParamFiltersToDb();

      ActionForward showApplication = mapping.findForward(CConstants.SHOW_APPLICATION_MAPKEY);

      LOG.debug("Foward to " + CConstants.SHOW_APPLICATION_MAPKEY + ", " + showApplication);

      StringBuffer url = new StringBuffer(showApplication.getPath());

      ActionForward forward = new ActionForward(url.toString());
      forward.setRedirect(true);
      LOG.debug("Leaving ApplyFilterAction.");
      return forward;
    }
    catch (Exception ex) {
      LOG.error(ex.getMessage(), ex);
      throw ex;
    }
  }

  private ThreeTuple<Step, String, JSONObject> getInputs(HttpServletRequest request) throws WdkUserException, WdkModelException {
    String filterName = request.getParameter(PARAM_FILTER);
    if (filterName == null || filterName.trim().isEmpty()) {
      throw new WdkUserException("Required filter parameter is missing.");
    }
    String strStepId = request.getParameter(PARAM_STEP);
    if (strStepId == null)
      throw new WdkUserException("Required step parameter is missing.");
    long stepId = Long.valueOf(strStepId);
    User user = ActionUtility.getUser(request).getUser();
    Step step = new StrategyLoader(ActionUtility.getWdkModel(servlet).getModel(), ValidationLevel.RUNNABLE).getStepById(stepId)
        .orElseThrow(() -> new WdkUserException("Step parameter does not contain a valid step ID."));
    if (user.getUserId() != step.getUser().getUserId()) {
      throw new WdkUserException("You do not have permission to modifiy this step.");
    }
    if (!step.isValid()) {
      throw new WdkUserException("New filters can only be applied to valid steps.");
    }
    if (step.getAnswerSpec().getQuestion().getFilterOrNull(filterName) == null) {
      throw new WdkUserException("Filter '" + filterName + "' cannot be applied to step with question '" + step.getAnswerSpec().getQuestionName());
    }
    JSONObject filterValue = prepareOptions(request);
    LOG.debug("Got filter: " + filterName + ", options=" + filterValue);
    return new ThreeTuple<>(step, filterName, filterValue);
  }

  private JSONObject prepareOptions(HttpServletRequest request) {
    JSONObject jsOptions = new JSONObject();
    Enumeration<?> names = request.getParameterNames();
    while (names.hasMoreElements()) {
      String name = (String)names.nextElement();
      if (name.equals(PARAM_FILTER) || name.equals(PARAM_STEP))
        continue;
      String[] values = request.getParameterValues(name);
      JSONArray jsValues = new JSONArray();
      for (String value : values) {
        jsValues.put(value);
      }
      jsOptions.put(name, jsValues);
    }
    return jsOptions;
  }
}
