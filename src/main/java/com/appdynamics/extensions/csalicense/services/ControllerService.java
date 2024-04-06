package com.appdynamics.extensions.csalicense.services;

import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;

import com.appdynamics.extensions.csalicense.model.CSAApplication;
import com.appdynamics.extensions.csalicense.model.CSANode;
import com.appdynamics.extensions.csalicense.model.CSATier;
import com.appdynamics.extensions.csalicense.model.ControllerInfo;
import com.appdynamics.extensions.csalicense.model.Node;
import com.appdynamics.extensions.csalicense.util.Common;
import com.appdynamics.extensions.csalicense.util.Constants;
import com.appdynamics.extensions.logging.ExtensionsLoggerFactory;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ControllerService {

	public static final Logger logger = ExtensionsLoggerFactory.getLogger(ControllerService.class);

	private HttpClient client;
	private List<String> cookies;

	public ControllerInfo controllerInfo;
	public Map<String, CSAApplication> listApplications;
	public Map<String, Integer> listServersLicensed;

	private ObjectMapper objectMapper;

	public ControllerService(ControllerInfo controllerInfo) throws Exception {
		this.controllerInfo = controllerInfo;

		logger.info("{} Connecting to controller: [{}] using username: [{}]",
				Common.getLogHeader(this, "constructor"),
				this.controllerInfo.getControllerHost(),
				this.controllerInfo.getUserName());

		logger.info("{} Verificando necessidade de proxy [{}]",
				Common.getLogHeader(this, "constructor"),
				this.controllerInfo.getProxyHost());

		if (this.controllerInfo.getProxyHost() != null && !this.controllerInfo.getProxyHost().equals("")) {
			logger.info("{} Setting proxy [{}] [{}] [{}]",
					Common.getLogHeader(this, "constructor"),
					this.controllerInfo.getProxyHost(),
					this.controllerInfo.getProxyPort(),
					this.controllerInfo.getProxySsl());

			InetSocketAddress proxyAddress = new InetSocketAddress(this.controllerInfo.getProxyHost(),
					this.controllerInfo.getProxyPort());
			ProxySelector proxySelector = ProxySelector.of(proxyAddress);
			this.client = HttpClient.newBuilder().proxy(proxySelector).version(HttpClient.Version.HTTP_2).build();

		} else {
			this.client = HttpClient.newBuilder().build();
		}

		String payload = String.format("userName=%s&password=%s&accountName=%s",
				controllerInfo.getUserName(),
				Base64.getEncoder().encodeToString(
						URLEncoder.encode(controllerInfo.getPassword(), StandardCharsets.UTF_8)
								.getBytes(StandardCharsets.UTF_8)),
				controllerInfo.getAccountName());

		HttpResponse<String> httpResponse = getRequest("/controller/auth?action=login",
				Constants.HTTP_METHOD_GET, payload);

		HttpHeaders headers = httpResponse.headers();
		this.cookies = headers.allValues("Set-Cookie");

		logger.info("Connected Controller Controller {} {}",
				this.controllerInfo.getControllerHost(), httpResponse.statusCode());

		this.controllerInfo.setIsAvaliable(false);
		this.controllerInfo.setIsAllowedCSA(false);
		if (httpResponse.statusCode() == 200) {
			this.controllerInfo.setIsAvaliable(true);
		}

		this.listApplications = new ConcurrentHashMap<>();
		this.listServersLicensed = new ConcurrentHashMap<>();
		objectMapper = new ObjectMapper();
		objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
	}

	public HttpResponse<String> getRequest(String uri, String method, String payload) throws Exception {

		HttpRequest httpRequest = null;
		HttpResponse<String> httpResponse;

		logger.debug("{} Requesting URL: [{}], method [{}] and payload: [{}]", Common.getLogHeader(this, "getRequest"),
				uri, method, payload);

		if (uri.equalsIgnoreCase("/controller/auth?action=login")) {
			logger.debug("{} It's login using form", Common.getLogHeader(this, "getRequest"));
			httpRequest = HttpRequest.newBuilder()
					.uri(new URI(controllerInfo.getControllerHost() + uri))
					.header("Content-Type", "application/x-www-form-urlencoded")
					.header("Accept", "application/json, text/plain, */*")
					.header("Accept-Encoding", "gzip, deflate, br, zstd")
					.POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
					.build();
		} else {
			switch (method) {
				case Constants.HTTP_METHOD_POST:
					httpRequest = HttpRequest.newBuilder()
							.uri(new URI(controllerInfo.getControllerHost() + uri))
							.header("Content-Type", "application/json;charset=UTF-8")
							.header("Accept", "application/json, text/plain, */*")
							.header("Cookie", Common.getCookies(this.cookies))
							.POST(HttpRequest.BodyPublishers.ofString(payload))
							.build();

					break;
				case Constants.HTTP_METHOD_GET:
					httpRequest = HttpRequest.newBuilder()
							.uri(new URI(controllerInfo.getControllerHost() + uri))
							.header("Content-Type", "application/json;charset=UTF-8")
							.header("Accept", "application/json, text/plain, */*")
							.header("Cookie", Common.getCookies(this.cookies))
							.build();
					break;

				default:
					logger.debug("{} Request type GET", Common.getLogHeader(this, "getRequest"));
					break;
			}
		}

		httpResponse = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
		logger.debug("{} Response Status Code: [{}]", Common.getLogHeader(this, "getRequest"),
				httpResponse.statusCode());
		logger.debug("{} Response Status Body: [{}]", Common.getLogHeader(this, "getRequest"), httpResponse.body());

		return httpResponse;

	}

	public void refreshApplication() throws Exception {
		logger.debug("{} Searching applications on CSA...", Common.getLogHeader(this, "refreshApplication"));

		this.listApplications = new ConcurrentHashMap<>();

		CSAApplication[] applications = new CSAApplication[0];

		HttpResponse<String> httpResponse = getRequest("/controller/argento/api/v2/applications",
				Constants.HTTP_METHOD_GET, "");

		if (httpResponse.statusCode() == 200) {
			this.controllerInfo.setIsAllowedCSA(true);
		}

		if (httpResponse.statusCode() == 200
				&& !httpResponse.body().equalsIgnoreCase("{\"applications\":[],\"rowCount\":0}")) {

			applications = objectMapper.readValue(
					cleanRespondeBody(
							httpResponse.body(),
							"{\"applications\":",
							"}],\"rowCount\":"),
					CSAApplication[].class);

			for (CSAApplication application : applications) {
				if (application.getTotalEnabled() > 0) {
					application.setTiers(refreshTiers(application));
					this.listApplications.put(application.getApplicationName(), application);
				}

			}
		}

		logger.debug("{} Found {} applications on CSA ", Common.getLogHeader(this, "refreshApplication"),
				this.listApplications.size());

	}

	public CSATier[] refreshTiers(CSAApplication application) throws Exception {
		// /controller/argento/api/v2/applications/81c937c0-e4ac-4711-857a-b68b4531d12d/tiers?order=ASC&max=10
		logger.debug("{} Searching tiers on CSA...", Common.getLogHeader(this, "refreshTiers"));

		CSATier[] tiers = new CSATier[0];

		HttpResponse<String> httpResponse = getRequest(
				String.format("/controller/argento/api/v2/applications/%s/tiers", application.getId()),
				Constants.HTTP_METHOD_GET, "");

		if (httpResponse.statusCode() == 200
				&& !httpResponse.body().equalsIgnoreCase("{\"tiers\":[],\"rowCount\":0}")) {
			tiers = objectMapper.readValue(
					cleanRespondeBody(
							httpResponse.body(),
							"{\"tiers\":",
							"}],\"rowCount\":"),
					CSATier[].class);
		}

		logger.debug("{} Found {} tiers on CSA ", Common.getLogHeader(this, "refreshTiers"), tiers.length);

		for (CSATier tier : tiers) {
			if (tier.getTotalEnabled() > 0) {
				tier.setNodes(refreshNodes(tier));
			}
		}

		return tiers;
	}

	public CSANode[] refreshNodes(CSATier tier) throws Exception {
		// /controller/argento/api/v2/tiers/5e2970c5-20d3-4545-aaf2-b8bd5c96f1c0/nodes?order=ASC&max=10
		logger.debug("{} Searching nodes on CSA...", Common.getLogHeader(this, "refreshNodes"));

		CSANode[] nodes = new CSANode[0];
		List<CSANode> nodesEnabled = new ArrayList<>();

		HttpResponse<String> httpResponse = getRequest(
				String.format("/controller/argento/api/v2/tiers/%s/nodes", tier.getId()),
				Constants.HTTP_METHOD_GET, "");

		if (httpResponse.statusCode() == 200
				&& !httpResponse.body().equalsIgnoreCase("{\"nodes\":[],\"rowCount\":0}")) {
			nodes = objectMapper.readValue(
					cleanRespondeBody(
							httpResponse.body(),
							"{\"nodes\":",
							"}],\"rowCount\":"),
					CSANode[].class);
		}

		for (CSANode node : nodes) {
			if (node.getTotalEnabled().equalsIgnoreCase("yes")) {
				nodesEnabled.add(node);
			}
		}

		logger.debug("{} Found {} nodes on CSA ", Common.getLogHeader(this, "refreshNodes"), nodes.length);

		return nodesEnabled.toArray(new CSANode[nodesEnabled.size()]);
	}

	public Node[] getNodesByApplication(CSAApplication application) throws Exception {
		// /controller/argento/api/v2/tiers/5e2970c5-20d3-4545-aaf2-b8bd5c96f1c0/nodes?order=ASC&max=10
		logger.debug("{} Searching nodes on CSA...", Common.getLogHeader(this, "refreshNodes"));

		Node[] nodes = new Node[0];

		String payload = String.format(
				"{\"requestFilter\":{\"queryParams\":{\"applicationId\":%s,\"performanceDataFilter\":\"REPORTING\",\"tags\":[]},\"filterAll\":false,\"filters\":[]},\"resultColumns\":[\"TIER_NAME\"],\"offset\":0,\"limit\":-1,\"searchFilters\":[],\"columnSorts\":[{\"column\":\"TIER_NAME\",\"direction\":\"ASC\"}],\"timeRangeStart\":%s,\"timeRangeEnd\":%s}",
				application.getAppdId(), System.currentTimeMillis() - 3600000, System.currentTimeMillis());

		HttpResponse<String> httpResponse = getRequest(
				String.format("/controller/restui/v1/tiers/list/health"), Constants.HTTP_METHOD_POST, payload);

		if (httpResponse.statusCode() == 200 && !httpResponse.body().startsWith("{ \"data\" : []")) {
			nodes = objectMapper.readValue(
					cleanRespondeBody(
							httpResponse.body(),
							"\"children\" :",
							"} ] } ], \"totalCount\" :"),
					Node[].class);
		}

		return nodes == null ? new Node[0] : nodes;
	}

	private String cleanRespondeBody(String body, String findStart, String findStop) throws Exception {

		String serverReponseClean = "";

		try {
			String bodyWithFormat = body.replace("\n", "").replace("\r", "").replace("\t", "").replaceAll(" +", " ");
			int indexStart = bodyWithFormat.indexOf(findStart);
			int indexStop = bodyWithFormat.indexOf(findStop);

			serverReponseClean = bodyWithFormat.substring(indexStart + findStart.length(), indexStop) + "}]";
		} catch (Exception e) {
			e.printStackTrace();
		}

		return serverReponseClean;

	}

	@SuppressWarnings("unchecked")
	public void getServerDetail(Node node) throws Exception {
		logger.debug("{} Getting server detail...", Common.getLogHeader(this, "getServerDetail"));

		HttpResponse<String> httpResponse = getRequest(
				String.format(
						"/controller/sim/v2/user/machines?appIds=&tierIds=&nodeIds=%s&format=LITE&tags=&types=PHYSICAL,CONTAINER_AWARE&offset=0&limit=-1",
						node.getNodeId()),
				Constants.HTTP_METHOD_GET, "");

		if (httpResponse.statusCode() == 200 && !httpResponse.body().equals("[]")) {

			Object serverDetailObject = objectMapper.readValue(httpResponse.body(), Object[].class)[0];
			LinkedHashMap<String, Object> values = (LinkedHashMap<String, Object>) serverDetailObject;
			LinkedHashMap<String, Object> properties = (LinkedHashMap<String, Object>) values.get("properties");

			String name = (String) values.get("name");
			String vCPU = (String) properties.get("vCPU");

			this.listServersLicensed.put(name, Integer.parseInt(vCPU));
		}
	}

}
