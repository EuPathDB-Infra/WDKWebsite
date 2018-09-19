package org.gusdb.wdk.controller.form;

import javax.servlet.http.HttpServletRequest;

import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;

public class WizardForm extends MapActionForm {

    public static final String ACTION_ADD = "add";
    public static final String ACTION_REVISE = "revise";
    public static final String ACTION_INSERT = "insert";

    /**
     * 
     */
    private static final long serialVersionUID = 3608316943911485689L;

    private String strategyKey;
    private int step;
    private String stage;
    private String action;

    /**
     * @return the strategy
     */
    public String getStrategy() {
        return strategyKey;
    }

    public long getStrategyId() {
        int pos = strategyKey.indexOf("_");
        String strId = (pos < 0) ? strategyKey : strategyKey.substring(0, pos);
        return Long.valueOf(strId);
    }

    /**
     * @param strategyKey
     *            the strategy to set
     */
    public void setStrategy(String strategyKey) {
        this.strategyKey = strategyKey;
    }

    /**
     * @return the step
     */
    public int getStep() {
        return step;
    }

    /**
     * @param step
     *            the step to set
     */
    public void setStep(int step) {
        this.step = step;
    }

    /**
     * @return the stage
     */
    public String getStage() {
        return stage;
    }

    /**
     * @param stage
     *            the stage to set
     */
    public void setStage(String stage) {
        this.stage = stage;
    }

    /**
     * @return the action
     */
    public String getAction() {
        return (action == null) ? ACTION_ADD : action;
    }

    /**
     * @param action
     *            the action to set
     */
    public void setAction(String action) {
        this.action = action;
    }

    @Override
    public ActionErrors validate(ActionMapping mapping,
            HttpServletRequest request) {
        ActionErrors errors = super.validate(mapping, request);
        if (errors == null) errors = new ActionErrors();

        // validate strategy id
        if (strategyKey == null || strategyKey.length() == 0) {
            ActionMessage message = new ActionMessage("mapped.properties",
                    "required strategy param is not assigned. Please assign "
                            + "it with strategy key.");
            errors.add(ActionErrors.GLOBAL_MESSAGE, message);
        }

        // validate action
        if (action == null) action = ACTION_ADD;
        if (!action.equals(ACTION_ADD) && !action.equals(ACTION_INSERT)
                && !action.equals(ACTION_REVISE)) {
            ActionMessage message = new ActionMessage("mapped.properties",
                    "Invalid action: " + action);
            errors.add(ActionErrors.GLOBAL_MESSAGE, message);
        }

        return errors;
    }
}
