<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1" session="false" isELIgnored="false"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<link rel="shortcut icon" href="${pageContext.request.contextPath}/ui/img/favicon.ico" />
<link href="${pageContext.request.contextPath}/ui/css/style.css" rel="stylesheet">
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<title><%= com.nextlabs.rms.locale.RMSMessageHandler.getClientString("404_error_title") %></title>
</head>
<body>
<center class="error-pages"><%= com.nextlabs.rms.locale.RMSMessageHandler.getClientString("404_error_msg") %></center>
</body>
</html>