<%@page import="org.apache.commons.lang3.StringEscapeUtils"%>
<%@page import="com.nextlabs.rms.shared.LocalizationUtil"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" isELIgnored="false" session="false"%>
<!DOCTYPE html>
<html lang="en">
  <head>
    <meta charset="utf-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta name="description" content="">
    <meta name="author" content="">
    <link rel="shortcut icon" href="${pageContext.request.contextPath }/ui/img/favicon.ico" />

    <title><%= LocalizationUtil.getMessage(request, "app.title", null, null) %></title>

    <!-- Bootstrap core CSS -->
    <link href="${pageContext.request.contextPath }/ui/lib/bootstrap/3.3.5/css/bootstrap.min.css" rel="stylesheet">
	<link href="${pageContext.request.contextPath }/ui/css/seccoll.css" rel="stylesheet">
	<link href="${pageContext.request.contextPath }/ui/css/font/fira.css" rel="stylesheet" />
  </head>
  <body>
    <br>
    <div class="container-error">
		<div id="display-error" style="text-align: center; vertical-align: middle;margin-bottom:1px;margin-left:auto;margin-right:auto; visibility:hidden;" >   
			<img src="${pageContext.request.contextPath }/ui/img/Alert_P.svg" alt="Error" />    
		</div>
	</div>
	<div id="footer">
		<div class="container">
			<p class="text-muted credit text-center">Copyright © 2021 <a href="http://www.nextlabs.com">NextLabs, Inc.</a> All rights reserved.</p>
		</div>
	</div>
	<script src="${pageContext.request.contextPath }/ui/lib/3rdParty/jquery-1.10.2.min.js"></script>
	<script src="${pageContext.request.contextPath }/ui/lib/jquery-ui/1.11.4/jquery-ui.min.js"></script>
	<script src="${pageContext.request.contextPath }/ui/lib/3rdParty/jquery.blockUI.js"></script>
	<script src="${pageContext.request.contextPath }/ui/lib/3rdParty/bootstrap.min.js"></script>
	<script>var version="${applicationScope['version']}"</script>
	<script src="${pageContext.request.contextPath }/ui/app/viewers/Viewer.js?v=${applicationScope['version']}"></script>
	<script type="text/javascript">
        function checkErrorMsg(){
            $('div.container-error').block({message: '',overlayCSS: { backgroundColor: '#fff' }});
            <%
                String errId = request.getParameter("d");
                String errMsg = request.getParameter("errMsg");
                String code = request.getParameter("code");
                if (errId != null) {
                    %>
		            var sessionId=getSessionId();
                    $.get('${pageContext.request.contextPath }/RMSViewer/GetErrorMsg?errorId=${param.d}&s=' + sessionId, function(data, status){
                        var errMsg=data.message;
                        if (errMsg != null) {
		                    $('#display-error').text(errMsg).css("visibility", "visible");
                            $('div.container-error').unblock();
                        }
                    });  
                    <%
                } else if (code != null || errMsg != null) {
                    String msg = code != null ? LocalizationUtil.getMessage(getServletContext(), code, null, null) : errMsg;
                    pageContext.setAttribute("errorMsg", StringEscapeUtils.ESCAPE_ECMASCRIPT.translate(msg));
                    %>
                    $('#display-error').text('${errorMsg}').css("visibility", "visible");
                    <% if (code != null && "err.unsupported".equals(code)) { %>
                    $('#display-error').append( "<div> <a onclick=\"showHelp('${pageContext.request.contextPath}/help_users/index.html')\" class=\"underline color-light-blue pointer-click\">Click here to view supported file formats.</a></div>");
                    <% } %>
                    <% if (code != null && "err.sap.ve.missing".equals(code)) { %>
                    $('#display-error').append('<div> <a href="https://store.sap.com/sap/cpa/ui/resources/store/html/SolutionDetails.html?pid=0000000233&catID=&pcntry=US&sap-language=EN&_cp_id=id-1431928966953-0" target="_blank">Click here to visit the SAP Store to download the installer.</a></div>');
                    <% } %>
		            $('div.container-error').unblock();
		            <%
		        }
            %>
        }
        window.onload = checkErrorMsg;
    </script>
  </body>
</html>
