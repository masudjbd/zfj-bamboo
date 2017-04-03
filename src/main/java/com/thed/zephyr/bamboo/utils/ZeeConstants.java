package com.thed.zephyr.bamboo.utils;

public class ZeeConstants {
	
	private ZeeConstants() {
		
	}

	public static final String PLUGIN_KEY = "com.thed.zephyr.zfj-bamboo";
	public static final String NAME_POST_BUILD_ACTION = "Publish test result to Zephyr Enterprise";
	public static final String ADD_ZEPHYR_GLOBAL_CONFIG = "Please Add Zephyr Server in the Global config";
	public static final String ZEPHYR_DEFAULT_MANAGER = "test.manager";
	public static final String ZEPHYR_DEFAULT_PASSWORD = "test.manager";
	public static final String NEW_CYCLE_KEY = "CreateNewCycle";
	public static final String CYCLE_PREFIX_DEFAULT = "Automation_";
	public static final String TEST_CASE_TAG = "API";
	public static final String TEST_CASE_PRIORITY = "1";
	public static final String EXTERNAL_ID = "99999";
	public static final String TEST_CASE_COMMENT = "Created via Jenkins Zephyr Plugin!";
	public static final String CYCLE_DURATION_1_DAY = "1 day";
	public static final String CYCLE_DURATION_7_DAYS = "7 days";
	public static final String CYCLE_DURATION_30_DAYS = "30 days";
	public static final String CONNECTION_ERROR = "Could not establish the connection";
	public static final String NO_SERVER_CONFIGURED = "No Zephyr server configured";
	public static final String SUCCESS = "success";
	public static final String NO_SAVE_NO_SERVER_CONFIGURED = "Cannot Save Task : No Zephyr server configured";
	public static final String NO_PROJECT = "No Project selected";
	public static final String NO_RELEASE = "No Release selected";
	public static final String NO_CYCLE = "No Cycle selected";

	
	public static final long NEW_CYCLE_KEY_IDENTIFIER = 1000000000L;
	public static final String NEW_CYCLE = "New Cycle";
	
	public static final boolean AUTOMATED = false;

	public static final String CREATE_PACKAGE = "createPackage";
	public static final String CYCLE_PREFIX = "cyclePrefix";
	public static final String CYCLE_DURATION = "cycleDuration";
	public static final String CYCLE_KEY = "cycleKey";
	public static final String RELEASE_KEY = "releaseKey";
	public static final String PROJECT_KEY = "projectKey";
	public static final String SERVER_ADDRESS = "serverAddress";

}
