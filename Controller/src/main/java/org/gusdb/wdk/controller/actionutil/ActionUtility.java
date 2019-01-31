package org.gusdb.wdk.controller.actionutil;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;

import org.gusdb.wdk.model.Utilities;
import org.gusdb.wdk.model.jspwrap.WdkModelBean;

/**
 * Heritage methods to access the current user,  model, and request params
 * 
 * @author xingao
 */
public class ActionUtility {

    public static WdkModelBean getWdkModel(HttpServlet servlet) {
        return (WdkModelBean)servlet.getServletContext().getAttribute(Utilities.WDK_MODEL_BEAN_KEY);
    }

    public static Map<String, String> getParams(ServletRequest request) {
        Map<String, String> params = new HashMap<String, String>();
        Enumeration<?> names = request.getParameterNames();
        while (names.hasMoreElements()) {
            String name = (String) names.nextElement();
            String[] values = request.getParameterValues(name);
            String value = Utilities.fromArray(values, ",");
            params.put(name, value);
        }
        return params;
    }

    public static void applyModel(HttpServletRequest request, Map<String, Object> model) {
      for (Entry<String, Object> modelValue : model.entrySet()) {
        request.setAttribute(modelValue.getKey(), modelValue.getValue());
      }
    }

}
