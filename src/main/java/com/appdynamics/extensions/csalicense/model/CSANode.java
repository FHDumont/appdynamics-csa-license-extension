package com.appdynamics.extensions.csalicense.model;

public class CSANode {

	private String id;
	private String nodeJvmId;
	private String agentType;
	private String nodeEnableSecurityStatus;

	private String totalActive;
	private String totalSecured;
	private String totalEnabled;
	private String totalReady;

	private String serverName;

	public String getServerName() {
		return serverName;
	}

	public void setServerName(String serverName) {
		this.serverName = serverName;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getNodeJvmId() {
		return nodeJvmId;
	}

	public void setNodeJvmId(String nodeJvmId) {
		this.nodeJvmId = nodeJvmId;
	}

	public String getAgentType() {
		return agentType;
	}

	public void setAgentType(String agentType) {
		this.agentType = agentType;
	}

	public String getNodeEnableSecurityStatus() {
		return nodeEnableSecurityStatus;
	}

	public void setNodeEnableSecurityStatus(String nodeEnableSecurityStatus) {
		this.nodeEnableSecurityStatus = nodeEnableSecurityStatus;
	}

	public String getTotalActive() {
		return totalActive;
	}

	public void setTotalActive(String totalActive) {
		this.totalActive = totalActive;
	}

	public String getTotalSecured() {
		return totalSecured;
	}

	public void setTotalSecured(String totalSecured) {
		this.totalSecured = totalSecured;
	}

	public String getTotalEnabled() {
		return totalEnabled;
	}

	public void setTotalEnabled(String totalEnabled) {
		this.totalEnabled = totalEnabled;
	}

	public String getTotalReady() {
		return totalReady;
	}

	public void setTotalReady(String totalReady) {
		this.totalReady = totalReady;
	}

}
