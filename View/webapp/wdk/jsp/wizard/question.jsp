<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="html" uri="http://struts.apache.org/tags-html" %>
<%@ taglib prefix="bean" uri="http://struts.apache.org/tags-bean" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="imp" tagdir="/WEB-INF/tags/imp" %>

<%@ page contentType="text/html;charset=iso-8859-1" %>

<c:set var="wdkQuestion" value="${requestScope.question}"/>
<c:set var="spanOnly" value="false"/>
<c:set var="checked" value=""/>
<c:set var="buttonVal">
  <c:choose>
    <c:when test="${allowBoolean == false || param.operation == 'SPAN'}">Continue...</c:when>
    <c:otherwise>Run Step</c:otherwise>
  </c:choose>
</c:set>
<c:set var="wdkStrategy" value="${requestScope.wdkStrategy}"/>
<c:set var="wdkStep" value="${requestScope.wdkStep}"/>
<c:set var="allowBoolean" value="${requestScope.allowBoolean}"/>
<c:set var="action" value="${requestScope.action}"/>

<c:if test="${wdkQuestion.recordClass.fullName != wdkStep.recordClass.fullName}">
	<c:set var="checked" value="checked=''"/>
	<c:set var="buttonVal" value="Continue...."/>
	<c:set var="spanOnly" value="true"/>
</c:if>

<c:set var="webProps" value="${wdkQuestion.propertyLists['websiteProperties']}" />
<c:set var="hideOperation" value="${false}" />
<c:forEach var="prop" items="${webProps}">
  <c:choose>
    <c:when test="${prop == 'hideOperation'}"><c:set var="hideOperation" value="${true}" /></c:when>
  </c:choose>
</c:forEach>

<c:set var="wizard" value="${requestScope.wizard}"/>
<c:set var="stage" value="${requestScope.stage}"/>

<%-- determine the next stage --%>
<c:choose>
  <c:when test="${wdkStep.previousStep == null && action == 'revise'}">
    <c:set var="nextStage" value="process_question" />
  </c:when>
  <c:when test="${allowBoolean == false || param.operation == 'SPAN'}">
    <c:set var="nextStage" value="span_from_question" />
  </c:when>
  <c:otherwise>
    <c:set var="nextStage" value="process_boolean" />
  </c:otherwise>
</c:choose>

<c:set var="newStepId">
  <c:choose>
    <c:when test="${action == 'add'}">${wdkStep.frontId + 1}</c:when>
    <c:otherwise>${wdkStep.frontId}</c:otherwise>
  </c:choose>
</c:set>
<c:set var="currentStepId" value="${newStepId - 1}" />



<html:form styleClass="is-${action}" method="post" enctype='multipart/form-data' action="/processFilter.do" onsubmit="wdk.addStepPopup.callWizard('wizard.do?action=${requestScope.action}&step=${wdkStep.stepId}&',this,null,null,'submit')">

<html:hidden property="stage" styleId="stage" value="${nextStage}" />

<span style="display:none" id="strategyId">${wdkStrategy.strategyId}</span>
<c:choose>
    <c:when test="${wdkStep.previousStep == null || action != 'revise'}">
        <c:set var="stepId" value="${wdkStep.stepId}" />
    </c:when>
    <c:otherwise>
        <c:set var="stepId" value="${wdkStep.previousStep.stepId}" />
    </c:otherwise>
</c:choose>
<span style="display:none" id="stepId">${stepId}</span>

<c:set var="Question_Header" scope="request">
	<%-- has nothing --%>
</c:set>

<c:if test="${param.stage ne 'basket'}">
<c:set var="Question_Details" scope="request">
    <imp:questionDescription />
</c:set>
</c:if>

${Question_Header}

<%-- display question param section --%>
<div class="filter params">
  <span class="h2center">
    <c:choose>
      <c:when test="${action == 'add'}">
        Add Step ${newStepId}
      </c:when>
      <c:when test="${action == 'insert'}">
        Insert Step ${newStepId}
      </c:when>
      <c:otherwise>
        Revise Step ${newStepId}
      </c:otherwise>
    </c:choose>
    : ${wdkQuestion.displayName}
  </span>
  <br><br>

<%--   ${Question_Details}
--%>

  <imp:questionForm />
</div>


<c:if test="${hideOperation == false}">

<%-- display operators section --%>
<c:set var="rcName" value="${wdkStep.question.recordClass.fullName}" />
<c:set var="allowSpan" value="${rcName eq 'TranscriptRecordClasses.TranscriptRecordClass'
                                || rcName eq 'OrfRecordClasses.OrfRecordClass'
                                || rcName eq 'DynSpanRecordClasses.DynSpanRecordClass'
                                || rcName eq 'SnpRecordClasses.SnpRecordClass'
                                || rcName eq 'SnpChipRecordClasses.SnpChipRecordClass'
                                || rcName eq 'SageTagRecordClasses.SageTagRecordClass'}" />

<div class="filter operators">
  <c:if test="${wdkStep.previousStep != null || action != 'revise'}">

      <c:if test="${wdkStep.previousStep != null && action == 'revise'}">
        <c:set var="wdkStep" value="${wdkStep.previousStep}" />
      </c:if>
      <span class="h2center">Combine ${wdkStep.recordClass.displayNamePlural} in Step <span class="current_step_num">${currentStepId}</span> with ${wdkQuestion.recordClass.displayNamePlural} in Step <span class="new_step_num">${newStepId}</span>:</span>

      <div style="text-align:center" id="operations">

<%-- operators table --%>
<imp:operators  allowSpan="${allowSpan}"
		operation="${param.operation}"
                spanStage="span_from_question"
/>

      </div>
   </c:if>       <%--     <c:if test="${wdkStep.previousStep != null || action != 'revise'}">    --%>
</div><%-- end of filter operators --%>

<div id="boolean_button" class="filter-button">
    <html:submit property="questionSubmit" value="${buttonVal}"/>
</div>

<imp:nameStep/>

</c:if> <%-- End of hideOperation --%>

</html:form>

${Question_Details}
