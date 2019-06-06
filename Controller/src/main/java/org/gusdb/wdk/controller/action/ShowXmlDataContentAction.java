package org.gusdb.wdk.controller.action;

import java.io.File;
import java.io.IOException;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.gusdb.wdk.controller.CConstants;
import org.gusdb.wdk.controller.actionutil.ActionUtility;
import org.gusdb.wdk.model.WdkException;
import org.gusdb.wdk.model.WdkRuntimeException;
import org.gusdb.wdk.model.jspwrap.WdkModelBean;
import org.gusdb.wdk.model.jspwrap.XmlAnswerBean;
import org.gusdb.wdk.model.jspwrap.XmlQuestionBean;
import org.gusdb.wdk.model.jspwrap.XmlQuestionSetBean;

/**
 * This Action is called by the ActionServlet when a WDK xml question is asked.
 * It 1) reads the question name param value,
 *    2) runs the xml query and saves the answer
 *    3) forwards control to a jsp page that displays the full result
 */

public class ShowXmlDataContentAction extends HttpServlet {

  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    try {
      String xmlQName = request.getParameter(CConstants.NAME);
      ActionUtility.getWdkModel(this).validateQuestionFullName(xmlQName);
      XmlQuestionBean xmlQuestion = getXmlQuestionByFullName(xmlQName);
      XmlAnswerBean xmlAnswer = xmlQuestion.getFullAnswer();
      request.setAttribute(CConstants.WDK_XMLANSWER_KEY, xmlAnswer);
      request.getRequestDispatcher(getForward(xmlAnswer)).forward(request, response);
    }
    catch (WdkException e) {
      throw new WdkRuntimeException("Could not process request.", e);
    }
  }

  protected XmlQuestionBean getXmlQuestionByFullName(String qFullName) {
    int dotI = qFullName.indexOf('.');
    String qSetName = qFullName.substring(0, dotI);
    String qName = qFullName.substring(dotI+1, qFullName.length());

    WdkModelBean wdkModel = ActionUtility.getWdkModel(this);

    XmlQuestionSetBean wdkQuestionSet = wdkModel.getXmlQuestionSetsMap().get(qSetName);
    XmlQuestionBean wdkQuestion = wdkQuestionSet.getQuestionsMap().get(qName);
    return wdkQuestion;
  }

  private String getForward (XmlAnswerBean xmlAnswer) {
    ServletContext svltCtx = getServletContext();
    String customViewDir = CConstants.WDK_CUSTOM_VIEW_DIR
        + File.separator + CConstants.WDK_PAGES_DIR;

    String defaultViewFile = customViewDir
        + File.separator + CConstants.WDK_XMLDATACONTENT_PAGE;

    customViewDir += File.separator + CConstants.WDK_QUESTIONS_DIR;

    System.out.println(defaultViewFile);

    String customViewFile1 = customViewDir + File.separator
        + xmlAnswer.getQuestion().getFullName() + ".jsp";
    String customViewFile2 = customViewDir + File.separator
        + xmlAnswer.getRecordClass().getFullName() + ".jsp";
    return
        ActionUtility.resourceExists(customViewFile1, svltCtx) ? customViewFile1 :
        ActionUtility.resourceExists(customViewFile2, svltCtx) ? customViewFile2 :
        defaultViewFile;
  }

}
