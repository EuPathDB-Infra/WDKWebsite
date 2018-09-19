package org.gusdb.wdk.controller.form;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.gusdb.wdk.controller.CConstants;
import org.gusdb.wdk.controller.actionutil.ActionUtility;
import org.gusdb.wdk.controller.actionutil.QuestionRequestParams;
import org.gusdb.wdk.model.Utilities;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.WdkUserException;
import org.gusdb.wdk.model.jspwrap.ParamBean;
import org.gusdb.wdk.model.jspwrap.QuestionBean;
import org.gusdb.wdk.model.jspwrap.UserBean;
import org.gusdb.wdk.model.jspwrap.WdkModelBean;
import org.gusdb.wdk.model.query.param.RequestParams;

/**
 * form bean for showing a wdk question from a question set.
 * 
 * The param values stored in the question form are stable values.
 */

public class QuestionForm extends MapActionForm {

  private static final long serialVersionUID = -7848685794514383434L;
  private static final Logger LOG = Logger.getLogger(QuestionForm.class);

  private String _questionFullName;
  private QuestionBean _question;
  private boolean _validating = true;
  private boolean _paramsFilled = false;
  private String _weight;
  private String _customName;

  /**
   * validate the properties that have been sent from the HTTP request, and return an ActionErrors object that
   * encapsulates any validation errors
   */
  @Override
  public ActionErrors validate(ActionMapping mapping, HttpServletRequest request) {
    LOG.debug("\n\n\n\n\n\nstart form validation...");
    ActionErrors errors = super.validate(mapping, request);
    if (errors == null)
      errors = new ActionErrors();

    UserBean user = ActionUtility.getUser(request);

    // set the question name into request
    request.setAttribute(CConstants.QUESTIONFORM_KEY, this);
    request.setAttribute(CConstants.QUESTION_FULLNAME_PARAM, _questionFullName);

    if (!_validating)
      return errors;

    String clicked = request.getParameter(CConstants.PQ_SUBMIT_KEY);
    if (clicked != null && clicked.equals(CConstants.PQ_SUBMIT_EXPAND_QUERY)) {
      return errors;
    }

    QuestionBean wdkQuestion;
    try {
      wdkQuestion = getQuestion();
    }
    catch (WdkUserException | WdkModelException ex) {
      ActionMessage message = new ActionMessage("mapped.properties", ex.getMessage());
      errors.add(ActionErrors.GLOBAL_MESSAGE, message);
      LOG.error("Unable to get question", ex);
      return errors;
    }
    if (wdkQuestion == null)
      return errors;

    Map<String, ParamBean<?>> params = wdkQuestion.getParamsMap();
    RequestParams requestParams = new QuestionRequestParams(request, this);

    // get the context values first
    Map<String, String> contextValues = new LinkedHashMap<>();
    for (String name : params.keySet()) {
      ParamBean<?> param = params.get(name);
      try {
        String stableValue = param.getStableValue(user, requestParams);
        contextValues.put(name, stableValue);
      }
      catch (Exception ex) {
        ActionMessage message = new ActionMessage("mapped.properties", param.getPrompt(), ex.getMessage());
        errors.add(ActionErrors.GLOBAL_MESSAGE, message);
        LOG.error("getting stable value failed", ex);
      }
    }

    // assign context values to the param bean
    for (ParamBean<?> param : params.values()) {
      param.setUser(user);
      param.setContextValues(contextValues);
    }

    // validate params
    for (String paramName : params.keySet()) {
      ParamBean<?> param = params.get(paramName);
      try {
        String stableValue = contextValues.get(paramName);
        param.validate(user, stableValue, contextValues);
      }
      catch (Exception ex) {
        ActionMessage message = new ActionMessage("mapped.properties", param.getPrompt(), ex.getMessage());
        errors.add(ActionErrors.GLOBAL_MESSAGE, message);
        LOG.error("validation failed.", ex);
      }
    }

    // validate weight
    boolean hasWeight = (_weight != null && _weight.length() > 0);
    if (hasWeight) {
      String message = null;
      if (!_weight.matches("[\\-\\+]?\\d+")) {
        message = "Invalid weight value: '" + _weight + "'. Only integer numbers are allowed.";
      }
      else if (_weight.length() > 9) {
        message = "Weight number is too big: " + _weight;
      }
      if (message != null) {
        ActionMessage am = new ActionMessage("mapped.properties", "Assigned weight", message);
        errors.add(ActionErrors.GLOBAL_MESSAGE, am);
        LOG.error(message);
      }
    }

    // add explicit exception to request for access later
    if (!errors.isEmpty()) {
      request.setAttribute(CConstants.WDK_EXCEPTION, new WdkUserException(
          "Unable to validate params in request."));
    }

    LOG.debug("finish validation...\n\n");
    return errors;
  }

  public void setQuestionFullName(String questionFullName) {
    _questionFullName = questionFullName;
  }

  public String getQuestionFullName() {
    return _questionFullName;
  }

  public void setQuestion(QuestionBean question) {
    _question = question;
    _questionFullName = question.getFullName();
  }

  public QuestionBean getQuestion() throws WdkModelException, WdkUserException {
    if (_question == null) {
      if (_questionFullName == null)
        return null;
      WdkModelBean wdkModel = ActionUtility.getWdkModel(getServlet());
      wdkModel.validateQuestionFullName(_questionFullName);
      _question = wdkModel.getQuestion(_questionFullName);
    }
    return _question;
  }

  public void setNonValidating() {
    _validating = false;
  }

  public void setParamsFilled(boolean paramsFilled) {
    _paramsFilled = paramsFilled;
  }

  public boolean getParamsFilled() {
    return _paramsFilled;
  }

  public void setWeight(String weight) {
    _weight = weight;
  }

  public String getWeight() {
    return _weight;
  }

  @Override
  public Object getValue(String key) {
    return getValueOrArray(key);
  }

  /**
   * @return the customName
   */
  public String getCustomName() {
    return _customName;
  }

  /**
   * @param customName
   *          the customName to set
   */
  public void setCustomName(String customName) {
    _customName = customName;
  }

  public Map<String, String> getInvalidParams() {
    Map<String, ParamBean<?>> params = _question.getParamsMap();
    Map<String, String> invalidParams = new LinkedHashMap<String, String>();
    for (String param : _values.keySet()) {
      if (!params.containsKey(param))
        invalidParams.put(param, _values.get(param).toString());
    }
    for (String param : _arrays.keySet()) {
      if (!params.containsKey(param)) {
        String value = Utilities.fromArray(_arrays.get(param));
        invalidParams.put(param, value);
      }
    }
    return invalidParams;
  }
}
