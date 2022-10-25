package com.nextlabs.rms.eval;

import java.util.List;

public interface IEvalAdapter {

    public EvalResponse evaluate(EvalRequest req);

    public List<EvalResponse> evaluate(List<EvalRequest> req);

    public void initializeSDK();

}
