package org.gusdb.wdk.controller.action;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.gusdb.wdk.controller.CConstants;
import org.gusdb.wdk.controller.actionutil.ActionUtility;
import org.gusdb.wdk.controller.actionutil.WdkAction;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.jspwrap.AttributeFieldBean;
import org.gusdb.wdk.model.jspwrap.QuestionBean;
import org.gusdb.wdk.model.jspwrap.QuestionSetBean;
import org.gusdb.wdk.model.jspwrap.WdkModelBean;

/**
 * This Action is a glue action to allow display of questionSetsFlat to be
 * handled uniformly. It forwards on the control
 */

public class ShowQuestionSetsAction extends Action {

    @Override
    public ActionForward execute(ActionMapping mapping, ActionForm form,
            HttpServletRequest request, HttpServletResponse response)
            throws Exception {

        ServletContext svltCtx = getServlet().getServletContext();
        String customViewDir = CConstants.WDK_CUSTOM_VIEW_DIR + File.separator
                + CConstants.WDK_PAGES_DIR;
        String customViewFile = customViewDir + File.separator
                + CConstants.WDK_CUSTOM_QUESTIONSETS_PAGE;

        ActionForward forward = null;
        if (WdkAction.resourceExists(customViewFile, svltCtx)) {
            forward = new ActionForward(customViewFile);
        } else {
            forward = mapping.findForward(CConstants.SHOW_QUESTIONSETS_MAPKEY);
        }

        sessionStart(request);

        return forward;
    }

    protected void sessionStart(HttpServletRequest request)
            throws WdkModelException {
      WdkModelBean wdkModel = ActionUtility.getWdkModel(servlet);
        Map<String, Map<String, Map<String, AttributeFieldBean>>> sumAttrsByQuestion = getSummaryAttributesByQuestionMap(wdkModel);
        request.getSession().setAttribute(CConstants.WDK_SUMMARY_ATTRS_KEY, sumAttrsByQuestion);
    }

    private static Map<String, Map<String, Map<String, AttributeFieldBean>>> getSummaryAttributesByQuestionMap(
            WdkModelBean wdkModel) throws WdkModelException {
        Map<String, Map<String, Map<String, AttributeFieldBean>>> sumAttrsByQuestion = new LinkedHashMap<String, Map<String, Map<String, AttributeFieldBean>>>();
        QuestionSetBean[] qSets = wdkModel.getQuestionSets();
        for (QuestionSetBean qSet : qSets) {
            QuestionBean[] qs = qSet.getQuestions();
            for (QuestionBean q : qs) {
                String key = qSet.getName() + "_" + q.getName();
                Map<String, AttributeFieldBean> toShow = q.getSummaryAttributesMap();
                Map<String, AttributeFieldBean> toAdd = q.getAdditionalSummaryAttributesMap();
                Map<String, Map<String, AttributeFieldBean>> theMap = new LinkedHashMap<String, Map<String, AttributeFieldBean>>();
                theMap.put("show", toShow);
                theMap.put("add", toAdd);
                sumAttrsByQuestion.put(key, theMap);
            }
        }
        return sumAttrsByQuestion;
    }
}
