package org.gusdb.wdk.controller.action;

import static org.gusdb.fgputil.FormatUtil.urlEncodeUtf8;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.gusdb.fgputil.FormatUtil;
import org.gusdb.wdk.controller.CConstants;
import org.gusdb.wdk.controller.actionutil.ActionUtility;
import org.gusdb.wdk.controller.form.WizardForm;
import org.gusdb.wdk.controller.wizard.Result;
import org.gusdb.wdk.controller.wizard.Stage;
import org.gusdb.wdk.controller.wizard.StageHandler;
import org.gusdb.wdk.controller.wizard.Wizard;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.WdkUserException;
import org.gusdb.wdk.model.jspwrap.StepBean;
import org.gusdb.wdk.model.jspwrap.StrategyBean;
import org.gusdb.wdk.model.jspwrap.UserBean;
import org.gusdb.wdk.model.user.StepUtilities;

public class WizardAction extends Action {

    private static final Logger logger = Logger.getLogger(WizardAction.class);

    private static final String PARAM_STAGE = "stage";

    public static final String ATTR_STRATEGY = "wdkStrategy";
    public static final String ATTR_STEP = "wdkStep";
    public static final String ATTR_ACTION = "action";

    /*
     * (non-Javadoc)
     * 
     * @seeorg.apache.struts.action.Action#execute(org.apache.struts.action.
     * ActionMapping, org.apache.struts.action.ActionForm,
     * javax.servlet.http.HttpServletRequest,
     * javax.servlet.http.HttpServletResponse)
     */
    @Override
    public ActionForward execute(ActionMapping mapping, ActionForm form,
            HttpServletRequest request, HttpServletResponse response)
            throws Exception {
        logger.debug("Entering WizardAction.....");
        
        UserBean user = ActionUtility.getUser(request);
        try {
            WizardForm wizardForm = (WizardForm) form;

            // load strategy
            loadStrategy(request, wizardForm, user);

            // get stage
            Wizard wizard = (Wizard) servlet.getServletContext().getAttribute(
                    CConstants.WDK_WIZARD_KEY);
            String stageName = request.getParameter(PARAM_STAGE);
            Stage stage;
            if (stageName == null || stageName.length() == 0) {
                stage = wizard.getDefaultStage();
                stageName = stage.getName();
            } else {
                stage = wizard.queryStage(stageName);
            }
            logger.info("stage: " + stageName);

            // check if there is a handler
            StageHandler handler = stage.getHandler();
            Map<String, Object> attributes;
            if (handler != null) {
                attributes = handler.execute(servlet, request, response, wizardForm);
            } else attributes = new HashMap<String, Object>();
            
            if (!attributes.containsKey(ATTR_ACTION))
                attributes.put(ATTR_ACTION, wizardForm.getAction());

            Result result = stage.getResult();
            String type = result.getType();
            String forward = result.getText();
            if (type.equals(Result.TYPE_VIEW)) { // forward to a jsp.
                logger.debug("wizard view: " + forward);

                // add the input strategy & step key into the attribute
                request.setAttribute("strategy", wizardForm.getStrategy());
                request.setAttribute("step", wizardForm.getStep());

                // put values into attibute
                for (String key : attributes.keySet()) {
                    request.setAttribute(key, attributes.get(key));
                }

                logger.debug("Leaving WizardAction.....");
                return new ActionForward(forward);
            } else if (type.equals(Result.TYPE_ACTION)) { // forward to an
                                                          // action
                StringBuilder builder = new StringBuilder(forward);

                // // forward to an action, and add values to the url
                boolean first = (forward.indexOf('?') < 0);
                for (String key : attributes.keySet()) {
                    Object value = attributes.get(key);
                    String strValue = (value == null) ? "" : value.toString();
                    builder.append(first ? "?" : "&");
                    builder.append(urlEncodeUtf8(key) + "=");
                    builder.append(urlEncodeUtf8(strValue));
                    first = false;
                }

                long strategyId = wizardForm.getStrategyId();
                if (!Long.toString(strategyId).equals(user.getViewStrategyId())) {
                  // set view results to last step of strategy being revised
                  StrategyBean strategy = new StrategyBean(user, StepUtilities.getStrategy(user.getUser(), strategyId));
                  long stepId = strategy.getLatestStepId();
                  user.getUser().getSession().setViewResults(Long.toString(strategyId), stepId, 0);
                }

                logger.debug("wizard action: " + builder);
                logger.debug("Leaving WizardAction.....");
                return new ActionForward(builder.toString());
            } else {
                throw new WdkModelException("Invalid result type: " + type);
            }
        } catch (Exception ex) {
            logger.error(ex);
            logger.error("Here's the trace\n" + FormatUtil.getStackTrace(ex));
            ShowStrategyAction.outputErrorJSON(user, response, ex);
            return null;
        }

    }

    private void loadStrategy(HttpServletRequest request,
            WizardForm wizardForm, UserBean user) throws WdkUserException,
            WdkModelException {
        long stratId = wizardForm.getStrategyId();
        StrategyBean strategy = user.getStrategy(stratId);

        int stepId = wizardForm.getStep();
        StepBean step;
        if (stepId == 0) {
            // get the last step from the strategy
            step = strategy.getLatestStep();
        } else {
            step = strategy.getStepById(stepId);
        }

        request.setAttribute(ATTR_STRATEGY, strategy);
        request.setAttribute(ATTR_STEP, step);
    }
}
