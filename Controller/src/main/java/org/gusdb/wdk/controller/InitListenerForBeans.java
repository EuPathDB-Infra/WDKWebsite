package org.gusdb.wdk.controller;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.log4j.Logger;
import org.gusdb.fgputil.events.Events;
import org.gusdb.wdk.events.NewUserEvent;
import org.gusdb.wdk.model.Utilities;
import org.gusdb.wdk.model.WdkModel;
import org.gusdb.wdk.model.jspwrap.UserBean;
import org.gusdb.wdk.model.jspwrap.WdkModelBean;

/**
 * WdkModelBean (attached to servlet context) and UserBean (attached to session
 * context) are unavailable to WDK Model/Service code, so we must add them to
 * those contexts in this package, which can see those classes.
 * 
 * This listener must be added AFTER the WdkModel object is created and added to
 * the webapp's servlet context.  To serve JSPs we will create a WdkModelBean of
 * the WdkModel and attach it also.
 * 
 * UserBeans will be attached to the session by subscribing to a NewUserEvent
 * and attaching a bean to the session at that time.
 * 
 * @author rdoherty
 */
public class InitListenerForBeans implements ServletContextListener {

  private static final Logger LOG = Logger.getLogger(InitListenerForBeans.class);

  @Override
  public void contextInitialized(ServletContextEvent sce) {
    try {
      // assign WdkModelBean onto context for JSPs/tags to read (service code will use the raw WdkModel)
      ServletContext servletContext = sce.getServletContext();
      WdkModel wdkModel = WdkInitializer.getWdkModel(servletContext);
      if (wdkModel == null) { /* no model to add */ return; }
      servletContext.setAttribute(Utilities.WDK_MODEL_BEAN_KEY, new WdkModelBean(wdkModel));
  
      // replace any previous UserBean object placed on session with one for new user
      Events.subscribe(event -> {
        NewUserEvent userEvent = (NewUserEvent)event;
        LOG.info("Assigning session UserBean for " + userEvent.getNewUser().getDisplayName());
        userEvent.getSession().setAttribute(Utilities.WDK_USER_BEAN_KEY,
            new UserBean(userEvent.getNewUser()));
      }, NewUserEvent.class);

      LOG.info("Successfully assigned WdkModelBean and subscribed to NewUserEvents.");
    }
    catch(Exception e) {
      LOG.error("Failed to initialize bean conversion.", e);
      throw e;
    }
  }

  @Override
  public void contextDestroyed(ServletContextEvent sce) {
    // nothing to do here
  }

}
