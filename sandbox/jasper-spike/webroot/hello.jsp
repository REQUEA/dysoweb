<%@ page session="false" contentType="text/plain" %><%
   // Taglib-free JSP (TLDs disabled, per Phase-0). Exercises scriptlet + expression
   // + the jsp:runtime path so we prove compile AND execute, not just parse.
   String who = "dysoweb";
%>SPIKE_OK who=<%= who %> result=<%= (2 + 40) %> loop=<%
   int sum = 0;
   for (int i = 1; i <= 4; i++) { sum += i; }
%><%= sum %>
