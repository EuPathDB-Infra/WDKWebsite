<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="imp" tagdir="/WEB-INF/tags/imp" %>

<%@ attribute name="tblName"
              description="name of table attribute"
%>

<%@ attribute name="isOpen"
              description="Is show/hide block initially open, by default?"
%>

<%@ attribute name="attribution"
              description="Dataset name, for attribution"
%>

<%@ attribute name="preamble"
              description="Text to go above the table and description"
%>

<%@ attribute name="postscript"
              description="Text to go below the table"
%>

<%@ attribute name="suppressColumnHeaders"
              description="Should the display of column headers be skipped?"
%>

<%@ attribute name="suppressDisplayName"
              description="Should the display name be skipped?"
%>

<%@ attribute name="dataTable"
              description="Should the table use dataTables?"
              type="java.lang.Boolean"
%>
<c:catch var="tableError">
  <c:set value="${requestScope.wdkRecord}" var="wdkRecord"/>
  <c:set value="${wdkRecord.tables[tblName]}" var="tbl"/>
  <c:if test="${suppressDisplayName == null || !suppressDisplayName}">
    <c:set value="${tbl.tableField.displayName}" var="tableDisplayName"/>
  </c:if>
  <c:set var="noData" value="false"/>

  <c:set var="tableClassName">
    <c:choose>
      <c:when test="${dataTable eq true}">recordTable wdk-data-table</c:when>
      <c:otherwise>recordTable</c:otherwise>
    </c:choose>
  </c:set>


<c:set var="tblContent">

  <div class="table-preamble">${preamble}</div>

    <%-- display the description --%>
    <div class="table-description">${tbl.tableField.description}</div>

    <c:choose>
      <c:when test="${tblName == 'UserComments'}">
        <c:set value="${requestScope.wdkRecord}" var="wdkRecord"/>
        <c:set var="primaryKey" value="${wdkRecord.primaryKey}"/>
        <c:set var="pkValues" value="${primaryKey.values}" />
        <c:set var="projectId" value="${pkValues['project_id']}" />
        <c:set var="id" value="${pkValues['source_id']}" />

        <table class="${tableClassName}" title="Click to go to the comments page"  style="cursor:pointer" onclick="window.location='<c:url value="/showComment.do?projectId=${projectId}&stableId=${id}&commentTargetId=gene"/>';">
      </c:when>
      <c:otherwise>
        <table class="${tableClassName}">
      </c:otherwise>
    </c:choose>

    <c:set var="fields" value="${tbl.tableField.attributeFields}"/>
    <c:if test="${suppressColumnHeaders == null || !suppressColumnHeaders}">
      <thead>
        <c:set var="h" value="0"/>
        <tr class="headerRow">
            <c:forEach var="hCol" items="${fields}">
               <c:if test="${hCol.internal == false}">
                 <c:set var="h" value="${h+1}"/>
                 <th align="left">${hCol.displayName}</th>
               </c:if>
            </c:forEach>
        </tr>
      </thead>
    </c:if>

      <tbody>
        <%-- table rows --%>
        <c:set var="i" value="0"/>
        <c:forEach var="row" items="${tbl.iterator}">
            <c:set var="hasRow" value="true" />
            <c:choose>
                <c:when test="${i % 2 == 0}"><tr class="rowLight"></c:when>
                <c:otherwise><tr class="rowMedium"></c:otherwise>
            </c:choose>

            <c:set var="j" value="0"/>
            <c:forEach var="field" items="${fields}">
              <c:set var="attributeValue" value="${row[field.name]}"/>
              <c:if test="${attributeValue.attributeField.internal == false}">
                <c:set var="j" value="${j+1}"/>
                <imp:wdkAttribute recordClass="${wdkRecord.recordClass}" attributeValue="${attributeValue}" truncate="false" />
              </c:if>
            </c:forEach>

            </tr>
            <c:set var="i" value="${i +  1}"/>
        </c:forEach>
      </tbody>
    </table>

    <c:if test="${i == 0}">
      <c:set var="noData" value="true"/>
    </c:if>

    ${postscript}

  </c:set>

</c:catch>

<c:if test="${tableError != null}">
    <c:set var="exception" value="${tableError}" scope="request"/>
    <c:set var="tblContent" value="<i>Error. Data is temporarily unavailable</i>"/>
</c:if>

<imp:toggle name="${tblName}" displayName="${tableDisplayName}"
             content="${tblContent}" isOpen="${isOpen}" noData="${noData}"
             attribution="${attribution}"/>
