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
	<script src="<%=request.getContextPath()%>/dysoweb/panel/js/prototype-1.5.1.js" type="text/javascript"></script>	
	<script type="text/javascript">
function dysowebRefreshStatus() {
	// send the ajax request command
	new Ajax.Updater('dysowebResults', '<%=request.getContextPath()%>/dysoweb/panel/secure/install?op=status', {
		onComplete:function(transport, param) { 
			var doc = transport.responseXML;
			if(doc) {
				var elResult = doc.documentElement;
				if(elResult) {
					var status = elResult.getAttribute("status");
					if(status == "done") {
						var launch = $('launch');
						if(launch != null) {
							launch.style.display = "block";
						}
						return;
					} else if(status == "error") {
						return;
					}
				}
			}
			// request for refresh
			setTimeout("dysowebRefreshStatus()", 100);
		}
	});
}
	</script>
	<style type="text/css" media="all">
#dysowebResults .err {
	color:#e00;
}
#dysowebResults .msg {
	color:#000;
}
	</style>	
</head>
<body onload="dysowebRefreshStatus()">
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
							<h2><span>Dysoweb Application installation</span></h2>
							<panel:error />
							<div id="dysowebResults">
								<p>Installation in progress. Please wait</p>
								<div class="rqwait"><span>wait please</span></div>
							</div>
							<div style="display:none" id="launch"><panel:feature style="rplaunch" property="launch" /></div>
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
