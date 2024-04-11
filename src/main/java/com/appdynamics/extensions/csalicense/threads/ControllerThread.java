package com.appdynamics.extensions.csalicense.threads;

import org.slf4j.Logger;

import com.appdynamics.extensions.csalicense.services.ControllerService;
import com.appdynamics.extensions.csalicense.util.Common;
import com.appdynamics.extensions.logging.ExtensionsLoggerFactory;

public class ControllerThread extends Thread {

	public static final Logger logger = ExtensionsLoggerFactory.getLogger(ControllerThread.class);

	private ControllerService controllerService;

	public ControllerThread(ControllerService controllerService) {
		this.controllerService = controllerService;
	}

	public void run() {
		try {
			controllerService.refreshServers();
			controllerService.refreshApplication();

			logger.info("{} Starting to get Server detail for {} CSA Nodes", Common.getLogHeader(this, "run"),
					controllerService.listCSANode.size());

			int totalServersChecked = 0;
			for (String serverName : controllerService.listCSANode.keySet()) {
				if (controllerService.listServers.containsKey(serverName)
						&& !controllerService.listServersLicensed.containsKey(serverName)) {
					totalServersChecked += 1;

					logger.info("{} Looking detail for server {}, total servers until now [{}] from [{}]",
							Common.getLogHeader(this, "run"),
							serverName, totalServersChecked, controllerService.listCSANode.size());
					controllerService.getServerDetail(controllerService.listServers.get(serverName));
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
