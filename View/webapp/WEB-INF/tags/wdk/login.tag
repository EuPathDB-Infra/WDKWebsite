<?xml version="1.0" encoding="UTF-8"?>
<jsp:root version="2.0"
    xmlns:jsp="http://java.sun.com/JSP/Page"
    xmlns:c="http://java.sun.com/jsp/jstl/core"
    xmlns:fn="http://java.sun.com/jsp/jstl/functions"
    xmlns:imp="urn:jsptagdir:/WEB-INF/tags/imp">

  <jsp:directive.attribute name="title" required="false"
      description="Value to appear as the login pop-up's title"/>

  <c:set var="title" value="${empty title ? 'Account Login' : title}"/>
  <c:set var="modelConfig" value="${applicationScope.wdkModel.model.modelConfig}"/>
  <c:set var="wdkUser" value="${sessionScope.wdkUser}"/>
  <c:set var="isLoggedIn" value="${wdkUser ne null and wdkUser.guest ne true}"/>
  <c:set var="userName" value="${wdkUser.firstName} ${wdkUser.lastName}"/>
  <c:set var="userName" value="${fn:escapeXml(userName)}"/>
  <c:set var="functionArg" value="{ &quot;isLoggedIn&quot;: ${isLoggedIn}, &quot;userName&quot;: &quot;${userName}&quot; }"/>

  <c:choose>
    <c:when test="${isLoggedIn eq true}">
      <li><a href="${pageContext.request.contextPath}/app/user/profile">${userName}'s Profile</a></li>
      <li id="user-control">
        <a href="javascript:void(0)" onclick="wdk.user.logout()">Logout</a>
      </li>
    </c:when>
    <c:otherwise>
      <li><a href="javascript:void(0)" onclick="wdk.user.login()">Login</a> </li>
      <li><a href="${pageContext.request.contextPath}/app/user/registration">Register</a></li>
    </c:otherwise>
  </c:choose>

</jsp:root>
