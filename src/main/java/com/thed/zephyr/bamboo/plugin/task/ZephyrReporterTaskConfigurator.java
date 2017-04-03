package com.thed.zephyr.bamboo.plugin.task;

/**
 * @author mohan.kumar
 */
import static com.thed.zephyr.bamboo.utils.ZeeConstants.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.atlassian.bamboo.collections.ActionParametersMap;
import com.atlassian.bamboo.task.AbstractTaskConfigurator;
import com.atlassian.bamboo.task.TaskConfigurationService;
import com.atlassian.bamboo.task.TaskDefinition;
import com.atlassian.bamboo.utils.error.ErrorCollection;
import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import com.atlassian.sal.api.transaction.TransactionTemplate;
import com.atlassian.sal.api.user.UserManager;
import com.thed.zephyr.bamboo.CredentialData;
import com.thed.zephyr.bamboo.model.ZephyrConfigModel;
import com.thed.zephyr.bamboo.utils.URLValidator;
import com.thed.zephyr.bamboo.utils.rest.Cycle;
import com.thed.zephyr.bamboo.utils.rest.Project;
import com.thed.zephyr.bamboo.utils.rest.RestClient;
import com.thed.zephyr.bamboo.utils.rest.ServerInfo;
import com.thed.zephyr.bamboo.utils.rest.Version;

public class ZephyrReporterTaskConfigurator extends AbstractTaskConfigurator {

	String cycleNamePrefixMap;

	private Logger logger = Logger.getLogger(ZephyrReporterTaskConfigurator.class);
	PluginSettingsFactory pluginSettingsFactory;
	private TaskConfigurationService taskConfigurationService;
	public ZephyrReporterTaskConfigurator(UserManager userManager,
			PluginSettingsFactory pluginSettingsFactory,
			TransactionTemplate transactionTemplate,
			TaskConfigurationService taskConfigurationService) {
		super();
		this.pluginSettingsFactory = pluginSettingsFactory;
		this.taskConfigurationService = taskConfigurationService;
	}
	@Override
	public Map<String, String> generateTaskConfigMap(
			ActionParametersMap params, TaskDefinition previousTaskDefinition) {
		Map<String, String> config = super.generateTaskConfigMap(params, previousTaskDefinition);
		config.put("serverAddress", params.getString("serverAddress"));
		config.put("projectKey", params.getString("projectKey"));
		config.put("releaseKey", params.getString("releaseKey"));
		config.put("cycleKey", params.getString("cycleKey"));
		config.put("cycleDuration", params.getString("cycleDuration"));
		config.put("cyclePrefix", params.getString("cyclePrefix"));

		return config;
	}

