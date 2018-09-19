<%@ taglib prefix="imp" tagdir="/WEB-INF/tags/imp" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="html" uri="http://struts.apache.org/tags-html" %>

<%@ attribute name="latestStep"
              type="org.gusdb.wdk.model.jspwrap.StepBean"
              required="true"
              description="Step object representing latest step in a strategy or substrategy"
%>

<%@ attribute name="indent"
              required="true"
              description="Number of pixels to indent this set of steps"
%>

<%@ attribute name="rowNum"
              required="true"
              description="Row number for the Strategy that latestStep belongs to"
%>

<!-- begin rowgroup for strategy steps -->
<c:set var="j" value="0"/>
<c:set var="steps" value="${latestStep.allSteps}"/>
<c:forEach items="${steps}" var="step">
  <c:choose>
    <c:when test="${rowNum % 2 == 0}"><tr class="lines" style="display: none;"></c:when>
    <c:otherwise><tr class="linesalt" style="display: none;"></c:otherwise>
  </c:choose>
  <!-- offer a rename here too? -->

  <c:choose>
    <c:when test="${j == 0}">
      <td nowrap><ul style="margin-left: ${indent}px;"><li>Step ${j + 1} (${step.estimateSize}): ${step.customName}</li></ul></td>
    </c:when>
    <c:otherwise>
      <!-- only for boolean, need to check for transforms -->
      <c:choose>
        <c:when test="${step.isBoolean}">
          <c:choose>
            <c:when test="${step.childStep.isCollapsible}">
              <c:set var="dispName" value="${step.childStep.collapsedName}"/>
            </c:when>
            <c:otherwise>
              <c:set var="dispName" value="${step.childStep.customName}"/>
            </c:otherwise>
          </c:choose>
          <c:choose>
            <c:when test="${j == 1}">
              <td nowrap><ul style="margin-left: ${indent}px;">
                <li>Step ${j + 1} (${step.estimateSize}): Step ${j}</li>
                <li class="operation ${step.operation}" />
                <li>${dispName}&nbsp;(${step.childStep.estimateSize})</li></ul></td>
            </c:when>
            <c:otherwise>
              <td nowrap><ul style="margin-left: ${indent}px;"><li>Step ${j + 1} (${step.estimateSize}): Step ${j}</li><li class="operation ${step.operation}" /><li>${dispName}&nbsp;(${step.childStep.estimateSize})</li></ul></td>
            </c:otherwise>
          </c:choose>
        </c:when>
        <c:otherwise>
          <td nowrap><ul style="margin-left: ${indent}px;"><li>Step ${j + 1} (${step.estimateSize}): ${step.customName}</li></ul></td>
        </c:otherwise>
      </c:choose>
    </c:otherwise>
  </c:choose>

  </tr>
  <c:if test="${step.childStep.isCollapsible}">
    <imp:stepRows latestStep="${step.childStep}" rowNum="${rowNum}" indent="${indent + 40}"/>
  </c:if>
  <c:set var="j" value="${j + 1}"/>
</c:forEach>
