package com.nextlabs.rms.destiny.services.log.v5;

import com.nextlabs.rms.destiny.interfaces.log.v5.LogServiceIF;

import java.net.MalformedURLException;
import java.net.URL;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import javax.xml.ws.WebEndpoint;
import javax.xml.ws.WebServiceClient;
import javax.xml.ws.WebServiceFeature;

/**
 * This class was generated by Apache CXF 3.0.4
 * 2015-07-13T15:37:52.212+08:00
 * Generated source version: 3.0.4
 *
 */
@WebServiceClient(name = "LogService.v5", wsdlLocation = "file:/C:/Users/tbiegeleisen/to-Tim/JAX-WS test/src/main/config/LogService.v5.wsdl", targetNamespace = "http://nextlabs.com/destiny/services/log/v5")
public class LogServiceV5 extends Service {

    public static final URL WSDL_LOCATION;
    public static final QName SERVICE = new QName("http://nextlabs.com/destiny/services/log/v5", "LogService.v5");
    public static final QName LOG_SERVICE_IF_PORT_V5 = new QName("http://nextlabs.com/destiny/services/log/v5", "LogServiceIFPort.v5");
    static {
        URL url = null;
        try {
            url = new URL("file:/C:/Users/tbiegeleisen/to-Tim/JAX-WS test/src/main/config/LogService.v5.wsdl");
        } catch (MalformedURLException e) {
            java.util.logging.Logger.getLogger(LogServiceV5.class.getName()).log(java.util.logging.Level.INFO, "Can not initialize the default wsdl from {0}", "file:/C:/Users/tbiegeleisen/to-Tim/JAX-WS test/src/main/config/LogService.v5.wsdl");
        }
        WSDL_LOCATION = url;
    }

    public LogServiceV5(URL wsdlLocation) {
        super(wsdlLocation, SERVICE);
    }

    public LogServiceV5(URL wsdlLocation, QName serviceName) {
        super(wsdlLocation, serviceName);
    }

    public LogServiceV5() {
        super(WSDL_LOCATION, SERVICE);
    }

    //This constructor requires JAX-WS API 2.2. You will need to endorse the 2.2
    //API jar or re-run wsdl2java with "-frontend jaxws21" to generate JAX-WS 2.1
    //compliant code instead.
    public LogServiceV5(WebServiceFeature... features) {
        super(WSDL_LOCATION, SERVICE, features);
    }

    //This constructor requires JAX-WS API 2.2. You will need to endorse the 2.2
    //API jar or re-run wsdl2java with "-frontend jaxws21" to generate JAX-WS 2.1
    //compliant code instead.
    public LogServiceV5(URL wsdlLocation, WebServiceFeature... features) {
        super(wsdlLocation, SERVICE, features);
    }

    //This constructor requires JAX-WS API 2.2. You will need to endorse the 2.2
    //API jar or re-run wsdl2java with "-frontend jaxws21" to generate JAX-WS 2.1
    //compliant code instead.
    public LogServiceV5(URL wsdlLocation, QName serviceName, WebServiceFeature... features) {
        super(wsdlLocation, serviceName, features);
    }

    /**
     *
     * @return
     *     returns LogServiceIF
     */
    @WebEndpoint(name = "LogServiceIFPort.v5")
    public LogServiceIF getLogServiceIFPortV5() {
        return super.getPort(LOG_SERVICE_IF_PORT_V5, LogServiceIF.class);
    }

    /**
     *
     * @param features
     *     A list of {@link javax.xml.ws.WebServiceFeature} to configure on the proxy.  Supported features not in the <code>features</code> parameter will have their default values.
     * @return
     *     returns LogServiceIF
     */
    @WebEndpoint(name = "LogServiceIFPort.v5")
    public LogServiceIF getLogServiceIFPortV5(WebServiceFeature... features) {
        return super.getPort(LOG_SERVICE_IF_PORT_V5, LogServiceIF.class, features);
    }
}
