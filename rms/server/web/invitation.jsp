 <%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" isELIgnored="false" session="false"%>

<!DOCTYPE html>
<html lang="en">
  <head>
    <meta charset="utf-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1" /> 
    <meta http-equiv="X-UA-Compatible" content="IE=edge" />
    <title>SkyDRM - Project Invitation</title>
    <link rel="icon" href="${pageContext.request.contextPath }/ui/img/favicon.ico" type="image/x-icon"/>
    <link rel="stylesheet" href="${pageContext.request.contextPath }/ui/css/login.min.css?v=${applicationScope['version']}">
    <link rel="stylesheet" href="${pageContext.request.contextPath }/ui/css/font/fira.css?v=${applicationScope['version']}">
    <script src="${pageContext.request.contextPath}/ui/lib/jquery/jquery-1.10.2.min.js"></script>
    <script src="${pageContext.request.contextPath}/ui/app/login.min.js?v=${applicationScope['version']}"></script>
    <script src="${pageContext.request.contextPath}/ui/lib/bootstrap/3.3.5/js/bootstrap.min.js"></script>
    
    <%
        String id = request.getParameter("id");
        String code = request.getParameter("code");
        java.util.Map<String,String> details = com.nextlabs.rms.service.ProjectService.getProjectInvitationDetails(id, code);

        if (details == null) {
            return;
        }

        String projectName = details.get(com.nextlabs.rms.service.ProjectService.PROJECT_NAME);
        String description = details.get(com.nextlabs.rms.service.ProjectService.PROJECT_DESCRIPTION);
        String inviter = details.get(com.nextlabs.rms.service.ProjectService.INVITER);
        String inviterDisplay = details.get(com.nextlabs.rms.service.ProjectService.INVITER_DISPLAY);
        String invitee = details.get(com.nextlabs.rms.service.ProjectService.INVITEE);
    %>

    
    <script type="text/javascript">

    var id = '<%=request.getParameter("id")%>';
    var code = '<%=request.getParameter("code")%>';

    function storeInvitationParams(){
        var date = new Date();
        date.setTime(date.getTime() + (30 * 60 * 1000)); // 30 minutes
        if(id && code){
            setCookie("id", id, date);
            setCookie("code", code, date);
        }
    }

    function signup() {
        storeInvitationParams();
        window.location.href = "${pageContext.request.contextPath}/register";
    }

    function signin() {
        storeInvitationParams();
        window.location.href = "${pageContext.request.contextPath}/login";
    }

    function openModalPage() {
        var declineReason = $('textarea#declineReason');
        declineReason.val("");
        var declineModal = $('div#declineModal');
        declineModal[0].style.visibility="visible";
    }

    function closeModalPage() {
        var declineModal = $('div#declineModal');
        declineModal[0].style.visibility="hidden";
    }

    window.onclick = function(event) {
        var declineModal = $('div#declineModal');
        if (event.target == declineModal[0]) {
            declineModal[0].style.visibility = "hidden";
        }
    }

    function decline() {
        var declineReason = $('textarea#declineReason').val();
        $.ajax({
            url: '${pageContext.request.contextPath}/rs/project/decline',
            type: 'POST',
            data: {"id": id, "code": code, "declineReason": declineReason},
            async : true,
            success: function (result) {
               var jsonRet = result;
               statusCode = jsonRet.statusCode;
               message = jsonRet.message;
                if (statusCode == 200) {
                    $('div#joinProjectDiv').html('<b style="color:lightseagreen; font-size:x-large">You have successfully declined the invitation</b>');
                    $('div#diclineDiv')[0].style.display = 'none';
                } else {
                    $('div#joinProjectDiv').html('<b style="color:red; font-size:x-large">' + message + '</b>');
                    $('div#diclineDiv')[0].style.display = 'none';
                }
            },
            error: function (error) {
                $('div#joinProjectDiv').html('<b style="color:red">Some error occured during declining the invitation</b>');
            }
        });
        closeModalPage();
    }

    </script>

 </head>
 <body>
<div id="personal_summary_row" class="row">
    <div class="project-card rms-tile-shadow" id="summary" style="margin: 0 auto;">
        <div id="main-container">
            <div class="row display-block">
                <div class="col-xs-10">
                    <br>
                    <h3>Welcome to SkyDRM!</h3>
                    <h5><span id="welcome">A faster way to collaborate with your colleagues securely!</span></h5>
                </div>
            </div>
            
            <div class="row display-block">
                <div class="col-xs-12">
                    <br>
                    <label class="ng-binding"><%=inviterDisplay%> (<a class="mail" href="mailto:<%=inviter%>"><%=inviter%></a>) invited you to join the</label>
                    <label class="ng-binding">project <b class="project-name"><%=projectName%></b></label>
                    <br><br><br>
                </div>
                <div id="declineModal" class="row modal-dialog invitation-modal hide-first">
                    <div class="col-xs-12 col-sm-10 col-md-8 col-lg-6 invitation-modal-content">
                        <div class="col-xs-12 padding-10">
                          Are you sure you want to decline the invitation?
                        </div>
                        <div style="border-top: none;" class="col-xs-12 padding-10">
                            <div class = "col-xs-12 no-padding">
                                <textarea id="declineReason" name="declineReason" style="width:100%;" ng-attr-placeholder="Provide a reason for declining (Optional, up to 250 characters)" maxlength="250"></textarea>
                            </div>
                            <div class = "col-xs-12 text-align-right no-padding margin-top-10">
                                <a class="btn btn-default rms-settings-button" onclick="closeModalPage()">Cancel</a>
                                <a class="btn btn-default rms-settings-button rms-settings-button-color" onclick="decline()">Confirm</a>
                            </div>
                        </div>
                    </div>
                </div>       
            </div>
            <div id="joinProjectDiv" class="row col-xs-12 display-block" style="height: 150px; padding: 0; text-align: center">
                <div class="col-xs-6 text-align-center padding-5" style="height:100%">
                    <div style="height:75%;">
                        <label class="file-info-label padding-5"><b>Do not have an account yet?</b></label>
                        <br>
                        <label class="file-info-label padding-5">Sign up using <a href="mailto:<%=inviter%>" class="mail"><%=invitee%></a> to join the project</label>
                    </div>
                    <div class="form-group">
                        <a class="btn btn-default rms-settings-button-color" onclick="signup()">Sign Up And Join</a>
                    </div>
                </div>
                <div class="col-xs-6 text-align-center verticalLine padding-5" style="height:100%">
                    <div style="height:75%;">
                        <label class="file-info-label padding-5"><b>Already have an account with <a href="mailto:<%=inviter%>" class="mail"><%=invitee%></a> </b>?</label>
                        <br>
                        <label class="file-info-label padding-5">Log in to join the project</label>
                    </div>
                    <div class="form-group">
                        <a class="btn btn-default rms-settings-button-color" onclick="signin()">Log In To Join</a>
                    </div>  
                </div>
            </div>

            <div id="instructionDiv" class="row display-block col-xs-12 text-align-center padding-10">
                <br><br>
                <div id="diclineDiv" class="row display-block text-align-center padding-10">
                    <span id="welcome">Not interested to join the project?</span>
                    <br><br>
                    <a class="btn btn-default" style="margin: 5px 5px;" onclick="openModalPage()">Decline</a>
                </div>

            </div>
        </div>
    </div>
</div>

</body>
</html>