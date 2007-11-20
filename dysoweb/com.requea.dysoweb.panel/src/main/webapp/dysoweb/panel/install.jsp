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
							<h2><span>Dysoweb Application installation</span></h2>
							<panel:error />
							<table width="100%" cellpadding="0" cellspacing="0" class="rpfeatlist"><tr valign="top">
							<td>
								<form method="post" name="frminstall" action="<%=request.getContextPath()%>/dysoweb/panel/install">
								<input type="hidden" name="op" value="install"/>
								<div class="rpfeatinst"><input type="submit" value="Install Selected features"></input></div>
								<table cellpadding="5">
								<panel:features>
								<tr valign="center">
									<td><panel:feature property="check"/></input></td>
									<td width="200"><panel:feature style="rpimg" property="image"/></td>
									<td align="left">
										<div class="rpfeatdesc"><panel:feature property="description"/></div>
										<div class="rpfeatldesc"><panel:feature property="longDesc"/></div>
									</td></tr>
								</panel:features>
								</table>
								<div class="rpfeatinst"><input type="submit" value="Install Selected features"></input></div>
								</form>
							</td>
							<td width="200">
								<a href="<%=request.getContextPath() %>/dysoweb/panel/install?refresh=true">Refresh list</a>
								<ul class="rpcat">Categories:
								<li><a href="<%=request.getContextPath()%>/dysoweb/panel/install" class="rpcatlnk">All categories</a></li>
								<panel:categories>
								<li><panel:category style="rpcatlnk" property="link"/></li>
								</panel:categories>
								</ul>								
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
