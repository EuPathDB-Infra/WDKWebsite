<%@ taglib prefix="imp" tagdir="/WEB-INF/tags/imp" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="html" uri="http://struts.apache.org/tags-html" %>
<%@ taglib prefix="bean" uri="http://struts.apache.org/tags-bean" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<%-- get wdkQuestion; setup requestScope HashMap to collect help info for footer --%>
<c:set var="wdkQuestion" value="${requestScope.wdkQuestion}"/>
<jsp:useBean scope="request" id="helps" class="java.util.LinkedHashMap"/>
<c:set var="qForm" value="${requestScope.questionForm}"/>

<%-- display page header with wdkQuestion displayName as banner --%>
<c:set var="wdkModel" value="${applicationScope.wdkModel}"/>
<c:set var="recordName" value="${wdkQuestion.recordClass.displayNamePlural}"/>
<c:set var="showParams" value="${requestScope.showParams}"/>

<%-- show all params of question, collect help info along the way --%>
<c:set value="Help for question: ${wdkQuestion.displayName}" var="fromAnchorQ"/>
<jsp:useBean id="helpQ" class="java.util.LinkedHashMap"/>

<input id="questionFullName" type="hidden" name="questionFullName" value="${wdkQuestion.fullName}"/>
<div id="questionName" style="display:none" name="${wdkQuestion.name}"></div>

<!-- show error messages, if any -->
<imp:errors/>

<c:set var="showParamsAttr">
  <c:choose>
    <c:when test="${showParams == null}">true</c:when>
    <c:otherwise>false</c:otherwise>
  </c:choose>
</c:set>

<c:set var="dataProps">
  <c:forEach items="${wdkQuestion.propertyLists}" var="propertyListEntry">
    <c:set var="values" value=""/>
    <c:set var="search" value='"'/>
    <c:set var="replace" value='\\"'/>
    <c:forEach items="${propertyListEntry.value}" var="value" varStatus="loop">
      <c:set var="escapedValue" value="${fn:replace(value, search, replace)}"/>
      <c:choose>
        <c:when test="${loop.first}">
          <c:set var="values" value='"${escapedValue}"'/>
        </c:when>
        <c:otherwise>
          <c:set var="values" value='${values}, "${escapedValue}"'/>
        </c:otherwise>
      </c:choose>
    </c:forEach>
    data-${propertyListEntry.key}='[${values}]'
  </c:forEach>
</c:set>

<div class="params"
    ${dataProps}
    data-controller="wdk.question.init"
    data-question-full-name="${wdkQuestion.fullName}"
    data-show-params="${showParamsAttr}">
    <imp:questionParams />
</div> <!-- end of params div -->


<%-- this is used by basket when being added as a step
     do not display description for wdkQuestion because question.jsp provides for it
<c:set var="descripId" value="query-description-section"/>
<div id="${descripId}"><b>Query description: </b>${wdkQuestion.description}</div>
--%>
