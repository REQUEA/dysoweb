<%@ page session="false" %>
<%@ page language="java" contentType="text/html; charset=UTF-8" %>
<%@ taglib uri="http://taglibs.requea.com/dw/2007/" prefix="dw"%>
<%@ taglib uri="http://taglibs.requea.com/panel/2007/" prefix="panel" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" lang="en" xml:lang="en">
<head>
	<title>Dysoweb Control Panel</title>
	<meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
    <link href="<%= request.getContextPath() %>/dysoweb/panel/css/style-1.0.9.css" rel="stylesheet" type="text/css" />
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
						<form method="post" action="<%=response.encodeURL(request.getContextPath()+'/dysoweb/panel/secure/install')">
							<panel:error />
							<panel:input name="info" style="testok" />
							<input name="ru" type="hidden" value="<%=request.getAttribute("com.requea.dysoweb.panel.ru") %>" />
                            <input name="settings" type="hidden" value="true" />
                            <input name="op" type="hidden" value="save" />
							<h2><span>Repository</span></h2>
							<table width="100%">
								<tr><td class="lbl">Authorization Key:</td><td class="val"><panel:input name="AuthKey" size="25"/></td></tr>
							</table>							
							<h2><span>Configuration</span></h2>
							<table width="100%">
                                <tr><td class="lbl">Settings</td><td class="val"><panel:input name="Settings" style="radio" /></td></tr>							
                                <panel:input name="ManualSettings">                              
								<tr><td class="lbl">Proxy Host:</td><td class="val"><panel:input name="ProxyHost"/></td></tr>
								<tr><td class="lbl">Proxy Port:</td><td class="val"><panel:input name="ProxyPort" size="4"/></td></tr>
								<tr><td class="lbl">Proxy Authorization Name:</td><td class="val"><panel:input name="ProxyUsername"/></td></tr>
                                <tr><td class="lbl">Proxy Authorization Password:</td><td class="val"><panel:input name="ProxyPassword"/></td></tr>
                                </panel:input>
							</table>							
							<table width="100%">
								<tr><td colspan="2" align="center"><input type="submit" value="Save"/></td></tr>
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
