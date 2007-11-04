<%@ page session="false" %>
<%@ page language="java" contentType="text/html; charset=UTF-8" %>
<%@ taglib uri="http://taglibs.requea.com/dw/2007/" prefix="dw"%>
<%@ taglib uri="http://taglibs.requea.com/panel/2007/" prefix="panel" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" lang="en" xml:lang="en">
<head>
	<title>Dysoweb Control Panel</title>
	<meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
    <link href="<%= request.getContextPath() %>/dysoweb/panel/css/style.css" rel="stylesheet" type="text/css" />
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
							<h2><span><panel:feature property="title"/></span></h2>
							<panel:error />
							<table cellpadding="10">
								<tr valign="top">
									<td width="50%" align="right">
										<panel:feature style="rpimg" property="image"/><br/>
										<panel:feature property="version"/><br/>
										<panel:feature property="infoURL"/>
									</td>
									<td><panel:feature property="longDesc"/></td>
								</tr>
								<tr><td colspan="2" align="center"><panel:feature style="rqbtn rpinstall" property="install"/></td></tr>
							</table>							
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
