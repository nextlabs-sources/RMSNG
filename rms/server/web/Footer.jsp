<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" session="false"%>
<%@page import="com.nextlabs.common.shared.WebConfig" %>
<%  String webContext=request.getContextPath();
    WebConfig webConfig = WebConfig.getInstance();
    String termsUrl = webConfig.getProperty(WebConfig.TERMS_URL, webContext + "/TermsAndConditions.html");
    String privacyUrl = webConfig.getProperty(WebConfig.PRIVACY_URL, webContext + "/PrivacyPolicy.html");
%>
<div id="footer" class="login-footer">
     <center id="footer-links">
        <span tabindex="90"><a href="<%=termsUrl%>" target="_blank">Terms And Conditions</a></span>
        <span tabindex="100"><a href="<%=privacyUrl%>"  target="_blank" id="privacy-policy-anchor">Privacy Policy</a></span>
        <span tabindex="110"><a href="mailto:support@skydrm.com">Contact Us</a></span>
     </center>   
</div>