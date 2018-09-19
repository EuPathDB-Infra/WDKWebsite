package org.gusdb.wdk.controller.actionutil;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.fileupload.servlet.ServletRequestContext;
import org.apache.log4j.Logger;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.gusdb.fgputil.IoUtil;
import org.gusdb.fgputil.web.HttpRequestData;
import org.gusdb.fgputil.web.RequestData;
import org.gusdb.wdk.controller.CConstants;
import org.gusdb.wdk.controller.WdkValidationException;
import org.gusdb.wdk.controller.actionutil.ParamDef.Count;
import org.gusdb.wdk.controller.actionutil.ParamDef.DataType;
import org.gusdb.wdk.controller.actionutil.ParamDef.Required;
import org.gusdb.wdk.controller.actionutil.ParameterValidator.SecondaryValidator;
import org.gusdb.wdk.model.Utilities;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.WdkResourceChecker;
import org.gusdb.wdk.model.WdkRuntimeException;
import org.gusdb.wdk.model.WdkUserException;
import org.gusdb.wdk.model.jspwrap.UserBean;
import org.gusdb.wdk.model.jspwrap.WdkModelBean;

/**
 * Abstract class meant to provide a variety of commonly used utilities, a
 * somewhat more restricted API for action development, and a common way to
 * respond to server requests.  Also always takes care of setting the
 * appropriate mime type and file name of requests, and provides a standard
 * framework to validate request parameters and view request attributes before
 * they are sent to the JSP. 
 * 
 * @author rdoherty
 */
public abstract class WdkAction implements SecondaryValidator, WdkResourceChecker {

  private static final Logger LOG = Logger.getLogger(WdkAction.class.getName());

  // global response strings (i.e. named action forwards)
  /** global response string indicating success */
  public static final String SUCCESS = "success";
  /** global response string indicating further user input is needed */
  public static final String INPUT = "input";
  /** global response string indicating user needs to login for this action */
  public static final String NEEDS_LOGIN = "needs_login";
  /** global response string indicating an error occurred */
  public static final String ERROR = "application_error";
  
  /** provides empty param map for actions expecting no params */
  protected static final Map<String, ParamDef> EMPTY_PARAMS = new HashMap<>();
  
  // accessors for exception information if error is thrown
  public static final String EXCEPTION_PAGE = "exceptionPage";
  public static final String EXCEPTION_USER = "exceptionUser";
  public static final String EXCEPTION_OBJ = "exceptionObj";

  // internal site URLs often contain this parameter from the auth service
  private static final String AUTH_TICKET = "auth_tkt";

  // timestamp param for spam checking
  private static final String SPAM_TIMESTAMP = "__ts";

  // default max upload file size
  private static final int DEFAULT_MAX_UPLOAD_SIZE_MB = 10;

  // default response type (for errors and validation failures)
  private static final ResponseType DEFAULT_RESPONSE_TYPE = ResponseType.html;
  
  private WdkModelBean _wdkModel;
  private HttpServlet _servlet;
  private HttpServletRequest _request;
  private HttpServletResponse _response;
  private ParamGroup _params;
  private ResponseType _responseType;
  private ActionForm _strutsActionForm;

  protected abstract boolean shouldValidateParams();
  protected abstract Map<String, ParamDef> getParamDefs();
  protected abstract ActionResult handleRequest(ParamGroup params) throws Exception;

