package com.appdynamics.extensions.csalicense.model;

public class CSAApplication {

	private String id;
	private int appdId;
	private String applicationName;
	private String appEnableSecurityStatus;
	private int totalActive;
	private int totalSecured;
	private int totalEnabled;
	private int totalReady;

	// TIERS
	private CSATier[] tiers;

	public CSATier[] getTiers() {
		if (tiers == null) {
			return new CSATier[0];
		} else {
			return tiers;
		}
	}

	public void setTiers(CSATier[] tiers) {
		this.tiers = tiers;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public int getAppdId() {
		return appdId;
	}

	public void setAppdId(int appdId) {
		this.appdId = appdId;
	}

	public String getApplicationName() {
		return applicationName;
	}

	public void setApplicationName(String applicationName) {
		this.applicationName = applicationName;
	}

	public String getAppEnableSecurityStatus() {
		return appEnableSecurityStatus;
	}

	public void setAppEnableSecurityStatus(String appEnableSecurityStatus) {
		this.appEnableSecurityStatus = appEnableSecurityStatus;
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

	public boolean isNodeEnabled() {
		boolean isNodeEnabled = true;
		if (getTotalEnabled() > 0) {
			for (CSATier tier : getTiers()) {
				if (tier.getTotalEnabled() > 0) {
					for (CSANode node : tier.getNodes()) {
						if (!node.getTotalEnabled().equalsIgnoreCase("yes")) {
							isNodeEnabled = false;
						}
					}
				} else {
					isNodeEnabled = false;
				}
			}
		} else {
			isNodeEnabled = false;
		}
		return isNodeEnabled;
	}

}
