<%@page import="org.apache.logging.log4j.LogManager"%>
<%@page import="org.apache.logging.log4j.Logger"%>
<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8" isErrorPage="true" session="false" isELIgnored="false"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<%! 
	private static final Logger LOGGER = LogManager.getLogger("com.nextlabs.rms.HTTPStatus500");
%>
<%
    if (exception != null && !com.nextlabs.rms.util.Errors.isSocketException(exception)) {
    	LOGGER.error(exception.getMessage(), exception);
    }
%>
<link rel="shortcut icon" href="${pageContext.request.contextPath}/ui/img/favicon.ico" />
<link href="${pageContext.request.contextPath}/ui/css/style.css" rel="stylesheet">
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title><%= com.nextlabs.rms.locale.RMSMessageHandler.getClientString("500_error_title") %></title>
</head>
<body>
<center class="error-pages"><%= com.nextlabs.rms.locale.RMSMessageHandler.getClientString("500_error_msg") %></center>
</body>
</html>