  /**
   * Executes this request, delegating business logic processing to the child
   * class.  Then translates the ActionResult returned by the handleRequest()
   * method and translates it into a response that can be understood by the
   * MVC framework.
   * 
   * @param mapping Struts1 action mapping
   * @param form Struts1 action form for this action
   * @param request servlet request
   * @param response servlet response
   * @param servlet servlet
   * @return the appropriate Struts1 ActionForward
   * @throws Exception if response cannot be translated into an ActionForward
   */
  public final ActionForward execute(ActionMapping mapping, ActionForm form,
      HttpServletRequest request, HttpServletResponse response, HttpServlet servlet)
        throws Exception {
    try {
      _request = request;
      _response = response;
      _servlet = servlet;
      _wdkModel = ActionUtility.getWdkModel(servlet);
      _responseType = DEFAULT_RESPONSE_TYPE;
      _strutsActionForm = form;
      
      if (requiresLogin() && getCurrentUser().isGuest()) {
        return getForwardFromResult(new ActionResult().setViewName(NEEDS_LOGIN), mapping);
      }
      
      ActionResult result;
      try {
        _params = createParamGroup(getRequestData().getTypedParamMap());
        result = handleRequest(_params);
      }
      catch (WdkValidationException wve) {
        // attach errors to request and return INPUT
        return getForwardFromResult(getValidationFailureResult(wve), mapping);
      }

      if (result == null || result.isEmptyResult()) {
        return null;
      }
      else {
        _response.setStatus(result.getHttpResponseStatus());
        if (result.isStream()) {
          // handle stream response
          if (result.getFileName().isEmpty()) {
            result.setFileName(result.getResponseType().getDefaultFileName());
          }
          _response.setContentType(result.getResponseType().getMimeType());
          _response.setHeader("Content-Disposition",
              (ResponseDisposition.ATTACHMENT.equals(result.getResponseDisposition()) ?
                  "attachment; filename=\"" + result.getFileName() + "\"" :
                  "inline"));  // default to inline if disposition is null
          IoUtil.transferStream(_response.getOutputStream(), result.getStream());
          return null;
        }
        else if (result.isExternalRedirect()){
          _response.sendRedirect(result.getExternalPath());
          return null;
        }
        else {
          // otherwise, handle normal response
          assignAttributesToRequest(result);
          return getForwardFromResult(result, mapping);
        }
      }
    }
    catch (Exception e) {
      // log error here and attach to request; do not depend on MVC framework
      LOG.error("Unable to execute action " + this.getClass().getName(), e);
      _request.setAttribute(EXCEPTION_USER, getCurrentUserOrNull());
      _request.setAttribute(EXCEPTION_PAGE, getRequestData().getFullRequestUrl());
      _request.setAttribute(EXCEPTION_OBJ, e);
      return getForwardFromResult(new ActionResult().setViewName(ERROR), mapping);
    }
  }
  
  /**
   * Creates an ActionResult to be returned when configured parameter validation
   * fails.  Default behavior is to return result with view name 'input' of type
   * HTML, and attach the validator to the request under the name 'validator'.
   * 
   * If the calling action wishes a different response, it can override this
   * method and return a different result.
   * 
   * @param wve validation exception containing failure causes
   * @return desired result for validation failure
   */
  protected ActionResult getValidationFailureResult(WdkValidationException wve) {
    return new ActionResult(_responseType)
        .setRequestAttribute("validator", wve.getValidator())
        .setViewName(INPUT);
  }
  
  private ParamGroup createParamGroup(Map<String, String[]> paramMap) throws WdkValidationException, WdkUserException {
    
    Map<String, FileItem> uploads = new HashMap<String, FileItem>();

    // parse multipart form data, including "normal" params and files
    if (ServletFileUpload.isMultipartContent(new ServletRequestContext(_request))) {
      try {
        ServletFileUpload uploadHandler = new ServletFileUpload(new DiskFileItemFactory());
        uploadHandler.setSizeMax(getMaxUploadSize()*1024*1024);
        List<FileItem> uploadList = uploadHandler.parseRequest(_request);
        Map<String, List<String>> params = new HashMap<String, List<String>>();
        for (FileItem upload : uploadList) {
          if (!upload.isFormField()) {
            LOG.debug("Got a disk item from multi-part request named " + upload.getFieldName() + ": " + upload);
            uploads.put(upload.getFieldName(), upload);
          }
          else {
            LOG.debug("Got a non-disk item from multi-part request named " + upload.getFieldName() + ": " + upload.toString());
            if (!params.containsKey(upload.getFieldName())) {
              params.put(upload.getFieldName(), new ArrayList<String>());
            }
            params.get(upload.getFieldName()).add(upload.getString());
          }
        }
        for (String key : params.keySet()) {
          paramMap.put(key, params.get(key).toArray(new String[0]));
        }
      }
      catch (FileUploadException e) {
        throw new WdkUserException("Error handling upload field.", e);
      }
    }

    ParamGroup params;
    if (shouldValidateParams()) {
      ParameterValidator validator = new ParameterValidator();
      params = validator.validateParameters(getExpectedParams(), paramMap, uploads, this);
    }
    else {
      params = buildParamGroup(paramMap, uploads);
    }

    if (shouldCheckSpam()) {
      String ts = paramMap.get(SPAM_TIMESTAMP) == null
        ? ""
        : paramMap.get(SPAM_TIMESTAMP)[0];
      SpamUtils.verifyTimeStamp(ts);
    }
    return params;
  }
  
  /**
   * Returns the maximum file upload size.  This can be overridden by subclasses.
   * 
   * @return max file size in megabytes
   */
  protected int getMaxUploadSize() {
    return DEFAULT_MAX_UPLOAD_SIZE_MB;
  }

