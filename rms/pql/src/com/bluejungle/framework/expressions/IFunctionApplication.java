/*
 * Created on Feb 11, 2011
 *
 * All sources, binaries and HTML pages (C) copyright 2011 by NextLabs Inc.,
 * San Mateo CA, Ownership remains with NextLabs Inc, All rights reserved
 * worldwide.
 *
 * @author amorgan
 * @version $Id: //depot/Destiny/D_Nimbus/pcv/Nimbus_Main/main/src/common/framework/src/java/main/com/bluejungle/framework/expressions/IFunctionApplication.java#1 $:
 */
package com.bluejungle.framework.expressions;

import java.util.List;

public interface IFunctionApplication extends IExpression {

    /**
     * @return the name of the service providing the function
     */
    String getServiceName();

    /**
     * @return the name of the function
     */
    String getFunctionName();

    /**
     * @return the arguments
     */
    List<IExpression> getArguments();
}
