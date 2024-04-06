package com.appdynamics.extensions.csalicense;

import java.io.FileInputStream;
import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.yaml.snakeyaml.Yaml;

public class CSALicenseExtension extends AManagedMonitor {

	private Logger logger = LogManager.getFormatterLogger();

	private Map<String, ControllerService> listControllerService;
	private Map<String, Object> yamlConfig;
	private String metricPrefix = "Custom Metrics|CSA-License|";

	private List<Thread> threads;

	@Override
	public TaskOutput execute(Map<String, String> configMap, TaskExecutionContext taskExecutionContext)
			throws TaskExecutionException {

		this.logger = taskExecutionContext.getLogger();

		if (configMap.getOrDefault("config-file", "unconfigured").equals("unconfigured")) {
			throw new TaskExecutionException("Confluent Config File Not Set, nothing to do");
		}

		yamlConfig = new HashMap<>();
		Instant startTime = Instant.now();
		Instant startSubTask;

		String finalMessage = "Task processed!";

		try {
			this.logger.info("{} Starting task", Common.getLogHeader(this, "run"));

			// READIN CONFIG.YML
			Yaml yaml = new Yaml();
			InputStream inputStream = new FileInputStream(
					taskExecutionContext.getTaskDir() + "/" + configMap.get("config-file"));
			yamlConfig = yaml.load(inputStream);

			if (yamlConfig.get(Constants.METRIC_PREFIX) != null
					&& !yamlConfig.get(Constants.METRIC_PREFIX).equals("")) {
				this.metricPrefix = (String) yamlConfig.get(Constants.METRIC_PREFIX);
			}
			this.logger.info("{} Metric prefix [{}]", Common.getLogHeader(this, "run"),
					this.metricPrefix);

			// ==> CONTROLLER CONFIGURATIONS
			ControllerInfo[] listControllerInfo = new ObjectMapper().convertValue(
					yamlConfig.get(Constants.CONTROLLERS),
					ControllerInfo[].class);

			listControllerService = new HashMap<>();
			threads = new ArrayList<>();
			for (ControllerInfo ci : listControllerInfo) {
				ControllerService cs = new ControllerService(ci);
				listControllerService.put(ci.getControllerHost(), cs);

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

			this.logger.info("{} ==> Waiting for controller data capture threads to finish...",
					Common.getLogHeader(this, "run"));
			for (Thread thread : threads) {
				thread.join();
			}

			this.logger.info("{} Controller data capture threads finished! Execution time {}s",
					Common.getLogHeader(this, "run"),
					Duration.between(startSubTask, Instant.now()).getSeconds());

			startSubTask = Instant.now();
			if (yamlConfig.get(Constants.PUBLISH_METRICS) != null
					&& (boolean) yamlConfig.get(Constants.PUBLISH_METRICS)) {
				this.logger.info("{} ==> Publishing metrics values...", Common.getLogHeader(this, "run"));
				int totalVCPU = 0;
				for (ControllerInfo ci : listControllerInfo) {
					int totalVCPUController = 0;
					ControllerService controllerService = this.getListControllerService().get(ci.getControllerHost());
					this.logger.info("==> Dados da Controller {}, connected {}, Allowed CSA {}",
							ci.getControllerHost(),
							controllerService.controllerInfo.isAvaliable(),
							controllerService.controllerInfo.isAllowedCSA());

					for (String serverName : controllerService.listServersLicensed.keySet()) {
						totalVCPUController += controllerService.listServersLicensed.get(serverName);
						this.logger.debug("  - Servidor {} e vCPU {}", serverName,
								controllerService.listServersLicensed.get(serverName));
					}

					totalVCPU += totalVCPUController;

					this.logger.info("  => TOTAL DE VCPU {} na Controller {}", totalVCPUController,
							controllerService.controllerInfo.getControllerHost());

					try {
						String metricName = ci.getControllerHost().split("//")[1] + "|";

						publicMetric(metricName + "Avaliable",
								controllerService.controllerInfo.isAvaliable() ? 1 : 0,
								MetricWriter.METRIC_AGGREGATION_TYPE_OBSERVATION,
								MetricWriter.METRIC_TIME_ROLLUP_TYPE_CURRENT,
								MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_COLLECTIVE);

						publicMetric(metricName + "CSA Allowed",
								controllerService.controllerInfo.isAllowedCSA() ? 1 : 0,
								MetricWriter.METRIC_AGGREGATION_TYPE_OBSERVATION,
								MetricWriter.METRIC_TIME_ROLLUP_TYPE_CURRENT,
								MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_COLLECTIVE);

						publicMetric(metricName + "License Used",
								totalVCPUController,
								MetricWriter.METRIC_AGGREGATION_TYPE_OBSERVATION,
								MetricWriter.METRIC_TIME_ROLLUP_TYPE_CURRENT,
								MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_COLLECTIVE);

					} catch (Exception e) {
						this.logger.error("{} {}...",
								Common.getLogHeader(this, "run"),
								e.getMessage(), e);
					}

				}

				try {

					publicMetric("License Used",
							totalVCPU,
							MetricWriter.METRIC_AGGREGATION_TYPE_OBSERVATION,
							MetricWriter.METRIC_TIME_ROLLUP_TYPE_CURRENT,
							MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_COLLECTIVE);

					this.logger.info("=> TOTAL DE VCPU NO CONTRATO: {}", totalVCPU);

				} catch (Exception e) {
					this.logger.error("{} {}...",
							Common.getLogHeader(this, "run"),
							e.getMessage(), e);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			this.logger.error("{} Exception on running task {}", Common.getLogHeader(this, "run"), e.getMessage(), e);
			finalMessage = "ERROR = " + e.getMessage();
		}

		try {
			finalMessage = String.format("Final execution time %ss, waiting next round.",
					Duration.between(startTime, Instant.now()).getSeconds());
			this.logger.info("{} {}", Common.getLogHeader(this, "run"), finalMessage);

		} catch (Exception e) {
			e.printStackTrace();
			finalMessage = "ERROR = " + e.getMessage();
		}

		return new TaskOutput(finalMessage);

	}

	public Map<String, ControllerService> getListControllerService() {
		return listControllerService;
	}

	protected void publicMetric(String metricName, Object metricValue,
			String aggregation, String timeRollup, String cluster) throws Exception {
		this.logger.info("Printing Metric [{}/{}/{}] [{}]=[{}]", aggregation, timeRollup, cluster,
				this.metricPrefix + metricName, metricValue);

		MetricWriter metricWriter = getMetricWriter(this.metricPrefix + metricName,
				aggregation,
				timeRollup,
				cluster);

		metricWriter.printMetric(String.valueOf(metricValue));

	}
}