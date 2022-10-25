package com.nextlabs.rms.eval;

import com.bluejungle.destiny.agent.pdpapi.IPDPApplication;
import com.bluejungle.destiny.agent.pdpapi.IPDPHost;
import com.bluejungle.destiny.agent.pdpapi.IPDPNamedAttributes;
import com.bluejungle.destiny.agent.pdpapi.IPDPResource;
import com.bluejungle.destiny.agent.pdpapi.IPDPUser;
import com.bluejungle.destiny.agent.pdpapi.PDPApplication;
import com.bluejungle.destiny.agent.pdpapi.PDPException;
import com.bluejungle.destiny.agent.pdpapi.PDPHost;
import com.bluejungle.destiny.agent.pdpapi.PDPNamedAttributes;
import com.bluejungle.destiny.agent.pdpapi.PDPResource;
import com.bluejungle.destiny.agent.pdpapi.PDPSDK;
import com.bluejungle.destiny.agent.pdpapi.PDPTimeout;
import com.bluejungle.destiny.agent.pdpapi.PDPUser;
import com.nextlabs.common.util.StringUtils;
import com.nextlabs.destiny.agent.controlmanager.IObligationResultData;
import com.nextlabs.destiny.agent.pdpapi.IPDPPermissionsCallback;
import com.nextlabs.destiny.agent.pdpapi.IPDPPermissionsResponse;
import com.nextlabs.nxl.Rights;
import com.nextlabs.rms.shared.LogConstants;
import com.nextlabs.rms.shared.Nvl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class EmbeddedPDPEvalAdapter implements IEvalAdapter {

    private static final Logger LOGGER = LogManager.getLogger(LogConstants.RMS_SERVER_LOG_NAME);

    @Override
    public EvalResponse evaluate(EvalRequest req) {
        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        EvalResponse response = null;
        try {
            long startTime = System.currentTimeMillis();
            IPDPUser user = getUser(req.getUser());
            IPDPHost host = getHost(req.getHost());
            Resource[] resources = req.getResources();
            IPDPResource srcRes = null;
            IPDPResource targetRes = null;
            if (resources.length == 1 && "from".equals(resources[0].getDimensionName())) {
                srcRes = getResourceWithAttribs(resources[0]);
            } else if (resources.length == 2) {
                if ("from".equals(resources[0].getDimensionName()) && "to".equals(resources[1].getDimensionName())) {
                    srcRes = getResourceWithAttribs(resources[0]);
                    targetRes = getResourceWithAttribs(resources[1]);
                } else if ("to".equals(resources[0].getDimensionName()) && "from".equals(resources[1].getDimensionName())) {
                    srcRes = getResourceWithAttribs(resources[1]);
                    targetRes = getResourceWithAttribs(resources[0]);
                    req.setResources(new Resource[] { resources[1], resources[0] });
                } else {
                    throw new IllegalArgumentException("Invalid Resource Definition");
                }
            } else {
                throw new IllegalArgumentException("Invalid Resource Definition");
            }

            int totalResource = 1 + (targetRes != null ? 1 : 0);
            IPDPResource[] resourceArr = new IPDPResource[totalResource];
            resourceArr[0] = srcRes;
            if (targetRes != null) {
                resourceArr[1] = targetRes;
            }
            for (IPDPResource resource : resourceArr) {
                resource.setAttribute("ce::nocache", "yes");
            }

            IPDPNamedAttributes[] environmentAttributes = getEnvironmentAttributes(req);
            IPDPApplication application = getApplication(req.getApplication());
            IPDPPermissionsResponse permissions = PDPSDK.PDPGetPermissions(resourceArr, user, application, host, environmentAttributes, req.getTimeoutInMins(), IPDPPermissionsCallback.NONE);
            response = populateResponses(permissions, req);
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Evaluation Completed");
            }
            long endTime = System.currentTimeMillis();
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Time taken for evaluating the request: " + (endTime - startTime) + " ms");
            }
        } catch (IllegalArgumentException e) {
            LOGGER.error("Illegal Argument Error occurred while evaluating request: " + e.getMessage(), e);
        } catch (PDPTimeout e) {
            LOGGER.error("Timeout Error occurred while evaluating request: " + e.getMessage(), e);
        } catch (PDPException | PolicyEvalException e) {
            LOGGER.error("Error occurred while evaluating request: " + e.getMessage(), e);
        } catch (Throwable e) {
            LOGGER.error("Error occurred while evaluating request: " + e.getMessage(), e);
        } finally {
            ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
            if (!originalClassLoader.equals(contextClassLoader)) {
                Thread.currentThread().setContextClassLoader(originalClassLoader);
            }
        }
        return Nvl.nvl(response, new EvalResponse());
    }

    @Override
    public List<EvalResponse> evaluate(List<EvalRequest> req) {
        List<EvalResponse> responses = new ArrayList<>();
        req.forEach(request -> responses.add(evaluate(request)));
        return responses;
    }

    private IPDPHost getHost(Host host) throws PolicyEvalException {
        if (host == null) {
            return new PDPHost(CentralPoliciesEvaluationHandler.LOCALHOST_IP);
        }
        Integer ipAddress = host.getIpAddress();
        String hostname = host.getHostname();
        IPDPHost pdpHost = StringUtils.hasText(hostname) && ipAddress == 0 ? new PDPHost(hostname) : new PDPHost(ipAddress);
        if (host.getAttributes() != null && !host.getAttributes().isEmpty()) {
            Map<String, List<String>> attributes = host.getAttributes();
            Set<Entry<String, List<String>>> entrySet = attributes.entrySet();
            for (Entry<String, List<String>> entry : entrySet) {
                List<String> values = entry.getValue();
                if (values != null && !values.isEmpty()) {
                    for (String value : values) {
                        pdpHost.setAttribute(entry.getKey(), value);
                    }
                }
            }
        }
        return pdpHost;
    }

    private IPDPApplication getApplication(Application application) {
        if (application == null) {
            return new PDPApplication("RMS");
        }
        Long pid = application.getPid();
        String name = application.getName();
        IPDPApplication app = pid != null ? new PDPApplication(name, pid) : new PDPApplication(name);
        if (application.getAttributes() != null && !application.getAttributes().isEmpty()) {
            Map<String, List<String>> attributes = application.getAttributes();
            Set<Entry<String, List<String>>> entrySet = attributes.entrySet();
            for (Entry<String, List<String>> entry : entrySet) {
                List<String> values = entry.getValue();
                if (values != null && !values.isEmpty()) {
                    for (String value : values) {
                        app.setAttribute(entry.getKey(), value);
                    }
                }
            }
        }
        if (StringUtils.hasText(application.getPath())) {
            // Currently CESDK consumes path as a custom attribute.
            app.setAttribute("path", application.getPath());
        }
        return app;
    }

    private String generateResponseLog(IPDPPermissionsResponse response, EvalRequest req) {
        StringBuilder log = new StringBuilder(100);
        User user = req.getUser();
        Resource srcResource = req.getResources()[0];
        log.append("Result of policy evaluation (User ID: ").append(user.getId()).append(", Resource name: ");
        log.append(srcResource.getResourceName()).append("): \n");
        for (String allowAction : response.getPermittedActionsForEffect("allow")) {
            log.append(String.format("\t%s : allow%n", allowAction));
        }
        for (String denyAction : response.getPermittedActionsForEffect("deny")) {
            log.append(String.format("\t%s : deny%n", denyAction));
        }
        return log.toString();
    }

    private EvalResponse populateResponses(IPDPPermissionsResponse permissionsResponse, EvalRequest req) {
        List<Obligation> obligationList = new ArrayList<>();
        if (LOGGER.isDebugEnabled()) {
            String responseLog = generateResponseLog(permissionsResponse, req);
            LOGGER.debug(responseLog);
        }
        Collection<String> allowRights = permissionsResponse.getPermittedActionsForEffect("allow");
        List<String> rights = new ArrayList<>();
        for (String right : allowRights) {
            if (right.startsWith(Rights.ACTION_PREFIX)) {
                rights.add(right);
                obligationList.addAll(populateObligations(permissionsResponse.getObligationsForAction("allow", right), right));
            }
        }
        EvalResponse response = new EvalResponse(Rights.fromStrings(rights));
        response.addObligations(obligationList);
        return response;
    }

    private List<Obligation> populateObligations(Collection<IObligationResultData> evalObligations, String right) {
        List<Obligation> obligations = new ArrayList<>();
        int i = 1;
        for (IObligationResultData evalObligation : evalObligations) {
            Obligation obligation = new Obligation();
            obligation.setId(i);
            obligation.setName(evalObligation.getObligationName());
            obligation.setPolicyName(evalObligation.getPolicyName());
            obligation.setRight(right);
            List<String> obligationArguments = evalObligation.getArguments();
            for (int j = 0; j < obligationArguments.size(); j += 2) {
                Attribute attribute = new Attribute();
                attribute.setName(obligationArguments.get(j));
                attribute.setValue(obligationArguments.get(j + 1));
                obligation.addAttribute(attribute);
            }
            i++;
            obligations.add(obligation);
        }
        return obligations;
    }

    private IPDPNamedAttributes[] getEnvironmentAttributes(EvalRequest req) {
        IPDPNamedAttributes[] environments = null;
        IPDPNamedAttributes[] results = null;
        NamedAttributes[] envAttributes = req.getEnvironments();
        String policy = req.getPolicy();
        boolean ignoreBuiltInPolicies = req.isIgnoreBuiltInPolicies();
        final boolean hasPolicy = StringUtils.hasText(policy);
        final boolean hasEnvAttributes = envAttributes != null && envAttributes.length > 0;
        int pdpEnvAttributesIndex = -1;
        boolean dontCareAttributePresent = false;
        if (hasEnvAttributes || hasPolicy) {
            int size = (hasEnvAttributes ? envAttributes.length : 0) + (hasPolicy ? 1 : 0);
            environments = new IPDPNamedAttributes[size];
            int idx = 0;
            if (hasEnvAttributes) {
                for (NamedAttributes attribute : envAttributes) {
                    String attributeName = attribute.getName();
                    environments[idx] = new PDPNamedAttributes(attributeName);
                    if (EvalRequest.ENVIRONMENT_ATTRIBUTE_NAME.equals(attributeName)) {
                        pdpEnvAttributesIndex = idx;
                    }
                    Map<String, List<String>> attributes = attribute.getAttributes();
                    if (attributes != null && !attributes.isEmpty()) {
                        Set<Entry<String, List<String>>> entrySet = attributes.entrySet();
                        for (Entry<String, List<String>> entry : entrySet) {
                            List<String> values = entry.getValue();
                            if (values != null && !values.isEmpty()) {
                                if (EvalRequest.ATTRIBVAL_RES_DONT_CARE_ACCEPTABLE.equals(entry.getKey()) && EvalRequest.ENVIRONMENT_ATTRIBUTE_NAME.equals(attributeName)) {
                                    dontCareAttributePresent = true;
                                }
                                for (String value : values) {
                                    environments[idx].setAttribute(entry.getKey(), value);
                                }
                            }
                        }
                    }
                    ++idx;
                }
            }
            if (hasPolicy) {
                environments[idx] = new PDPNamedAttributes("policies");
                environments[idx].setAttribute("pql", policy);
                environments[idx].setAttribute("ignoredefault", ignoreBuiltInPolicies ? "yes" : "no");
            }
        }
        if (!dontCareAttributePresent) {
            results = environments != null ? new IPDPNamedAttributes[environments.length + (pdpEnvAttributesIndex != -1 ? 0 : 1)] : new IPDPNamedAttributes[1];
            if (environments != null) {
                System.arraycopy(environments, 0, results, 0, environments.length);
            }
            if (pdpEnvAttributesIndex != -1) {
                environments[pdpEnvAttributesIndex].setAttribute(EvalRequest.ATTRIBVAL_RES_DONT_CARE_ACCEPTABLE, "yes");
            } else {
                results[results.length - 1] = new PDPNamedAttributes(EvalRequest.ENVIRONMENT_ATTRIBUTE_NAME);
                results[results.length - 1].setAttribute(EvalRequest.ATTRIBVAL_RES_DONT_CARE_ACCEPTABLE, "yes");
                pdpEnvAttributesIndex = results.length - 1;
            }
        } else {
            results = environments;
        }
        results[pdpEnvAttributesIndex].setAttribute(EvalRequest.ATTRIBVAL_RES_RESOURCE_TYPE_EVAL, "true");
        results[pdpEnvAttributesIndex].setAttribute(EvalRequest.ATTRIBVAL_PERFORM_OBLIGATIONS, req.isPerformObligations() ? "all" : "pep");
        return results;
    }

    private IPDPResource getResourceWithAttribs(Resource resource) {
        if (resource == null) {
            return null;
        }
        String dimension = resource.getDimensionName();
        String resourceName = resource.getResourceName();
        String type = resource.getResourceType();
        IPDPResource ipdpResource = new PDPResource(dimension, resourceName, type);
        Map<String, String[]> attributesList = resource.getAttributes();
        Map<String, String[]> classification = resource.getClassification();
        if (classification != null) {
            if (attributesList == null) {
                attributesList = classification;
            } else {
                attributesList.putAll(classification);
            }
        }
        if (attributesList != null && !attributesList.isEmpty()) {
            Set<Entry<String, String[]>> entrySet = attributesList.entrySet();
            for (Entry<String, String[]> entry : entrySet) {
                String[] values = entry.getValue();
                if (values != null && values.length != 0) {
                    for (String value : values) {
                        ipdpResource.setAttribute(entry.getKey(), value);
                    }
                }
            }
        }
        return ipdpResource;
    }

    private IPDPUser getUser(User user) {
        Map<String, List<String>> attributes = user.getAttributes();
        IPDPUser pdpUser = new PDPUser(user.getId(), user.getEmail());
        if (attributes != null && !attributes.isEmpty()) {
            Set<Entry<String, List<String>>> entrySet = attributes.entrySet();
            for (Entry<String, List<String>> entry : entrySet) {
                List<String> values = entry.getValue();
                if (values != null && !values.isEmpty()) {
                    for (String value : values) {
                        pdpUser.setAttribute(entry.getKey(), value);
                    }
                }
            }
        }
        return pdpUser;
    }

    @Override
    public void initializeSDK() {
    }

}
