package com.appdynamics.extensions.csalicense.model;

public class ControllerInfo {

	private String displayName;
	private String controllerHost;
	private Boolean createDashboard;
	private String userName;
	private String password;
	private String accountName;
	private String proxyHost;
	private int proxyPort;
	private Boolean proxySsl;

	private Boolean isAvaliable;
	private Boolean isAllowedCSA;
	private String encryptionKey;

	public String getEncryptionKey() {
		return encryptionKey;
	}

	public void setEncryptionKey(String encryptionKey) {
		this.encryptionKey = encryptionKey;
	}

	public Boolean isAllowedCSA() {
		return isAllowedCSA;
	}

	public void setIsAllowedCSA(Boolean isAllowedCSA) {
		this.isAllowedCSA = isAllowedCSA;
	}

	public Boolean isAvaliable() {
		return isAvaliable;
	}

	public void setIsAvaliable(Boolean isAvaliable) {
		this.isAvaliable = isAvaliable;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public String getControllerHost() {
		return controllerHost;
	}

	public void setControllerHost(String controllerHost) {
		this.controllerHost = controllerHost;
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getAccountName() {
		return accountName;
	}

	public void setAccountName(String accountName) {
		this.accountName = accountName;
	}

	public String getProxyHost() {
		return proxyHost;
	}

	public void setProxyHost(String proxyHost) {
		this.proxyHost = proxyHost;
	}

	public int getProxyPort() {
		return proxyPort;
	}

	public void setProxyPort(int proxyPort) {
		this.proxyPort = proxyPort;
	}

	public Boolean getProxySsl() {
		return proxySsl;
	}

	public void setProxySsl(Boolean proxySsl) {
		this.proxySsl = proxySsl;
	}

	/**
	 * @return the createDashboard
	 */
	public Boolean isCreateDashboard() {
		return createDashboard;
	}

	/**
	 * @param createDashboard the createDashboard to set
	 */
	public void setCreateDashboard(Boolean createDashboard) {
		this.createDashboard = createDashboard;
	}
}
