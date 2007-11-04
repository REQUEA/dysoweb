<%@ page session="false" %>
<%@ page language="java" contentType="text/html; charset=UTF-8" %>
<%@ taglib uri="http://taglibs.requea.com/dw/2007/" prefix="dw"%>
<%@ taglib uri="http://taglibs.requea.com/panel/2007/" prefix="panel" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" lang="en" xml:lang="en">
<head>
	<title>Dysoweb Control Panel</title>
	<meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
    <link href="<%= request.getContextPath() %>/dysoweb/panel/css/style.css" rel="stylesheet" type="text/css" />
    <link href="<%= request.getContextPath() %>/dysoweb/demo/style.css" rel="stylesheet" type="text/css" />
	<dw:insert bundle="com.requea.dysoweb.demo2">
    <link href="<%= request.getContextPath() %>/dysoweb/demo/style2.css" rel="stylesheet" type="text/css" />
	</dw:insert>
    
</head>
<body>
<div id="it">
	<div id="il">
		<div id="inner">
			<div class="rqbanner">
			<dw:insert path="/dysoweb/panel/top.jsp"/>		
			<dw:insert path="/dysoweb/panel/mbar.jsp"/>		
			</div>	
			<div class="rqdefaultpage">
				<div class="rqportlet">
					<div class="rqview">
						<div class="rqerror">Error: For security reason this operation is not allowed</div>
						<p>Hi, this is a security feature implemented as a Servlet Filter in the demo5 bundle. I control all operations on bundles and make sure that 
						only operations on bundles demo2, demo3, demo4 are processed.
						<br/>
						Otherwise, the site may go down if another bundle is stopped. Since you would have no way to restart this bundle, we just prevent you to do it.
						</div>
					</div>
				</div>
			</div>
			<dw:insert path="/dysoweb/panel/footer.jsp"/>		
		</div>
	</div>
</div>
</body>
</html>
