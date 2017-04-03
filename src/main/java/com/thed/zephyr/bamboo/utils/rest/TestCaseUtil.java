package com.thed.zephyr.bamboo.utils.rest;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.eclipse.jetty.util.log.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.thed.zephyr.bamboo.model.TestCaseResultModel;
import com.thed.zephyr.bamboo.model.ZephyrConfigModel;

public class TestCaseUtil {
	
	private static Logger log = Logger.getLogger(TestCaseUtil.class);
    public static final long NEW_CYCLE_KEY_IDENTIFIER = 1000000000L;


	private static final String URL_GET_ALL_TESTS = "{SERVER}/rest/api/2/search";
	private static final String URL_CREATE_TESTS = "{SERVER}/rest/api/2/issue/bulk";
	private static final String URL_ASSIGN_TESTS = "{SERVER}/rest/zapi/latest/execution/addTestsToCycle/";
	private static final String URL_CREATE_EXECUTIONS_URL = "{SERVER}/rest/zapi/latest/execution?projectId={projectId}&versionId={versionId}&cycleId={cycleId}";
	private static final String URL_EXECUTE_TEST = "{SERVER}/rest/zapi/latest/execution/updateBulkStatus";
	private static final String JQL_SEARCH_TESTS = "jql=project={pId}&issuetype={issueTypeId}&maxResults=1000&startAt={searchIssueStartcount}";
	
	private static final String URL_GET_CYCLES = "{SERVER}/rest/zapi/latest/cycle";