  /**
   * Should the request be verified as not spam?
   *
   * This can be overridden by subclasses.
   */
  protected boolean shouldCheckSpam() {
    return false;
  }
  
  private ParamGroup buildParamGroup(Map<String, String[]> parameters, Map<String, FileItem> uploads) {
    // generate param definitions based on passed params so calling code
    //   sees a complete ParamGroup structure
    Map<String, ParamDef> definitions = new HashMap<String, ParamDef>();
    for (String key : parameters.keySet()) {
      String[] values = parameters.get(key);
      definitions.put(key, new ParamDef(Required.OPTIONAL,
          values.length > 1 ? Count.MULTIPLE : Count.SINGULAR));
    }
    for (String key : uploads.keySet()) {
      definitions.put(key, new ParamDef(Required.OPTIONAL, DataType.FILE));
    }
    return new ParamGroup(definitions, parameters, uploads);
  }
  
  /**
   * Tells WdkAction whether execution of this request requires a user to be
   * logged in.  Default is false.  Can be overridden by children.
   * 
   * @return true if login required, else false
   */
  protected boolean requiresLogin() {
    return false;
  }
  
  /**
   * Allows children to explicitly set response type.  Note that this value is
   * only used if an exception is throw during handleRequest().  Under normal
   * conditions, the response type on the ActionResult is used to determine the
   * type of response.  Default type is HTML.
   * 
   * @param responseType
   */
  protected void setResponseType(ResponseType responseType) {
    _responseType = responseType;
  }
  
  private void assignAttributesToRequest(ActionResult result) throws WdkModelException {
    // assign the current request URL for access by the resulting page
    _request.setAttribute(CConstants.WDK_REFERRER_URL_KEY, getRequestData().getReferrer());
    _request.setAttribute(Utilities.WDK_USER_KEY, getCurrentUser());
    _request.setAttribute(Utilities.WDK_MODEL_KEY, getWdkModel());
    for (String attribKey : result) {
      _request.setAttribute(attribKey, result.getRequestAttribute(attribKey));
    }
  }
  
  /**
   * @returns the full path of the web application root URL
   *   (e.g. "http://subdomain.myserver.com:8080/mywebapp")
   */
  protected String getWebAppRoot() {
    int port = _request.getServerPort();
    return _request.getScheme() + "://" + _request.getServerName() +
        (port == 80 || port == 443 ? "" : ":" + port) + _request.getContextPath();
  }
  
  /**
   * Allows children to define additional parameter validation.  This validation
   * will occur after primary parameter validation has completed.
   */
  @Override
  public void performAdditionalValidation(ParamGroup params) throws WdkValidationException {
    // do nothing here; to be overridden by subclass
  }
  
  private ActionForward getForwardFromResult(ActionResult result, ActionMapping mapping)
      throws WdkModelException {
    if (result.usesExplicitPath()) {
      ActionForward forward = new ActionForward();
      forward.setPath(result.getViewPath());
      forward.setRedirect(result.isRedirect());
      return forward;
    }
    else {
      ActionForward strutsForward = mapping.findForward(result.getViewName());
      if (strutsForward == null) {
        String msg = "No forward exists with name " + result.getViewName() +
            " for action " + this.getClass().getName();
        LOG.error(msg);
        throw new WdkModelException(msg);
      }
      LOG.info("Returning Struts forward with name " + result.getViewName() + ": " + strutsForward);
      return strutsForward;
    }
  }
  
  /**
   * Allows class-wide access to validated params
   * 
   * @return param group for this request
   */
  protected ParamGroup getParams() {
    return _params;
  }
  
  /**
   * @return reference to the site's WdkModelBean
   */
  protected WdkModelBean getWdkModel() {
    return _wdkModel;
  }
  
  /**
   * @return GUS_HOME web app parameter
   */
  protected String getGusHome() {
    ServletContext context = _servlet.getServletContext();
    String gusHomeBase = context.getInitParameter(Utilities.SYSTEM_PROPERTY_GUS_HOME);
    return context.getRealPath(gusHomeBase);
  }
  
  /**
   * Invalidates the current session and establishes a new one
   */
  protected void resetSession() {
    HttpSession session = _request.getSession();
    if (session != null) {
      session.invalidate();
    }
    // create new session
    _request.getSession(true);
  }
  
