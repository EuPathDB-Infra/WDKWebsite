<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="imp" tagdir="/WEB-INF/tags/imp" %>

<c:set var="wdkUser" value="${sessionScope.wdkUser}" />
<c:set var="baskets" value="${requestScope.baskets}" />
<c:set var="total" value= "${0}" />
<c:forEach items="${baskets}" var="item">
  <c:set var="total" value="${total + item.value}" />
</c:forEach>

<div data-controller="wdk.basket.configureBasket">

  <div id="basket-control-panel">
    <imp:basketControls />
  </div>

  <!-- the order of tabs is determined in apicommonmodel.xml -->
  <c:choose>
    <c:when test="${total > 0}">
      <div style="border:none" id="basket-menu" class="tabs">

        <ul>
          <c:forEach items="${baskets}" var="item">
            <c:set var="recordClass" value="${item.key}" />
            <c:set var="count" value="${item.value}" />
            <c:if test="${count > 0}">
              <li id="${fn:replace(recordClass.fullName, '.', '_')}" 
                  data-recordClass="${recordClass.fullName}">
                <a href="<c:url value='/showBasket.do?recordClass=${recordClass.fullName}'/>"
                  >${recordClass.displayName} (<span class="count">${item.value}</span>)
                  <span> </span> </a>
              </li>
            </c:if>
          </c:forEach>
        </ul>

      </div>
    </c:when>
    <c:otherwise>
      <div style="font-size:120%;line-height:1.2em;text-indent:10em;padding:0.5em">You have no items in any of your baskets.</div>
    </c:otherwise>
  </c:choose>

</div>
