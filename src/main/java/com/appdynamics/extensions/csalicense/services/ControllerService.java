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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;

import com.appdynamics.extensions.crypto.Decryptor.DecryptionException;
import com.appdynamics.extensions.csalicense.model.CSAApplication;
import com.appdynamics.extensions.csalicense.model.CSANode;
import com.appdynamics.extensions.csalicense.model.CSATier;
import com.appdynamics.extensions.csalicense.model.ControllerInfo;
import com.appdynamics.extensions.csalicense.model.Server;
import com.appdynamics.extensions.csalicense.util.Common;
import com.appdynamics.extensions.csalicense.util.Constants;
import com.appdynamics.extensions.logging.ExtensionsLoggerFactory;
import com.appdynamics.extensions.util.CryptoUtils;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;

public class ControllerService {

	public static final Logger logger = ExtensionsLoggerFactory.getLogger(ControllerService.class);

	private HttpClient client;
	private List<String> cookies;

	public ControllerInfo controllerInfo;
	public Map<String, CSAApplication> listApplications;
	public Map<String, Integer> listServersLicensed;
	public Map<String, Server> listServers;
	public Map<String, CSANode> listCSANode;

	private ObjectMapper objectMapper;

	public ControllerService(ControllerInfo controllerInfo, Map<String, Object> yamlConfig) throws Exception {
		this.controllerInfo = controllerInfo;

		try {
			this.controllerInfo
					.setPassword(getPassword(controllerInfo.getPassword(),
							(String) yamlConfig.get(Constants.ENCRYPTION_KEY)));
		} catch (DecryptionException exDecryption) {
			logger.error("Error getPassword for controller {}", controllerInfo.getControllerHost(), exDecryption);
		}
		this.controllerInfo.setMachineAgentName((String) yamlConfig.get(Constants.MACHINE_AGENT_NAME));

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
		this.listCSANode = new ConcurrentHashMap<>();

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
							"{\"applications\":", "}],\"rowCount\":", "}]"),
					CSAApplication[].class);

			for (CSAApplication application : applications) {
				if (application.getTotalEnabled() > 0) {
					refreshTiers(application);
				}

			}
		}

		logger.info("{} Found {} applications on CSA ", Common.getLogHeader(this, "refreshApplication"),
				this.listApplications.size());

	}

	public void refreshTiers(CSAApplication application) throws Exception {
		// /controller/argento/api/v2/applications/81c937c0-e4ac-4711-857a-b68b4531d12d/tiers?order=ASC&max=10
		logger.info("{} Searching tiers on CSA {}...", Common.getLogHeader(this, "refreshTiers"),
				application.getApplicationName());

		CSATier[] tiers = new CSATier[0];

		HttpResponse<String> httpResponse = getRequest(
				String.format(
						"/controller/argento/api/v2/applications/%s/tiers?max=5000",
						application.getId()),
				Constants.HTTP_METHOD_GET, "");

		// /controller/argento/api/v2/applications/b92f35d5-d5ee-436e-bd37-1c80fc8f634f/tiers?filter=tierSecurityEnabledComputed+eq+%22true%22&sort=countSecurityEnabled&order=DESC&max=10

		if (httpResponse.statusCode() == 200
				&& !httpResponse.body().equalsIgnoreCase("{\"tiers\":[],\"rowCount\":0}")) {
			tiers = objectMapper.readValue(
					cleanRespondeBody(
							httpResponse.body(),
							"{\"tiers\":", "}],\"rowCount\":", "}]"),
					CSATier[].class);
		}

		logger.info("{} Found {} tiers on CSA {} ", Common.getLogHeader(this, "refreshTiers"), tiers.length,
				application.getApplicationName());

		for (CSATier tier : tiers) {
			if (tier.getTotalEnabled() > 0) {
				refreshNodes(tier);
			}
		}

		logger.info("{} Finished to looking for CSA data", Common.getLogHeader(this, "refreshTiers"));

	}

	public void refreshNodes(CSATier tier) throws Exception {
		// /controller/argento/api/v2/tiers/5e2970c5-20d3-4545-aaf2-b8bd5c96f1c0/nodes?order=ASC&max=10
		logger.debug("{} Searching nodes on CSA and tier {}...", Common.getLogHeader(this, "refreshNodes"),
				tier.getTierName());

		CSANode[] nodes = new CSANode[0];

		HttpResponse<String> httpResponse = getRequest(
				String.format("/controller/argento/api/v2/tiers/%s/nodes?max=5000", tier.getId()),
				Constants.HTTP_METHOD_GET, "");

		if (httpResponse.statusCode() == 200
				&& !httpResponse.body().equalsIgnoreCase("{\"nodes\":[],\"rowCount\":0}")) {
			nodes = objectMapper.readValue(
					cleanRespondeBody(
							httpResponse.body(),
							"{\"nodes\":", "}],\"rowCount\":", "}]"),
					CSANode[].class);
		}

		for (CSANode node : nodes) {
			if (node.getTotalEnabled().equalsIgnoreCase("yes")) {
				String newNodeName = node.getNodeJvmId();
				int indexHifen = newNodeName.indexOf("-");
				if (indexHifen != -1) {
					newNodeName = newNodeName.substring(0, indexHifen);
				}
				if (!this.listCSANode.containsKey(newNodeName)) {
					node.setServerName(newNodeName);
					this.listCSANode.put(newNodeName, node);
				}
			}
		}

		logger.info("{} Found {} nodes on CSA and tier {}, but {} total unique nodes",
				Common.getLogHeader(this, "refreshNodes"),
				nodes.length,
				tier.getTierName(),
				this.listCSANode.size());

	}

	private String cleanRespondeBody(String body, String findStart, String findStop, String addFinal) throws Exception {

		String serverReponseClean = "";

		try {
			String bodyWithFormat = body.replace("\n", "").replace("\r", "").replace("\t", "").replaceAll(" +", " ");
			int indexStart = bodyWithFormat.indexOf(findStart);
			int indexStop = bodyWithFormat.indexOf(findStop);

			serverReponseClean = bodyWithFormat.substring(indexStart + findStart.length(), indexStop) + addFinal;
		} catch (Exception e) {
			e.printStackTrace();
		}

		return serverReponseClean;

	}

	@SuppressWarnings("unchecked")
	public void getServerDetail(Server server) throws Exception {
		logger.debug("{} Getting server detail [{}]...", Common.getLogHeader(this, "getServerDetail"),
				server.getServerName());

		HttpResponse<String> httpResponse = getRequest(
				String.format(
						"/controller/sim/v2/user/machines/%s",
						server.getMachineId()),
				Constants.HTTP_METHOD_GET, "");

		if (httpResponse.statusCode() == 200 && !httpResponse.body().equals("{}")) {

			Object serverDetailObject = objectMapper.readValue(httpResponse.body(), Object.class);
			LinkedHashMap<String, Object> values = (LinkedHashMap<String, Object>) serverDetailObject;
			LinkedHashMap<String, Object> properties = (LinkedHashMap<String, Object>) values.get("properties");

			String name = (String) values.get("name");
			String vCPU = (String) properties.get("vCPU");

			this.listServersLicensed.put(name, Integer.parseInt(vCPU));
		}
	}

	public void createDashboard(String taskDir) throws Exception {
		if (!isDashboardExists()) {

			logger.info("{} Creating the dashboard...", Common.getLogHeader(this, "isDashboardExists"));

			HttpRequest httpRequest = null;
			HttpResponse<String> httpResponse;

			Path filePath = Path.of(taskDir + "/csa_license.json");

			String contentFile = Files.readString(filePath, StandardCharsets.UTF_8);
			contentFile = contentFile.replaceAll("MACHINE_AGENT_NAME", this.controllerInfo.getMachineAgentName());
			byte[] fileContent = contentFile.getBytes(StandardCharsets.UTF_8);

			String boundary = UUID.randomUUID().toString();

			StringBuffer formData = new StringBuffer();

			formData.append("--" + boundary + "\r\n");
			formData.append(
					"Content-Disposition: form-data; name=\"file\"; filename=\"" + filePath.getFileName()
							+ "\"\r\n\r\n");

			byte[] partHeaderBytes = formData.toString().getBytes();
			byte[] partFooterBytes = ("\r\n--" + boundary + "--\r\n").getBytes();

			// Concatenando as partes para formar o corpo da requisição
			byte[] requestBody = new byte[partHeaderBytes.length + fileContent.length + partFooterBytes.length];
			System.arraycopy(partHeaderBytes, 0, requestBody, 0, partHeaderBytes.length);
			System.arraycopy(fileContent, 0, requestBody, partHeaderBytes.length, fileContent.length);
			System.arraycopy(partFooterBytes, 0, requestBody, partHeaderBytes.length + fileContent.length,
					partFooterBytes.length);

			httpRequest = HttpRequest.newBuilder()
					.uri(new URI(controllerInfo.getControllerHost() + "/CustomDashboardImportExportServlet"))
					.header("Content-Type", "multipart/form-data;boundary=" + boundary)
					.header("Accept", "text/html,application/xhtml+xml,application/xml")
					.header("Cookie", Common.getCookies(this.cookies))
					.POST(HttpRequest.BodyPublishers.ofByteArray(requestBody))
					.build();

			httpResponse = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
			logger.debug("{} Response Status Code: [{}]", Common.getLogHeader(this, "getRequest"),
					httpResponse.statusCode());
			logger.debug("{} Response Status Body: [{}]", Common.getLogHeader(this, "getRequest"), httpResponse.body());

		}

	}

	public boolean isDashboardExists() throws Exception {

		// /controller/restui/dashboards/searchDashboardSummaries?offset=0&batch_size=50&query=CSA%20License&sort_key=NAME&sort_direction=ASC&created_by=
		logger.debug("{} Checking if there is the dashboard...", Common.getLogHeader(this, "isDashboardExists"));

		Boolean thereIsDashboard = false;

		HttpResponse<String> httpResponse = getRequest(
				"/controller/restui/dashboards/searchDashboardSummaries?offset=0&batch_size=50&query=CSA%20License&sort_key=NAME&sort_direction=ASC&created_by=",
				Constants.HTTP_METHOD_GET, "");

		if (httpResponse.statusCode() == 200) {

			if (httpResponse.body().indexOf("\"name\" : \"CSA License\"") != -1) {
				thereIsDashboard = true;
			}
		}

		logger.info("{} There is the dashboard {}", Common.getLogHeader(this, "isDashboardExists"), thereIsDashboard);

		return thereIsDashboard;
	}

	private String getPassword(String password, String encryptionKey) {
		if (!Strings.isNullOrEmpty(encryptionKey)) {
			Map<String, String> cryptoMap = Maps.newHashMap();
			cryptoMap.put(com.appdynamics.extensions.Constants.ENCRYPTED_PASSWORD, password);
			cryptoMap.put(com.appdynamics.extensions.Constants.ENCRYPTION_KEY, encryptionKey);
			return CryptoUtils.getPassword(cryptoMap);
		}

		return password;
	}

	public void refreshServers() throws Exception {
		logger.debug("{} Searching servers...", Common.getLogHeader(this, "refreshServers"));

		this.listServers = new ConcurrentHashMap<>();

		String payload = String.format(
				"{\"filter\":{\"appIds\":[],\"nodeIds\":[],\"tierIds\":[],\"types\":[\"PHYSICAL\",\"CONTAINER_AWARE\"],\"timeRangeStart\":%s,\"timeRangeEnd\":%s},\"sorter\":{\"field\":\"HEALTH\",\"direction\":\"ASC\"}}",
				System.currentTimeMillis() - 3600000, System.currentTimeMillis());

		HttpResponse<String> httpResponse = getRequest("/controller/sim/v2/user/machines/keys",
				Constants.HTTP_METHOD_POST, payload);

		String serverReponseClean = httpResponse.body().replace("],\"simEnabledMachineExists\":true}", "")
				.replace("{\"machineKeys\":[", "");

		serverReponseClean = "[" + serverReponseClean.replace("}{", "},{") + "]";

		Server[] servers = objectMapper.readValue(serverReponseClean, Server[].class);
		for (Server server : servers) {
			this.listServers.put(server.getServerName().toLowerCase(), server);
		}

		logger.debug("{} Found {} servers (machine agents)", Common.getLogHeader(this, "refreshServers"),
				this.listServers.size());

	}

}
