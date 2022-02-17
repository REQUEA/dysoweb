<%@ taglib uri="http://taglibs.requea.com/dw/2007/" prefix="dw"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" lang="en" xml:lang="en">
<head>
	<script src="<%=request.getContextPath()%>/dysoweb/shell/js/prototype-1.7.3.js" type="text/javascript"></script>
	<script type="text/javascript">
function dysowebExecuteCommand() {
	// send the ajax request command
	new Ajax.Updater('dysowebResults', '<%=response.encodeURL(request.getContextPath()+"/dysoweb/shell/exec")%>', {
	  parameters: $('dysowebForm').serialize(true)
	  });	
	// empty the command
	dysowebForm["command"].value = "";
}
	</script>
	<style type="text/css" media="all">
body {
	background:#eee;
	margin:0px;
}
#dysowebResults {
	font-family:"Courier New", Courier, monospace;
}
#dysowebResults .err {
	color:#e00;
}
#dysowebResults .msg {
	color:#000;
}
#top {
	height:150px;
	background:url('<%=request.getContextPath()%>/dysoweb/shell/css/logo.gif') no-repeat #99cc17;
}
#main {
	margin:10px;
	padding:20px;
	border:1px solid #888;
	background:#fff;
}
	</style>	
</head>
<body>
<div id="top"></div>
<div id="main">
<form id="dysowebForm" action="javascript:dysowebExecuteCommand()">
	<p>This shell allows you to enter OSGi commands and administer the platform. Type 'help' for more information.</p>
	Command: <input name="command" value="" size="128" />
</form>
<div id="dysowebResults">
</div>
</div>

</body>
</html>
