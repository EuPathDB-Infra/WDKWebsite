<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="imp" tagdir="/WEB-INF/tags/imp" %>

<jsp:useBean id="idgen" class="org.gusdb.wdk.model.jspwrap.NumberUtilBean" scope="application" />

<%@ attribute name="wdkAnswer"
              required="true"
              type="org.gusdb.wdk.model.jspwrap.AnswerValueBean"
              description="the AnswerValueBean for this attribute list"
%>

<%@ attribute name="commandUrl"
              required="true"
              type="java.lang.String"
%>

<input type="button" onclick="wdk.resultsPage.openAttributeList(this);" class="addAttributesButton" value="Add Columns" />

<div class="attributesList formPopup" title="Select Columns">

  <div class="attributesFormWrapper">
    <form name="addAttributes" action="${commandUrl}">
      <input type="hidden" name="command" value="update"/>
    <div class="formButtonPanel">
      <input type="submit" value="Update Columns"/>
    </div>
    <c:if test="${wdkAnswer.useCheckboxTree}">
      <c:set var="checkboxTreeId" value="sfcbt-${wdkAnswer.recordClass.name}-${idgen.nextId}"/>
      <input type="checkbox" name="selectedFields" value="${wdkAnswer.recordClass.primaryKeyAttribute.name}" checked="checked" style="display:none;"/>
      <imp:checkboxTree id="${checkboxTreeId}" tree="${wdkAnswer.attributes.displayableAttributeTree}" checkboxName="selectedFields" showSelectAll="false" showResetCurrent="true" useHelp="true"/>
    </c:if>
    <c:if test="${not wdkAnswer.useCheckboxTree}">
	    <div class="formButtonPanel">
	      <imp:selectClearAll groupName="selectedFields" />
	    </div>
		  <c:set var="allAttributes" value="${wdkAnswer.attributes.displayableAttributes}" />
    	<c:set var="i" value="0"/>
    	<fmt:formatNumber var="columnSize" value="${(fn:length(allAttributes) + 1)/3}" pattern="0"/>
    	<table>
        <tr>
	        <c:forEach items="${allAttributes}" var="attribute">
	          <c:if test="${i == 0}">
	            <td>
	              <ul>
	          </c:if>
	          <c:set var="inputProps" value=""/>
	          <c:set var="j" value="0"/>
	          <c:forEach items="${wdkAnswer.attributes.summaryAttributes}" var="summary">
	            <c:if test="${attribute.name eq summary.name}">
	              <c:set var="inputProps" value="checked" />
	              <c:if test="${attribute.removable == false}">
	                <c:set var="inputProps" value="${inputProps} disabled" />
	              </c:if>
	            </c:if>
	            <c:set var="j" value="${j + 1}"/>
	          </c:forEach>
	          <li>
	            <input id="${attribute.name}" type="checkbox" name="selectedFields" value="${attribute.name}" title="${attribute.help}" ${inputProps} />
	            <label for="${attribute.name}" title="${attribute.help}">${attribute.displayName}</label>
	          </li>
	          <c:set var="i" value="${i + 1}"/>
	          <c:if test="${i == columnSize}">
	              </ul>
	            </td>
	            <c:set var="i" value="0"/>
	          </c:if>
	        </c:forEach>
        </tr>
      </table>
      <div class="formButtonPanel">
        <imp:selectClearAll groupName="selectedFields" />
      </div>
    </c:if>
    <div class="formButtonPanel">
      <input type="submit" value="Update Columns"/>
    </div>
  </form>
  </div>

</div>  <%--   class="attributesList formPopup"  --%> 
