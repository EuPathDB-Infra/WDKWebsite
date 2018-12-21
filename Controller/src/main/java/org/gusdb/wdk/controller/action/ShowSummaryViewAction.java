package org.gusdb.wdk.controller.action;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.gusdb.fgputil.validation.ValidObjectFactory.RunnableObj;
import org.gusdb.wdk.controller.actionutil.ActionUtility;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.WdkUserException;
import org.gusdb.wdk.model.answer.SummaryView;
import org.gusdb.wdk.model.answer.spec.AnswerSpec;
import org.gusdb.wdk.model.jspwrap.StepBean;
import org.gusdb.wdk.model.jspwrap.UserBean;
import org.gusdb.wdk.model.user.Step;
import org.gusdb.wdk.model.user.User;

public class ShowSummaryViewAction extends SummaryViewAction {

  private static final Logger LOG = Logger.getLogger(ShowSummaryViewAction.class);

  public static final String ATTR_STEP = "wdkStep";
  public static final String ATTR_VIEW = "wdkView";
  public static final String ATTR_REQUEST_URI = "requestUri";

  @Override
  protected ActionForward handleSummaryViewRequest(
      User user,
      RunnableObj<Step> step,
      SummaryView summaryView,
      Map<String, String[]> params,
      HttpServletRequest request,
      ActionMapping mapping) throws WdkModelException, WdkUserException {

    // process the handler and add resulting view data to request scope
    RunnableObj<AnswerSpec> answerSpec = Step.getRunnableAnswerSpec(step);
    ActionUtility.applyModel(request, getHandler(summaryView).process(answerSpec, params, user));

    // set this summary view as the user's current preference
    user.getPreferences().setCurrentSummaryView(answerSpec.getObject().getQuestion(), summaryView);

    // add attributes needed by JSP
    LOG.debug("step id: " + step.getObject().getStepId());
    request.setAttribute(ATTR_STEP, new StepBean(new UserBean(user), step.getObject()));

    LOG.debug("request uri: " + request.getRequestURI());
    request.setAttribute(ATTR_REQUEST_URI, request.getRequestURI());

    LOG.debug("summary view: " + summaryView.getName());
    request.setAttribute(ATTR_VIEW, summaryView);

    LOG.debug("view=" + summaryView.getName() + ", jsp=" + summaryView.getJsp());
    ActionForward forward = new ActionForward(summaryView.getJsp());

    LOG.debug("Leaving ShowSummaryViewAction");
    return forward;
  }
}
