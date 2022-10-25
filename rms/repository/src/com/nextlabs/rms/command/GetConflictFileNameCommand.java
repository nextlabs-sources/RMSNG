package com.nextlabs.rms.command;

import com.nextlabs.common.shared.JsonResponse;
import com.nextlabs.common.util.StringUtils;
import com.nextlabs.rms.shared.JsonUtil;
import com.nextlabs.rms.util.RepositoryFileUtil;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class GetConflictFileNameCommand extends AbstractCommand {

    @Override
    public void doAction(HttpServletRequest request, HttpServletResponse response) {
        String fileName = request.getParameter("fileName");
        if (!StringUtils.hasText(fileName)) {
            JsonUtil.writeJsonToResponse(new JsonResponse(400, "Missing required parameters.").toJson(), response);
            return;
        }
        String conflictFileName = RepositoryFileUtil.getConflictFileName(fileName);
        JsonResponse resp = new JsonResponse("OK");
        resp.putResult("conflictFileName", conflictFileName);
        JsonUtil.writeJsonToResponse(resp, response);
    }
}
