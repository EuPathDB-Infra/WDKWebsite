/**
 * 
 */
package org.gusdb.wdk.controller.action;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.gusdb.wdk.controller.actionutil.ActionUtility;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.WdkUserException;
import org.gusdb.wdk.model.jspwrap.RecordClassBean;
import org.gusdb.wdk.model.jspwrap.StepBean;
import org.gusdb.wdk.model.jspwrap.UserBean;
import org.gusdb.wdk.model.jspwrap.WdkModelBean;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * @author xingao
 * 
 *         the actions for shopping basket include following:
 *         <ul>
 *         <li>add: add a list of records of a given type into basket;</li>
 *         <li>remove: remove a list of records of a given type into basket;</li>
 *         <li>add-all: add all records from a step into basket;</li>
 *         <li>remove-all:: remove all records from a step into basket;</li>
 *         <li>clear: remove all records from basket of a given type;</li>
 *         <li>check: check if given record is already in basket and return boolean;</li>
 *         </ul>
 */
public class ProcessBasketAction extends Action {

    /**
     * the action to shopping basket
     */
    private static final String PARAM_ACTION = "action";
    /**
     * the record type of the basket. It is a full recordClass name
     */
    private static final String PARAM_TYPE = "type";
    /**
     * the data for the corresponding action. It can be a JSON list of primary
     * keys, or a step display id.
     */
    private static final String PARAM_DATA = "data";

    /**
     * add a list of ids into basket. It requires TYPE & DATA params, and DATA
     * is a JSON list of primary keys.
     */
    private static final String ACTION_ADD = "add";
    /**
     * remove a list of ids into basket. It requires TYPE & DATA params, and
     * DATA is a JSON list of primary keys.
     */
    private static final String ACTION_REMOVE = "remove";
    /**
     * Add all records from a step into shopping basket. it requires only DATA
     * param, and DATA is a step display id.
     */
    private static final String ACTION_ADD_ALL = "add-all";
    /**
     * remove all records from a step into shopping basket. it requires only
     * DATA param, and DATA is a step display id.
     */
    private static final String ACTION_REMOVE_ALL = "remove-all";
    /**
     * clear the shopping basket. it requires only TYPE param, and TYPE is the
     * full recordClass name.
     */
    private static final String ACTION_CLEAR = "clear";

    private static final String ACTION_CHECK = "check";
    
    private static final Logger logger = Logger.getLogger(ProcessBasketAction.class);

    @Override
    public ActionForward execute(ActionMapping mapping, ActionForm form,
            HttpServletRequest request, HttpServletResponse response)
            throws Exception {
        logger.debug("Entering ProcessBasketAction...");

        try {
        UserBean user = ActionUtility.getUser(request);
        WdkModelBean wdkModel = ActionUtility.getWdkModel(servlet);
        String action = request.getParameter(PARAM_ACTION);
        String data = request.getParameter(PARAM_DATA);
        String type = request.getParameter(PARAM_TYPE);
        if (action == null)
          throw new WdkUserException("required action param is missing");
        
        int numProcessed = 0;
        if (action.equalsIgnoreCase(ACTION_ADD)) {
            // need type & data params, where data is a JSON list of record ids
            RecordClassBean recordClass = getRecordClass(type, wdkModel);
            List<String[]> records = getRecords(data, recordClass);
            user.addToBasket(recordClass, records);
        } else if (action.equalsIgnoreCase(ACTION_REMOVE)) {
            // need type & data params, where data is a JSON list of record ids
            RecordClassBean recordClass = getRecordClass(type, wdkModel);
            List<String[]> records = getRecords(data, recordClass);
            user.removeFromBasket(recordClass, records);
        } else if (action.equalsIgnoreCase(ACTION_ADD_ALL)) {
            // only need the data param, and it is a step display id
            StepBean step = getStep(data, user);
            user.addToBasket(step);
        } else if (action.equalsIgnoreCase(ACTION_REMOVE_ALL)) {
            // only need the data param, and it is a step display id
            StepBean step = getStep(data, user);
            user.removeFromBasket(step);
        } else if (action.equalsIgnoreCase(ACTION_CLEAR)) {
            // only need the type param, and it is the recordClass full name
            RecordClassBean recordClass = getRecordClass(type, wdkModel);
            user.clearBasket(recordClass);
        } else if (action.equalsIgnoreCase(ACTION_CHECK)) {
        	RecordClassBean recordClass = getRecordClass(type, wdkModel);
        	List<String[]> records = getRecords(data, recordClass);
        	numProcessed = user.getBasketCount(records, recordClass);
        } else {
            throw new WdkUserException("Unknown Basket operation: '" + action
                    + "'.");
        }

        // output the total count
        JSONObject jsMessage = new JSONObject();

        int count = user.getBasketCount();
        jsMessage.put("all", count);
        jsMessage.put("processed", numProcessed);

        // output each record count
        JSONObject jsRecordCounts = new JSONObject();
        Map<RecordClassBean, Integer> counts = user.getBasketCounts();
        for (RecordClassBean record : counts.keySet()) {
          jsRecordCounts.put(record.getFullName(), counts.get(record));
        }
        jsMessage.put("records", jsRecordCounts);

        PrintWriter writer = response.getWriter();
        writer.print(jsMessage.toString());

        logger.debug("Leaving ProcessBasketAction...");
        return null;
        } catch (Exception ex) {
            logger.error(ex);
            ex.printStackTrace();
            throw ex;
        }
    }

    protected RecordClassBean getRecordClass(String type,
            WdkModelBean wdkModel) throws WdkModelException, WdkUserException {
        // get recordClass
        if (type == null)
          throw new WdkUserException("required type param is missing");
        return wdkModel.findRecordClass(type);
    }

    private StepBean getStep(String data, UserBean user)
            throws WdkUserException, WdkModelException {
        // get the step from step id
        if (data == null || !data.matches("^\\d+$"))
            throw new WdkUserException("The content for '" + PARAM_DATA
                    + "' is not a valid step display id: '" + data + "'.");

        int stepId = Integer.parseInt(data);
        return user.getStep(stepId);
    }

    /**
     * @throws WdkModelException  
     */
    protected List<String[]> getRecords(String data,
            RecordClassBean recordClass) throws JSONException, WdkUserException, WdkModelException {
        if (data == null)
            throw new WdkUserException("the record ids list is missing.");

        String[] pkColumns = recordClass.getPrimaryKeyColumns();
        JSONArray array = new JSONArray(data);
        List<String[]> ids = new ArrayList<String[]>();
        for (int i = 0; i < array.length(); i++) {
            JSONObject object = array.getJSONObject(i);
            String[] values = new String[pkColumns.length];
            for (int j = 0; j < values.length; j++) {
                values[j] = object.getString(pkColumns[j]);
            }
            ids.add(values);
        }
        return ids;
    }
}
