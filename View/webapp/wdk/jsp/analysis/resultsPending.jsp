<?xml version="1.0" encoding="UTF-8"?>
<jsp:root version="2.0"
    xmlns:jsp="http://java.sun.com/JSP/Page"
    xmlns:c="http://java.sun.com/jsp/jstl/core">
  <jsp:directive.page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"/>
  <html>
    <body>
      <div class="analysis-pending-pane" data-analysisid="${analysisId}"
           data-controller="wdk.stepAnalysis.analysisRefresh">
        <h3>Results Pending...</h3>
        <p>
          The results of this analysis are not yet available.<br/>
          We will check again in <span class="countdown">3</span> seconds.
        </p>
      </div>
    </body>
  </html>
</jsp:root>
