<%@ taglib uri="http://taglibs.requea.com/dw/2007/" prefix="dw"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" lang="en" xml:lang="en">
	<head>
		<link href="<%=request.getContextPath()%>/css/style.css" rel="stylesheet" type="text/css" />
	</head>
	<body>
		<div id="top"></div>
		<dw:insert path="/jsp/menu.jsp" />
		<div id="page">
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
				<a href="<%=request.getContextPath()%>/demo.html" target="_blank">Launch a Dysoweb presentation</a>
			</p>
			<h2>About Requea</h2>
			<p>
			    Requea is a private software company based in France behind the Dysoweb project.<br/>
			    We provide Request management software for IT, Facilities and HR services.<br/> 
				<br/>
			    With our software, your employees can easily submit their requests such as time off requests, room reservation requests, it requests and so on.<br/> 
			    Based on a powerful workflow engine, those requests are properly managed. The results are time saving, less frustration and significant costs savings.<br/>
				<br/>
			    You can visit our web site at: <a href="http://www.requea.com">http://www.requea.com</a>
			</p>
			<h2>About This site</h2>
			<p>
				This site run on a Dysoweb  Platform and provides a few services packaged as bundles. Some bundles may be started and stopped dynamically.<br/>
				See the demo page: <a href="<%=request.getContextPath()%>/demo.jsp">demo</a>
			</p>
		</div>
	</body>
</html>
