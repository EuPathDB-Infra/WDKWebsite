/**
 * 
 */
package org.gusdb.wdk.controller.action;

import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.gusdb.fgputil.FormatUtil;
import org.gusdb.wdk.controller.CConstants;
import org.gusdb.wdk.controller.actionutil.ActionUtility;
import org.gusdb.wdk.model.jspwrap.StrategyBean;
import org.gusdb.wdk.model.jspwrap.UserBean;
import org.gusdb.wdk.model.jspwrap.WdkModelBean;

/**
 * @author ctreatma
 * 
 */
public class CopyStrategyAction extends Action {

  private static Logger logger = Logger.getLogger(CopyStrategyAction.class);

  @Override
  public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request,
      HttpServletResponse response) throws Exception {
    logger.debug("Entering Copy Strategy...");

    UserBean wdkUser = ActionUtility.getUser(request);
    WdkModelBean wdkModel = ActionUtility.getWdkModel(servlet);
    try {
      String state = request.getParameter(CConstants.WDK_STATE_KEY);

      String strStratId = request.getParameter(CConstants.WDK_STRATEGY_ID_KEY);

      if (strStratId == null || strStratId.length() == 0) {
        throw new Exception("No Strategy was given for saving");
      }

      int stratId = Integer.parseInt(strStratId);
      StrategyBean strategy = wdkUser.getStrategy(stratId);

      // verify the checksum
      String checksum = request.getParameter(CConstants.WDK_STRATEGY_CHECKSUM_KEY);
      if (checksum != null && !strategy.getChecksum().equals(checksum)) {
        ShowStrategyAction.outputOutOfSyncJSON(wdkModel, wdkUser, response, state);
        return null;
      }
      boolean opened = (wdkUser.getStrategyOrder(strStratId) > 0);

      StrategyBean copy = new StrategyBean(wdkUser,
          wdkModel.getModel().getStepFactory().copyStrategy(strategy.getStrategy(), new HashMap<Long, Long>()));

      // forward to strategyPage.jsp
      ActionForward showStrategy = mapping.findForward(CConstants.SHOW_STRATEGY_MAPKEY);
      StringBuffer url = new StringBuffer(showStrategy.getPath());
      url.append("?state=" + FormatUtil.urlEncodeUtf8(state));

      url.append("&").append(CConstants.WDK_STRATEGY_ID_KEY);
      url.append("=").append(copy.getStrategyId());

      if (!opened)
        url.append("&").append(CConstants.WDK_OPEN_KEY).append("=false");

      ActionForward forward = new ActionForward(url.toString());
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