	public static Map<Long, Map<String, Boolean>> getTestCaseDetails(ZephyrConfigModel zephyrData) {

		JSONObject bulkIssues = new JSONObject();
		JSONArray issueUpdates = new JSONArray();

		Map<Long, Map<String, Boolean>> testCaseResultMap = new HashMap<Long, Map<String, Boolean>>();
		List<TestCaseResultModel> testCases = zephyrData.getTestcases();
		if (testCases == null || testCases.size() == 0) {
			return testCaseResultMap;
		}
		
			
		Map<String, Map<Long, String>> searchedTests = searchIssues(zephyrData);
		
		for (Iterator<TestCaseResultModel> iterator = testCases.iterator(); iterator.hasNext();) {
			TestCaseResultModel testCaseWithStatus = (TestCaseResultModel) iterator
					.next();
			
				if (searchedTests.containsKey(testCaseWithStatus.getTestCaseName())) {
					
					Map<Long, String> tempTestIdTestKeyMap = searchedTests.get(testCaseWithStatus.getTestCaseName());
					Set<Entry<Long, String>> entrySet = tempTestIdTestKeyMap.entrySet();
					Entry<Long, String> entry = entrySet.iterator().next();
					
					
					Map<String, Boolean> map = new HashMap<String, Boolean>();
					map.put(entry.getValue(), testCaseWithStatus.getPassed());
					testCaseResultMap.put(entry.getKey(), map);
				} else {
					String testCase = testCaseWithStatus.getTestCase();
					JSONObject issue = new JSONObject(testCase);
					issueUpdates.put(issue);
				}
	}
		
		


		bulkIssues.put("issueUpdates", issueUpdates);
		
		String createURL = URL_CREATE_TESTS.replace("{SERVER}", zephyrData.getRestClient().getUrl());
		HttpPost httpPost = new HttpPost(createURL);
		httpPost.addHeader("Content-Type", "application/json");
		
		HttpResponse response1 = null;
		try {
			StringEntity se = new StringEntity(bulkIssues.toString());
			httpPost.setEntity(se);
			
			response1 = zephyrData.getRestClient().getHttpclient().execute(httpPost);
		} catch (UnsupportedEncodingException e1) {
			e1.printStackTrace();
		} catch (ClientProtocolException e1) {
			e1.printStackTrace();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
		int statusCode1 = response1.getStatusLine().getStatusCode();

		if (statusCode1 >= 200 && statusCode1 < 300) {
			
			Map<String, Map<Long, String>> searchedTestsAfterCreation = searchIssues(zephyrData);
			
			for (Iterator<TestCaseResultModel> iterator = testCases.iterator(); iterator.hasNext();) {
				TestCaseResultModel testCaseWithStatus = (TestCaseResultModel) iterator.next();
				
				if (searchedTestsAfterCreation.containsKey(testCaseWithStatus.getTestCaseName())) {
					
					Map<Long, String> tempTestIdTestKeyMap = searchedTestsAfterCreation.get(testCaseWithStatus.getTestCaseName());
					Set<Entry<Long, String>> entrySet = tempTestIdTestKeyMap.entrySet();
					Entry<Long, String> entry = entrySet.iterator().next();
					
					
					Map<String, Boolean> map = new HashMap<String, Boolean>();
					map.put(entry.getValue(), testCaseWithStatus.getPassed());
					testCaseResultMap.put(entry.getKey(), map);
				}
							
		}
		}
		return testCaseResultMap;
	}
	
	public static Map<Long, String> getAllCyclesByVersionId(ZephyrConfigModel zephyrData) {


		Map<Long, String> cycles = new TreeMap<Long, String>();
		
		HttpResponse response = null;
		
		final String url = URL_GET_CYCLES.replace("{SERVER}", zephyrData.getRestClient().getUrl()) + "?projectId=" + zephyrData.getZephyrProjectId() + "&versionId=" + zephyrData.getVersionId();
		try {
			response = zephyrData.getRestClient().getHttpclient().execute(new HttpGet(url));
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		int statusCode = response.getStatusLine().getStatusCode();

		if (statusCode >= 200 && statusCode < 300) {
			HttpEntity entity = response.getEntity();
			String string = null;
			try {
				string = EntityUtils.toString(entity);
			} catch (ParseException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}

			
			try {
				JSONObject projObj = new JSONObject(string);
				for(int i = 0; i < projObj.length(); i++) {
					Iterator<String> keys = projObj.keys();
					
					while (keys.hasNext()) {
						String key = (String) keys.next();
						if (!key.trim().equals("-1")) {
							JSONObject cycleObject = projObj.getJSONObject(key);
							String cycleName = cycleObject.getString("name");
							long id = Long.parseLong(key);
							cycles.put(id, cycleName);
						}
					}
					
				}
				
				
			} catch (JSONException e) {
				e.printStackTrace();
			}
			
			
		} else {
			
			cycles.put(0L, "No Cycle");
			try {
				throw new ClientProtocolException("Unexpected response status: "
						+ statusCode);
			} catch (ClientProtocolException e) {
				e.printStackTrace();
			}
		}
	
		return cycles;
	}

	public static Long assignTests(ZephyrConfigModel zephyrData, JSONObject jsonObject) {

		Long cycleId = 0L;

		HttpResponse response = null;
		try {
			String assignTestsToCycleURL = URL_ASSIGN_TESTS.replace("{SERVER}", zephyrData.getRestClient().getUrl());
			
		
			StringEntity se = new StringEntity(jsonObject.toString());
			
			HttpPost createCycleRequest = new HttpPost(assignTestsToCycleURL);
			
			createCycleRequest.setHeader("Content-Type", "application/json");
			createCycleRequest.setEntity(se);
			response = zephyrData.getRestClient().getHttpclient().execute(createCycleRequest);
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		int statusCode = response.getStatusLine().getStatusCode();

		if (statusCode >= 200 && statusCode < 300) {
			HttpEntity entity = response.getEntity();
			String string = null;
			try {
				string = EntityUtils.toString(entity);
			} catch (ParseException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}

			
			
		} else {
			try {
				throw new ClientProtocolException("Unexpected response status: "
						+ statusCode);
			} catch (ClientProtocolException e) {
				e.printStackTrace();
			}
		}
	
		return cycleId;
	}
	
	public static Map<String, Long> fetchExecutionIds(ZephyrConfigModel zephyrData, JSONObject jsonObject) {

		Map<String, Long> issueKeyExecutionIdMap = new HashMap<String, Long>();

		HttpResponse response = null;
		try {
			
			String executionsURL = URL_CREATE_EXECUTIONS_URL.replace("{SERVER}", zephyrData.getRestClient().getUrl()).replace("{projectId}", zephyrData.getZephyrProjectId()+"").replace("{versionId}", zephyrData.getVersionId()+"").replace("{cycleId}", zephyrData.getCycleId()+"");
		
			HttpGet executionsURLRequest = new HttpGet(executionsURL);
			response = zephyrData.getRestClient().getHttpclient().execute(executionsURLRequest);
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		int statusCode = response.getStatusLine().getStatusCode();

		if (statusCode >= 200 && statusCode < 300) {
			HttpEntity entity = response.getEntity();
			String string = null;
			try {
				string = EntityUtils.toString(entity);
			} catch (ParseException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}

			
			try {
				JSONObject executionObject = new JSONObject(string);
				JSONArray executions = executionObject.getJSONArray("executions");
				
				for (int i = 0; i < executions.length(); i++) {
					JSONObject execution = executions.getJSONObject(i);
					String issueKey = execution.getString("issueKey").trim();
					long executionId = execution.getLong("id");
					
					if (!issueKeyExecutionIdMap.containsKey(issueKey)) {
						issueKeyExecutionIdMap.put(issueKey, executionId);
					}
				}
				
			} catch (JSONException e) {
				e.printStackTrace();
			}
			
			
		} else {
			try {
				throw new ClientProtocolException("Unexpected response status: "
						+ statusCode);
			} catch (ClientProtocolException e) {
				e.printStackTrace();
			}
		}
	
		return issueKeyExecutionIdMap;
	}
	
	public static void executeTests(ZephyrConfigModel zephyrData, List<Long> passList, List<Long> failList) {


		CloseableHttpResponse response = null;
		try {
			String bulkExecuteTestsURL = URL_EXECUTE_TEST.replace("{SERVER}", zephyrData.getRestClient().getUrl());
			
			
			if (failList.size() > 0) {
				JSONArray failedTests = new JSONArray();
				JSONObject failObj = new JSONObject();
				
				for (long failedTest: failList) {
					failedTests.put(failedTest);
				}
				failObj.put("executions", failedTests);
				failObj.put("status", 2);
				StringEntity failEntity = new StringEntity(failObj.toString());
				HttpPut bulkUpdateFailedTests = new HttpPut(bulkExecuteTestsURL);
				bulkUpdateFailedTests.setHeader("Content-Type", "application/json");
				bulkUpdateFailedTests.setEntity(failEntity);
				response = zephyrData.getRestClient().getHttpclient().execute(bulkUpdateFailedTests);
			}
			
		
			if (passList.size() > 0) {
				if(response != null) {
					response.close();
				}
			
			JSONArray passedTests = new JSONArray();
			JSONObject passObj = new JSONObject();
			for (long passedTest: passList) {
				passedTests.put(passedTest);
			}
			passObj.put("executions", passedTests);
			passObj.put("status", 1);
			StringEntity passEntity = new StringEntity(passObj.toString());
			HttpPut bulkUpdatePassedTests = new HttpPut(bulkExecuteTestsURL);
			bulkUpdatePassedTests.setHeader("Content-Type", "application/json");
			bulkUpdatePassedTests.setEntity(passEntity);
			response = zephyrData.getRestClient().getHttpclient().execute(bulkUpdatePassedTests);
			}


		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		int statusCode = response.getStatusLine().getStatusCode();

		if (statusCode >= 200 && statusCode < 300) {
			HttpEntity entity = response.getEntity();
			String string = null;
			try {
				string = EntityUtils.toString(entity);
			} catch (ParseException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}

			
		} else {
			try {
				throw new ClientProtocolException("Unexpected response status: "
						+ statusCode);
			} catch (ClientProtocolException e) {
				e.printStackTrace();
			}
		}
	
	}
	public static void processTestCaseDetails(ZephyrConfigModel zephyrData) {
		Map<Long, Map<String, Boolean>> testCaseDetails = getTestCaseDetails(zephyrData);
		
		long cycleId = 0;
		if(zephyrData.getCycleId() == NEW_CYCLE_KEY_IDENTIFIER) {
			cycleId = Cycle.createCycle(zephyrData);
			zephyrData.setCycleId(cycleId);
		} 
		
		JSONObject jsonObject = new JSONObject();
		jsonObject.put("versionId", zephyrData.getVersionId());
		jsonObject.put("cycleId", zephyrData.getCycleId());
		jsonObject.put("projectId", zephyrData.getZephyrProjectId());
		jsonObject.put("method", "1");
		
		JSONArray jArr = new JSONArray();
		
		JSONObject createExecutionsJObj = new JSONObject();
		Set<Entry<Long, Map<String, Boolean>>> entrySet = testCaseDetails.entrySet();
		for (Iterator<Entry<Long, Map<String, Boolean>>> iterator = entrySet.iterator(); iterator.hasNext();) {
			Entry<Long, Map<String, Boolean>> entry = iterator
					.next();
			Map<String, Boolean> value = entry.getValue();
			Set<String> keySet = value.keySet();
			for (Iterator<String> iterator2 = keySet.iterator(); iterator2.hasNext();) {
				String issueKey = iterator2.next();
				
				jArr.put(issueKey);
			}
		}
		
		jsonObject.put("issues", jArr);

		assignTests(zephyrData, jsonObject);
		Map<String, Long> issueKeyExecutionIdMap = fetchExecutionIds(zephyrData, createExecutionsJObj);

		List<Long> passList = new ArrayList<Long>();
		List<Long> failList = new ArrayList<Long>();
		for (Iterator<Entry<Long, Map<String, Boolean>>> iterator = entrySet.iterator(); iterator.hasNext();) {
			Entry<Long, Map<String, Boolean>> entry = iterator
					.next();
			Map<String, Boolean> value = entry.getValue();
			Set<String> keySet = value.keySet();
			for (Iterator<String> iterator2 = keySet.iterator(); iterator2.hasNext();) {
				String issueKey = iterator2.next();
				
				createExecutionsJObj.put("issueId", entry.getKey());
				createExecutionsJObj.put("versionId", zephyrData.getVersionId());
				createExecutionsJObj.put("cycleId", zephyrData.getCycleId());
				createExecutionsJObj.put("projectId", zephyrData.getZephyrProjectId());
				

//				executeTests(zephyrData, executionId, value.get(issueKey));
				
				boolean pass = value.get(issueKey);
				Long executionId = issueKeyExecutionIdMap.get(issueKey);
				if(pass) {
					passList.add(executionId );
				} else {
					failList.add(executionId);
				}
			}
		}
		executeTests(zephyrData, passList, failList);
	}
	
	
	private static Map<String, Map<Long, String>> searchIssues(ZephyrConfigModel zephyrData) {
		long searchIssueStartcount = -1L;
		long totalIssueCount = 0L;
		long searchedIsssuesCount = 0L;


		Map<String, Map<Long, String>> searchedTests = new HashMap<String, Map<Long, String>>();
		
		CustomReturnTypeBean returnValue = searchTests(zephyrData, searchIssueStartcount,
				searchedTests);

		totalIssueCount = returnValue.getTotalIssuesCount();
		searchedIsssuesCount = returnValue.getSearchedIssuesCount();
		while (searchedIsssuesCount < totalIssueCount) {
			log.info("Total Issues : " + totalIssueCount);
			log.info("Total issues fetched so far : " + searchedTests.size());
			CustomReturnTypeBean returnValue1 = searchTests(zephyrData, searchedIsssuesCount, searchedTests);
			searchedIsssuesCount = searchedIsssuesCount + returnValue1.getSearchedIssuesCount();
		}
		return searchedTests;
	}

	private static CustomReturnTypeBean searchTests(ZephyrConfigModel zephyrData,
			long searchIssueStartcount,
			Map<String, Map<Long, String>> searchedTests) {
		
		CustomReturnTypeBean customReturnTypeBean = new CustomReturnTypeBean();
		long totalIssueCount = 0L;
		HttpResponse response = null;
		String isssueSearchURL = null;

		String searchJQL = JQL_SEARCH_TESTS;
		searchJQL = searchJQL.replace("{pId}", zephyrData.getZephyrProjectId()+"");
		searchJQL = searchJQL.replace("{issueTypeId}", zephyrData.getTestIssueTypeId()+"");
		searchJQL = searchJQL.replace("{searchIssueStartcount}", searchIssueStartcount+"");
		
		isssueSearchURL = URL_GET_ALL_TESTS.replace("{SERVER}", zephyrData.getRestClient().getUrl()) + "?" + searchJQL;
		JSONArray searchedIssues = null;
		try {
			response = zephyrData.getRestClient().getHttpclient().execute(new HttpGet(isssueSearchURL));
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		int statusCode = response.getStatusLine().getStatusCode();

		if (statusCode >= 200 && statusCode < 300) {
			HttpEntity entity = response.getEntity();
			String string = null;
			try {
				string = EntityUtils.toString(entity);
			} catch (ParseException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			try {
				
				JSONObject testCaseIssues = new JSONObject(string);
				totalIssueCount = testCaseIssues.getLong("total");
				log.info("Total Issues count : " + totalIssueCount);
				searchedIssues = testCaseIssues.getJSONArray("issues");
				
				customReturnTypeBean.setSearchedIssuesCount(searchedIssues.length());
				
			} catch (JSONException e) {
				e.printStackTrace();
			}
		} else {
			try {
				throw new ClientProtocolException("Unexpected response status: "
						+ statusCode);
			} catch (ClientProtocolException e) {
				e.printStackTrace();
			}
		}
		
		try {
			
			if(searchedIssues != null && searchedIssues.length() > 0 ) {
				
				for (int i = 0; i < searchedIssues.length(); i++) {
					JSONObject jsonObject = searchedIssues.getJSONObject(i);
					String testName = jsonObject.getJSONObject("fields").getString("summary").trim();
					long testId = jsonObject.getLong("id");
					String testKey = jsonObject.getString("key").trim();
					
					Map<Long,String> tempTestIdTestKeyMap = new HashMap<Long, String>();
					tempTestIdTestKeyMap.put(testId, testKey);
					searchedTests.put(testName, tempTestIdTestKeyMap);
				}
			}
			
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		customReturnTypeBean.setTotalIssuesCount(totalIssueCount);
		return customReturnTypeBean;
	}
	
	static class CustomReturnTypeBean {
		
		private long totalIssuesCount;
		private long searchedIssuesCount;

		public long getTotalIssuesCount() {
			return totalIssuesCount;
		}

		public void setTotalIssuesCount(long totalIssuesCount) {
			this.totalIssuesCount = totalIssuesCount;
		}

		public long getSearchedIssuesCount() {
			return searchedIssuesCount;
		}

		public void setSearchedIssuesCount(long searchedIssuesCount) {
			this.searchedIssuesCount = searchedIssuesCount;
		}
	}
}
