<%@ taglib uri="http://taglibs.requea.com/dw/2007/" prefix="dw" %>
<%@ taglib uri="http://taglibs.requea.com/rqp/2007/" prefix="rqp" %>
<html>
<body>
<img src="<%= request.getContextPath() %>/test/img/test.jpg" />
<p>Hello from test with a taglib</p>
<rqp:test property="tata" />

<table width="100%">
<tr><th>Name</th><th>Symbolic name</th><th>Version</th><th>State</th></tr>
<dw:bundles>
<tr><td><dw:bundle property="Bundle-Name" /></td><td><dw:bundle property="Bundle-SymbolicName" /></td><td><dw:bundle property="Bundle-Version" /></td><td><dw:bundle property="state" /></td></tr>
</dw:bundles>
</table>
</body>
</html>
