/*
 * Created on Jan 22, 2013
 *
 * All sources, binaries and HTML pages (C) copyright 2012 by NextLabs Inc.,
 * San Mateo CA, Ownership remains with NextLabs Inc, All rights reserved
 * worldwide.
 *
 * @author amorgan
 * @version $Id: //depot/Destiny/D_Nimbus/pcv/Nimbus_Main/main/src/common/pf/src/java/main/com/bluejungle/pf/domain/destiny/serviceprovider/ISubjectAttributeProvider.java#1 $:
 */

package com.bluejungle.pf.domain.destiny.serviceprovider;

import com.bluejungle.framework.expressions.IEvalValue;
import com.bluejungle.pf.domain.destiny.subject.IDSubject;

public interface ISubjectAttributeProvider extends IServiceProvider {

    IEvalValue getAttribute(IDSubject subj, String attribute) throws ServiceProviderException;
}
