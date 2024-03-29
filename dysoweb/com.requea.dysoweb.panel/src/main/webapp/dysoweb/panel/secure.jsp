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
						<div>
							<h2><span>Dysoweb Security Registration</span></h2>
							<p>This platform has not been secured. Please register and enter a password.</p>
							<panel:error />
							<form method="post" action="<%=response.encodeURL(request.getContextPath()+"/dysoweb/panel/secure/install")%>" class="secure">
							<input name="ru" type="hidden" value="<%=request.getAttribute("com.requea.dysoweb.panel.ru") %>" />
							<input name="op" type="hidden" value="register" />
							<table width="100%">
                                <tr><td class="lbl">Authorization Key:</td><td class="val"><panel:input name="AuthKey" size="34"/>&nbsp;<a href="https://my.requea.com/do/rqRepoKeyRequest:new" target="_blank">Request an authorization key</a></td></tr>
								<tr><td class="lbl"><span class="req">*</span>Password for this platform:</td><td class="val"><panel:input name="Password" type="password"/></td></tr>
								<tr><td class="lbl"><span class="req">*</span>Confirm Password:</td><td class="val"><panel:input name="Password2" type="password"/></td></tr>
								<tr><td colspan="2" align="center"><input type="submit" name="btnSubmit" value="Secure this Dysoweb Platform"/></td></tr>
							</table>							
                            <h2><span>Network Configuration</span></h2>
                            <table width="100%">
                                <tr><td class="lbl">Settings</td><td class="val"><panel:input name="Settings" style="radio" /></td></tr>                            
                                <panel:input name="ManualSettings">                              
                                <tr><td class="lbl">Proxy PAC:</td><td class="val"><panel:input name="ProxyPAC"/></td></tr>
                                <tr><td class="lbl">Proxy Host:</td><td class="val"><panel:input name="ProxyHost"/></td></tr>
                                <tr><td class="lbl">Proxy Port:</td><td class="val"><panel:input name="ProxyPort" size="4"/></td></tr>
                                <tr><td class="lbl">Proxy Authorization Name:</td><td class="val"><panel:input name="ProxyUsername"/></td></tr>
                                <tr><td class="lbl">Proxy Authorization Password:</td><td class="val"><panel:input name="ProxyPassword" type="password" /></td></tr>
                                </panel:input>
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
