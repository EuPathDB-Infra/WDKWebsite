<?xml version="1.0" encoding="UTF-8"?>
<jsp:root version="2.0"
    xmlns:jsp="http://java.sun.com/JSP/Page"
    xmlns:c="http://java.sun.com/jsp/jstl/core"
    xmlns:imp="urn:jsptagdir:/WEB-INF/tags/imp">
  
  <!-- This tag does not contain drop-downs, but smallMenu overrides should -->
  <!--   add this line if they want automatic drop-down logic               -->  
  <!-- <span class="onload-function" data-function="wdk.setUpNavDropDowns"><jsp:text/></span> -->
  
  <div id="nav-top-div">
	  <ul id="nav-top">
	    <li><a href="${pageContext.request.contextPath}/home.do">Home</a></li>
	    <imp:login/>
	    <li class="nav-last no-divider">
	      <c:set var="supportEmail" value="${wdkModel.model.modelConfig.supportEmail}"/>
	      <a href="mailto:${supportEmail}">Contact Us</a>
	    </li>
	  </ul>
  </div>
  
</jsp:root>
