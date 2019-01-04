package org.gusdb.wdk.model.jspwrap;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.gusdb.fgputil.validation.ValidationLevel;
import org.gusdb.wdk.model.Group;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.WdkUserException;
import org.gusdb.wdk.model.analysis.StepAnalysis;
import org.gusdb.wdk.model.answer.AnswerValue;
import org.gusdb.wdk.model.answer.SummaryView;
import org.gusdb.wdk.model.answer.factory.AnswerValueFactory;
import org.gusdb.wdk.model.answer.spec.AnswerSpec;
import org.gusdb.wdk.model.filter.Filter;
import org.gusdb.wdk.model.query.param.AnswerParam;
import org.gusdb.wdk.model.query.param.Param;
import org.gusdb.wdk.model.query.spec.QueryInstanceSpec;
import org.gusdb.wdk.model.query.spec.QueryInstanceSpecBuilder.FillStrategy;
import org.gusdb.wdk.model.question.Question;
import org.gusdb.wdk.model.question.SearchCategory;
import org.gusdb.wdk.model.record.Field;
import org.gusdb.wdk.model.record.FieldScope;
import org.gusdb.wdk.model.record.RecordClass;
import org.gusdb.wdk.model.record.TableField;
import org.gusdb.wdk.model.record.attribute.AttributeField;
import org.gusdb.wdk.model.user.StepContainer;
import org.json.JSONObject;

/**
 * A wrapper on a {@link Question} that provides simplified access for
 * consumption by a view
 */
public class QuestionBean {

  private static final Logger LOG = Logger.getLogger(QuestionBean.class.getName());

  Question _question;

  private Map<String, String> _params = new LinkedHashMap<String, String>();
  private UserBean _user;
  private int _weight;

  private Map<String, ParamBean<?>> _paramBeanMap;

  /**
   * the recordClass full name for the answerParams input type.
   */
  private String inputType;

  public QuestionBean(Question question) throws WdkModelException {
    _question = question;
    initializeParamBeans();
  }

  private void initializeParamBeans() throws WdkModelException {
    Param[] params = _question.getParams();
    _paramBeanMap = new LinkedHashMap<String, ParamBean<?>>();
    for (int i = 0; i < params.length; i++) {
      _paramBeanMap.put(params[i].getName(),
          ParamBeanFactory.createBeanFromParam(_question.getWdkModel(), _user, params[i]));
    }
  }

  public RecordClassBean getRecordClass() {
    return new RecordClassBean(_question.getRecordClass());
  }

  public ParamBean<?>[] getParams() {
    return _paramBeanMap.values().toArray(new ParamBean[0]);
  }

  public Map<String, ParamBean<?>> getParamsMap() {
    return _paramBeanMap;
  }

  public String getParamValuesJson() {
    return new JSONObject(getParamsMap()
        .values()
        .stream()
        .collect(Collectors.toMap(ParamBean::getName, ParamBean::getStableValue)))
      .toString();
  }

  /**
   * @return
   * @see org.gusdb.wdk.model.Question#getParamMapByGroups()
   */
  public Map<GroupBean, Map<String, ParamBean<?>>> getParamMapByGroups() {
    Map<Group, Map<String, Param>> paramGroups = _question.getParamMapByGroups();
    Map<GroupBean, Map<String, ParamBean<?>>> paramGroupBeans = new LinkedHashMap<GroupBean, Map<String, ParamBean<?>>>();
    for (Group group : paramGroups.keySet()) {
      GroupBean groupBean = new GroupBean(group);
      Map<String, Param> paramGroup = paramGroups.get(group);
      Map<String, ParamBean<?>> paramGroupBean = new LinkedHashMap<String, ParamBean<?>>();
      for (String paramName : paramGroup.keySet()) {
        paramGroupBean.put(paramName, _paramBeanMap.get(paramName));
      }
      paramGroupBeans.put(groupBean, paramGroupBean);
    }
    return paramGroupBeans;
  }

  /**
   * @param displayType
   * @return
   * @see org.gusdb.wdk.model.Question#getParamMapByGroups(java.lang.String)
   */
  public Map<GroupBean, Map<String, ParamBean<?>>> getParamMapByGroups(
      String displayType) {
    Map<Group, Map<String, Param>> paramGroups = _question.getParamMapByGroups(displayType);
    Map<GroupBean, Map<String, ParamBean<?>>> paramGroupBeans = new LinkedHashMap<GroupBean, Map<String, ParamBean<?>>>();
    for (Group group : paramGroups.keySet()) {
      GroupBean groupBean = new GroupBean(group);
      Map<String, Param> paramGroup = paramGroups.get(group);
      Map<String, ParamBean<?>> paramGroupBean = new LinkedHashMap<String, ParamBean<?>>();
      for (String paramName : paramGroup.keySet()) {
        paramGroupBean.put(paramName, _paramBeanMap.get(paramName));
      }
      paramGroupBeans.put(groupBean, paramGroupBean);
    }
    return paramGroupBeans;
  }

