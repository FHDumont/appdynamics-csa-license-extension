package com.appdynamics.extensions.csalicense.model;

public class CSATier {

	private String id;
	private int appdTierId;
	private String tierName;
	private int totalActive;
	private int totalSecured;
	private int totalEnabled;
	private int totalReady;

	private CSANode[] nodes;

	public CSANode[] getNodes() {
		if (nodes == null) {
			return new CSANode[0];
		} else {
			return nodes;
		}
	}

	public void setNodes(CSANode[] nodes) {
		this.nodes = nodes;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public int getAppdTierId() {
		return appdTierId;
	}

	public void setAppdTierId(int appdTierId) {
		this.appdTierId = appdTierId;
	}

	public String getTierName() {
		return tierName;
	}

	public void setTierName(String tierName) {
		this.tierName = tierName;
	}

	public int getTotalActive() {
		return totalActive;
	}

	public void setTotalActive(int totalActive) {
		this.totalActive = totalActive;
	}

	public int getTotalSecured() {
		return totalSecured;
	}

	public void setTotalSecured(int totalSecured) {
		this.totalSecured = totalSecured;
	}

	public int getTotalEnabled() {
		return totalEnabled;
	}

	public void setTotalEnabled(int totalEnabled) {
		this.totalEnabled = totalEnabled;
	}

	public int getTotalReady() {
		return totalReady;
	}

	public void setTotalReady(int totalReady) {
		this.totalReady = totalReady;
	}

}
