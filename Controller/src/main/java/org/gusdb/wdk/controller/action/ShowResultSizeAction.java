package org.gusdb.wdk.controller.action;

import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.gusdb.fgputil.FormatUtil;
import org.gusdb.wdk.cache.CacheMgr;
import org.gusdb.wdk.controller.actionutil.ActionUtility;
import org.gusdb.wdk.model.WdkModel;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.WdkUserException;
import org.json.JSONObject;

public class ShowResultSizeAction extends Action {

  @SuppressWarnings("unused")
  private static Logger LOG = Logger.getLogger(ShowResultSizeAction.class);

  @Override
  public ActionForward execute(ActionMapping mapping, ActionForm form,
      HttpServletRequest request, HttpServletResponse response) throws Exception {

    String stepIdStr = request.getParameter("step");
    if (!FormatUtil.isInteger(stepIdStr)) {
      throw new WdkUserException("Parameter 'step' must be a valid step ID");
    }

    int stepId = Integer.parseInt(stepIdStr);
    String filterName = request.getParameter("filter");

    String result = (filterName == null ?
        getFilterResultSizes(stepId) :
        getSingleFilterResultSize(stepId, filterName));

    response.getWriter().print(result);

    return null;
  }

  // filter name specified, return only the size of the given filter
  private String getSingleFilterResultSize(int stepId, String filterName)
      throws WdkModelException, WdkUserException {
    WdkModel wdkModel = ActionUtility.getWdkModel(getServlet()).getModel();
    int size = CacheMgr.get().getFilterSizeCache()
        .getFilterSize(stepId, filterName, wdkModel);
    return String.valueOf(size);
  }

  // no filter is specified, will return all (legacy) filter sizes for the given step
  protected String getFilterResultSizes(int stepId)
      throws WdkModelException, WdkUserException {
    WdkModel wdkModel = ActionUtility.getWdkModel(getServlet()).getModel();
    Map<String, Integer> sizes = CacheMgr.get().getFilterSizeCache()
        .getFilterSizes(stepId, wdkModel);
    return getFilterSizesJson(sizes);
  }

  protected String getFilterSizesJson(Map<String, Integer> sizes) {
    JSONObject json = new JSONObject();
    for (Entry<String, Integer> entry : sizes.entrySet()) {
      json.put(entry.getKey(), entry.getValue());
    }
    return json.toString();
  }
}
