package com.thed.zephyr.bamboo.plugin.task;

/**
 * @author mohan.kumar
 */

import static com.thed.zephyr.bamboo.utils.ZeeConstants.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.json.JSONObject;

import com.atlassian.bamboo.build.logger.BuildLogger;
import com.atlassian.bamboo.configuration.ConfigurationMap;
import com.atlassian.bamboo.results.tests.TestResults;
import com.atlassian.bamboo.task.CommonTaskContext;
import com.atlassian.bamboo.task.CommonTaskType;
import com.atlassian.bamboo.task.TaskContext;
import com.atlassian.bamboo.task.TaskException;
import com.atlassian.bamboo.task.TaskResult;
import com.atlassian.bamboo.task.TaskResultBuilder;
import com.atlassian.bamboo.v2.build.CurrentBuildResult;
import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import com.thed.zephyr.bamboo.model.TestCaseResultModel;
import com.thed.zephyr.bamboo.model.ZephyrConfigModel;
import com.thed.zephyr.bamboo.model.ZephyrInstance;
import com.thed.zephyr.bamboo.utils.rest.RestClient;
import com.thed.zephyr.bamboo.utils.rest.ServerInfo;
import com.thed.zephyr.bamboo.utils.rest.TestCaseUtil;

public class ZephyrReporterTask implements CommonTaskType {

	private PluginSettingsFactory pluginSettingsFactory;

	public ZephyrReporterTask(PluginSettingsFactory pluginSettingsFactory) {
		this.pluginSettingsFactory = pluginSettingsFactory;
	}

	@Override
	public TaskResult execute(CommonTaskContext taskContext) throws TaskException {
		BuildLogger buildLogger = taskContext.getBuildLogger();
		TaskResultBuilder taskResultBuilder = TaskResultBuilder.newBuilder(taskContext);
		
		 CurrentBuildResult buildResult = ((TaskContext) taskContext).getBuildContext().getBuildResult();

		if (buildResult == null
				|| ((buildResult.getSuccessfulTestResults() == null || buildResult.getSuccessfulTestResults().isEmpty()) && buildResult
						.getFailedTestResults() == null)
				|| ((buildResult.getFailedTestResults() == null || buildResult.getFailedTestResults().isEmpty()) && buildResult
						.getFailedTestResults() == null)) {
			buildLogger.addBuildLogEntry("Error parsing surefire reports.");
			buildLogger.addBuildLogEntry("Please ensure \"Publish JUnit test result report is added\" as a post build action");
			return taskResultBuilder.failed().build();
    	}

		
		buildLogger.addBuildLogEntry("##########################################################");
		buildLogger.addBuildLogEntry("Zephyr for JIRA Test Result Reporter is processing the results");
		buildLogger.addBuildLogEntry("##########################################################");
		
		ConfigurationMap config = taskContext.getConfigurationMap();
		
		 String serverAddress = config.get(SERVER_ADDRESS).trim();
		 String projectKey = config.get(PROJECT_KEY).trim();
		 String releaseKey = config.get(RELEASE_KEY).trim();
		 String cycleKey = config.get(CYCLE_KEY).trim();
		 String cycleDuration = config.get(CYCLE_DURATION).trim();
		 String cyclePrefix = config.get(CYCLE_PREFIX).trim();
		 
		 buildLogger.addBuildLogEntry("Server Address :" + serverAddress);
		 buildLogger.addBuildLogEntry("Project id : " + projectKey);
		 buildLogger.addBuildLogEntry("Version id : " + releaseKey);
		 if (cycleKey.equals(Long.toString(NEW_CYCLE_KEY_IDENTIFIER))) {
			 buildLogger.addBuildLogEntry("New cycle is selected");
		 } else {
			 buildLogger.addBuildLogEntry("Cycle id : " + cycleKey);
		 }
		 buildLogger.addBuildLogEntry("Cycle duration : " + cycleDuration);
		 buildLogger.addBuildLogEntry("Cycle prefix : " + cyclePrefix);
		 
			if (!validateBuildConfig(serverAddress, projectKey, releaseKey, cycleKey)) {
				buildLogger.addBuildLogEntry("Cannot Proceed. Please verify the job configuration");
				return taskResultBuilder.failed().build();
			}

			ZephyrConfigModel zephyrConfig = initializeZephyrData(serverAddress, projectKey, releaseKey, cycleKey, cycleDuration, cyclePrefix);
		

			if(!ServerInfo.findUserHasBrowseProjectPermission(zephyrConfig.getRestClient())) {
				buildLogger.addBuildLogEntry("User has no browse projects permission, hence cannot upload results");
				zephyrConfig.getRestClient().destroy();
				return taskResultBuilder.failed().build();
				
			}
			prepareZephyrTests(taskContext, zephyrConfig);
			
			try {
				TestCaseUtil.processTestCaseDetails(zephyrConfig);

	            zephyrConfig.getRestClient().destroy();
	            buildLogger.addBuildLogEntry("Done");

			} catch (Exception e) {
				e.printStackTrace();
				buildLogger.addBuildLogEntry("Done processing");
				zephyrConfig.getRestClient().destroy();
				return taskResultBuilder.failed().build();

			}
			
			
		 buildLogger.addBuildLogEntry("Done processing");
		return taskResultBuilder.success().build();
	}

