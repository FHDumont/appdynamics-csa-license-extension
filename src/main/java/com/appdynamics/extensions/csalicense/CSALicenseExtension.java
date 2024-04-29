package com.appdynamics.extensions.csalicense;

import java.io.FileInputStream;
import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.yaml.snakeyaml.Yaml;

import com.appdynamics.extensions.csalicense.model.ControllerInfo;
import com.appdynamics.extensions.csalicense.services.ControllerService;
import com.appdynamics.extensions.csalicense.threads.ControllerThread;
import com.appdynamics.extensions.csalicense.util.Common;
import com.appdynamics.extensions.csalicense.util.Constants;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.singularity.ee.agent.systemagent.api.AManagedMonitor;
import com.singularity.ee.agent.systemagent.api.MetricWriter;
import com.singularity.ee.agent.systemagent.api.TaskExecutionContext;
import com.singularity.ee.agent.systemagent.api.TaskOutput;
import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;

public class CSALicenseExtension extends AManagedMonitor {

	private Logger logger = LogManager.getFormatterLogger();

	private ControllerInfo[] listControllerInfo;
	private Map<String, ControllerService> listControllerService;
	private String metricPrefix = "Custom Metrics|CSA-License|";

	@Override
	public TaskOutput execute(Map<String, String> configMap, TaskExecutionContext taskExecutionContext) throws TaskExecutionException {

		this.logger = taskExecutionContext.getLogger();

		if (configMap.getOrDefault("config-file", "unconfigured").equals("unconfigured")) {
			throw new TaskExecutionException("Confluent Config File Not Set, nothing to do");
		}

		while (true) {
			Map<String, Object> yamlConfig = new HashMap<>();
			List<Thread> threads = new ArrayList<>();

			this.listControllerService = new HashMap<>();

			int frequency = 30;

			Instant startTime = Instant.now();
			Instant startSubTask;

			try {
				// READIN CONFIG.YML
				Yaml yaml = new Yaml();
				InputStream inputStream = new FileInputStream(taskExecutionContext.getTaskDir() + "/" + configMap.get("config-file"));
				yamlConfig = yaml.load(inputStream);

				// METRIC PREFIX
				if (yamlConfig.get(Constants.METRIC_PREFIX) != null && !yamlConfig.get(Constants.METRIC_PREFIX).equals("")) {
					metricPrefix = (String) yamlConfig.get(Constants.METRIC_PREFIX);
				}

				// FREQUENCY
				if (yamlConfig.get(Constants.FREQUENCY) != null && !yamlConfig.get(Constants.FREQUENCY).equals("")) {
					frequency = Integer.valueOf(yamlConfig.get(Constants.FREQUENCY).toString());
				}

				this.logger.info("{} Metric prefix [{}]", Common.getLogHeader(this, "run"), metricPrefix);

				this.logger.info("{} Starting task with frequency {} minutes", Common.getLogHeader(this, "run"), frequency);

				// ==> CONTROLLER CONFIGURATIONS
				this.listControllerInfo = new ObjectMapper().convertValue(yamlConfig.get(Constants.CONTROLLERS), ControllerInfo[].class);

				for (ControllerInfo ci : this.listControllerInfo) {
					ControllerService cs = new ControllerService(ci, yamlConfig);
					this.listControllerService.put(ci.getControllerHost(), cs);

					ControllerThread controllerThread = new ControllerThread(cs);
					controllerThread.setName(ci.getControllerHost());
					threads.add(controllerThread);
				}

				String saveDashboard = (String) yamlConfig.get(Constants.SAVE_DASHBOARD);
				if (saveDashboard != null && !saveDashboard.equals("")) {
					ControllerService cs = this.listControllerService.get(saveDashboard);
					cs.createDashboard(taskExecutionContext.getTaskDir());
				}

				startSubTask = Instant.now();
				this.logger.debug("{} Starting controller data capture threads...", Common.getLogHeader(this, "run"));
				for (Thread thread : threads) {
					thread.start();
				}

				this.logger.info("{} ==> Waiting for controller data capture threads to finish...", Common.getLogHeader(this, "run"));
				for (Thread thread : threads) {
					thread.join();
				}

				this.logger.info("{} Controller data capture threads finished! Execution time {}s", Common.getLogHeader(this, "run"),
						Duration.between(startSubTask, Instant.now()).getSeconds());

			} catch (Exception exception) {
				exception.printStackTrace();
				this.logger.error("{} Exception on running task {}", Common.getLogHeader(this, "run"), exception.getMessage(), exception);
			}

			try {

				this.logger.info(
						"{} Final execution time {}s, waiting for the next round in the next {} minutes. The metrics will now be published every minute until the next round.",
						Common.getLogHeader(this, "run"), Duration.between(startTime, Instant.now()).getSeconds(), frequency);

				startSubTask = Instant.now();
				for (int totalExecution = 1; totalExecution <= frequency; totalExecution++) {
					if (yamlConfig.get(Constants.PUBLISH_METRICS) != null && (boolean) yamlConfig.get(Constants.PUBLISH_METRICS)) {
						this.publishMetrics();
						Thread.sleep(60000);
					}
				}

			} catch (Exception exception) {
				exception.printStackTrace();
			}

		}

	}

