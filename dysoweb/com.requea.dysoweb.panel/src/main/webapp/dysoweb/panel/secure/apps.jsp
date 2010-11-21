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
							<h2><span>Dysoweb Application list</span></h2>
							<panel:error />
							<table width="100%" class="tblreq" cellpadding="1" cellspacing="0">
							<thead><tr>
								<th>Installation date</th>
								<th></th>
								<th>Title</th>
								<th>Description</th>
								<th>Version</th>
							</tr></thead>
							<panel:installedfeatures>
							<tr valign="middle">
								<td><panel:installedfeature property="date"/></td>
								<td><panel:installedfeature property="image"/></td>
								<td><panel:installedfeature property="title"/><br/>
									<panel:installedfeature property="id"/>
									<panel:installedfeature property="version"/>
								</td>
								<td><panel:installedfeature property="description"/></td>
								<td><panel:installedfeature property="version"/></td>
							</panel:installedfeatures>
							</table>							
							<div class="rqsep"></div>
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
