package com.nextlabs.rms.eval;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.nextlabs.common.util.RestClient;
import com.nextlabs.nxl.Rights;
import com.nextlabs.rms.eval.pojos.ObligationAttribute;
import com.nextlabs.rms.eval.pojos.PolicyEvalRestResponse;
import com.nextlabs.rms.eval.pojos.PolicyEvalRestResponseWrapper;
import com.nextlabs.rms.eval.pojos.XacmlEvalRequest;
import com.nextlabs.rms.eval.pojos.XacmlObligation;
import com.nextlabs.rms.shared.LogConstants;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class RestPDPEvalAdapter implements IEvalAdapter {

    private final Logger logger = LogManager.getLogger(LogConstants.RMS_SERVER_LOG_NAME);

    private static final String AUTH_ENDPOINT = "/cas/token";
    private static final String GRANT_TYPE_KEY = "grant_type";
    private static final String CLIENT_ID_KEY = "client_id";
    private static final String CLIENT_SECRET_KEY = "client_secret";
    private static final String EXPIRES_IN_KEY = "expires_in";
    private static final String ACCESS_TOKEN_KEY = "access_token";
    private static final String GRANT_TYPE_VALUE = "client_credentials";
    private static final Type JPC_RESPONSE_TYPE = new TypeToken<Map<String, String>>() {
    }.getType();

    private static final String EVAL_ENDPOINT = "/dpc/PDPConnector/pdppermissions";

    private Map<String, String> cachedToken;

    private final String consoleUrl;
    private final String pdpUrl;
    private final String clientId;
    private final String clientSecret;

    public RestPDPEvalAdapter(String consoleUrl, String clientId, String clientSecret, String pdpUrl) {
        this.consoleUrl = consoleUrl;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.pdpUrl = pdpUrl;
    }

    @Override
    public EvalResponse evaluate(EvalRequest req) {
        try {
            PolicyEvalRestResponseWrapper response = evaluate(XacmlEvalRequest.getXacmlRequest(req));
            if (response != null && !response.getPolicyEvalRestResponseList().isEmpty()) {
                return populateResponse(response.getPolicyEvalRestResponseList().get(0));
            }
        } catch (URISyntaxException | IOException | PolicyEvalException e) {
            logger.error("Error in policy evaluation", e);
        }
        return new EvalResponse();
    }

    @Override
    public List<EvalResponse> evaluate(List<EvalRequest> evalRequests) {
        try {
            PolicyEvalRestResponseWrapper response = evaluate(XacmlEvalRequest.getXacmlRequest(evalRequests));
            List<EvalResponse> responses = new ArrayList<>();
            if (response != null) {
                response.getPolicyEvalRestResponseList().forEach(resp -> responses.add(populateResponse(resp)));
            }
            return responses;
        } catch (URISyntaxException | IOException | PolicyEvalException e) {
            logger.error("Error in policy evaluation", e);
        }
        return Collections.emptyList();
    }

    private PolicyEvalRestResponseWrapper evaluate(XacmlEvalRequest xacmlEvalRequest)
            throws URISyntaxException, IOException, PolicyEvalException {
        ObjectMapper objectMapper = new ObjectMapper();
        String jsonRequest = objectMapper.writeValueAsString(xacmlEvalRequest);
        if (logger.isDebugEnabled()) {
            logger.debug(String.format("Policy eval request: %s", jsonRequest));
        }
        String respStr;
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpResponse response = executeAPICall(client, jsonRequest);
            StatusLine statusLine = response.getStatusLine();
            if (statusLine.getStatusCode() == HttpURLConnection.HTTP_UNAUTHORIZED) {
                logger.debug("API call was unauthorized, retying with new token");
                getAuthToken(true);
                EntityUtils.consume(response.getEntity());
                response = executeAPICall(client, jsonRequest);
            }
            respStr = EntityUtils.toString(response.getEntity());
        }
        if (logger.isDebugEnabled()) {
            logger.debug(String.format("Policy eval response: %s", respStr));
        }
        return objectMapper.readValue(respStr, PolicyEvalRestResponseWrapper.class);
    }

    private HttpResponse executeAPICall(CloseableHttpClient client, String payload)
            throws IOException, PolicyEvalException, URISyntaxException {
        String serviceUrl = pdpUrl + EVAL_ENDPOINT;
        HttpPost post = new HttpPost(serviceUrl);
        post.setEntity(new StringEntity(payload));
        post.setHeader(HTTP.CONTENT_TYPE, "application/json");

        post.setHeader("Authorization", String.format("Bearer %s", getAuthToken(false)));
        post.setHeader("Service", "EVAL");
        post.setHeader("Version", "1.0");
        return client.execute(post);
    }

    @Override
    public void initializeSDK() {
    }

    private synchronized String getAuthToken(boolean isForce)
            throws URISyntaxException, IOException, PolicyEvalException {
        if (cachedToken != null && !isForce) {
            return cachedToken.get(ACCESS_TOKEN_KEY);
        }
        String endpoint = String.format("%s%s", consoleUrl, AUTH_ENDPOINT);
        String uri = new URIBuilder(endpoint).addParameter(GRANT_TYPE_KEY, GRANT_TYPE_VALUE).addParameter(CLIENT_ID_KEY, clientId).addParameter(CLIENT_SECRET_KEY, clientSecret).addParameter(EXPIRES_IN_KEY, "3600").toString();
        String respStr = RestClient.post(uri, null);
        Map<String, String> response = new Gson().fromJson(respStr, JPC_RESPONSE_TYPE);
        if (response.containsKey("error")) {
            throw new PolicyEvalException("Error while authenticating with CC OAuth endpoint: " + response.get("error"));
        }
        cachedToken = response;
        return cachedToken.get(ACCESS_TOKEN_KEY);
    }

    private List<Obligation> populateObligations(List<XacmlObligation> evalObligations, String right) {
        List<Obligation> obligations = new ArrayList<>();
        int i = 1;
        for (XacmlObligation evalObligation : evalObligations) {
            Obligation obligation = new Obligation();
            obligation.setId(i);
            obligation.setName(evalObligation.getObligationId());
            obligation.setRight(right);
            List<ObligationAttribute> obligationAttributes = evalObligation.getAttributeList();
            for (ObligationAttribute attr : obligationAttributes) {
                Attribute attribute = new Attribute();
                attribute.setName(attr.getAttributeId());
                attribute.setValue(attr.getValue().get(0));
                obligation.addAttribute(attribute);
            }
            i++;
            obligations.add(obligation);
        }
        return obligations;
    }

    private EvalResponse populateResponse(PolicyEvalRestResponse policyEvalRestResponse) {
        List<Obligation> obligations = new ArrayList<>();
        List<String> allowRights = new ArrayList<>();
        policyEvalRestResponse.getDecisionMap().get("allow").forEach(decision -> {
            String action = decision.getAction();
            if (action.startsWith(Rights.ACTION_PREFIX)) {
                allowRights.add(action);
                obligations.addAll(populateObligations(decision.getObligations(), action));
            }
        });
        EvalResponse evalResponse = new EvalResponse(Rights.fromStrings(allowRights));
        evalResponse.addObligations(obligations);
        return evalResponse;
    }

}
