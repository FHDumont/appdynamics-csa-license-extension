package com.appdynamics.extensions.csalicense.threads;

import org.slf4j.Logger;

import com.appdynamics.extensions.csalicense.model.CSAApplication;
import com.appdynamics.extensions.csalicense.model.Node;
import com.appdynamics.extensions.csalicense.services.ControllerService;
import com.appdynamics.extensions.logging.ExtensionsLoggerFactory;

public class ControllerThread extends Thread {

	public static final Logger logger = ExtensionsLoggerFactory.getLogger(ControllerThread.class);

	private ControllerService controllerService;

	public ControllerThread(ControllerService controllerService) {
		this.controllerService = controllerService;
	}

	public void run() {
		try {
			controllerService.refreshApplication();

			for (String applicationName : controllerService.listApplications.keySet()) {
				CSAApplication application = controllerService.listApplications.get(applicationName);
				Node[] nodes = controllerService.getNodesByApplication(application);
				for (Node node : nodes) {
					controllerService.getServerDetail(node);
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
