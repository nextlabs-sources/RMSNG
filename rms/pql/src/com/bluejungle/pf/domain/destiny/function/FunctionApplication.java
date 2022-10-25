/*
 * Created on Feb 11, 2011
 *
 * All sources, binaries and HTML pages (C) copyright 2011 by NextLabs Inc.,
 * San Mateo CA, Ownership remains with NextLabs Inc, All rights reserved
 * worldwide.
 *
 * @author amorgan
 * @version $Id: //depot/Destiny/D_Nimbus/pcv/Nimbus_Main/main/src/common/pf/src/java/main/com/bluejungle/pf/domain/destiny/function/FunctionApplication.java#1 $:
 */
package com.bluejungle.pf.domain.destiny.function;

import com.bluejungle.framework.expressions.IArguments;
import com.bluejungle.framework.expressions.IEvalValue;
import com.bluejungle.framework.expressions.IExpression;
import com.bluejungle.framework.expressions.IExpressionVisitor;
import com.bluejungle.framework.expressions.IFunctionApplication;
import com.bluejungle.framework.expressions.IRelation;
import com.bluejungle.framework.expressions.Relation;
import com.bluejungle.framework.expressions.RelationOp;
import com.bluejungle.pf.domain.destiny.serviceprovider.IFunctionServiceProvider;
import com.bluejungle.pf.domain.destiny.serviceprovider.IServiceProvider;
import com.bluejungle.pf.domain.destiny.serviceprovider.IServiceProviderManager;
import com.bluejungle.pf.domain.destiny.serviceprovider.ServiceProviderException;
import com.bluejungle.pf.engine.destiny.EvaluationRequest;

import java.util.ArrayList;
import java.util.List;

public class FunctionApplication implements IFunctionApplication {

    private final String serviceName;

    private final String functionName;

    private final List<IExpression> funargs;

    public FunctionApplication(String serviceName, String functionName, List funargs) {
        this.serviceName = serviceName;
        this.functionName = functionName;

        this.funargs = new ArrayList<IExpression>(funargs.size());
        for (Object o : funargs) {
            this.funargs.add((IExpression)o);
        }
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getFunctionName() {
        return functionName;
    }

    public List<IExpression> getArguments() {
        return funargs;
    }

    private IEvalValue[] evaluateFunargs(IArguments args) {
        List<IEvalValue> evaluatedFunargsList = new ArrayList<IEvalValue>(funargs.size());

        for (IExpression funarg : funargs) {
            evaluatedFunargsList.add(funarg.evaluate(args));
        }

        return evaluatedFunargsList.toArray(new IEvalValue[funargs.size()]);
    }

    public IEvalValue evaluate(IArguments args) {
        IEvalValue res = IEvalValue.NULL;
        if (args instanceof EvaluationRequest) {
            EvaluationRequest req = (EvaluationRequest)args;
            IServiceProviderManager serviceProviderManager = req.getServiceProviderManager();
            IServiceProvider serviceProvider = serviceProviderManager.getServiceProvider(getServiceName());

            if (serviceProvider != null && serviceProvider instanceof IFunctionServiceProvider) {
                IFunctionServiceProvider funcService = (IFunctionServiceProvider)serviceProvider;
                try {
                    res = funcService.callFunction(getFunctionName(), evaluateFunargs(args));
                } catch (ServiceProviderException spe) {
                    //??
                }

            }
        }
        return res;
    }

    public IRelation buildRelation(RelationOp op, IExpression expr) {
        return new Relation(op, this, expr);
    }

    /**
     * @see com.bluejungle.framework.expressions.IExpression#acceptVisitor(com.bluejungle.framework.expressions.IExpressionVisitor)
     */
    public void acceptVisitor(IExpressionVisitor visitor, IExpressionVisitor.Order order) {
        visitor.visit((IFunctionApplication)this);
    }

    public String toString() {
        StringBuilder sb = new StringBuilder(" call_function(\"");
        sb.append(getServiceName());
        sb.append("\", \"");
        sb.append(getFunctionName());
        sb.append("\"");

        for (IExpression singleArg : getArguments()) {
            sb.append(", ");
            sb.append(singleArg.toString());
        }
        sb.append(")");

        return sb.toString();
    }
}
