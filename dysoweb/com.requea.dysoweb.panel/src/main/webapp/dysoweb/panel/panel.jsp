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
							<h2>About Dysoweb</h2>
							<p>
								Requea Dysoweb  solves the problem of the modular deployment of Web applications. What we mean is the ability to add, remove, upgrade some services within a web application without having to package a new war file, mess with web descriptors and restart the server.<br/>
								
								Dysoweb  is a technology that allows you to bundle parts of a web application as an OSGi bundle to provide a dynamic service. Each bundle is a "mini Web application" with its own web.xml descriptor and may define its own set of static files, JSP pages, taglibs, servlet and filters.<br/>
								<br/>
								Each dysoweb application operates as a complete service that can be dynamically installed, started, stopped, updated on a running instance of a Dysoweb  platform.<br/>
								Since each bundle is a dynamic service, it may be stopped at any time without restarting the web server, and it may be updated (meaning new code, classes, jars, JSP, static files) and brought online at any time<br/>
								<br/>
								The Dysoweb  Platform is an OSGi platform and may host any OSGi compliant bundle to provide additional services for Dysoweb applications.<br/>
								<br/>
								The Dysoweb platform runs as a standard WebApp (.WAR file) in a standard J2EE servlet container (such as Tomcat)<br/>
								<br/>				
							</p>
							<h2>About Requea</h2>
							<p>
							    Requea is a private software company based in France behind the Dysoweb project.<br/>
							    We provide Request management software for IT, Facilities and HR services.<br/> 
								<br/>
							    With our software, your employees can easily submit their requests such as time off requests, room reservation requests, it requests and so on.<br/> 
							    Based on a powerful workflow engine, those requests are properly managed. The results are time saving, less frustration and significant costs savings.<br/>
								<br/>
							    You can visit our web site at: <a target="_blank" href="http://www.requea.com">http://www.requea.com</a>
							</p>
							<h2>Dysoweb Documentation</h2>
							<p>
								The Dysoweb documentation is available on a wiki at: <a target="_blank" href="http://dysoweb.requea.com/dysopedia">http://dysoweb.requea.com/dysopedia</a><br/>			
								<br/>
								You will find links to other resources as well as tips and tools to develop with Dysoweb.<br/>
								<br/>
								Feel free to add your contribution to this documentation.<br/>
							</p>
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
