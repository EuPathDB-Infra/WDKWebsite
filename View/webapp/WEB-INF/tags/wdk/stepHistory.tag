<%@ taglib prefix="imp" tagdir="/WEB-INF/tags/imp" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="html" uri="http://struts.apache.org/tags-html" %>

<%@ attribute name="model"
             type="org.gusdb.wdk.model.jspwrap.WdkModelBean"
             required="false"
             description="Wdk Model Object for this site"
%>

<%@ attribute name="user"
              type="org.gusdb.wdk.model.jspwrap.UserBean"
              required="false"
              description="Currently active user object"
%>

<c:set var="steps" value="${user.steps}"/>
<c:set var="modelName" value="${model.name}"/>
<c:set var="invalidSteps" value="${user.invalidSteps}" />


<!-- decide if there are any steps -->
<c:choose>
  <c:when test="${user == null || fn:length(steps) == 0}">
  <div align="center">You have no searches in your history.  Please run a search from the <a href="/">home</a> page, or by using the "New Search" menu above, or by selecting a search from the <a href="/queries_tools.jsp">searches</a> page.</div>
  </c:when>
  <c:otherwise>
  <!-- begin display steps -->
  <div id="complete_history">
    <table width="100%">
       <tr class="headerrow">
          <th onmouseover="wdk.history.hideAnyName()" style="width: 2em;">ID</th>
          <th onmouseover="wdk.history.hideAnyName()">Query</th>
          <th onmouseover="wdk.history.hideAnyName()" style="width: 5em;">Type</th>
          <th onmouseover="wdk.history.hideAnyName()" style="width: 5em;">Date</th>
          <th onmouseover="wdk.history.hideAnyName()" style="width: 5em;">Version</th>
          <th align="right" style="width: 5em;">Size</th>
          <th onmouseover="wdk.history.hideAnyName()" style="width: 5em;">&nbsp;</th>
          <th onmouseover="wdk.history.hideAnyName()" style="width: 5em;">&nbsp;</th>
       </tr>
       <c:forEach items="${steps}" var="step">
         <c:set var="type" value="${step.recordClass.fullName}"/>
         <c:set var="recDispName" value="${step.question.recordClass.displayNamePlural}"/>
         <c:choose>
           <c:when test="${i % 2 == 0}"><tr class="lines"></c:when>
           <c:otherwise><tr class="linesalt"></c:otherwise>
         </c:choose>
            <td>${step.stepId}
               <div id="div_${step.stepId}" class="medium"
                 style="display:none;font-size:8pt;width:610px;position:absolute;left:0;top:0;"
                 onmouseover="wdk.history.hideAnyName()">
                 <table cellpadding="2" cellspacing="0" border="0" style="background-color:#ffffcc;">
                            <tr>
                                <td>
                                    <%-- simple question --%>
                                    <imp:showParams step="${step}" />
                                </td>
                            </tr>
                 </table>
               </div>
            </td>
            <td onmouseover="displayName('${step.stepId}')" onmouseout="wdk.history.hideAnyName()">${step.customName}
              <c:if test="${step.isValid == false}"><imp:image width="12" src="wdk/images/invalidIcon.png"/></c:if>
            </td>
            <td onmouseover="wdk.history.hideAnyName()">${recDispName}</td>
	    <td onmouseover="wdk.history.hideAnyName()" nowrap>${step.createdTimeFormatted}</td>
	    <td onmouseover="wdk.history.hideAnyName()" nowrap>
	    <c:choose>
	      <c:when test="${step.version == null || step.version eq ''}">${wdkModel.version}</c:when>
              <c:otherwise>${step.version}</c:otherwise>
            </c:choose>
            </td>
            <td onmouseover="wdk.history.hideAnyName()" align='right' nowrap>${step.estimateSize}</td>
            <c:set value="${step.question.fullName}" var="qName" />
            <td onmouseover="wdk.history.hideAnyName()" style="text-align:center;" nowrap><a onclick="wdk.stratTabCookie.setCurrentTabCookie('strategy_results', false);" href="showSummary.do?questionFullName=${qName}${step.summaryUrlParams}">view</a></td>
            <td onmouseover="wdk.history.hideAnyName()" nowrap><a href="app/step/${step.stepId}/download">download</a></td>
         </tr>
         <c:set var="i" value="${i+1}"/>
       </c:forEach>
       <!-- end of forEach step -->
    </table>
</div>
<!-- end of showing steps -->
       <div style="padding:5px 0;">
            <html:form method="get" action="/processBooleanExpression.do">
               <span id="comb_title_${type}">Combine results</span>:
               <span id="comb_input_${type}">
                  <html:text property="booleanExpression" value=""/>
               </span>

               <c:if test="${showTransform}">
                  <html:checkbox property="useBooleanFilter">On gene level</html:checkbox>
               </c:if>
               
               <html:hidden property="historySectionId" value="${type}"/>
               <html:submit property="submit" value="Get Combined Result"/>
               <font size="-1">[eg: 1 or ((4 and 3) not 2)]</font>
            </html:form>
       </div>
<table>
   <tr>
      <td>
         <!-- display helper information -->
         <font class="medium"><b>Understanding AND, OR and NOT</b>:</font>
         <table border='0' cellspacing='3' cellpadding='0'>
            <tr>
               <td width='100'><font class="medium"><b>1 and 2</b></font></td>
               <td><font class="medium">Genes that 1 and 2 have in common. You can also use "1 intersect 2".</font></td>
            </tr>
            <tr>
               <td width='100'><font class="medium"><b>1 or 2</b></font></td>
               <td><font class="medium">Genes present in 1 or 2, or both. You can also use "1 union 2".</font></td>
            </tr>
            <tr>
               <td width='100'><font class="medium"><b>1 not 2</b></font></td>
               <td><font class="medium">Genes in 1 but not in 2. You can also use "1 minus 2".</font></td>
            </tr>
         </table>
      </td>
   </tr>
</table>
  </c:otherwise>
</c:choose> 
<!-- end of deciding step emptiness -->


