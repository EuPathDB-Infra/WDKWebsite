package org.gusdb.wdk.model.jspwrap;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.Vector;

import org.gusdb.wdk.model.Reference;
import org.gusdb.wdk.model.WdkModel;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.WdkUserException;
import org.gusdb.wdk.model.dbms.ConnectionContainer;
import org.gusdb.wdk.model.query.param.Param;
import org.gusdb.wdk.model.query.param.ParamSet;
import org.gusdb.wdk.model.question.QuestionSet;
import org.gusdb.wdk.model.question.SearchCategory;
import org.gusdb.wdk.model.record.RecordClass;
import org.gusdb.wdk.model.record.RecordClassSet;
import org.gusdb.wdk.model.xml.XmlQuestionSet;
import org.gusdb.wdk.model.xml.XmlRecordClassSet;

/**
 * A wrapper on a {@link WdkModel} that provides simplified access for
 * consumption by a view
 */
public class WdkModelBean implements ConnectionContainer {

    WdkModel wdkModel;

    public WdkModelBean(WdkModel wdkModel) {
        this.wdkModel = wdkModel;
    }

    public Map<String, String> getProperties() {
        return wdkModel.getProperties();
    }

    public String getVersion() {
        return wdkModel.getVersion();
    }

    public String getBuild() {
        return wdkModel.getBuildNumber();
    }

    public String getDisplayName() {
        return wdkModel.getDisplayName();
    }

    public String getIntroduction() {
        return wdkModel.getIntroduction();
    }

    // to do: figure out how to do this without using getModel()
    public WdkModel getModel() {
        return wdkModel;
    }

    /**
     * used by the controller
     */
    public RecordClassBean findRecordClass(String recClassRef)
            throws WdkModelException {
        return new RecordClassBean(wdkModel.getRecordClass(recClassRef));
    }

    public Map<String, CategoryBean> getWebsiteRootCategories() {
        Map<String, CategoryBean> beans = new LinkedHashMap<String, CategoryBean>();
        Map<String, SearchCategory> roots = wdkModel.getRootCategories(SearchCategory.USED_BY_WEBSITE);
        for (SearchCategory category : roots.values()) {
            CategoryBean bean = new CategoryBean(category);
            beans.put(category.getName(), bean);
        }
        return beans;
    }

    public Map<String, CategoryBean> getWebserviceRootCategories() {
        Map<String, CategoryBean> beans = new LinkedHashMap<String, CategoryBean>();
        Map<String, SearchCategory> roots = wdkModel.getRootCategories(SearchCategory.USED_BY_WEBSERVICE);
        for (SearchCategory category : roots.values()) {
            CategoryBean bean = new CategoryBean(category);
            beans.put(category.getName(), bean);
        }
        return beans;
    }

    public Map<QuestionBean, CategoryBean> getWebsiteQuestions()
            throws WdkModelException {
        Map<QuestionBean, CategoryBean> questions = new LinkedHashMap<QuestionBean, CategoryBean>();
        Map<String, CategoryBean> categories = getWebsiteRootCategories();
        Stack<CategoryBean> stack = new Stack<CategoryBean>();
        stack.addAll(categories.values());
        while (!stack.isEmpty()) {
            CategoryBean category = stack.pop();
            for (QuestionBean question : category.getWebsiteQuestions()) {
                questions.put(question, category);
            }
            // add the children in reversed order to make sure they have the
            // correct order when popping out from stack.
            List<CategoryBean> children = new ArrayList<CategoryBean>(
                    category.getWebsiteChildren().values());
            for (int i = children.size() - 1; i >= 0; i--) {
                stack.push(children.get(i));
            }
        }
        return questions;
    }

    // getWebsiteQuestions does not include all expression questions included in an internal page
    // we need this for the searchesLookup table
 public Map<QuestionBean, CategoryBean> getAllQuestions()
            throws WdkModelException {
        Map<QuestionBean, CategoryBean> questions = new LinkedHashMap<QuestionBean, CategoryBean>();
        Map<String, CategoryBean> categories = getWebserviceRootCategories();
        Stack<CategoryBean> stack = new Stack<CategoryBean>();
        stack.addAll(categories.values());
        while (!stack.isEmpty()) {
            CategoryBean category = stack.pop();
            for (QuestionBean question : category.getWebserviceQuestions()) {
                questions.put(question, category);
            }
            // add the children in reversed order to make sure they have the
            // correct order when popping out from stack.
            List<CategoryBean> children = new ArrayList<CategoryBean>(
                    category.getWebserviceChildren().values());
            for (int i = children.size() - 1; i >= 0; i--) {
                stack.push(children.get(i));
            }
        }
        return questions;
    }

