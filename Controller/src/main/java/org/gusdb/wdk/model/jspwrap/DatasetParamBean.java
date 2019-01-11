/**
 * 
 */
package org.gusdb.wdk.model.jspwrap;

import java.util.Collection;

import org.apache.log4j.Logger;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.record.RecordClass;
import org.gusdb.wdk.model.dataset.DatasetParser;
import org.gusdb.wdk.model.query.param.DatasetParam;
import org.gusdb.wdk.model.user.Strategy;

/**
 * @author xingao
 * 
 */
public class DatasetParamBean extends ParamBean<DatasetParam> {

  @SuppressWarnings("unused")
  private static final Logger logger = Logger.getLogger(DatasetParamBean.class.getName());

  private final DatasetParam datasetParam;

  public DatasetParamBean(DatasetParam datasetParam) {
    super(datasetParam);
    this.datasetParam = datasetParam;
  }

  public DatasetBean getDataset() throws WdkModelException {
    long userDatasetId = Long.valueOf(_stableValue);
    DatasetBean dataset = _userBean.getDataset(userDatasetId);
    return dataset;
  }

  public String getDefaultType() {
    return _param.getDefaultType();
  }

  /**
   * @return
   * @see org.gusdb.wdk.model.query.param.DatasetParam#getTypeSubParam()
   */
  public String getTypeSubParam() {
    return datasetParam.getTypeSubParam();
  }

  /**
   * @return
   * @see org.gusdb.wdk.model.query.param.DatasetParam#getFileSubParam()
   */
  public String getFileSubParam() {
    return datasetParam.getFileSubParam();
  }

  public String getDataSubParam() {
    return datasetParam.getDataSubParam();
  }

  public String getStrategySubParam() {
    return datasetParam.getStrategySubParam();
  }

  public String getParserSubParam() {
    return datasetParam.getParserSubParam();
  }
  
  public Collection<DatasetParser> getParsers() {
    return datasetParam.getParsers();
  }

  public RecordClassBean getRecordClass() {
    RecordClass recordClass = datasetParam.getRecordClass();
    return (recordClass == null) ? null : new RecordClassBean(recordClass);
  }

  public Integer getBasketCount() throws WdkModelException {
    return datasetParam.getBasketCount(_userBean.getUser());
  }

  public Strategy[] getStrategies() throws WdkModelException {
    return datasetParam.getStrategies(_userBean.getUser());
  }
}
