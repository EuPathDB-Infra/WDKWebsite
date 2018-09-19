<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="imp" tagdir="/WEB-INF/tags/imp" %>
<%@ attribute name="title"
			  required="false"
			  description="Title to appear in the header"
%>
<div id="query_form_overlay" class="ui-widget-overlay"></div>
<div id="query_form" style="min-height:140px;">
<span class="dragHandle">
	<div class="modal_name">
    <div style="font-size:130%;margin-top:4px;text-align:center;" id="query_form_title"><strong>${title}</strong></div>
	</div>
	<a class="back" href="javascript:wdk.addStepPopup.backStage()">
		<imp:image src="wdk/images/backbutton.png" alt='Close'/>
	</a>
	<a class='close_window' href='javascript:wdk.addStepPopup.closeAll()'>
		<imp:image src="wdk/images/closebutton.png" alt='Close'/>
	</a>
</span>
<div id="errors"></div>
<div id="qf_content">
