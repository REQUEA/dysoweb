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
	<script type="text/javascript">
function pb(elt) {
    // retrieve the parent form
    var form = elt;
    while(form != null && form.tagName != 'FORM') {
        // get the parent
        form = form.parentNode;
    }
	form["op"].value = "postback";
	form.submit();
}
function test(elt) {
    // retrieve the parent form
    var form = elt;
    while(form != null && form.tagName != 'FORM') {
        // get the parent
        form = form.parentNode;
    }
	form["op"].value = "test";
	form.submit();
}
	</script>
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
						<form method="post" action="<%=request.getContextPath()%>/dysoweb/panel/secure/settings">
							<panel:error />
							<panel:input name="info" style="testok" />
							<input name="ru" type="hidden" value="<%=request.getAttribute("com.requea.dysoweb.panel.ru") %>" />
							<input name="op" type="hidden" value="save" />
							<h2><span>Repository settings</span></h2>
							<table width="100%">
								<tr><td class="lbl">Repository URL:</td><td class="val"><panel:input name="RepoURL" size="60"/></td></tr>
								<tr><td class="lbl">Authorization Key:</td><td class="val"><panel:input name="AuthKey" size="25"/></td></tr>
							</table>							
							<h2><span>Proxy settings</span></h2>
							<table width="100%">
								<tr><td class="lbl">Proxy</td><td class="val"><panel:input name="Proxy" style="radio" /></td></tr>
								<panel:input name="ProxySettings">								
								<tr><td class="lbl">Host:</td><td class="val"><panel:input name="ProxyHost"/></td></tr>
								<tr><td class="lbl">Port:</td><td class="val"><panel:input name="ProxyPort" size="4"/></td></tr>
								<tr><td class="lbl">Authorization:</td><td class="val"><panel:input name="ProxyAuth"/></td></tr>
								</panel:input>
							</table>							
							<table width="100%">
								<tr><td colspan="2" align="center"><input type="button" onclick="test(this)" value="Test"/><input type="submit" value="Save"/></td></tr>
							</table>							
						</form>  
					</div>
				</div>
			</div>
		</div>
	</div>
</div>
<dw:insert path="/dysoweb/panel/footer.jsp"/>		
</body>
</html>
