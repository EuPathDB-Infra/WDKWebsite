package org.gusdb.wdk.controller.action;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.gusdb.wdk.controller.actionutil.ActionUtility;
import org.gusdb.wdk.model.jspwrap.AttributeFieldBean;
import org.gusdb.wdk.model.jspwrap.QuestionBean;
import org.gusdb.wdk.model.jspwrap.StepBean;
import org.gusdb.wdk.model.jspwrap.UserBean;
import org.gusdb.wdk.model.record.attribute.plugin.AttributePlugin;
import org.gusdb.wdk.model.record.attribute.plugin.AttributePluginReference;

public class InvokeAttributePluginAction extends Action {
    
    private static final String PARAM_STEP = "step";
    private static final String PARAM_ATTRIBUTE = "attribute";
    private static final String PARAM_PLUGIN = "plugin";

    private static final String ATTR_PLUGIN = "plugin";
    private static final String ATTR_QUESTION = "question";
    private static final String ATTR_ATTRIBUTE = "attribute";

    private static final String FORWARD_DISPLAY = "display";

    private Logger logger = Logger.getLogger(InvokeAttributePluginAction.class);

    @Override
    public ActionForward execute(ActionMapping mapping, ActionForm form,
                HttpServletRequest request, HttpServletResponse response) throws Exception {
        logger.debug("Entering InvokeAttributePluginAction...");
        UserBean user = ActionUtility.getUser(request);

        // get the step
        String strStep = request.getParameter(PARAM_STEP);
        long stepId = Long.valueOf(strStep);
        StepBean step = user.getStep(stepId);

        // get the attribute
        String attributeName = request.getParameter(PARAM_ATTRIBUTE);
        QuestionBean questions = step.getQuestion();
        AttributeFieldBean attribute = questions.getAttributeFields().get(attributeName);

        // get the plugin
        String pluginName = request.getParameter(PARAM_PLUGIN);
        AttributePluginReference reference = attribute.getAttributePlugins().get(pluginName);
        AttributePlugin plugin = reference.getPlugin();

        logger.debug("Processing attribute plugin: " + pluginName);
        Map<String, Object> results = plugin.process(step.getStep());
        for (String key : results.keySet()) {
            request.setAttribute(key, results.get(key));
        }
        request.setAttribute(ATTR_PLUGIN, plugin);
        request.setAttribute(ATTR_QUESTION,  step.getQuestion());
        request.setAttribute(ATTR_ATTRIBUTE, attribute);

        ActionForward forward = mapping.findForward(FORWARD_DISPLAY);
        logger.debug("Leaving InvokeAttributePluginAction. to: " + forward);
        return forward;
    }
}
