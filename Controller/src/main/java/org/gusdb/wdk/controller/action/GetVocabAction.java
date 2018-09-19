package org.gusdb.wdk.controller.action;

import java.io.File;
import java.io.PrintWriter;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.gusdb.fgputil.FormatUtil;
import org.gusdb.fgputil.json.JsonUtil;
import org.gusdb.wdk.controller.CConstants;
import org.gusdb.wdk.controller.actionutil.ActionUtility;
import org.gusdb.wdk.controller.actionutil.QuestionRequestParams;
import org.gusdb.wdk.controller.form.QuestionForm;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.WdkUserException;
import org.gusdb.wdk.model.jspwrap.EnumParamBean;
import org.gusdb.wdk.model.jspwrap.QuestionBean;
import org.gusdb.wdk.model.jspwrap.UserBean;
import org.gusdb.wdk.model.jspwrap.WdkModelBean;
import org.gusdb.wdk.model.query.param.RequestParams;
import org.json.JSONArray;
import org.json.JSONObject;

public class GetVocabAction extends Action {

  private static final Logger logger = Logger.getLogger(GetVocabAction.class);

  @Override
  public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request,
      HttpServletResponse response) throws Exception {
    logger.trace("Entering GetVocabAction...");
    WdkModelBean wdkModel = ActionUtility.getWdkModel(servlet);
    try {
      QuestionBean question = getQuestion(request, wdkModel);
      EnumParamBean param = getParam(request, question);

      UserBean user = ActionUtility.getUser(request);
      QuestionForm qForm = (QuestionForm) form;
      RequestParams requestParams = new QuestionRequestParams(request, qForm);
      param.prepareDisplay(user, requestParams);
      logger.debug("Setting vocabParam to " + param.getName() + " with display type " +
          param.getDisplayType() + " and multipick " + param.getMultiPick());
      request.setAttribute("vocabParam", param);

      boolean getJson = Boolean.valueOf(request.getParameter("json"));
      boolean getXml = Boolean.valueOf(request.getParameter("xml"));
      if (getJson) {    // output json string directly
        response.setContentType("application/json");
        PrintWriter writer = response.getWriter();
        JSONObject jsValues = param.getJsonValues();
        writer.print(jsValues.toString());
        writer.flush();
        return null;
      }
      else {    // output xml or html.
        String xmlVocabFile = CConstants.WDK_DEFAULT_VIEW_DIR + File.separator + CConstants.WDK_PAGES_DIR +
            File.separator + "vocabXml.jsp";

        String htmlVocabFile = CConstants.WDK_DEFAULT_VIEW_DIR + File.separator + CConstants.WDK_PAGES_DIR +
            File.separator + "vocabHtml.jsp";

        ActionForward forward = new ActionForward(getXml ? xmlVocabFile : htmlVocabFile);

        logger.trace("Leaving GetVocabAction...");
        return forward;
      }
    }
    catch (Exception ex) {
      logger.error("Could not load vocabulary", ex);
      throw ex;
    }
  }
  
  protected QuestionBean getQuestion(HttpServletRequest request, WdkModelBean wdkModel) throws WdkUserException, WdkModelException {
      String qFullName = request.getParameter(CConstants.QUESTION_FULLNAME_PARAM);
      wdkModel.validateQuestionFullName(qFullName);
      QuestionBean question = wdkModel.getQuestion(qFullName);
      return question;
  }
  
  protected EnumParamBean getParam(HttpServletRequest request, QuestionBean question) throws WdkUserException {
    String paramName = request.getParameter("name");
    if (paramName == null) throw new WdkUserException("Required parameter 'name' is missing.");

    // the dependent values are a JSON representation of {name: [values],
    // name: [values],...}
    Map<String, String> dependedValues = new LinkedHashMap<>();
    String values = FormatUtil.urlDecodeUtf8(request.getParameter("dependedValue"));
    if (values != null && values.length() > 0) {
      JSONObject jsValues = new JSONObject(values);
      for (String pName : JsonUtil.getKeys(jsValues)) {
        JSONArray jsArray = jsValues.getJSONArray(pName);
        StringBuilder buffer = new StringBuilder();
        for (int i = 0; i < jsArray.length(); i++) {
          if (buffer.length() > 0)
            buffer.append(",");
          buffer.append(jsArray.getString(i));
        }
        dependedValues.put(pName, buffer.toString());
      }
    }

    EnumParamBean param = (EnumParamBean) question.getParamsMap().get(paramName);
    param.setContextValues(dependedValues);
    
    return param;
  }
}
