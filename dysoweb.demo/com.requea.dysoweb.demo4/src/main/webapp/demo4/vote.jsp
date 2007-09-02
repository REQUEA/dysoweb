<%@ taglib uri="http://taglibs.requea.com/dw/2007/" prefix="dw"%>
<form action="<%=request.getContextPath()%>/demo4/vote">
	<p>What is the most important Dysoweb feature for you?</p>
	<input name="op" type="hidden" value="vote" />
	<input name="response" value="modular" type="radio">Modular</input>: Ability to package Web applications as bundles with their own set of Servlets, Filters, JSP pages, Taglibs and Static files<br/>
	<input name="response" value="dynamic" type="radio">Dynamic</input>: Ability to start / stop / update services at any time<br/>
	<input name="response" value="standard" type="radio">Standard</input>: Runs as a standard Web application (WAR) in a servlet container (such as Tomcat)<br/>
	Vote to see the result :&nbsp;<input type="submit" value="Vote" />
</form>
