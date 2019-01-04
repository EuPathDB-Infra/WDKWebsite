package org.gusdb.wdk.model.jspwrap;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import org.gusdb.fgputil.FormatUtil;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.WdkUserException;
import org.gusdb.wdk.model.user.Step;
import org.gusdb.wdk.model.user.Strategy;

public class StrategyBean {

  private UserBean user;
  Strategy strategy;

  public StrategyBean(UserBean user, Strategy strategy) {
    this.user = user;
    this.strategy = strategy;
  }

  public UserBean getUser() {
    return user;
  }

  public boolean getIsDeleted() {
    return strategy.isDeleted();
  }

  public String getVersion() {
    return strategy.getVersion();
  }

  public String getName() {
    return strategy.getName();
  }

  public void setName(String name) {
    strategy.setName(name);
  }

  public String getSavedName() {
    return strategy.getSavedName();
  }

  public void setSavedName(String name) {
    strategy.setSavedName(name);
  }

  public void setIsSaved(boolean saved) {
    strategy.setIsSaved(saved);
  }

  public boolean getIsSaved() {
    return strategy.getIsSaved();
  }

  public String getLastRunTimeFormatted() {
    return formatDate(strategy.getLastRunTime());
  }

  public Date getLastRunTime() {
    return strategy.getLastRunTime();
  }

  public String getCreatedTimeFormatted() {
    return formatDate(strategy.getCreatedTime());
  }

  public Date getCreatedTime() {
    return strategy.getCreatedTime();
  }

  public String getLastModifiedTimeFormatted() {
    return formatDate(strategy.getLastModifiedTime());
  }

  /**
   * @return
   * @see org.gusdb.wdk.model.user.Strategy#getLastModifiedTime()
   */
  public Date getLastModifiedTime() {
    return strategy.getLastModifiedTime();
  }

  public boolean getIsPublic() {
    return strategy.getIsPublic();
  }

  public void setIsPublic(boolean isPublic) {
    strategy.setIsPublic(isPublic);
  }

  public StepBean getLatestStep() throws WdkModelException {
    return new StepBean(user, strategy.getLatestStep());
  }

  public long getLatestStepId() {
    return strategy.getLatestStepId();
  }

  public long getStrategyId() {
    return strategy.getStrategyId();
  }

  public StepBean getStep(int index) throws WdkModelException {
    return new StepBean(user, strategy.getStep(index));
  }

  public StepBean[] getAllSteps() throws WdkModelException {
    StepBean latestStep = new StepBean(user, strategy.getLatestStep());
    return latestStep.getMainBranch();
  }

  public void setLatestStep(StepBean step) throws WdkModelException {
    strategy.setLatestStep(step.step);
  }

  public StepBean getStepById(long stepId) throws WdkModelException {
    Step target = strategy.getStepById(stepId);
    if (target != null) {
      return new StepBean(user, target);
    }
    return null;
  }

  public int getLength() throws WdkModelException {
    return getAllSteps().length;
  }

  public void update(boolean overwrite) throws WdkUserException, WdkModelException {
    strategy.update(overwrite);
  }

  public Map<Long, Long> insertStepAfter(StepBean newStep, long targetId) throws WdkModelException, WdkUserException {
    return strategy.insertStepAfter(newStep.getStep(), targetId);
  }

  public  Map<Long, Long> insertStepBefore(StepBean newStep, long targetId) throws WdkModelException,
      WdkUserException {
    return strategy.insertStepBefore(newStep.getStep(), targetId);
  }

  public Map<Long, Long> deleteStep(StepBean step) throws WdkModelException, WdkUserException {
    return strategy.deleteStep(step.getStep());
  }

  public String getImportId() {
    return strategy.getSignature();
  }

  public StepBean getFirstStep() throws WdkModelException {
    return new StepBean(user, strategy.getFirstStep());
  }

  public String getChecksum() throws WdkModelException {
    return strategy.getChecksum();
  }

  /**
   * @return
   * @see org.gusdb.wdk.model.user.Strategy#getDescription()
   */
  public String getDescription() {
    return strategy.getDescription();
  }

  /**
   * @return
   * @see org.gusdb.wdk.model.user.Strategy#setDescription()
   */
  public void setDescription(String description) {
    strategy.setDescription(description);
  }

  /**
   * @return
   * @see org.gusdb.wdk.model.user.Strategy#getSignature()
   */
  public String getSignature() {
    return strategy.getSignature();
  }

  /**
   * @return
   * @throws WdkModelException 
   * @see org.gusdb.wdk.model.user.Strategy#getEstimateSize()
   */
  public int getEstimateSize() throws WdkModelException {
    return strategy.getEstimateSize();
  }

  public String getEstimateSizeNoCalculate() {
    return strategy.getEstimateSizeNoCalculate();
  }

  private String formatDate(Date date) {
    if (date == null)
      return "-";
    DateFormat formatter = new SimpleDateFormat(FormatUtil.STANDARD_DATETIME_FORMAT_DASH);
    return formatter.format(date);
  }

  public boolean isValidBasedOnStepFlags() {
    return strategy.isValidBasedOnStepFlags();
  }

  /**
   * @return
   * @see org.gusdb.wdk.model.user.Strategy#isValid()
   */
  public boolean isValid() throws WdkModelException {
    return strategy.isValid();
  }

  public RecordClassBean getRecordClass() throws WdkModelException {
    return new RecordClassBean(strategy.getRecordClass());
  }

  public Strategy getStrategy() {
    return strategy;
  }
}
