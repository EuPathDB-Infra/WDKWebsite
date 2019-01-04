package org.gusdb.wdk.model.jspwrap;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.gusdb.fgputil.functional.TreeNode;
import org.gusdb.wdk.model.FieldTree;
import org.gusdb.wdk.model.SelectableItem;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.WdkUserException;
import org.gusdb.wdk.model.query.param.AbstractEnumParam;
import org.gusdb.wdk.model.query.param.EnumParamTermNode;
import org.gusdb.wdk.model.query.param.EnumParamVocabInstance;
import org.gusdb.wdk.model.query.param.Param;
import org.json.JSONObject;

/**
 * A wrapper on a {@link AbstractEnumParam} that provides simplified access for consumption by a view.
 * 
 * Note on dependent params: if this is a dependent param and depended param is set, will access values based
 * on that value; otherwise will access values based on the default value of the depended param (i.e. are
 * assuming caller knows what they are doing).
 */
public class EnumParamBean extends ParamBean<AbstractEnumParam> {

  private static final Logger logger = Logger.getLogger(EnumParamBean.class);

  private final AbstractEnumParam enumParam;

  private String[] currentValues;
  private String[] originalValues;

  // if this obj wraps a dependent param, holds depended values
  private boolean _dependedValueChanged = false;
  private EnumParamVocabInstance _cache;

  public EnumParamBean(AbstractEnumParam param) {
    super(param);
    this.enumParam = param;
  }

  public Boolean getMultiPick() {
    return _param.getMultiPick();
  }

  public boolean getQuote() {
    return _param.getQuote();
  }

  public boolean isSkipValidation() {
    return _param.isSkipValidation();
  }

  public String getDisplayType() {
    return _param.getDisplayType();
  }

  public int getMinSelectedCount() {
    return _param.getMinSelectedCount();
  }

  public int getMaxSelectedCount() {
    return _param.getMaxSelectedCount();
  }

  public boolean getCountOnlyLeaves() {
    return _param.getCountOnlyLeaves();
  }
  
  public int getDepthExpanded() {
    return _param.getDepthExpanded();
  }

  public boolean isDependentParam() {
    return _param.isDependentParam();
  }

  @Override
  public void setContextValues(Map<String, String> contextValues) {
    super.setContextValues(contextValues);
    if ((this._contextValues == null && contextValues != null) ||
        (this._contextValues != null && !compareValues(this._contextValues, contextValues))) {
      this._contextValues = contextValues;
      _dependedValueChanged = true;
    }
  }

  private boolean compareValues(Map<String, String> left, Map<String, String> right) {
    if (left.size() != right.size())
      return false;
    for (String name : left.keySet()) {
      String value = left.get(name);
      if (!right.containsKey(name))
        return false;

      String rightValue = right.get(name);
      if (rightValue == null || !rightValue.equals(value))
        return false;
    }
    return true;
  }

  @Override
  public String getDefault() {
    return getVocabInstance().getDefaultValue();
  }

  // NOTE: not threadsafe! This class is expected only to be used in a single thread
  protected EnumParamVocabInstance getVocabInstance() {
    if (_cache == null || _dependedValueChanged) {
      _cache = _param.getVocabInstance(_userBean.getUser(), _contextValues);
      _dependedValueChanged = false;
    }
    return _cache;
  }

  public String[] getVocabInternal() {
    return getVocabInstance().getVocabInternal(enumParam.isNoTranslation());
  }

  public String[] getVocab() {
    return getVocabInstance().getVocab();
  }

  public Map<String, String> getVocabMap() {
    return getVocabInstance().getVocabMap();
  }

  public String[] getDisplays() {
    return getVocabInstance().getDisplays();
  }

  public Map<String, String> getDisplayMap() {
    return getVocabInstance().getDisplayMap();
  }

  public Map<String, String> getParentMap() {
    return getVocabInstance().getParentMap();
  }

  @Override
  public Set<ParamBean<?>> getDependedParams() throws WdkModelException {
    Set<Param> dependedParams = _param.getDependedParams();
    if (dependedParams != null) {
      Set<ParamBean<?>> paramBeans = new LinkedHashSet<>();
      for (Param param : dependedParams) {
        paramBeans.add(ParamBeanFactory.createBeanFromParam(param.getWdkModel(), _userBean, param));
      }
      return paramBeans;
    }
    return null;
  }

