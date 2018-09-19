<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="bean" uri="http://struts.apache.org/tags-bean" %>
<%@ taglib prefix="html" uri="http://struts.apache.org/tags-html" %>
<%@ taglib prefix="imp" tagdir="/WEB-INF/tags/imp" %>

<%--
Provides form input element for a given EnumParam.

For a multi-selectable parameter a form element is provided as either a
series of checkboxes or a multiselect menu depending on number of
parameter options. Also, if number of options is over a threshold, this tag
includes a checkAll button to select all options for the parameter.

Otherwise a standard select menu is used.
--%>

<jsp:useBean id="idgen" class="org.gusdb.wdk.model.jspwrap.NumberUtilBean" scope="application" />

<%@ attribute name="qp"
              type="org.gusdb.wdk.model.jspwrap.EnumParamBean"
              required="true"
              description="parameter name"
%>

<%@ attribute name="layout"
              required="false"
              description="parameter name"
%>

<c:set var="qP" value="${qp}"/>
<c:set var="pNam" value="${qP.name}"/>
  <c:set var="pPrompt" value="${qP.prompt}"/>
<c:set var="opt" value="0"/>
<c:set var="displayType" value="${qP.displayType}"/>
<c:set var="dependedParams" value="${qP.dependedParamNames}"/>
<c:if test="${dependedParams != null}">
  <c:set var="dependedParam" value="${dependedParams}" />
  <c:set var="dependentClass" value="dependentParam" />
</c:if>
<%-- Setting a variable to display the items in the parameter in a horizontal layout --%>
<c:set var="v" value=""/>
<c:if test="${layout == 'horizontal'}">
  <c:set var="v" value="style='display:inline'"/>
</c:if>

