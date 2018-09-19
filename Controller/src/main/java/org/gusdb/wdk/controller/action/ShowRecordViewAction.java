package org.gusdb.wdk.controller.action;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.gusdb.wdk.controller.actionutil.ActionUtility;
import org.gusdb.wdk.model.WdkUserException;
import org.gusdb.wdk.model.jspwrap.RecordBean;
import org.gusdb.wdk.model.jspwrap.RecordClassBean;
import org.gusdb.wdk.model.jspwrap.UserBean;
import org.gusdb.wdk.model.jspwrap.WdkModelBean;
import org.gusdb.wdk.model.record.RecordView;
import org.gusdb.wdk.model.record.RecordViewHandler;

public class ShowRecordViewAction extends Action {

  public static final String PARAM_NAME = "name";
  public static final String PARAM_PRIMARY = "primaryKey";
  public static final String PARAM_VIEW = "view";

  public static final String ATTR_RECORD = "wdkRecord";

  private static final Logger logger = Logger.getLogger(ShowRecordViewAction.class.getName());

  @Override
  public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request,
      HttpServletResponse response) throws Exception {
    try {
      logger.debug("Entering ShowRecordFeatureAction");

      // get record
      WdkModelBean wdkModel = ActionUtility.getWdkModel(servlet);
      UserBean wdkUser = ActionUtility.getUser(request);

      String rcName = request.getParameter(PARAM_NAME);
      if (rcName == null || rcName.length() == 0)
        throw new WdkUserException("Required name parameter is missing. "
            + "A full recordClass name is needed.");

      wdkModel.validateRecordClassName(rcName);
      RecordClassBean recordClass = wdkModel.findRecordClass(rcName);
      String[] pkColumns = recordClass.getPrimaryKeyColumns();

      Map<String, Object> pkValues = new LinkedHashMap<String, Object>();
      for (String column : pkColumns) {
        String value = request.getParameter(column);
        pkValues.put(column, value);
      }

      RecordBean wdkRecord = new RecordBean(wdkUser, recordClass, pkValues);

      request.setAttribute(ATTR_RECORD, wdkRecord);

      String viewName = request.getParameter(PARAM_VIEW);
      RecordView view;
      if (viewName == null || viewName.length() == 0) {
        view = recordClass.getDefaultRecordView();
      }
      else {
        Map<String, RecordView> views = recordClass.getRecordViews();
        view = views.get(viewName);
        if (view == null)
          throw new WdkUserException("The view \"" + viewName + "\" doesn't exist in record " + rcName);
      }

      // process the view handler
      RecordViewHandler handler = view.getHandler();
      if (handler != null) {
        Map<String, Object> result = handler.process(wdkRecord.getRecordInstance());
        for (String key : result.keySet()) {
          request.setAttribute(key, result.get(key));
        }
      }

      wdkUser.setCurrentRecordView(recordClass, view);

      ActionForward forward;

      logger.info("view=" + view.getName() + ", jsp=" + view.getJsp());
      forward = new ActionForward(view.getJsp());

      logger.info("Leaving ShowRecordFeatureAction");
      return forward;
    }
    catch (Exception e) {
      logger.error("Exception processing show record.", e);
      throw e;
    }
  }
}
