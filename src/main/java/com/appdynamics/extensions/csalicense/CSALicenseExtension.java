package com.appdynamics.extensions.csalicense;

import java.io.FileInputStream;
import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.yaml.snakeyaml.Yaml;

import com.appdynamics.extensions.csalicense.model.ControllerInfo;
import com.appdynamics.extensions.csalicense.model.MetricToPublish;
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

	public static Map<String, MetricToPublish> listControllerLicensedHistoric;

	private String metricPrefix = "Custom Metrics|CSA-License|";

	@Override
	public TaskOutput execute(Map<String, String> configMap, TaskExecutionContext taskExecutionContext) throws TaskExecutionException {

		this.logger = taskExecutionContext.getLogger();

		if (configMap.getOrDefault("config-file", "unconfigured").equals("unconfigured")) {
			throw new TaskExecutionException("Confluent Config File Not Set, nothing to do");
		}

		SummaryThread summaryThread = new SummaryThread(configMap, taskExecutionContext, this.metricPrefix);
		summaryThread.start();

		CSALicenseExtension.listControllerLicensedHistoric = new ConcurrentHashMap<String, MetricToPublish>();
		while (true) {
			logger.info("{} ==> Publishing metrics values...", Common.getLogHeader(this, "run"));

			Instant startTask = Instant.now();
			int totalVCPU = 0;

			try {
				for (String controllerHost : CSALicenseExtension.listControllerLicensedHistoric.keySet()) {
					MetricToPublish metricToPublish = CSALicenseExtension.listControllerLicensedHistoric.get(controllerHost);

					logger.debug("{}    Controller data [{}], connected {}, Allowed CSA {}", Common.getLogHeader(this, "run"),
							metricToPublish.getControllerHost(), metricToPublish.isAvaliable(), metricToPublish.isAllowedCSA());

					totalVCPU += metricToPublish.getTotalVCPU();

					logger.info("{}    vCPU {} on Controller {}", Common.getLogHeader(this, "run"), metricToPublish.getTotalVCPU(),
							metricToPublish.getControllerHost());

					String metricName = metricToPublish.getControllerHost() + "|";

					publicMetric(metricName + "Avaliable", metricToPublish.isAvaliable() ? 1 : 0, MetricWriter.METRIC_AGGREGATION_TYPE_OBSERVATION,
							MetricWriter.METRIC_TIME_ROLLUP_TYPE_CURRENT, MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_COLLECTIVE);

					publicMetric(metricName + "CSA Allowed", metricToPublish.isAllowedCSA() ? 1 : 0, MetricWriter.METRIC_AGGREGATION_TYPE_OBSERVATION,
							MetricWriter.METRIC_TIME_ROLLUP_TYPE_CURRENT, MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_COLLECTIVE);

					publicMetric(metricName + "License Used", metricToPublish.getTotalVCPU(), MetricWriter.METRIC_AGGREGATION_TYPE_OBSERVATION,
							MetricWriter.METRIC_TIME_ROLLUP_TYPE_CURRENT, MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_COLLECTIVE);

				}

			} catch (Exception e) {
				logger.error("{} {}...", Common.getLogHeader(this, "run"), e.getMessage(), e);
			}

			try {

				publicMetric("License Used", totalVCPU, MetricWriter.METRIC_AGGREGATION_TYPE_OBSERVATION, MetricWriter.METRIC_TIME_ROLLUP_TYPE_CURRENT,
						MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_COLLECTIVE);

				logger.info("{}    vCPU on all controllers {}, it took {}s to publish metrics, waiting next round (1 minute)", Common.getLogHeader(this, "run"),
						totalVCPU, Duration.between(startTask, Instant.now()).getSeconds());

			} catch (Exception e) {
				logger.error("{} {}...", Common.getLogHeader(this, "run"), e.getMessage(), e);
			}

			try {
				Thread.sleep(60000);
			} catch (Exception e) {
				logger.error("{} {}...", Common.getLogHeader(this, "run"), e.getMessage(), e);
			}
		}

	}

	protected void publicMetric(String metricName, Object metricValue, String aggregation, String timeRollup, String cluster) throws Exception {
		logger.debug("Printing Metric [{}/{}/{}] [{}]=[{}]", aggregation, timeRollup, cluster, this.metricPrefix + metricName, metricValue);

		MetricWriter metricWriter = getMetricWriter(this.metricPrefix + metricName, aggregation, timeRollup, cluster);

		metricWriter.printMetric(String.valueOf(metricValue));

	}

	class SummaryThread extends Thread {

		private Logger logger = LogManager.getFormatterLogger();

		private Map<String, String> configMap;
		private TaskExecutionContext taskExecutionContext;
		private String metricPrefix;

		private ControllerInfo[] listControllerInfo;
		private Map<String, ControllerService> listControllerService;

		SummaryThread(Map<String, String> configMap, TaskExecutionContext taskExecutionContext, String metricPrefix) {
			this.logger = taskExecutionContext.getLogger();

			this.configMap = configMap;
			this.taskExecutionContext = taskExecutionContext;
			this.metricPrefix = metricPrefix;
		}

		@Override
		public void run() {

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
					InputStream inputStream = new FileInputStream(this.taskExecutionContext.getTaskDir() + "/" + this.configMap.get("config-file"));
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

						if (ci.isCreateDashboard() != null && ci.isCreateDashboard()) {
							cs.createDashboard(this.taskExecutionContext.getTaskDir());
						}
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

				// RECUPERANDO O RESULTADO E SALVANDO EM CACHE PARA SER PUBLICADO A CADA MINUTO ATÉ A PRÓXIMA ATUALIZAÇÃO
				// NECESSÁRIO PARA O GRÁFICO (TIMESERIES) NÃO FICAR COM GAPS
				try {
					CSALicenseExtension.listControllerLicensedHistoric = new ConcurrentHashMap<String, MetricToPublish>();
					for (ControllerInfo ci : listControllerInfo) {
						int totalVCPUController = 0;
						ControllerService controllerService = listControllerService.get(ci.getControllerHost());
						logger.debug("{}    Dados da Controller {}, connected {}, Allowed CSA {}", Common.getLogHeader(this, "run"), ci.getControllerHost(),
								controllerService.controllerInfo.isAvaliable(), controllerService.controllerInfo.isAllowedCSA());

						for (String serverName : controllerService.listServersLicensed.keySet()) {
							totalVCPUController += controllerService.listServersLicensed.get(serverName);
							logger.debug("{}    - Servidor {} e vCPU {}", Common.getLogHeader(this, "run"), serverName,
									controllerService.listServersLicensed.get(serverName));
						}

						MetricToPublish metricToPublish = new MetricToPublish();
						metricToPublish.setControllerHost(ci.getControllerHost().split("//")[1]);
						metricToPublish.setIsAvaliable(controllerService.controllerInfo.isAvaliable());
						metricToPublish.setIsAllowedCSA(controllerService.controllerInfo.isAllowedCSA());
						metricToPublish.setTotalVCPU(totalVCPUController);
						CSALicenseExtension.listControllerLicensedHistoric.put(ci.getControllerHost(), metricToPublish);

						logger.info("{}    vCPU {} on Controller {}", Common.getLogHeader(this, "run"), metricToPublish.getTotalVCPU(),
								metricToPublish.getControllerHost());

					}
				} catch (Exception exception) {
					exception.printStackTrace();
				}

				try {

					this.logger.info(
							"{} Final execution time {}s, waiting for the next round in the next {} minutes. The metrics will now be published every minute until the next round.",
							Common.getLogHeader(this, "run"), Duration.between(startTime, Instant.now()).getSeconds(), frequency);

					Thread.sleep(frequency * 60000);

				} catch (Exception exception) {
					exception.printStackTrace();
				}

			}

		}

	}
}