<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="pg" uri="http://jsptags.com/tags/navigation/pager" %>
<%@ taglib prefix="imp" tagdir="/WEB-INF/tags/imp" %>

<%@ attribute name="strategy"
              type="org.gusdb.wdk.model.jspwrap.StrategyBean"
              required="true"
              description="Strategy bean we are looking at"
%>
<%@ attribute name="step"
              type="org.gusdb.wdk.model.jspwrap.StepBean"
              required="true"
              description="Step bean we are looking at"
%>

<c:set var="wdkModel" value="${applicationScope.wdkModel}" />
<c:set var="wdkUser" value="${sessionScope.wdkUser}" />
<c:set var="projectId" value="${wdkModel.projectId}" />
<c:set var="dispModelName" value="${wdkModel.displayName}" />
<c:set var="wdkAnswer" value="${step.answerValue}"/>
<c:set var="recordClass" value="${wdkAnswer.question.recordClass}" />
<c:set var="recordName" value="${recordClass.fullName}" />
<c:set var="recHasBasket" value="${recordClass.useBasket}" />

<c:set var="recordName" value="${wdkStep.recordClass.displayNamePlural}"/>
<c:set var="isBasket" value="${strategy eq null}"/>
<c:choose>
  <c:when test="${isBasket}">
    <c:set var="viewId" value="basket-${recordClass.fullName}"/>
  </c:when>
  <c:otherwise>
    <c:set var="viewId" value="strategy"/>
  </c:otherwise>
</c:choose>


<!-- ================ TAG SHARED BY BASKET AND OPENED TABS =============== -->
<!-- handle empty result set situation -->
<c:choose>
  <c:when test='${isBasket and wdkUser.guest and wdkAnswer.resultSize eq 0}'>
    Please login to use the basket
  </c:when>
  <c:when test='${isBasket and wdkAnswer.resultSize eq 0}'>
    Basket Empty
  </c:when>
  <c:otherwise>

<!-- ================ RESULTS TITLE AND LINKS TO BASKET AND DOWNLOADS   =============== -->

<imp:resultSummary strategy="${wdkStrategy}" step="${wdkStep}"/>


<!-- ================ FILTERS DEFINED IN MODEL.XML =============== -->

<c:if test="${strategy != null}">
  <imp:filterLayouts strategyId="${strategy.strategyId}" 
                     stepId="${step.stepId}"
                     answerValue="${wdkAnswer}" />
</c:if>


<!-- ================ New filter architecture ================= -->


<!-- FIXME Uncomment sometime
<c:if test="${strategy != null}">
  <imp:resultFilters step="${step}" />
</c:if> 
-->

<!--<div><a href="javascript:wdk.stepAnalysis.showAllAnalyses()">Magic Button</a></div>-->

<c:if test="${strategy != null}">
  <c:set var="stepFilterProps">{ "stepId": ${step.stepId}, "viewId": "${viewId}" }</c:set>
  <div
    data-controller="wdk.clientAdapter"
    data-name="StepFiltersController"
    data-props="${fn:escapeXml(stepFilterProps)}"
  ><jsp:text/></div>
</c:if>

<!-- ================ SUMMARY VIEWS (EXTRA TABS DEFINED IN MODEL.XML)  =============== -->

<c:set var="resultPanelProps">
  {
    "stepId": ${step.stepId},
    "viewId": "${viewId}",
    "initialTab": "${param.selectedTab}"
  }
</c:set>
<div
  class="result-panel-container"
  data-controller="wdk.clientAdapter"
  data-name="ResultPanelController"
  data-props="${fn:escapeXml(resultPanelProps)}"
>
  <jsp:text/>
</div>

  </c:otherwise>
</c:choose>
