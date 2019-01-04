package org.gusdb.wdk.model.jspwrap;

import java.util.Map;

import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.answer.AnswerValue;
import org.gusdb.wdk.model.query.param.AnswerParam;
import org.gusdb.wdk.model.record.RecordClass;
import org.gusdb.wdk.model.user.Step;
import org.gusdb.wdk.model.user.User;

/**
 * @author xingao
 * 
 */
public class AnswerParamBean extends ParamBean<AnswerParam> {

    public AnswerParamBean(AnswerParam answerParam) {
        super(answerParam);
    }

    @Deprecated
    public StepBean[] getSteps(UserBean user) throws WdkModelException {
        // only get the steps for the first record class
        Map<String, RecordClass> recordClasses = _param.getAllowedRecordClasses();
        RecordClass recordClass = recordClasses.values().iterator().next();
        return user.getSteps(recordClass.getFullName());
    }

    public AnswerValueBean getAnswerValue() throws Exception {
      AnswerParam answerParam = _param;
        try {
            User user = this._userBean.getUser();
            Step step = (Step)answerParam.getRawValue(user, _stableValue);
            AnswerValue answerValue = step.getAnswerValue();
            return new AnswerValueBean(answerValue);
        } catch (Exception ex) {
            ex.printStackTrace();
            throw ex;
        }
    }

    /**
     * @param recordClassName
     * @return
     * @see org.gusdb.wdk.model.query.param.AnswerParam#allowRecordClass(java.lang.String)
     */
    public boolean allowRecordClass(String recordClassName) {
        return _param.allowRecordClass(recordClassName);
    }
}