<c:choose>
  <c:when test="${qP.multiPick}">
    <%-- multiPick is true, use input that allows multiple selections --%>
    <c:choose>
      <%-- use checkboxes --%>
      <c:when test="${displayType eq 'checkBox'}">
        <div class="param enumParam param-multiPick ${dependentClass}" dependson="${dependedParam}" name="${pNam}" prompt="${pPrompt}">
          <c:set var="initialCount" value="${fn:length(qP.currentValues)}"/>
          <imp:enumCountWarning enumParam="${qP}" initialCount="${initialCount}"/>
          <c:set var="changeCode" value="window.wdk.parameterHandlers.adjustEnumCountBoxes('${qP.name}aaa')"/>
          <c:set var="i" value="0"/>
          <table><tr><td>
            <ul>
              <c:forEach items="${qP.displayMap}" var="entity" varStatus="loop">
                <c:if test="${i == 0}"><c:set var="checked" value="checked"/></c:if>
                <li>
                  <label>
                    <html:multibox property="array(${pNam})" value="${entity.key}" styleId="${pNam}_${loop.index}" onchange="${changeCode}"/>
                    <c:choose>
                      <%-- test for param labels to italicize --%>
                      <c:when test="${pNam == 'organism' or pNam == 'ecorganism'}">
                        <i>${entity.value}</i>&nbsp;
                      </c:when>
                      <c:otherwise> <%-- use multiselect menu --%>
                        ${entity.value}&nbsp;
                      </c:otherwise>
                    </c:choose>
                    <c:set var="i" value="${i+1}"/>
                    <c:set var="checked" value=""/>
                  </label>
                </li>
              </c:forEach>
            </ul>
            &nbsp;<imp:selectAllParamOpt enumParam="${qP}" onchange="${changeCode}"/>
          </td></tr></table>
        </div>
      </c:when>

      <%-- use a tree list --%>
      <c:when test="${displayType eq 'treeBox'}">
        <imp:legacyParamAdapterInput qp="${qP}" />
      </c:when>

      <%-- use a type ahead --%>
      <c:when test="${displayType eq 'typeAhead'}">
          <div class="param ${dependentClass}" data-type="type-ahead"
            data-multiple="true" dependson="${dependedParam}" name="${pNam}" prompt="${pPrompt}">
          <div class="loading">Loading...</div>
          <html:hidden property="value(${pNam})" style="width:450px"/>
            <!-- <input type="hidden" style="width:450px" name="value(${pNam})"/> -->
          <div class="type-ahead-help" style="margin:2px;">
            Begin typing to see suggestions to choose from (CTRL or CMD click to select multiple)<br/>
           <%--  Or paste a list of IDs separated by a comma, new-line, white-space, or semi-colon.<br/>
             wildcard support has been dropped due to SQL complications
            Or use * as a wildcard, like this: *your-term*
            --%>
          </div>
        </div>
      </c:when>

      <%-- use a multi-select box --%>
      <c:when test="${displayType eq 'select'}">
        <div class="param ${dependentClass}" data-type="multi-pick" dependson="${dependedParam}" name="${pNam}" prompt="${pPrompt}">
          <c:set var="initialCount" value="${fn:length(qP.currentValues)}"/>
          <imp:enumCountWarning enumParam="${qP}" initialCount="${initialCount}"/>
          <c:set var="changeCode" value="window.wdk.parameterHandlers.adjustEnumCountSelect('${qP.name}aaa')"/>
          <html:select property="array(${pNam})" multiple="1" styleId="${pNam}" onchange="${changeCode}">
            <html:options property="array(${pNam}-values)" labelProperty="array(${pNam}-labels)" />
          </html:select>
          <br/><imp:selectAllParamOpt enumParam="${qP}" onchange="${changeCode}"/>
        </div>
      </c:when>

      <c:otherwise>
        <div>Invalid display type.</div>
      </c:otherwise>
    </c:choose>
  </c:when> <%-- end of multi-pick --%>

  <c:otherwise>
    <%-- pick single item --%>
    <c:choose>
      <%-- use radio boxes --%>
      <c:when test="${displayType eq 'checkBox'}">
        <div class="param enumParam ${dependentClass}" dependson="${dependedParam}" name="${pNam}" prompt="${pPrompt}">
          <ul>
            <c:forEach items="${qP.displayMap}" var="entity">
              <li ${v}>
                <label>
                  <html:radio property="array(${pNam})" value="${entity.key}" /> <span>${entity.value}</span>
                </label>
              </li>
            </c:forEach>
          </ul>
        </div>
      </c:when>

      <%-- use a tree list; only one value allowed --%>
      <c:when test="${displayType eq 'treeBox'}">
        <div class="param ${dependentClass}" dependson="${dependedParam}" name="${pNam}" prompt="${pPrompt}">
          <imp:enumCountWarning enumParam="${qP}" initialCount="0"/>
          <c:set var="updateCountFunc">window.wdk.parameterHandlers.adjustEnumCountTree('${qP.name}aaa',${qP.countOnlyLeaves})</c:set>
            <imp:checkboxTree
              id="${pNam}CBT${idgen.nextId}"
              tree="${qP.paramTree}"
              checkboxName="array(${pNam})"
              depthExpanded="${qP.depthExpanded}"
              buttonAlignment="left"
              onchange="${updateCountFunc}"
              onload="${updateCountFunc}"
            />
        </div>
      </c:when>

      <%-- use a type ahead --%>
      <c:when test="${displayType eq 'typeAhead'}">
        <div class="param ${dependentClass}" data-multiple="false" data-type="type-ahead"
          dependson="${dependedParam}" name="${pNam}" prompt="${pPrompt}">
          <div class="loading">Loading...</div>
          <html:hidden property="value(${pNam})" style="width:450px"/>
          <div class="type-ahead-help" style="margin:2px;">
            Begin typing to see suggestions from which to choose<br/>
            <%-- Or use * as a wildcard, like this: *your-term* --%>
          </div>
        </div>
      </c:when>

      <%-- use a pull down menu --%>
      <c:when test="${displayType eq 'select'}">
        <div class="param ${dependentClass}" dependson="${dependedParam}" name="${pNam}" prompt="${pPrompt}">
          <html:select property="array(${pNam})" styleId="${pNam}">
            <c:set var="opt" value="${opt+1}"/>
            <c:set var="sel" value=""/>
            <c:if test="${opt == 1}"><c:set var="sel" value="selected"/></c:if>
            <html:options property="array(${pNam}-values)" labelProperty="array(${pNam}-labels)"/>
          </html:select>
        </div>
      </c:when>

      <c:otherwise>
        <div>Invalid display type.</div>
      </c:otherwise>
    </c:choose>
  </c:otherwise> <%-- end of pick single item --%>
</c:choose>

<%-- display invalid terms, if any. --%>
<c:set var="invalidKey" value="${qP.name}_invalid" />
<c:set var="invalidTerms" value="${requestScope[invalidKey]}" />

<c:if test="${fn:length(invalidTerms) gt 1}">
  <div class="invalid-values">
    <p>Some of the option(s) you previously selected are no longer available.</p>
    <p>Here is a list of the values you selected (unavailable options are marked in red):</p>
    <ul>
      <c:forEach items="${invalidTerms}" var="invalidTerm">
        <li class="invalid">${invalidTerm}</li>
      </c:forEach>
    </ul>
  </div>
</c:if>
