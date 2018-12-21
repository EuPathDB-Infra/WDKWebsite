package org.gusdb.wdk.controller.action;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.gusdb.fgputil.validation.ValidObjectFactory.RunnableObj;
import org.gusdb.fgputil.validation.ValidationLevel;
import org.gusdb.wdk.controller.actionutil.ActionUtility;
import org.gusdb.wdk.controller.summary.DefaultSummaryViewHandler;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.WdkUserException;
import org.gusdb.wdk.model.answer.SummaryView;
import org.gusdb.wdk.model.answer.SummaryViewHandler;
import org.gusdb.wdk.model.question.Question;
import org.gusdb.wdk.model.user.Step;
import org.gusdb.wdk.model.user.User;

public abstract class SummaryViewAction extends Action {

  private static final Logger LOG = Logger.getLogger(SummaryViewAction.class);

  private static final String PARAM_STEP = "step";
  private static final String PARAM_VIEW = "view";

  protected abstract ActionForward handleSummaryViewRequest(
      User user,
      RunnableObj<Step> step,
      SummaryView summaryView,
      Map<String, String[]> params,
      HttpServletRequest request,
      ActionMapping mapping) throws WdkModelException, WdkUserException;

  @Override
  public ActionForward execute(ActionMapping mapping, ActionForm form,
      HttpServletRequest request, HttpServletResponse response)
          throws Exception {
    LOG.debug("Entering " + getClass().getName() + "...");
    User user = ActionUtility.getUser(request).getUser();
    RunnableObj<Step> step = getStep(user, request.getParameter(PARAM_STEP));
    SummaryView summaryView = getSummaryView(step, request);
    Map<String, String[]> params = getParamMap(request);
    return handleSummaryViewRequest(user, step, summaryView, params, request, mapping);
  }

  protected static SummaryViewHandler getHandler(SummaryView summaryView) throws WdkModelException {
    SummaryViewHandler handler = summaryView.getHandlerInstance();
    if (handler == null) {
      handler = new DefaultSummaryViewHandler();
    }
    return handler;
  }

  private static RunnableObj<Step> getStep(User user, String stepIdStr) throws WdkUserException, WdkModelException {
    long stepId = getStepId(stepIdStr);
    return user.getWdkModel().getStepFactory()
        .getStepById(stepId, ValidationLevel.RUNNABLE)
        .orElseThrow(() -> new WdkUserException("No step with ID " + stepId + " can be found."))
        .getRunnable()
        .getOrThrow(spec -> new WdkUserException("Cannot process summary " +
            "view for invalid step." + spec.getValidationBundle().toString()));
  }

  private static long getStepId(String parameterValue) throws WdkUserException {
    if (parameterValue == null || parameterValue.length() == 0)
        throw new WdkUserException("Required step parameter is missing.");
    try {
      return Long.valueOf(parameterValue);
    }
    catch(NumberFormatException ex) {
      throw new WdkUserException("The step id is invalid: " + parameterValue);
    }
  }

  private static SummaryView getSummaryView(RunnableObj<Step> step, HttpServletRequest request) throws WdkUserException {
    Question question = step.getObject().getAnswerSpec().getQuestion();
    String viewName = request.getParameter(PARAM_VIEW);
    if (viewName == null)
      throw new WdkUserException("Parameter " + PARAM_VIEW + " is required.");
    SummaryView view = question.getSummaryViews().get(viewName);
    if (view == null)
      throw new WdkUserException("Question " + question.getName() + " has no summary view named " + view);
    return view;
  }

  private static Map<String, String[]> getParamMap(HttpServletRequest request) {
    @SuppressWarnings("unchecked")
    Map<String, String[]> params = new HashMap<>(request.getParameterMap());
    params.remove(PARAM_STEP);
    params.remove(PARAM_VIEW);
    return params;
  }
}
