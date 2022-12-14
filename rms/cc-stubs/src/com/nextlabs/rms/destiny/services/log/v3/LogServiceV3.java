package com.nextlabs.rms.destiny.services.log.v3;

import com.nextlabs.rms.destiny.interfaces.log.v3.LogServiceIF;

import java.net.MalformedURLException;
import java.net.URL;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import javax.xml.ws.WebEndpoint;
import javax.xml.ws.WebServiceClient;
import javax.xml.ws.WebServiceFeature;

/**
 * This class was generated by Apache CXF 3.0.4
 * 2015-04-20T18:06:18.169+08:00
 * Generated source version: 3.0.4
 *
 */
@WebServiceClient(name = "LogService.v3", wsdlLocation = "file:/C:/Users/tbiegeleisen/to-Tim/JAX-WS test/src/main/config/LogService.v3.wsdl", targetNamespace = "http://nextlabs.com/destiny/services/log/v3")
public class LogServiceV3 extends Service {

    public static final URL WSDL_LOCATION;
    public static final QName SERVICE = new QName("http://nextlabs.com/destiny/services/log/v3", "LogService.v3");
    public static final QName LOG_SERVICE_IF_PORT_V3 = new QName("http://nextlabs.com/destiny/services/log/v3", "LogServiceIFPort.v3");
    static {
        URL url = null;
        try {
            url = new URL("file:/C:/Users/tbiegeleisen/to-Tim/JAX-WS test/src/main/config/LogService.v3.wsdl");
        } catch (MalformedURLException e) {
            java.util.logging.Logger.getLogger(LogServiceV3.class.getName()).log(java.util.logging.Level.INFO, "Can not initialize the default wsdl from {0}", "file:/C:/Users/tbiegeleisen/to-Tim/JAX-WS test/src/main/config/LogService.v3.wsdl");
        }
        WSDL_LOCATION = url;
    }

    public LogServiceV3(URL wsdlLocation) {
        super(wsdlLocation, SERVICE);
    }

    public LogServiceV3(URL wsdlLocation, QName serviceName) {
        super(wsdlLocation, serviceName);
    }

    public LogServiceV3() {
        super(WSDL_LOCATION, SERVICE);
    }

    //This constructor requires JAX-WS API 2.2. You will need to endorse the 2.2
    //API jar or re-run wsdl2java with "-frontend jaxws21" to generate JAX-WS 2.1
    //compliant code instead.
    //public LogServiceV3(WebServiceFeature ... features) {
    //    super(WSDL_LOCATION, SERVICE, features);
    //}

    //This constructor requires JAX-WS API 2.2. You will need to endorse the 2.2
    //API jar or re-run wsdl2java with "-frontend jaxws21" to generate JAX-WS 2.1
    //compliant code instead.
    //public LogServiceV3(URL wsdlLocation, WebServiceFeature ... features) {
    //    super(wsdlLocation, SERVICE, features);
    //}

    //This constructor requires JAX-WS API 2.2. You will need to endorse the 2.2
    //API jar or re-run wsdl2java with "-frontend jaxws21" to generate JAX-WS 2.1
    //compliant code instead.
    //public LogServiceV3(URL wsdlLocation, QName serviceName, WebServiceFeature ... features) {
    //    super(wsdlLocation, serviceName, features);
    //}

    /**
     *
     * @return
     *     returns LogServiceIF
     */
    @WebEndpoint(name = "LogServiceIFPort.v3")
    public LogServiceIF getLogServiceIFPortV3() {
        return super.getPort(LOG_SERVICE_IF_PORT_V3, LogServiceIF.class);
    }

    /**
     *
     * @param features
     *     A list of {@link javax.xml.ws.WebServiceFeature} to configure on the proxy.  Supported features not in the <code>features</code> parameter will have their default values.
     * @return
     *     returns LogServiceIF
     */
    @WebEndpoint(name = "LogServiceIFPort.v3")
    public LogServiceIF getLogServiceIFPortV3(WebServiceFeature... features) {
        return super.getPort(LOG_SERVICE_IF_PORT_V3, LogServiceIF.class, features);
    }

}
