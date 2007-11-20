<%@ page session="false" %>
<%@ taglib uri="http://taglibs.requea.com/dw/2007/" prefix="dw"%>
<%@ taglib uri="http://taglibs.requea.com/panel/2007/" prefix="panel" %>
<div class="mbar">
	<div class="mb">
		<ul class="mbp">
			<li><dw:menu style="mbpmen" currentStyle="mbpcmen" path="/dysoweb/panel/panel.jsp" label="Home" /></li>
			<dw:insert bundle="com.requea.dysoweb.demo1"><li><dw:menu style="mbpmen" currentStyle="mbpcmen" path="/dysoweb/demo/demo.jsp" label="Demo" /></li></dw:insert>
			<li><dw:menu style="mbpmen" currentStyle="mbpcmen" path="/dysoweb/panel/apps.jsp" label="Applications" /></li>
			<li><dw:menu style="mbpmen" currentStyle="mbpcmen" path="/dysoweb/panel/install" label="Install" /></li>
			<li><dw:menu style="mbpmen" currentStyle="mbpcmen" path="/dysoweb/panel/bundles.jsp" label="Bundles" /></li>
			<dw:insert bundle="com.requea.dysoweb.shell"><li><dw:menu style="mbpmen" currentStyle="mbpcmen" path="/dysoweb/panel/shell.jsp" label="Shell" /></li></dw:insert>
			<li><dw:menu style="mbpmen" currentStyle="mbpcmen" path="/dysoweb/panel/contact.jsp" label="Contact" /></li>
		</ul>
	</div>
</div>