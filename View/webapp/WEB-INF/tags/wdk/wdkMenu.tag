<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="imp" tagdir="/WEB-INF/tags/imp" %>

<%@ attribute name="refer" 
                          type="java.lang.String"
                          required="false" 
                          description="Page calling this tag"
%>


<c:set var="model" value="${applicationScope.wdkModel}" />
<c:set var="wdkUser" value="${sessionScope.wdkUser}" />
<c:set var="basketCount" value="${wdkUser.basketCount}"/>

<%-- new search menu --%>
<li>
  <a title="START a NEW search strategy. Searches are organized by the genomic feature they return."
    ><span>New Search</span>
  </a>
  <ul><imp:searchCategories /></ul>
</li>

<%-- strategy menu --%>
<li>
  <a id="mysearch" onclick="wdk.stratTabCookie.setCurrentTabCookie('application','strategy_results');" 
     href="<c:url value="/showApplication.do"/>" title="Access your Search Strategies Workspace"
    ><span>My Strategies</span>
  </a>
</li>

<%-- basket menu --%>
<c:choose>
  <c:when test="${wdkUser == null || wdkUser.guest}">
    <c:set var="clickEvent" value="wdk.stratTabCookie.setCurrentTabCookie('application', 'basket'); wdk.user.login('use baskets', wdk.webappUrl('showApplication.do'));" />
    <c:set var="title" value="Group IDs together to work with them. You can add IDs from a result, or from a details page." />
    <c:set var="href" value="javascript:void(0)" />
  </c:when>
  <c:when test="${refer == 'summary'}">
    <c:set var="clickEvent" value="wdk.addStepPopup.showPanel('basket');" />
    <c:set var="title" value="Group IDs together to later make a step in a strategy." />
    <c:set var="href" value="javascript:void(0)" />
  </c:when>
  <c:otherwise>
    <c:set var="clickEvent" value="wdk.stratTabCookie.setCurrentTabCookie('application', 'basket');" />
    <c:set var="title" value="Group IDs together to later make a step in a strategy." />
    <c:url var="href" value="/showApplication.do" />
  </c:otherwise>
</c:choose>
<li>
  <a id="mybasket" onclick="${clickEvent}" href="${href}" title="${title}"
    ><span>My Basket <span class="subscriptCount">(${basketCount})</span></span>
  </a>
</li>

<%-- favorite menu --%>
<li id="favorite-menu">
<c:choose>
  <c:when test="${wdkUser eq null or wdkUser.guest}">
      <a id="mybasket" onclick="wdk.user.login('use favorites', wdk.webappUrl('showFavorite.do'));"
         href="javascript:void(0)"
         title="Store IDs for easy access to their details page. You can add IDs *only* from the details page, one at a time." >My Favorites</a>
  </c:when>
  <c:otherwise>
      <a href="<c:url value="/showFavorite.do"/>"
         title="Store IDs for easy access to their details page. You can add IDs *only* from the details page, one at a time.">My Favorites</a>
  </c:otherwise>
</c:choose> 
</li>