  @Override
  public String getDependedParamNames() throws WdkModelException {
    Set<Param> dependedParams = _param.getDependedParams();
    if (dependedParams == null)
      return null;
    StringBuilder buffer = new StringBuilder();
    for (Param p : dependedParams) {
      if (buffer.length() > 0)
        buffer.append(",");
      buffer.append(p.getName());
    }
    return buffer.toString();
  }

  public EnumParamTermNode[] getVocabTreeRoots() {
    return getVocabInstance().getVocabTreeRoots();
  }

  public String[] getTerms(String termList) {
    return _param.convertToTerms(termList);
  }

  public String getRawDisplayValue() throws WdkModelException {
    String[] terms = _param.getTerms(_userBean.getUser(), _stableValue);
		if (terms == null) return null;


    // if (!param.isSkipValidation()) {
    Map<String, String> displays = getDisplayMap();
    StringBuffer buffer = new StringBuffer();
    for (String term : terms) {
      if (buffer.length() > 0)
        buffer.append(", ");
      String display = displays.get(term.trim());
      if (display == null)
        display = term;
      buffer.append(display);
    }
    return buffer.toString();
    // } else {
    // return rawValue;
    // }
  }

  /**
   * Sets the currently selected values (as set on a user form) on the bean.
   * 
   * @param currentValues
   *          currently selected values
   */
  public void setCurrentValues(String[] currentValues) {
    this.currentValues = currentValues;
  }

  public String[] getCurrentValues() {
    return currentValues;
  }

  public void setOriginalValues(String[] originalValues) {
    this.originalValues = originalValues;
  }

  /**
   * Returns map where keys are vocab values and values are booleans telling whether each value is currently
   * selected or not.
   * 
   * @return map from value to selection status
   */
  public Map<String, Boolean> getOriginalValues() {
    if (originalValues == null)
      return new LinkedHashMap<String, Boolean>();

    Map<String, Boolean> values = new LinkedHashMap<String, Boolean>();
    Map<String, String> terms = getVocabMap();
    // ignore the validation for type-ahead params.
    for (String term : originalValues) {
      boolean valid = terms.containsKey(term);
      values.put(term, valid);
    }
    return values;
  }

  /**
   * Returns a TreeNode containing all values for this tree param, with the "currently selected" values
   * checked
   * 
   * @return up-to-date tree of this param
   */
  public FieldTree getParamTree() {
    EnumParamTermNode[] rootNodes = getVocabTreeRoots();
    FieldTree tree = getParamTree(getName(), rootNodes);
    populateParamTree(tree, currentValues);
    logger.debug("param " + getName() + ", stable=" + _stableValue + ", current=" + currentValues);

    return tree;
  }

  public static FieldTree getParamTree(String treeName, EnumParamTermNode[] rootNodes) {
    FieldTree tree = new FieldTree(new SelectableItem(treeName, "top"));
    TreeNode<SelectableItem> root = tree.getRoot();
    for (EnumParamTermNode paramNode : rootNodes) {
      if (paramNode.getChildren().length == 0) {
        root.addChild(new SelectableItem(paramNode.getTerm(), paramNode.getDisplay(), paramNode.getDisplay()));
      }
      else {
        root.addChildNode(paramNode.toFieldTree().getRoot());
      }
    }
    return tree;
  }

  public static void populateParamTree(FieldTree tree, String[] values) {
    if (values != null && values.length > 0) {
      List<String> currentValueList = Arrays.asList(values);
      tree.setSelectedLeaves(currentValueList);
      tree.addDefaultLeaves(currentValueList);
    }
  }

  public boolean isSuppressNode() {
    return _param.isSuppressNode();
  }

  @Override
  public void setStableValue(String stabletValue) throws WdkModelException {
    super.setStableValue(stabletValue);
    // also set the current values
    if (_stableValue == null)
      _stableValue = getDefault();

    if (_stableValue != null)
      currentValues = (String[]) _param.getRawValue(_userBean.getUser(), stabletValue);

  }

  /**
   * @param _userBean
   * @param _contextValues
   * @param cache
   * @return
   * @throws WdkModelException
   * @throws WdkUserException 
   * @see org.gusdb.wdk.model.query.param.AbstractEnumParam#getJSONValues(org.gusdb.wdk.model.user.User,
   *      java.util.Map, org.gusdb.wdk.model.query.param.EnumParamVocabInstance)
   */
  public JSONObject getJsonValues() throws WdkModelException, WdkUserException {
    return enumParam.getJsonValues(getVocabInstance());
  }

  public boolean isFilterParam() {
    return false;
  }
}
