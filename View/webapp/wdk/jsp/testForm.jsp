<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="html" uri="http://struts.apache.org/tags-html" %>

<html:errors/>

<html:form method="get" action="/processTestForm.do">
    <html:text property="paramA"/><br>
    <html:text property="paramB"/><br>

    <html:submit property="submit" value="GO"/></td>
</html:form>