  public Map<String, AttributeFieldBean> getSummaryAttributesMap() {
    Map<String, AttributeField> attribs = _question.getSummaryAttributeFieldMap();
    Map<String, AttributeFieldBean> beanMap = new LinkedHashMap<String, AttributeFieldBean>();
    for (AttributeField field : attribs.values()) {
      beanMap.put(field.getName(), new AttributeFieldBean(field));
    }
    return beanMap;
  }

  public Map<String, AttributeFieldBean> getDisplayableAttributeFields() {
    Map<String, AttributeField> attribs = _question.getAttributeFieldMap(FieldScope.NON_INTERNAL);
    Map<String, AttributeFieldBean> beanMap = new LinkedHashMap<String, AttributeFieldBean>();
    for (AttributeField field : attribs.values()) {
      beanMap.put(field.getName(), new AttributeFieldBean(field));
    }
    return beanMap;
  }

  public Map<String, AttributeFieldBean> getReportMakerAttributesMap() {
    Map<String, AttributeField> attribs = _question.getAttributeFieldMap(FieldScope.REPORT_MAKER);
    Iterator<String> ai = attribs.keySet().iterator();

    Map<String, AttributeFieldBean> rmaMap = new LinkedHashMap<String, AttributeFieldBean>();
    while (ai.hasNext()) {
      String attribName = ai.next();
      rmaMap.put(attribName, new AttributeFieldBean(attribs.get(attribName)));
    }
    return rmaMap;
  }

  public Map<String, TableFieldBean> getReportMakerTablesMap() {
    Map<String, TableField> tables = _question.getRecordClass().getTableFieldMap(
        FieldScope.REPORT_MAKER);
    Iterator<String> ti = tables.keySet().iterator();

    Map<String, TableFieldBean> rmtMap = new LinkedHashMap<String, TableFieldBean>();
    while (ti.hasNext()) {
      String tableName = ti.next();
      rmtMap.put(tableName, new TableFieldBean(tables.get(tableName)));
    }
    return rmtMap;
  }

  public Map<String, FieldBean> getReportMakerFieldsMap() {
    Map<String, Field> fields = _question.getFields(FieldScope.REPORT_MAKER);
    Iterator<String> fi = fields.keySet().iterator();

    Map<String, FieldBean> rmfMap = new LinkedHashMap<String, FieldBean>();
    while (fi.hasNext()) {
      String fieldName = fi.next();
      Field field = fields.get(fieldName);
      if (field instanceof AttributeField) {
        rmfMap.put(fieldName, new AttributeFieldBean((AttributeField) field));
      } else if (field instanceof TableField) {
        rmfMap.put(fieldName, new TableFieldBean((TableField) field));
      }
    }
    return rmfMap;
  }

  public Map<String, AttributeFieldBean> getAttributeFields() {
    Map<String, AttributeField> fields = _question.getAttributeFieldMap();
    Map<String, AttributeFieldBean> beans = new LinkedHashMap<String, AttributeFieldBean>();
    for (AttributeField field : fields.values()) {
      AttributeFieldBean bean = new AttributeFieldBean(field);
      beans.put(field.getName(), bean);
    }
    return beans;
  }

  public Map<String, AttributeFieldBean> getAdditionalSummaryAttributesMap() {
    Map<String, AttributeFieldBean> all = getReportMakerAttributesMap();
    Map<String, AttributeFieldBean> dft = getSummaryAttributesMap();
    Map<String, AttributeFieldBean> opt = new LinkedHashMap<String, AttributeFieldBean>();
    Iterator<String> ai = all.keySet().iterator();
    while (ai.hasNext()) {
      String attribName = ai.next();
      if (dft.get(attribName) == null) {
        opt.put(attribName, all.get(attribName));
      }
    }
    return opt;
  }

  public String getName() {
    return _question.getName();
  }

  public String getFullName() {
    return _question.getFullName();
  }

  public String getUrlSegment() { return _question.getUrlSegment(); }

  /**
   * @return
   * @see org.gusdb.wdk.model.Question#getQuestionSetName()
   */
  public String getQuestionSetName() {
    return _question.getQuestionSetName();
  }

  public String getQueryName() {
    return _question.getQueryName();
  }

  public String getDisplayName() {
    return _question.getDisplayName();
  }

  public String getShortDisplayName() {
    return _question.getShortDisplayName();
  }

  public String getHelp() {
    return _question.getHelp();
  }

  public String getDescription() {
    return _question.getDescription();
  }

  public String getSummary() {
    return _question.getSummary();
  }

  public String getCustomJavascript() {
    return _question.getCustomJavascript();
  }

  /**
   * A indicator to the controller whether this question bean should make answer
   * beans that contains all records in one page or not.
   * 
   * @return
   * @see org.gusdb.wdk.model.Question#isFullAnswer()
   */
  public boolean isFullAnswer() {
    return _question.isFullAnswer();
  }

  /**
   * @param propertyListName
   * @return
   * @see org.gusdb.wdk.model.Question#getPropertyList(java.lang.String)
   */
  public String[] getPropertyList(String propertyListName) {
    return _question.getPropertyList(propertyListName);
  }

  /**
   * @return
   * @see org.gusdb.wdk.model.Question#getPropertyLists()
   */
  public Map<String, String[]> getPropertyLists() {
    return _question.getPropertyLists();
  }