	/**
	 * @param serverAddress
	 * @param projectKey
	 * @param releaseKey
	 * @param cycleKey
	 */
	private boolean validateBuildConfig(String serverAddress, String projectKey,
			String releaseKey, String cycleKey) {
		boolean valid = true;
		if (StringUtils.isBlank(serverAddress)
				|| StringUtils.isBlank(projectKey)
				|| StringUtils.isBlank(releaseKey)
				|| StringUtils.isBlank(cycleKey)
				|| ADD_ZEPHYR_GLOBAL_CONFIG.equals(serverAddress.trim())
				|| ADD_ZEPHYR_GLOBAL_CONFIG.equals(projectKey.trim())
				|| ADD_ZEPHYR_GLOBAL_CONFIG.equals(releaseKey.trim())
				|| ADD_ZEPHYR_GLOBAL_CONFIG.equals(cycleKey.trim())) {
			valid = false;
		}
		
		return valid;
	}

	/**
	 * @param taskContext
	 * @param zephyrData
	 */
	private void prepareZephyrTests(CommonTaskContext taskContext,
			ZephyrConfigModel zephyrConfig) {
		Map<String, Boolean> zephyrTestCaseMap = prepareTestResults(taskContext);
 

		taskContext.getBuildLogger().addBuildLogEntry("Total Test Cases : " + zephyrTestCaseMap.size());
		List<TestCaseResultModel> testcases = new ArrayList<TestCaseResultModel>();

		
		Set<String> keySet = zephyrTestCaseMap.keySet();
		
		for (Iterator<String> iterator = keySet.iterator(); iterator.hasNext();) {
			String testCaseName = iterator.next();
			Boolean isPassed = zephyrTestCaseMap.get(testCaseName);
			
			
			JSONObject isssueType = new JSONObject();
			isssueType.put("id", zephyrConfig.getTestIssueTypeId()+"");
			
			JSONObject project = new JSONObject();
			project.put("id", zephyrConfig.getZephyrProjectId());

			JSONObject fields = new JSONObject();
			fields.put("project", project);
			fields.put("summary", testCaseName);
			fields.put("description", "Creating the Test via Bamboo");
			fields.put("issuetype", isssueType);
			
			JSONObject issue = new JSONObject();
			issue.put("fields", fields);
			
			TestCaseResultModel caseWithStatus = new TestCaseResultModel();
			caseWithStatus.setPassed(isPassed);
			caseWithStatus.setTestCase(issue.toString());
			caseWithStatus.setTestCaseName(testCaseName);
			testcases.add(caseWithStatus);
		}
		
		zephyrConfig.setTestcases(testcases);
	}

