package com.appdynamics.extensions.csalicense;

import java.util.HashMap;
import java.util.Map;

import com.singularity.ee.agent.systemagent.api.TaskExecutionContext;

public class DebugExtension {

	public static void main(String... args) {

		try {

			Map<String, String> configMap = new HashMap<>();
			TaskExecutionContext taskExecutionContext = new TaskExecutionContext();
			CSALicenseExtension csaLicenseExtension = new CSALicenseExtension();

			configMap.put("config-file", "config.yml");
			taskExecutionContext.setTaskDir("/Users/fdumont/Developer/GitHub/appdynamics-csa-license-extension/src/main/resources/conf");

			csaLicenseExtension.execute(configMap, taskExecutionContext);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
