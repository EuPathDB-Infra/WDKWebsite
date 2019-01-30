package org.gusdb.wdk.controller.summary;

import java.util.Map;

import org.gusdb.fgputil.validation.ValidObjectFactory.RunnableObj;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.WdkUserException;
import org.gusdb.wdk.model.answer.SummaryViewHandler;
import org.gusdb.wdk.model.answer.factory.AnswerValueFactory;
import org.gusdb.wdk.model.answer.spec.AnswerSpec;
import org.gusdb.wdk.model.jspwrap.AnswerValueBean;
import org.gusdb.wdk.model.question.Question;
import org.gusdb.wdk.model.user.User;

public class DefaultSummaryViewHandler implements SummaryViewHandler {

  @Override
  public Map<String, Object> process(RunnableObj<AnswerSpec> answerSpec, Map<String, String[]> parameters,
      User user) throws WdkModelException, WdkUserException {
    AnswerValueBean answer = new AnswerValueBean(AnswerValueFactory.makeAnswer(user, answerSpec));
    answer.getRecords();
    return ResultTablePaging.processPaging(parameters,
        answerSpec.getObject().getQuestion(), user, answer.getAnswerValue());
  }

  @Override
  public String processUpdate(RunnableObj<AnswerSpec> answerSpec, Map<String, String[]> parameters, User user)
      throws WdkModelException, WdkUserException {
    Question question = answerSpec.getObject().getQuestion();
    return SummaryTableUpdateProcessor.processUpdates(question, parameters, user, "");
  }

}
