package com.appdynamics.extensions.csalicense.model;

public class Server implements Cloneable {

	private int machineId;
	private String serverName;

	public int getMachineId() {
		return machineId;
	}

	public void setMachineId(int machineId) {
		this.machineId = machineId;
	}

	public String getServerName() {
		return serverName;
	}

	public void setServerName(String serverName) {
		this.serverName = serverName;
	}

	@Override
	public Object clone() throws CloneNotSupportedException {
		return super.clone();
	}

}
