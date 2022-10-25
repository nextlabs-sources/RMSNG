<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" session="false" isELIgnored="false"%>
<!DOCTYPE html>
<html lang="en">
  <head>
    <meta charset="utf-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta name="description" content="">
    <meta name="author" content="">
    <title>SkyDRM</title>
    <script type="text/javascript">
    window.location.href=getParameterByName('url');
	
	function getParameterByName(name, url) {
		if (!url) url = window.location.href;
		name = name.replace(/[\[\]]/g, "\\$&");
		var regex = new RegExp("[?&]" + name + "(=([^&#]*)|&|#|$)"),
			results = regex.exec(url);
		if (!results) return null;
		if (!results[2]) return '';
		return decodeURIComponent(results[2].replace(/\+/g, " "));
	}
    </script>
  </head>
  <body>
  	<p>Please open the SkyDRM app on your device.</p>
  </body>
</html>
