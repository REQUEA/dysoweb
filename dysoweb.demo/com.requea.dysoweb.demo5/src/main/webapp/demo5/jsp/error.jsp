<%@ taglib uri="http://taglibs.requea.com/dw/2007/" prefix="dw"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" lang="en" xml:lang="en">
<head>
<link href="<%=request.getContextPath()%>/css/demo.css" rel="stylesheet"
	type="text/css" />
<dw:insert bundle="com.requea.dysoweb.demo2">
<link href="<%=request.getContextPath()%>/demo2/css/demo2.css" rel="stylesheet"
	type="text/css" />
</dw:insert>
<link href="http://www.google.com/uds/css/gsearch.css" type="text/css" rel="stylesheet"/>
</head>
<body>
<div id="top"></div>
<dw:insert path="/jsp/menu.jsp" />
<div id="page">
<h2>Error: For security reason this operation is not allowed</h2>
<p>Hi, this is a security feature implemented as a Servlet Filter in the demo5 bundle. I control all operations on bundles and make sure that 
only operations on bundles demo2, demo3, demo4 are processed.
<br/>
Otherwise, the site may go down if another bundle is stopped. Since you would have no way to restart this bundle, we just prevent you to do it.
</div>
</body>
</html>
