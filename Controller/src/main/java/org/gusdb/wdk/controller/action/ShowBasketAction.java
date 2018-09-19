package org.gusdb.wdk.controller.action;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.gusdb.wdk.controller.CConstants;
import org.gusdb.wdk.controller.actionutil.ActionUtility;
import org.gusdb.wdk.model.Utilities;
import org.gusdb.wdk.model.jspwrap.QuestionBean;
import org.gusdb.wdk.model.jspwrap.RecordClassBean;
import org.gusdb.wdk.model.jspwrap.StepBean;
import org.gusdb.wdk.model.jspwrap.UserBean;
import org.gusdb.wdk.model.jspwrap.WdkModelBean;

/**
 * This action is called by the UI in order to "close" a strategy. It removes the specified strategy id from
 * the strategy id list stored in the session.
 */

public class ShowBasketAction extends Action {

  private static final String PARAM_RECORD_CLASS = "recordClass";
  private static final String MAPKEY_SHOW_BASKET = "showBasket";
  private static final String BASKET_MENUBAR_PAGE = "basketMenu.jsp";

  private static Logger logger = Logger.getLogger(ShowBasketAction.class);

  @Override
  public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request,
      HttpServletResponse response) throws Exception {
    logger.debug("Entering ShowBasketAction...");

    UserBean user = ActionUtility.getUser(request);
    WdkModelBean wdkModel = ActionUtility.getWdkModel(servlet);
    try {
      String rcName = request.getParameter(PARAM_RECORD_CLASS);
      String path;

      if (rcName != null && rcName.trim().length() > 0) {
        // A RecordClass was specified, so load that basket to be
        // displayed as a result page
        RecordClassBean recordClass = wdkModel.findRecordClass(rcName);
        QuestionBean question = recordClass.getRealtimeBasketQuestion();
        // the only param is user_id, the value of which is added automatically in QueryInstance
        Map<String, String> params = new LinkedHashMap<String, String>();
        StepBean step = user.createStep(null, question, params, null, true, false, Utilities.DEFAULT_WEIGHT);

        ActionForward forward = mapping.findForward(MAPKEY_SHOW_BASKET);
        path = forward.getPath() + "?" + CConstants.WDK_RESULT_SET_ONLY_KEY + "=true&" +
            CConstants.WDK_STEP_ID_PARAM + "=" + step.getStepId();
      }
      else {
        // No RecordClass was specified, load the basket count only for
        // all records
        Map<RecordClassBean, Integer> baskets = user.getBasketCounts();

        path = CConstants.WDK_DEFAULT_VIEW_DIR + File.separator + CConstants.WDK_PAGES_DIR + File.separator +
            BASKET_MENUBAR_PAGE;

        request.setAttribute("baskets", baskets);
      }
      return new ActionForward(path, false);
    }
    catch (Exception ex) {
      logger.error(ex);
      ex.printStackTrace();
      throw ex;
    }
    finally {
      logger.debug("Leaving ShowBasketAction...");
    }
  }
}