  /**
   * @return
   * @see org.gusdb.wdk.model.Question#isNoSummaryOnSingleRecord()
   */
  public boolean isNoSummaryOnSingleRecord() {
    return _question.isNoSummaryOnSingleRecord();
  }

  public boolean getIsBoolean() {
    return _question.getQuery().isBoolean();
  }

  public boolean getIsCombined() {
    return _question.getQuery().isCombined();
  }

  public boolean getIsTransform() {
    return _question.getQuery().isTransform();
  }

  public void setInputType(String inputType) {
    this.inputType = inputType;
  }

  public List<AnswerParamBean> getTransformParams() throws WdkModelException {
    List<AnswerParamBean> beans = new ArrayList<AnswerParamBean>();
    RecordClass input = _question.getWdkModel().getRecordClass(inputType);
    for (AnswerParam answerParam : _question.getTransformParams(input)) {
      beans.add(new AnswerParamBean(answerParam));
    }
    return beans;
  }

  public void setParam(String nameValue) {
    String[] parts = nameValue.split("=");
    String name = parts[0].trim();
    String value = parts[1].trim();
    _params.put(name, value);
  }

  public void setUser(UserBean user) {
    _user = user;
  }

  public void setWeight(int weight) {
    _weight = weight;
  }

  public AnswerValueBean getAnswerValue() throws WdkModelException {
    try {
      if (_user == null)
        throw new WdkUserException("User is not set. Please set user to "
            + "the questionBean before calling to create answerValue.");

      AnswerValue answerValue = AnswerValueFactory.makeAnswer(_user.getUser(),
          AnswerSpec.builder(_question.getWdkModel())
          .setQuestionName(_question.getFullName())
          .setQueryInstanceSpec(QueryInstanceSpec.builder()
              .putAll(_params)
              .setAssignedWeight(_weight))
          .buildRunnable(_user.getUser(), StepContainer.emptyContainer()));

      // reset the params
      _params.clear();

      return new AnswerValueBean(answerValue);
    }
    catch (Exception ex) {
      LOG.error("Exception thrown in getAnswerValue(): " + ex);
      for (StackTraceElement elem : Thread.currentThread().getStackTrace()) {
        LOG.error("  " + elem.toString());
      }
      throw new WdkModelException(ex);
    }
  }

  /**
   * @return
   * @see org.gusdb.wdk.model.Question#getSummaryViews()
   */
  public Map<String, SummaryView> getSummaryViews() {
    return _question.getSummaryViews();
  }

  /**
   * @return
   * @see org.gusdb.wdk.model.Question#getDefaultSummaryView()
   */
  public SummaryView getDefaultSummaryView() {
    return _question.getDefaultSummaryView();
  }

  /**
   * @return
   * @see org.gusdb.wdk.model.Question#getStepAnalyses()
   */
  public Map<String, StepAnalysis> getStepAnalyses() {
    return _question.getStepAnalyses();
  }
  
  public boolean getContainsWildcardTextParam() {
    for (ParamBean<?> param : _paramBeanMap.values()) {
      if (param.getName().equals("text_expression")) {
        return true;
      }
    }
    return false;
  }

  /**
   * @return
   * @see org.gusdb.wdk.model.Question#isNew()
   */
  public boolean isNew() {
    return _question.isNew();
  }

  public boolean isRevised() {
    return _question.isRevised();
  }

  public List<CategoryBean> getWebsiteCategories() {
    return getCategories(SearchCategory.USED_BY_WEBSITE, false);
  }

  public List<CategoryBean> getWebServiceCategories() {
    return getCategories(SearchCategory.USED_BY_WEBSERVICE, false);
  }

  public List<CategoryBean> getDatasetCategories() {
    return getCategories(SearchCategory.USED_BY_DATASET, true);
  }

  private List<CategoryBean> getCategories(String usedBy, boolean strict) {
    List<CategoryBean> beans = new ArrayList<>();
    Map<String, SearchCategory> categories = _question.getCategories(usedBy,
        strict);
    for (SearchCategory category : categories.values()) {
      beans.add(new CategoryBean(category));
    }
    return beans;
  }

  public void fillContextParamValues(UserBean user,
      Map<String, String> contextParamValues) throws WdkModelException {
    QueryInstanceSpec spec = QueryInstanceSpec.builder().buildValidated(
        user.getUser(), _question.getQuery(), StepContainer.emptyContainer(),
        ValidationLevel.RUNNABLE, FillStrategy.FILL_PARAM_IF_MISSING);
    contextParamValues.putAll(spec.toMap());
  }

  public Map<String, Filter> getFilters() {
    return _question.getFilters();
  }

  public Filter getFilter(String filterName) throws WdkModelException {
    return _question.getFilter(filterName);
  }

  public Question getQuestion() {
    return _question;
  }
  
  /**
   * 
   * @param filterName
   * @return null if filter not found
   * @throws WdkModelException
   */
  public Filter getFilterOrNull(String filterName) throws WdkModelException {
    return _question.getFilterOrNull(filterName);
  }
  
}
