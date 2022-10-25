package com.nextlabs.rms.util;

import com.nextlabs.nxl.Rights;
import com.nextlabs.rms.eval.EvalResponse;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class PolicyEvalUtil {

    private PolicyEvalUtil() {
    }

    public static EvalResponse getFirstAllowResponse(List<EvalResponse> responseList) {
        for (EvalResponse response : responseList) {
            if (response.getRights().length > 0) {
                return response;
            }
        }
        return new EvalResponse();
    }

    public static Rights[] getUnionRights(List<EvalResponse> responseList) {
        Set<Rights> rights = new HashSet<>();
        for (EvalResponse response : responseList) {
            Arrays.stream(response.getRights()).forEach(res -> rights.add(res));
        }
        return rights.toArray(new Rights[rights.size()]);
    }
}
