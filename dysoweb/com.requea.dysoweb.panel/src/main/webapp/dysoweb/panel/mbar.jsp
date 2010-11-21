<%@ page session="false" %>
<%@ taglib uri="http://taglibs.requea.com/dw/2007/" prefix="dw"%>
<%@ taglib uri="http://taglibs.requea.com/panel/2007/" prefix="panel" %>
<div class="mbar">
	<div class="mb">
		<ul class="mbp">
            <li><dw:menu style="mbpmen" currentStyle="mbpcmen" path="/dysoweb/panel/secure/bundles.jsp" label="Bundles" /></li>
            <li><dw:menu style="mbpmen" currentStyle="mbpcmen" path="/dysoweb/panel/secure/install" label="Install" /></li>
			<dw:insert bundle="com.requea.dysoweb.shell"><li><dw:menu style="mbpmen" currentStyle="mbpcmen" path="/dysoweb/panel/secure/shell.jsp" label="Shell" /></li></dw:insert>
		</ul>
	</div>
</div>