	@Override
	public void populateContextForView(Map<String, Object> context,
			TaskDefinition taskDefinition) {

		context.put("serverAddress", taskDefinition.getConfiguration().get("serverAddress"));
		context.put("projectKey", taskDefinition.getConfiguration().get("projectKey"));
		context.put("releaseKey", taskDefinition.getConfiguration().get("releaseKey"));
		context.put("cycleKey", taskDefinition.getConfiguration().get("cycleKey"));
		context.put("cycleDuration", taskDefinition.getConfiguration().get("cycleDuration"));
		context.put("cyclePrefix", taskDefinition.getConfiguration().get("cyclePrefix"));

		super.populateContextForView(context, taskDefinition);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void populateContextForEdit(Map<String, Object> context,
			TaskDefinition taskDefinition) {
		super.populateContextForEdit(context, taskDefinition);
		context.put("serverValid", SUCCESS);
		Map<String, String> cycleDurationMap = new HashMap<String, String>();
		Map<String, String> serverMap = new HashMap<String, String>();
		Map<Long, String> projectMap = new HashMap<Long, String>();
		Map<Long, String> releaseMap = new HashMap<Long, String>();
		Map<Long, String> cycleMap = new HashMap<Long, String>();

		String serverAddress = taskDefinition.getConfiguration().get("serverAddress");

		if (StringUtils.isEmpty(serverAddress)) {
			context.put("serverValid", NO_SERVER_CONFIGURED);
			return;
		} else if (URLValidator.validateURL(serverAddress).equals(CONNECTION_ERROR)) {
			context.put("serverValid", CONNECTION_ERROR);
			return;
		}

		context.put("serverAddress", serverAddress);
		context.put("projectKey", taskDefinition.getConfiguration().get("projectKey"));
		context.put("releaseKey", taskDefinition.getConfiguration().get("releaseKey"));
		context.put("cycleKey", taskDefinition.getConfiguration().get("cycleKey"));
		context.put("cycleDuration", taskDefinition.getConfiguration().get("cycleDuration"));
		context.put("cyclePrefix", taskDefinition.getConfiguration().get("cyclePrefix"));

		String projectKey = null;
		String releaseKey = null;
		try {
			projectKey = (String) context.get("projectKey");
			releaseKey = (String) context.get("releaseKey");

		} catch (Exception e) {
			logger.info("Project Key or Release Key is not found in the configuration");
			if (logger.isDebugEnabled()) {
				logger.debug("Project Key or Release Key is not found in the configuration");
				logger.debug(e.getMessage());
			}
		}

		PluginSettings settings = pluginSettingsFactory
				.createGlobalSettings();

		List<CredentialData> configs = new ArrayList<CredentialData>();
		List<Map<String, String>> credList = null;
		try {
			credList =  (List<Map<String, String>>) settings.get(PLUGIN_KEY + ".zephyrserverconfig");
			if(credList == null || credList.size() == 0) {

				logger.info("server configuration is not found");
				context.put("serverValid", "Server configuration is not found. It may have been removed from the configuration");
				return;
			}
		} catch (Exception e) {
			if (logger.isDebugEnabled()) {
				logger.debug("Error fetching server configuration details");
				logger.debug(e.getMessage());
			}
		}

		for (Iterator<Map<String, String>> iterator = credList.iterator(); iterator.hasNext();) {
			Map<String, String> map = iterator.next();

			serverMap.put(map.get("serverAddr"), map.get("serverAddr"));
			CredentialData cData = new CredentialData();

			cData.setServerAddr(map.get("serverAddr"));
			cData.setUser(map.get("user"));
			cData.setPass(map.get("password"));
			cData.setStatusMsg(map.get("statusMsg"));
			cData.setStatus(true);
			configs.add(cData);
		}

		context.put("serverMap", serverMap);

		RestClient restClient = null;
		projectMap = null;
		String userName = null;
		String password = null;
		try {

			if (serverAddress == null || serverAddress.trim().equals("")) {
				serverAddress = serverMap.entrySet().iterator().next().getValue();
			}

			boolean serverConfigExists = false;
			for(CredentialData cr: configs) {
				if(cr.getServerAddr().equalsIgnoreCase(serverAddress)) {
					userName = cr.getUser();
					password = cr.getPass();
					serverConfigExists = true;
					break;
				}
			}

			if (!serverConfigExists) {
				logger.info("server configuration is not found");
				context.put("serverValid", "Server configuration is not found. It may have been removed from the configuration");
				return;
			}
			restClient = RestClient.getRestclient(serverAddress, userName, password);
			
			if (!ServerInfo.findUserHasBrowseProjectPermission(restClient)) {
				logger.info("User has no browse projects permission");
				context.put("serverValid", "User " + userName + " has no browse projects permission (" + serverAddress +")");
				return;
			}
			projectMap = Project.getAllProjects(restClient);

			if (projectMap == null || projectMap.size() == 0) {
				logger.info("server configuration is not found");
				context.put("serverValid", "No Projects");
				return;
			}

		} catch(Exception e) {
			logger.info(e.getMessage());
			logger.info("Error fetching projects");

			if(logger.isDebugEnabled()) {
				logger.debug(e.getLocalizedMessage());
			}
			return;
		}

		finally {
			//			ConfigResource.closeHTTPClient(restClient);
		}


		long projectID;
		try {
			projectID = projectMap.entrySet().iterator().next().getKey();
			try {
				if (projectKey != null) {
					projectID = Long.parseLong(projectKey);
				}
			} catch (Exception e) {
			}
			releaseMap = Version.getVersionsByProjectID(projectID, restClient);
		} finally {
			//			ConfigResource.closeHTTPClient(restClient);
		}

		cycleMap.put(NEW_CYCLE_KEY_IDENTIFIER, NEW_CYCLE);
		try {
			long releaseID = releaseMap.entrySet().iterator().next().getKey();

			try {
				if (releaseKey != null) {
					releaseID = Long.parseLong(releaseKey);
				}
			} catch (Exception e) {
			}

			cycleMap.putAll(Cycle.getAllCyclesByVersionId(releaseID, restClient, projectKey));
		} finally {
			//			ConfigResource.closeHTTPClient(restClient);
		}

		ZephyrConfigModel zephyrData = new ZephyrConfigModel();
		zephyrData.setZephyrProjectId(projectID);

		cycleDurationMap.put(CYCLE_DURATION_30_DAYS, CYCLE_DURATION_30_DAYS);
		cycleDurationMap.put(CYCLE_DURATION_7_DAYS, CYCLE_DURATION_7_DAYS);
		cycleDurationMap.put(CYCLE_DURATION_1_DAY, CYCLE_DURATION_1_DAY);

		context.put("serverMap", serverMap);
		context.put("projectMap", projectMap);
		context.put("releaseMap", releaseMap);
		context.put("cycleMap", cycleMap);
		context.put("cycleDurationMap", cycleDurationMap);
	}

	@Override
	public void populateContextForCreate(Map<String, Object> context) {
		super.populateContextForCreate(context);
		context.put("serverValid", SUCCESS);
		PluginSettings settings = pluginSettingsFactory
				.createGlobalSettings();
		Map<String, String> serverMap = new HashMap<String, String>();
		Map<String, String> projectMap = new HashMap<String, String>();
		Map<String, String> releaseMap = new HashMap<String, String>();
		Map<String, String> cycleMap = new HashMap<String, String>();


		@SuppressWarnings("unchecked")
		List<Map<String, String>> credList=  (List<Map<String, String>>) settings.get(PLUGIN_KEY + ".zephyrserverconfig");

		if(credList != null && credList.size() > 0) {
			for (Iterator<Map<String, String>> iterator = credList.iterator(); iterator.hasNext();) {
				Map<String, String> map = iterator.next();
				String serverAddr = map.get("serverAddr");
				serverMap.put(serverAddr, serverAddr);
			}
		} else {
			context.put("serverValid", NO_SERVER_CONFIGURED);
			return;
		}

		Map<String, String> cycleDurationMap = new HashMap<String, String>();

		context.put("serverMap", serverMap);
		context.put("projectMap", projectMap);
		context.put("releaseMap", releaseMap);
		context.put("cycleMap", cycleMap);
		context.put("cycleDurationMap", cycleDurationMap);
	}

	@Override
	public void validate(ActionParametersMap params,
			ErrorCollection errorCollection) {
		super.validate(params, errorCollection);

		String serverAddress = params.getString("serverAddress");
		if (StringUtils.isEmpty(serverAddress)) {
			errorCollection.addErrorMessage(NO_SERVER_CONFIGURED);
			return;
		}
		String projectKey = params.getString("projectKey");
		if (StringUtils.isEmpty(projectKey)) {
			errorCollection.addErrorMessage(NO_PROJECT);
			return;
		}
		String releaseKey = params.getString("releaseKey");
		if (StringUtils.isEmpty(releaseKey)) {
			errorCollection.addErrorMessage(NO_RELEASE);
			return;
		}
		String cycleKey = params.getString("cycleKey");
		if (StringUtils.isEmpty(cycleKey)) {
			errorCollection.addErrorMessage(NO_CYCLE);
			return;
		}
	}
}