	protected void publishMetrics() throws Exception {
		Instant startSubTask = Instant.now();
		logger.info("{} ==> Publishing metrics values...", Common.getLogHeader(this, "run"));
		int totalVCPU = 0;
		for (ControllerInfo ci : listControllerInfo) {
			int totalVCPUController = 0;
			ControllerService controllerService = listControllerService.get(ci.getControllerHost());
			logger.debug("==> Dados da Controller {}, connected {}, Allowed CSA {}", ci.getControllerHost(), controllerService.controllerInfo.isAvaliable(),
					controllerService.controllerInfo.isAllowedCSA());

			for (String serverName : controllerService.listServersLicensed.keySet()) {
				totalVCPUController += controllerService.listServersLicensed.get(serverName);
				logger.debug("  - Servidor {} e vCPU {}", serverName, controllerService.listServersLicensed.get(serverName));
			}

			totalVCPU += totalVCPUController;

			logger.info("=> vCPU {} on Controller {}", totalVCPUController, controllerService.controllerInfo.getControllerHost());

			try {
				String metricName = ci.getControllerHost().split("//")[1] + "|";

				publicMetric(metricName + "Avaliable", controllerService.controllerInfo.isAvaliable() ? 1 : 0, MetricWriter.METRIC_AGGREGATION_TYPE_OBSERVATION,
						MetricWriter.METRIC_TIME_ROLLUP_TYPE_CURRENT, MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_COLLECTIVE);

				publicMetric(metricName + "CSA Allowed", controllerService.controllerInfo.isAllowedCSA() ? 1 : 0,
						MetricWriter.METRIC_AGGREGATION_TYPE_OBSERVATION, MetricWriter.METRIC_TIME_ROLLUP_TYPE_CURRENT,
						MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_COLLECTIVE);

				publicMetric(metricName + "License Used", totalVCPUController, MetricWriter.METRIC_AGGREGATION_TYPE_OBSERVATION,
						MetricWriter.METRIC_TIME_ROLLUP_TYPE_CURRENT, MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_COLLECTIVE);

			} catch (Exception e) {
				logger.error("{} {}...", Common.getLogHeader(this, "run"), e.getMessage(), e);
			}

		}

		try {

			publicMetric("License Used", totalVCPU, MetricWriter.METRIC_AGGREGATION_TYPE_OBSERVATION, MetricWriter.METRIC_TIME_ROLLUP_TYPE_CURRENT,
					MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_COLLECTIVE);

			logger.info("=> vCPU on all controllers {}, it took {}s to publish metrics, waiting next round (1 minute)", totalVCPU,
					Duration.between(startSubTask, Instant.now()).getSeconds());

		} catch (Exception e) {
			logger.error("{} {}...", Common.getLogHeader(this, "run"), e.getMessage(), e);
		}
	}

	protected void publicMetric(String metricName, Object metricValue, String aggregation, String timeRollup, String cluster) throws Exception {
		logger.debug("Printing Metric [{}/{}/{}] [{}]=[{}]", aggregation, timeRollup, cluster, this.metricPrefix + metricName, metricValue);

		MetricWriter metricWriter = getMetricWriter(this.metricPrefix + metricName, aggregation, timeRollup, cluster);

		metricWriter.printMetric(String.valueOf(metricValue));

	}

}