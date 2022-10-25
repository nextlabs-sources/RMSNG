/*
 * All sources, binaries and HTML pages (C) copyright 2004-2011 by NextLabs, Inc.,
 * San Mateo CA, Ownership remains with NextLabs, Inc., All rights reserved
 * worldwide.
 */

package com.bluejungle.pf.domain.destiny.serviceprovider;

import com.bluejungle.framework.expressions.IEvalValue;

public interface IFunctionServiceProvider extends IServiceProvider {

    IEvalValue callFunction(String functionName, IEvalValue args[]) throws ServiceProviderException;
}
