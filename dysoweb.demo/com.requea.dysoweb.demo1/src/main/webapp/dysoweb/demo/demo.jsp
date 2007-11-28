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
						<div>
							<h2><span>Dysoweb bundles</span></h2>
							<table width="100%"><tr valign="top"><td>
							<p>
								This site is build on a Dysoweb  platform (as you may have guessed). Some demo bundles may be turned on or off to see the effects of the dysoweb architecture. 
								Here is the list of those demo bundles:
							</p>
							
							<h3>Requea Dysoweb  Demo2 CSS Relooking (com.requea.dysoweb.demo2)</h3>
							<p>
								A dysoweb application that provides an alternate CSS for the site. It includes the following J2EE elemenents:
								<ul>
									<li>CSS, Images (JPG) for the look and feel</li>
								</ul>
							</p>
							<p>This bundle is currently '<dw:bundle bundle="com.requea.dysoweb.demo2" property="stateimg" />&nbsp;&nbsp;<dw:bundle bundle="com.requea.dysoweb.demo2" property="state" />'. <dw:bundle bundle="com.requea.dysoweb.demo2" property="command" command="dwdemobndl" /></p>
							
							<h3>Requea Dysoweb  Demo3 Google Search bar (com.requea.dysoweb.demo3)</h3>
							<p>
								A dysoweb application that provides a JSP snipet to search Google. It includes the following J2EE elemenents:
								<ul>
									<li>A JSP page that is included into the demo page on the condition that this bundle is started</li>
								</ul>
							</p>
							<p>This bundle is currently '<dw:bundle bundle="com.requea.dysoweb.demo3" property="stateimg" />&nbsp;&nbsp;<dw:bundle bundle="com.requea.dysoweb.demo3" property="state" />'. <dw:bundle bundle="com.requea.dysoweb.demo3" property="command" command="dwdemobndl" /></p>
							
							<h3>Requea Dysoweb  Demo4 Vote widget (com.requea.dysoweb.demo4)</h3>
							<p>
								A dysoweb application that provides the vote functionality. It includes the following J2EE elemenents:
								<ul>
									<li>A JSP page that is included into the demo page on the condition that this bundle is started</li>
									<li>A servlet to register the vote and render the vote result</li>
									<li>Java classes and supporting jar files (JFreeChart)</li>
								</ul>
							</p>
							<p>This bundle is currently '<dw:bundle bundle="com.requea.dysoweb.demo4" property="stateimg" />&nbsp;&nbsp;<dw:bundle bundle="com.requea.dysoweb.demo4" property="state" />'. <dw:bundle bundle="com.requea.dysoweb.demo4" property="command" command="dwdemobndl" /></p>
							</div>
							</td>
							<td width="320">
							<div class="plugin">
							<div class="bundleinfo">
								Vote widget is provided by bundle demo4 <dw:bundle bundle="com.requea.dysoweb.demo4" property="command" command="dwdemobndl" />
							</div>
							<dw:insert bundle="com.requea.dysoweb.demo4" path="/demo4/vote" />
							</div>
							
							<div class="plugin">
							<div class="bundleinfo">
								Google search is provided by bundle demo3 <dw:bundle bundle="com.requea.dysoweb.demo3" property="command"  command="dwdemobndl" />
							</div>
							<dw:insert bundle="com.requea.dysoweb.demo3" path="/google/jsp/requea.jsp" />
							</div>
							</td>
							</tr></table>
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
