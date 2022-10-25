<%@page import="com.nextlabs.rms.shared.LocalizationUtil"%>
<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1" isELIgnored="false" session="false" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<link rel="shortcut icon" href="${pageContext.request.contextPath }/ui/img/favicon.ico" />
<link href="${pageContext.request.contextPath }/ui/css/style.css" rel="stylesheet">
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<title><%= LocalizationUtil.getMessage(request, "app.title", null, null) %></title>
</head>
<body>
<center class="error-pages"><%= LocalizationUtil.getMessage(request, "err.http.403", null, null) %></center>
</body>
</html>