    /**
     * @return Map of questionSetName --> {@link QuestionSetBean}
     */
    public Map<String, QuestionSetBean> getQuestionSetsMap() {
        Map<String, QuestionSet> qSets = wdkModel.getQuestionSets();
        Map<String, QuestionSetBean> qSetBeans = new LinkedHashMap<String, QuestionSetBean>();
        for (String qSetKey : qSets.keySet()) {
            QuestionSetBean qSetBean = new QuestionSetBean(qSets.get(qSetKey));
            qSetBeans.put(qSetKey, qSetBean);
        }
        return qSetBeans;
    }

    public QuestionSetBean[] getQuestionSets() {
        Map<String, QuestionSetBean> qSetMap = getQuestionSetsMap();
        QuestionSetBean[] qSetBeans = new QuestionSetBean[qSetMap.size()];
        qSetMap.values().toArray(qSetBeans);
        return qSetBeans;
    }

    public RecordClassBean[] getRecordClasses() {

        Vector<RecordClassBean> recordClassBeans = new Vector<RecordClassBean>();
        RecordClassSet sets[] = wdkModel.getAllRecordClassSets();
        for (int i = 0; i < sets.length; i++) {
            RecordClassSet nextSet = sets[i];
            RecordClass recordClasses[] = nextSet.getRecordClasses();
            for (int j = 0; j < recordClasses.length; j++) {
                RecordClass nextClass = recordClasses[j];
                RecordClassBean bean = new RecordClassBean(nextClass);
                recordClassBeans.addElement(bean);
            }
        }

        RecordClassBean[] returnedBeans = new RecordClassBean[recordClassBeans.size()];
        for (int i = 0; i < recordClassBeans.size(); i++) {
            RecordClassBean nextReturnedBean = recordClassBeans.elementAt(i);
            returnedBeans[i] = nextReturnedBean;
        }
        return returnedBeans;
    }

    public Map<String, RecordClassBean> getRecordClassMap() {
        Map<String, RecordClassBean> recordClassMap = new LinkedHashMap<String, RecordClassBean>();
        RecordClassSet[] rcsets = wdkModel.getAllRecordClassSets();
        for (RecordClassSet rcset : rcsets) {
            RecordClass[] rcs = rcset.getRecordClasses();
            for (RecordClass rc : rcs) {
                recordClassMap.put(rc.getFullName(), new RecordClassBean(rc));
            }
        }
        return recordClassMap;
    }

    public Map<String, String> getRecordClassDisplayNames() {
        RecordClassBean[] recClasses = getRecordClasses();
        Map<String, String> types = new LinkedHashMap<String, String>();
        for (RecordClassBean r : recClasses) {
            types.put(r.getFullName(), r.getDisplayName());
        }
        return types;
    }

    public XmlQuestionSetBean[] getXmlQuestionSets() {
        XmlQuestionSet[] qsets = wdkModel.getXmlQuestionSets();
        XmlQuestionSetBean[] qsetBeans = new XmlQuestionSetBean[qsets.length];
        for (int i = 0; i < qsets.length; i++) {
            qsetBeans[i] = new XmlQuestionSetBean(qsets[i]);
        }
        return qsetBeans;
    }

    /**
     * @return Map of questionSetName --> {@link XmlQuestionSetBean}
     */
    public Map<String, XmlQuestionSetBean> getXmlQuestionSetsMap() {
        XmlQuestionSetBean[] qSets = getXmlQuestionSets();
        Map<String, XmlQuestionSetBean> qSetsMap = new LinkedHashMap<String, XmlQuestionSetBean>();
        for (int i = 0; i < qSets.length; i++) {
            qSetsMap.put(qSets[i].getName(), qSets[i]);
        }
        return qSetsMap;
    }

    public XmlRecordClassSetBean[] getXmlRecordClassSets() {
        XmlRecordClassSet[] rcs = wdkModel.getXmlRecordClassSets();
        XmlRecordClassSetBean[] rcBeans = new XmlRecordClassSetBean[rcs.length];
        for (int i = 0; i < rcs.length; i++) {
            rcBeans[i] = new XmlRecordClassSetBean(rcs[i]);
        }
        return rcBeans;
    }

    public UserFactoryBean getUserFactory() {
        return new UserFactoryBean(wdkModel, wdkModel.getUserFactory());
    }

    public String getProjectId() {
        return wdkModel.getProjectId();
    }

    public String getName() {
        return wdkModel.getProjectId();
    }

