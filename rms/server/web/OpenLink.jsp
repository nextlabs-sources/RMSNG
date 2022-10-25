 <%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" session="false"%>
<!DOCTYPE html>
<html>
<head>

<meta charset="utf-8">
<title>SkyDRM</title>
  <link rel="shortcut icon" href="ui/img/favicon.ico" />
  <link rel="stylesheet" href="ui/css/login.min.css">
  <link rel="stylesheet" href="/ui/css/font/fira.css">

    <style type="text/css">
        .wellnew {
        height:60%;
        width:40%;
        margin-right: auto;
        margin-left: auto;
        border-width:5px;
        border-color:#565051;
        min-width:436px;
        border-style:solid;"
        }
    </style>

  <!--<script src="js/jquery-1.10.2.min.js"></script>-->

  <script language="javascript" type="text/javascript">
/*    $(window).load(function() {
        openSecurePopup('<%=request.getParameter("redirectURL")%>');
        window.close();
      });*/

     function openSecurePopup(){
            var win;
            var settings = "scrollbars=yes, location=no, directories=no, status=no, menubar=no, toolbar=no, resizable=yes, dependent=no";
            var url=window.location.search.substring(1);
            url=(url.substring(url.indexOf('=')+1))
            var parameters=(url.substring(url.indexOf('?')+1));
            var filePath=getQueryVariable(parameters,"filePath");
            var fileIsRH;
            if(!(filePath == undefined)){
                fileIsRH=endsWith(filePath,'rh');
                if(!fileIsRH){
                    url='<%=java.net.URLDecoder.decode(request.getParameter("redirectURL"),"UTF-8")%>';
                }
            }else{
                url='<%=java.net.URLDecoder.decode(request.getParameter("redirectURL"),"UTF-8")%>';
            }
            win = window.open(url, "NextLabsRMS", settings);
            if(win==undefined){
                document.getElementById("tempWindow").innerHTML="Your web browser's pop up blocker might be preventing SkyDRM from displaying this file. In order to view this file, you might want to disable your pop up blocker for SkyDRM."
            }
            else{
                document.getElementById("tempWindow").innerHTML="The Rights Protected Document has been opened in a new window. You may close this window now."
                win.focus();
            }  
      }
      function getQueryVariable(parameters,variable) {
           var query=decodeURIComponent(parameters);
            var vars = query.split("&");
            for (var i=0;i<vars.length;i++) {
                var pair = vars[i].split("=");
                if (pair[0] == variable) {
                  return pair[1];
                }
            } 
        }
     
     function endsWith(str, suffix) {
        return str.indexOf(suffix, str.length - suffix.length) !== -1;
    }
  </script>
</head>
<body onload="openSecurePopup()">
  <br>
  <br>
  <div id="tempWindow"class="well wellnew" style="margin:auto vertical-align: middle;">
      The Rights Protected Document has been opened in a new window. You may close this window now.
  </div>

</body>
</html>