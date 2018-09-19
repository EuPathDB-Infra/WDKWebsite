package org.gusdb.wdk.controller.action;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.gusdb.fgputil.FormatUtil;
import org.gusdb.wdk.controller.CConstants;
import org.gusdb.wdk.controller.actionutil.ActionUtility;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.jspwrap.StrategyBean;
import org.gusdb.wdk.model.jspwrap.UserBean;
import org.gusdb.wdk.model.jspwrap.WdkModelBean;

/**
 * This Action handles moving a step in a search strategy to a different position. It moves the step, updates
 * the relevant filter userAnswers, and forwards to ShowSummaryAction.
 * 
 * Jerric - deprecated since the moving of a step is very complex, especially with transforms and other
 * combined steps that can change result types. Instead of fixing the code that has never been used, I will
 * just mark it as deprecated.
 * 
 **/
@Deprecated
public class MoveStepAction extends ProcessFilterAction {

  private static final Logger logger = Logger.getLogger(MoveStepAction.class);

  @Override
  public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request,
      HttpServletResponse response) throws Exception {
    logger.debug("Entering MoveStepAction...");

    UserBean wdkUser = ActionUtility.getUser(request);
    WdkModelBean wdkModel = ActionUtility.getWdkModel(servlet);
    try {
      String state = request.getParameter(CConstants.WDK_STATE_KEY);

      // Make sure strategy, step, and moveto are defined
      String strStratId = request.getParameter(CConstants.WDK_STRATEGY_ID_KEY);
      //String strBranchId = null;
      String strMoveFromId = request.getParameter("movefrom");
      String op = request.getParameter("op");
      String strMoveToId = request.getParameter("moveto");

      // Make sure necessary arguments are provided
      if (strStratId == null || strStratId.length() == 0) {
        throw new WdkModelException("No strategy was specified for moving steps!");
      }
      if (strMoveFromId == null || strMoveFromId.length() == 0) {
        throw new WdkModelException("No step was specified for moving!");
      }
      else if (op == null || op.length() == 0) {
        throw new WdkModelException("No operation specified for moving first step.");
      }
      if (strMoveToId == null || strMoveToId.length() == 0) {
        throw new WdkModelException("No destination was specified for moving!");
      }

      if (strStratId.indexOf("_") > 0) {
        //strBranchId = strStratId.split("_")[1];
        strStratId = strStratId.split("_")[0];
      }

      StrategyBean strategy = wdkUser.getStrategy(Integer.parseInt(strStratId));

      // verify the checksum
      String checksum = request.getParameter(CConstants.WDK_STRATEGY_CHECKSUM_KEY);
      if (checksum != null && !strategy.getChecksum().equals(checksum)) {
        ShowStrategyAction.outputOutOfSyncJSON(wdkModel, wdkUser, response, state);
        return null;
      }

      // Forward to ShowStrategyAction
      ActionForward showStrategy = mapping.findForward(CConstants.SHOW_STRATEGY_MAPKEY);
      StringBuffer url = new StringBuffer(showStrategy.getPath());
      url.append("?state=" + FormatUtil.urlEncodeUtf8(state));
      ActionForward forward = new ActionForward(url.toString());
      forward.setRedirect(true);
      return forward;
    }
    catch (Exception ex) {
      logger.error(ex);
      ex.printStackTrace();
      ShowStrategyAction.outputErrorJSON(wdkUser, response, ex);
      return null;
    }
  }
}