    /**
     * @param paramName
     * @return
     * @see org.gusdb.wdk.model.WdkModel#queryParamDisplayName(java.lang.String)
     */
    public String queryParamDisplayName(String paramName) {
        return wdkModel.queryParamDisplayName(paramName);
    }

    public String getSecretKey() {
        return wdkModel.getModelConfig().getSecretKey();
    }

    public boolean getUseWeights() {
        return wdkModel.getModelConfig().getUseWeights();
    }

    public UserBean getSystemUser() {
        return new UserBean(wdkModel.getSystemUser());
    }

    /**
     * @return
     * @see org.gusdb.wdk.model.WdkModel#getReleaseDate()
     */
    public String getReleaseDate() {
        return wdkModel.getReleaseDate();
    }

    public QuestionBean getQuestion(String questionFullName)
            throws WdkModelException {
        return new QuestionBean(wdkModel.getQuestion(questionFullName)
            .orElseThrow(() -> new WdkModelException("No question exists with name " + questionFullName)));
    }

    public Map<String, ParamBean<?>> getParams(UserBean user) throws WdkModelException {
        Map<String, ParamBean<?>> params = new LinkedHashMap<String, ParamBean<?>>();
        for (ParamSet paramSet : wdkModel.getAllParamSets()) {
            for (Param param : paramSet.getParams()) {
                ParamBean<?> bean = ParamBeanFactory.createBeanFromParam(wdkModel, user, param);
                params.put(param.getFullName(), bean);
            }
        }
        return params;
    }
    
    public RecordClassBean getRecordClass(String rcName) throws WdkModelException {
        return new RecordClassBean(wdkModel.getRecordClass(rcName));
    }

    @Override
    public Connection getConnection(String key) 
        throws WdkModelException, SQLException {
      return wdkModel.getConnection(key);
    }

    /**
     * Checks for a valid question name and throws WdkUserException if param is
     * not valid.  For now we simply check that it is a valid two-part name
     * (i.e. \S+\.\S+), so we will still get a WdkModelException down the line
     * if the question name is the correct format but does not actually exist.
     * We do this because sometimes developers change question names in one
     * place but not another and if so, then we want to know about it.  If we
     * mask this mistake with a WdkUserException, we might see bad consequences
     * down the line.
     * 
     * @param qFullName potential question name
     * @throws WdkUserException if name is not in format *.*
     */
    public void validateQuestionFullName(String qFullName) throws WdkUserException {
      String message = "The search '" + qFullName + "' is not or is no longer available.";
      try {
        // First check to see if this is a 'regular' question; if not, check XML questions
        if (qFullName == null || wdkModel.getQuestion(qFullName) == null) {
          throw new WdkModelException("Question name is null or resulting question is null");
        }
      }
      catch (WdkModelException e) {
        try {
          // exception will be thrown below; will mean that name is neither 'regular' question nor XML
          Reference r = new Reference(qFullName);
          XmlQuestionSet xqs = wdkModel.getXmlQuestionSet(r.getSetName());
          xqs.getQuestion(r.getElementName());
        }
        catch (WdkModelException e2) {
          throw new WdkUserException(message, e2);
        }
      }
    }

    /**
     * Checks for a valid record class name and throws WdkUserException if param
     * is not valid.  For now we simply check that it is a valid two-part name
     * (i.e. \S+\.\S+), so we will still get a WdkModelException down the line
     * if the record class name is the correct format but does not actually
     * exist.  We do this because sometimes developers change record class names
     * in one place but not another and if so, then we want to know about it.
     * If we mask this mistake with a WdkUserException, we might see bad
     * consequences down the line.
     * 
     * @param recordClassName potential record class name
     * @throws WdkUserException if name is not in format *.*
     */
    public void validateRecordClassName(String recordClassName) throws WdkUserException {
      String message = "The record type '" + recordClassName + "' is not or is no longer available.";
      try {
        // First check to see if this is a 'regular' record class; if not, check XML record classes
        if (recordClassName == null || wdkModel.getRecordClass(recordClassName) == null) {
          throw new WdkModelException("RecordClass name is null or resulting RecordClass is null");
        }
      }
      catch (WdkModelException e) {
        try {
          // exception will be thrown below; will mean that name is neither 'regular' RC nor XML
          Reference r = new Reference(recordClassName);
          XmlRecordClassSet xrcs = wdkModel.getXmlRecordClassSet(r.getSetName());
          xrcs.getRecordClass(r.getElementName());
        }
        catch (WdkModelException e2) {
          throw new WdkUserException(message, e2);
        }
      }
    }
}
