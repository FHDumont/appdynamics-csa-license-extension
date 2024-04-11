package com.appdynamics.extensions.csalicense.model;

public class Node {

	private int nodeId;
	private String nodeName;
	private Boolean machineAgentInstalled;

	private String nodeNameSimplified;

	public String getNodeNameSimplified() {
		return nodeNameSimplified;
	}

	public void setNodeNameSimplified(String nodeNameSimplified) {
		this.nodeNameSimplified = nodeNameSimplified;
	}

	public Boolean getMachineAgentInstalled() {
		return machineAgentInstalled;
	}

	public void setMachineAgentInstalled(Boolean machineAgentInstalled) {
		this.machineAgentInstalled = machineAgentInstalled;
	}

	public int getNodeId() {
		return nodeId;
	}

	public void setNodeId(int nodeId) {
		this.nodeId = nodeId;
	}

	public String getNodeName() {
		return nodeName;
	}

	public void setNodeName(String nodeName) {
		this.nodeName = nodeName;
	}

}
