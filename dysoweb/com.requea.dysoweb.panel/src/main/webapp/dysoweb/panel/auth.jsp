<%@ page session="false" %>
<%@ page language="java" contentType="text/html; charset=UTF-8" %>
<%@ taglib uri="http://taglibs.requea.com/dw/2007/" prefix="dw"%>
<%@ taglib uri="http://taglibs.requea.com/panel/2007/" prefix="panel" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" lang="en" xml:lang="en">
<head>
	<title>Dysoweb Control Panel</title>
	<meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
    <link href="<%= request.getContextPath() %>/dysoweb/panel/css/style-1.0.10.css" rel="stylesheet" type="text/css" />
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
						<div>
							<h2><span>Dysoweb Login</span></h2>
							<p>This platform requires authentication. Please enter your password.</p>
							<panel:error />
							<form method="post" action="<%=response.encodeURL(request.getContextPath()+"/dysoweb/panel/auth")%>" class="secure">
							<input name="ru" type="hidden" value="<%=request.getAttribute("com.requea.dysoweb.panel.ru") %>" />
							<input name="op" type="hidden" value="auth" />
							<table width="100%">
								<tr><td class="lbl"><span class="req">*</span>Password:</td><td class="val"><panel:input name="Password" type="password"/></td></tr>
								<tr><td colspan="2" align="center"><input type="submit" name="submit" value="Login"/></td></tr>
							</table>
							</form>  
						</div>
					</div>
				</div>
			</div>
		</div>
	</div>
</div>
<dw:insert path="/dysoweb/panel/footer.jsp"/>		
</body>
</html>