  /**
   * Returns the current user.  If no user is logged in and a guest user has
   * not yet been created, creates a guest user, adds it to the session, and
   * returns it.  This method should never return null; thus no request should
   * make it to handleParams() without having a user (guest or otherwise) on
   * the session.
   * 
   * @return the current user (logged in or guest)
   * @throws WdkModelException if guest user is needed but unable to create guest user
   */
  protected UserBean getCurrentUser() throws WdkModelException {
    UserBean user = ActionUtility.getUser(_request);
    // if guest is null, means the session is timed out; create the guest again
    if (user == null) {
      user = _wdkModel.getUserFactory().getGuestUser();
      setCurrentUser(user);
    }
    return user;
  }
  
  protected UserBean getCurrentUserOrNull() {
    try {
      return getCurrentUser();
    } catch (Exception e) {
      LOG.error("Could not access or create user", e);
      return null;
    }
  }
  
  /**
   * Sets the current user.  This method should only be called by login and
   * logout operations.
   * 
   * @param user new user for this session
   */
  protected void setCurrentUser(UserBean user) {
    setSessionAttribute(Utilities.WDK_USER_KEY, user);
  }
  
  /**
   * Sets an attribute on the session
   * 
   * @param key name of the attribute
   * @param value value of the attribute
   */
  protected void setSessionAttribute(String key, Object value) {
    _request.getSession().setAttribute(key, value);
  }
  
  /**
   * Removes an attribute from the session
   * 
   * @param key name of the attribute to remove
   */
  protected void unsetSessionAttribute(String key) {
    _request.getSession().removeAttribute(key);
  }
  
  /**
   * Retrieves an attribute from the session
   * 
   * @param key name of the attribute to retrieve
   * @return session attribute
   */
  protected Object getSessionAttribute(String key) {
    return _request.getSession().getAttribute(key);
  }
  
  /**
   * Adds HTTP cookie onto response
   * 
   * @param cookie cookie to add
   */
  public void addCookieToResponse(Cookie cookie) {
    _response.addCookie(cookie);
  }
  
  /**
   * @return the cookies passed along with the current request
   */
  protected Cookie[] getRequestCookies() {
    return _request.getCookies();
  }
  
  /**
   * Looks for a named resource within the web application and returns whether
   * it exists or not.
   * 
   * @param name path to the resource from web application root
   * @return true if resource exists, otherwise false
   */
  @Override
  public boolean wdkResourceExists(String name) {
    return resourceExists(name, _servlet.getServletContext());
  }

  public static boolean resourceExists(String path, ServletContext servletContext)
      throws WdkRuntimeException {
    try {
      URL url = servletContext.getResource(path);
      return url != null;
    }
    catch (MalformedURLException e) {
      throw new WdkRuntimeException("Malformed URL passed", e);
    }
  }

  /**
   * Returns requested response type.  This is a "global" parameter that may
   * or may not be honored by the child action; however, it is always available.
   * 
   * @return
   */
  protected ResponseType getRequestedResponseType() {
    String typeStr = _params.getValue(CConstants.WDK_RESPONSE_TYPE_KEY);
    try {
      return ResponseType.valueOf(typeStr);
    }
    catch (IllegalArgumentException iae) {
      return null;
    }
  }
  
  private Map<String, ParamDef> getExpectedParams() {
    // make a copy of the child-defined set of expected params
    Map<String, ParamDef> definedParams = new HashMap<>(getParamDefs());
    
    // now add params that we may expect from any request (i.e. global params)
    definedParams.put(AUTH_TICKET, new ParamDef(Required.OPTIONAL));
    definedParams.put(SPAM_TIMESTAMP, new ParamDef(shouldCheckSpam() ? Required.REQUIRED : Required.OPTIONAL));
    definedParams.put("_", new ParamDef(Required.OPTIONAL));
    definedParams.put(CConstants.WDK_RESPONSE_TYPE_KEY, new ParamDef(Required.OPTIONAL));
    
    return definedParams;
  }

  /**
   * Returns encapsulated request information
   * 
   * @return request data object
   */
  public RequestData getRequestData() {
    return new HttpRequestData(_request);
  }
  
  /**
   * Returns Struts ActionForm associated with this request
   * 
   * @return action form for this request
   */
  @Deprecated
  public ActionForm getStrutsActionForm() {
    return _strutsActionForm;
  }
  
  /**
   * Returns the standard location for custom JSP overrides.
   * TODO: this should not be needed.  Users of this software can write their
   * own JSPs and add them to struts config as desired.
   * 
   * @return
   */
  public static String getCustomViewDir() {
    return new StringBuilder()
      .append(CConstants.WDK_CUSTOM_VIEW_DIR)
      .append(File.separator)
      .append(CConstants.WDK_PAGES_DIR)
      .append(File.separator)
      .toString();
  }
  
}
