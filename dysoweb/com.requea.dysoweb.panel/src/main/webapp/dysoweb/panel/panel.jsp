<%@ page session="false" %>
<%@ page language="java" contentType="text/html; charset=UTF-8" %>
<%@ taglib uri="http://taglibs.requea.com/dw/2007/" prefix="dw"%>
<%@ taglib uri="http://taglibs.requea.com/panel/2007/" prefix="panel" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" lang="en" xml:lang="en">
<head>
	<title>Dysoweb Control Panel</title>
	<meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
    <link href="<%= request.getContextPath() %>/dysoweb/panel/css/style-1.0.0.css" rel="stylesheet" type="text/css" />
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
							<h2>Dysoweb Documentation</h2>
							<p>
								The Dysoweb documentation is available on a wiki at: <a target="_blank" href="http://dysopedia.requea.com">http://dysopedia.requea.com</a><br/>			
								<br/>
								You will find links to other resources as well as tips and tools to develop with Dysoweb.<br/>
								<br/>
								Feel free to add your contribution to this documentation.<br/>
							</p>
						</div>
						<div>
							<h2>Dysoweb Tools for this OSGi Platform</h2>
							<ul class="mll">
								<li><dw:menu style="mli" path="/dysoweb/panel/secure/install" label="Install new Dysoweb applications from the Requea Repository" /></li>
								<li><dw:menu style="mli" path="/dysoweb/panel/secure/bundles.jsp" label="List of Bundles for this OSGi Platform" /></li>
								<dw:insert bundle="com.requea.dysoweb.shell"><li><dw:menu style="mli" path="/dysoweb/panel/secure/shell.jsp" label="OSGi Shell to the platform" /></li></dw:insert>
							</ul>
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
