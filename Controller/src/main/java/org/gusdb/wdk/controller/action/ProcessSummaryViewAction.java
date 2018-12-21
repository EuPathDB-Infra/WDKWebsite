package org.gusdb.wdk.controller.action;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.gusdb.fgputil.validation.ValidObjectFactory.RunnableObj;
import org.gusdb.wdk.controller.CConstants;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.WdkUserException;
import org.gusdb.wdk.model.answer.SummaryView;
import org.gusdb.wdk.model.answer.spec.AnswerSpec;
import org.gusdb.wdk.model.record.RecordClass;
import org.gusdb.wdk.model.user.Step;
import org.gusdb.wdk.model.user.User;

public class ProcessSummaryViewAction extends SummaryViewAction {

  private static Logger LOG = Logger.getLogger(ProcessSummaryViewAction.class);

  private static final String FORWARD_SHOW_SUMMARY_VIEW = "show-summary-view";

  @Override
  protected ActionForward handleSummaryViewRequest(
      User user,
      RunnableObj<Step> step,
      SummaryView summaryView,
      Map<String, String[]> params,
      HttpServletRequest request,
      ActionMapping mapping) throws WdkModelException, WdkUserException {

    RunnableObj<AnswerSpec> answerSpec = Step.getRunnableAnswerSpec(step);
    String newQueryString = getHandler(summaryView).processUpdate(answerSpec, params, user);

    if (isRequestFromBasket(request)) {
      return getBasketForward(mapping, step.getObject().getRecordClass());
    }

    // construct url to show summary action
    ActionForward showSummaryView = mapping.findForward(FORWARD_SHOW_SUMMARY_VIEW);
    StringBuilder url = new StringBuilder(showSummaryView.getPath())
        .append("?step=").append(step.getObject().getStepId())
        .append("&view=").append(summaryView.getName());

    // append handler's new query string if present
    if (newQueryString != null && !newQueryString.isEmpty()) {
      url.append(newQueryString.startsWith("&") ? "" : "&")
         .append(newQueryString);
    }

    ActionForward forward = new ActionForward(url.toString());
    forward.setRedirect(true);
    return forward;
  }

  private ActionForward getBasketForward(ActionMapping mapping, RecordClass recordClass) {
    ActionForward showBasket = mapping.findForward(CConstants.PQ_SHOW_BASKET_MAPKEY);
    StringBuilder url = new StringBuilder(showBasket.getPath());
    url.append("?recordClass=" + recordClass.getFullName());
    return new ActionForward(url.toString());
  }

  private boolean isRequestFromBasket(HttpServletRequest request) {
    String strBasket = request.getParameter("from_basket");
    boolean fromBasket = (strBasket != null && strBasket.equals("true"));
    LOG.debug("to basket: " + fromBasket);
    return fromBasket;
  }
}
