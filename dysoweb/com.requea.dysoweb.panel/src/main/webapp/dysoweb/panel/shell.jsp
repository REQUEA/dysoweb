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
	<script src="<%=request.getContextPath()%>/dysoweb/panel/js/prototype-1.5.1.js" type="text/javascript"></script>	
	<script type="text/javascript">
function dysowebExecuteCommand() {
	// send the ajax request command
	new Ajax.Updater('dysowebResults', '<%=request.getContextPath()%>/dysoweb/shell/exec', {
	  parameters: $('dysowebForm').serialize(true)
	  });	
	// empty the command
	dysowebForm["command"].value = "";
}
	</script>
	<style type="text/css" media="all">
#dysowebResults {
	font-family:"Courier New", Courier, monospace;
	font-size:130%;
}
#dysowebResults .err {
	color:#e00;
}
#dysowebResults .msg {
	color:#000;
}
	</style>	
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
							<h2><span>Dysoweb Shell</span></h2>
							<form id="dysowebForm" action="javascript:dysowebExecuteCommand()">
								<p>This shell allows you to enter OSGi commands and administer the platform. Type 'help' for more information.</p>
								Command: <input name="command" value="" size="128" />
							</form>
							<div id="dysowebResults">
							</div>
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
