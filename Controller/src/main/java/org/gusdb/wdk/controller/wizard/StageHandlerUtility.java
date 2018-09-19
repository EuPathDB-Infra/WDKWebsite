package org.gusdb.wdk.controller.wizard;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.gusdb.wdk.controller.action.WizardAction;
import org.gusdb.wdk.controller.actionutil.ActionUtility;
import org.gusdb.wdk.controller.form.WizardForm;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.WdkUserException;
import org.gusdb.wdk.model.jspwrap.StepBean;
import org.gusdb.wdk.model.jspwrap.StrategyBean;
import org.gusdb.wdk.model.jspwrap.UserBean;

public class StageHandlerUtility {
   
    private static final Logger logger = Logger.getLogger(StageHandlerUtility.class);
 
    public static StrategyBean getCurrentStrategy(HttpServletRequest request) {
        return (StrategyBean)request.getAttribute(WizardAction.ATTR_STRATEGY);
    }

    public static StepBean getCurrentStep(HttpServletRequest request) {
        return (StepBean) request.getAttribute(WizardAction.ATTR_STEP);
    }

    public static StepBean getRootStep(HttpServletRequest request, WizardForm wizardForm)
            throws WdkUserException, NumberFormatException, WdkModelException {
        // get current strategy
        String strategyKey = wizardForm.getStrategy();
        if (strategyKey == null || strategyKey.length() == 0)
            throw new WdkUserException("No strategy was specified for "
                    + "processing!");

        // did we get strategyId_stepId?
        int pos = strategyKey.indexOf("_");
        String strStratId = (pos > 0) ? strategyKey.substring(0, pos)
                : strategyKey;

        // get strategy, and verify the checksum
        UserBean user = ActionUtility.getUser(request);
        StrategyBean strategy = user.getStrategy(Integer.parseInt(strStratId));

        logger.debug("strategy key: " + strategyKey);

        // load branch root, if exists
        StepBean rootStep;
        if (pos > 0) {
            long branchRootId = Long.valueOf(strategyKey.substring(pos + 1));
            rootStep = strategy.getStepById(branchRootId);
        } else {
            rootStep = strategy.getLatestStep();
        }
        logger.debug("root step: " + rootStep);
        return rootStep;
    }

    public static StepBean getPreviousStep(HttpServletRequest request, WizardForm wizardForm)
            throws NumberFormatException, WdkUserException, WdkModelException {
        StepBean previousStep;
        String action = wizardForm.getAction();
        if (action.equals(WizardForm.ACTION_ADD)) {
            // add, the current step is the last step of a strategy or a
            // sub-strategy, use it as the input;
            previousStep = StageHandlerUtility.getRootStep(request, wizardForm);
        }
        else { // revise or insert,
            // the current step is always the lower step in the graph, no
            // matter whether it's a boolean, or a combined step. Use the
            // previous step as the input.
            StepBean currentStep = StageHandlerUtility.getCurrentStep(request);
            previousStep = currentStep.getPreviousStep();
        }
        return previousStep;
    }
}