	/**
	 * @param taskContext
	 * @return
	 */
	private Map<String, Boolean> prepareTestResults(
			CommonTaskContext taskContext) {
		Map<String, Boolean> zephyrTestCaseMap = new HashMap<String, Boolean>();

		
 CurrentBuildResult buildResult = ((TaskContext) taskContext).getBuildContext().getBuildResult();
 Collection<TestResults> failedTestResults = buildResult.getFailedTestResults();
 Collection<TestResults> passedTestResults = buildResult.getSuccessfulTestResults();

 for (Iterator<TestResults> iterator = failedTestResults.iterator(); iterator
			.hasNext();) {
		TestResults testResults = iterator.next();
		zephyrTestCaseMap.put(testResults.getClassName()+"."+testResults.getActualMethodName(), false);
		
}
 
 for (Iterator<TestResults> iterator = passedTestResults.iterator(); iterator
				.hasNext();) {
			TestResults testResults = iterator.next();
			zephyrTestCaseMap.put(testResults.getClassName()+"."+testResults.getActualMethodName(), true);
			
		}
		return zephyrTestCaseMap;
	}

	/**
	 * @param serverAddress
	 * @param projectKey
	 * @param releaseKey
	 * @param cycleKey
	 * @param cycleDuration
	 * @param cyclePrefix
	 * @param createPackage
	 * @return
	 */
	private ZephyrConfigModel initializeZephyrData(String serverAddress, String projectKey, String releaseKey, String cycleKey, String cycleDuration,
			 String cyclePrefix) {
		
		 ZephyrInstance instance = prepareUserCredentials(serverAddress);


		ZephyrConfigModel zephyrConfig = new ZephyrConfigModel();
		zephyrConfig .setRestClient(new RestClient(instance));
		

		zephyrConfig.setZephyrProjectId(Long.parseLong(projectKey));
		zephyrConfig.setVersionId(Long.parseLong(releaseKey));
		zephyrConfig.setCycleId(Long.parseLong(cycleKey));
		zephyrConfig.setCycleDuration(cycleDuration);
		zephyrConfig.setCyclePrefix(determineCyclePrefix(cyclePrefix));
		
        determineTestIssueTypeId(zephyrConfig);

		return zephyrConfig;
	}

	private void determineTestIssueTypeId(ZephyrConfigModel zephyrConfig) {
		long testIssueTypeId = ServerInfo.findTestIssueTypeId(zephyrConfig.getRestClient());
		zephyrConfig.setTestIssueTypeId(testIssueTypeId);
			
	}

	private String determineCyclePrefix(String cyclePrefix) {
		if (StringUtils.isNotBlank(cyclePrefix)) {
			return cyclePrefix + "_";
		} else {
			return CYCLE_PREFIX_DEFAULT;
		}
	}

	private ZephyrInstance prepareUserCredentials(String serverAddr) {
		ZephyrInstance instance = new ZephyrInstance();

		PluginSettings settings = pluginSettingsFactory
				.createGlobalSettings();
		
		@SuppressWarnings("unchecked")
		List<Map<String, String>> credList=  (List<Map<String, String>>) settings.get(PLUGIN_KEY + ".zephyrserverconfig");
		
		if(credList != null && credList.size() > 0) {
			for (Iterator<Map<String, String>> iterator = credList.iterator(); iterator.hasNext();) {
				Map<String, String> map = iterator.next();
				String serverAddrFromStore = map.get("serverAddr");
				
				if (serverAddrFromStore.equals(serverAddr)) {
					String userName = map.get("user");
					String password = map.get("password");
					
					instance.setServerAddress(serverAddr);
					instance.setUsername(userName);
					instance.setPassword(password);

				}
			}
		}
		
		return instance;
	}
	
	public static String findPackageName(String testCaseNameWithPackage) {
		String packageName = "";

		if (StringUtils.isBlank(testCaseNameWithPackage)) {
			return packageName;
		}

		String[] split = testCaseNameWithPackage.split("\\.");

		int splitCount = split.length;

		for (int i = 0; i < split.length; i++) {
			if (splitCount > 2) {

				if (i == splitCount - 2)
					break;

				packageName += split[i];
				packageName += ".";
			}
		}

		String removeEnd = StringUtils.removeEnd(packageName, ".");
		return removeEnd;
	}
}
