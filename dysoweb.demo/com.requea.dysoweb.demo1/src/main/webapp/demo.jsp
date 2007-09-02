<%@ taglib uri="http://taglibs.requea.com/dw/2007/" prefix="dw"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" lang="en" xml:lang="en">
	<head>
		<link href="<%=request.getContextPath()%>/css/style.css" rel="stylesheet" type="text/css" />
		<dw:insert bundle="com.requea.dysoweb.demo2">
		<link href="<%=request.getContextPath()%>/demo2/css/style.css" rel="stylesheet" type="text/css" />
		</dw:insert>
	</head>
	<body>
		<div id="top"></div>
		<dw:insert path="/jsp/menu.jsp" />
			<div id="page">
			<table><tr valign="top"><td width="100%">
			<div class="content">
			
			<h2>Demonstration page</h2>
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
			<p>This bundle is currently '<dw:bundle bundle="com.requea.dysoweb.demo2" property="stateimg" />&nbsp;&nbsp;<dw:bundle bundle="com.requea.dysoweb.demo2" property="state" />'. <dw:bundle bundle="com.requea.dysoweb.demo2" property="command" /></p>
			
			<h3>Requea Dysoweb  Demo3 Google Search bar (com.requea.dysoweb.demo3)</h3>
			<p>
				A dysoweb application that provides a JSP snipet to search Google. It includes the following J2EE elemenents:
				<ul>
					<li>A JSP page that is included into the demo page on the condition that this bundle is started</li>
				</ul>
			</p>
			<p>This bundle is currently '<dw:bundle bundle="com.requea.dysoweb.demo3" property="stateimg" />&nbsp;&nbsp;<dw:bundle bundle="com.requea.dysoweb.demo3" property="state" />'. <dw:bundle bundle="com.requea.dysoweb.demo3" property="command" /></p>
			
			<h3>Requea Dysoweb  Demo4 Vote widget (com.requea.dysoweb.demo4)</h3>
			<p>
				A dysoweb application that provides the vote functionality. It includes the following J2EE elemenents:
				<ul>
					<li>A JSP page that is included into the demo page on the condition that this bundle is started</li>
					<li>A servlet to register the vote and render the vote result</li>
					<li>Java classes and supporting jar files (JFreeChart)</li>
				</ul>
			</p>
			<p>This bundle is currently '<dw:bundle bundle="com.requea.dysoweb.demo4" property="stateimg" />&nbsp;&nbsp;<dw:bundle bundle="com.requea.dysoweb.demo4" property="state" />'. <dw:bundle bundle="com.requea.dysoweb.demo4" property="command" /></p>
			</div>
			</td>
			<td width="320">
			<div class="plugin">
			<div class="bundleinfo">
				Vote widget is provided by bundle demo4 <dw:bundle bundle="com.requea.dysoweb.demo4" property="command" />
			</div>
			<dw:insert bundle="com.requea.dysoweb.demo4" path="/demo4/vote" />
			</div>
			
			<div class="plugin">
			<div class="bundleinfo">
				Google search is provided by bundle demo3 <dw:bundle bundle="com.requea.dysoweb.demo3" property="command" />
			</div>
		<dw:insert bundle="com.requea.dysoweb.demo3" path="/google/jsp/requea.jsp" />
		</div>
		</td>
		</tr></table>
		</div>
</body>
</html>
