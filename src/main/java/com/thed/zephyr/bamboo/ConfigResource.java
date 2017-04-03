package com.thed.zephyr.bamboo;

import static com.thed.zephyr.bamboo.utils.ZeeConstants.PLUGIN_KEY;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import com.atlassian.sal.api.transaction.TransactionCallback;
import com.atlassian.sal.api.transaction.TransactionTemplate;
import com.atlassian.sal.api.user.UserManager;
import com.thed.zephyr.bamboo.utils.rest.RestClient;
@Path("/")
public class ConfigResource {
	private final UserManager userManager;
	private static PluginSettingsFactory pluginSettingsFactory;
	private final TransactionTemplate transactionTemplate;

	public ConfigResource(UserManager userManager,
			PluginSettingsFactory pluginSettingsFactory,
			TransactionTemplate transactionTemplate) {
		this.userManager = userManager;
		ConfigResource.pluginSettingsFactory = pluginSettingsFactory;
		this.transactionTemplate = transactionTemplate;
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response get(@Context HttpServletRequest request) {

		return Response.ok(
				transactionTemplate.execute(new TransactionCallback() {
					public Object doInTransaction() {
						PluginSettings settings = pluginSettingsFactory
								.createGlobalSettings();
						
						List<CredentialData> configs = new ArrayList<CredentialData>();
						@SuppressWarnings("unchecked")
						List<Map<String, String>> credList=  (List<Map<String, String>>) settings.get(PLUGIN_KEY + ".zephyrserverconfig");
						if (credList == null || credList.size() == 0) {
							return null;
						}
						for (Iterator<Map<String, String>> iterator = credList.iterator(); iterator.hasNext();) {
							Map<String, String> map = iterator.next();
							
							CredentialData cData = new CredentialData();

							cData.setServerAddr(map.get("serverAddr"));
							cData.setUser(map.get("user"));
							cData.setPass(map.get("password"));
							cData.setStatusMsg(map.get("statusMsg"));
							cData.setStatus(true);
							configs.add(cData);
						}

						return configs;
					}
				})).build();
	}

	
	
	
	@GET
	@Path(value = "/serverAddr")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getServerConfigByServer(@Context HttpServletRequest request, @QueryParam("serverAddr") final String serverAddr) {

		return Response.ok(
				transactionTemplate.execute(new TransactionCallback() {
					public Object doInTransaction() {
						PluginSettings settings = pluginSettingsFactory
								.createGlobalSettings();
						
						List<CredentialData> configs = new ArrayList<CredentialData>();
						@SuppressWarnings("unchecked")
						List<Map<String, String>> credList=  (List<Map<String, String>>) settings.get(PLUGIN_KEY + ".zephyrserverconfig");
						if (credList == null || credList.size() == 0) {
							return null;
						}
						for (Iterator<Map<String, String>> iterator = credList.iterator(); iterator.hasNext();) {
							Map<String, String> map = iterator.next();
							
							
							if(map.get("serverAddr").equals(serverAddr.trim())) {
								CredentialData cData = new CredentialData();
								
								cData.setServerAddr(map.get("serverAddr"));
								cData.setUser(map.get("user"));
								cData.setPass(map.get("password"));
								cData.setStatusMsg(map.get("statusMsg"));
								cData.setStatus(true);
								configs.add(cData);
								
								break;
							}
						}

						return configs;
					}
				})).build();
	}
	
	
	
	
	
	@PUT
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response put(final List<CredentialData> configs, @Context HttpServletRequest request)
 {

		return Response.ok(
				transactionTemplate.execute(new TransactionCallback() {
					@SuppressWarnings("unchecked")
					public Object doInTransaction() {
						PluginSettings pluginSettings = pluginSettingsFactory
								.createGlobalSettings();
						List<CredentialData> configsToBePersisted = new ArrayList<CredentialData>();
						List<CredentialData> configsMsg = new ArrayList<CredentialData>();

						for (Iterator<CredentialData> iterator = configs
								.iterator(); iterator.hasNext();) {
							CredentialData credentialData = iterator.next();
							configsMsg.add(credentialData);
							String serverAddr = credentialData.getServerAddr();
							String user = credentialData.getUser();
							String pass = credentialData.getPass();

							credentialData.setServerAddr(serverAddr);
							credentialData.setStatus(true);
							credentialData
									.setStatusMsg("Connection to Zephyr has been validated");

							configsToBePersisted.add(credentialData);

						}
						List<Map<String, String>> credList = null;

						credList = (List<Map<String, String>>) pluginSettings
								.get(PLUGIN_KEY + ".zephyrserverconfig");

						if (credList == null || credList.size() == 0) {
							credList = new ArrayList<Map<String, String>>();
						}

						for (CredentialData credData : configsToBePersisted) {
							String serverAddr = credData.getServerAddr();
							String user = credData.getUser();
							String password = credData.getPass();
							String statusMsg = credData.getStatusMsg();
							Map<String, String> credMap = new HashMap<String, String>();
							credMap.put("serverAddr", serverAddr);
							credMap.put("user", user);
							credMap.put("password", password);
							credMap.put("statusMsg", statusMsg);

							boolean exists = false;
							for (Map<String, String> cl : credList) {
								if (cl.get("serverAddr").trim()
										.equalsIgnoreCase(serverAddr.trim())) {
									exists = true;
									break;
								}
							}

							if (!exists) {
								credList.add(credMap);
							}
						}

						pluginSettings.put(PLUGIN_KEY + ".zephyrserverconfig",
								credList);
						return configsMsg;
					}
				})).build();
	}
	
	
	@PUT
	@Path(value = "/edit")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response saveEditedConfig(final List<CredentialData> configs, @Context HttpServletRequest request)
	{

	  return Response.ok(transactionTemplate.execute(new TransactionCallback()
	  {
	    @SuppressWarnings("unchecked")
		public Object doInTransaction()
	    {
	      PluginSettings pluginSettings = pluginSettingsFactory.createGlobalSettings();
	      List<CredentialData> configsMsg = new ArrayList<CredentialData>();
	      
			CredentialData credentialData = configs.get(0);
			String oldServerAddr = credentialData.getServerAddr().split("::")[0];

			String serverAddr = credentialData.getServerAddr().split("::")[1];
			String user = credentialData.getUser();
			String pass = credentialData.getPass();
			
	      List<Map<String, String>> credList = null;
	      
			credList=  (List<Map<String, String>>) pluginSettings.get(PLUGIN_KEY + ".zephyrserverconfig");
			
			if(credList == null || credList.size() == 0) {
				credList = new ArrayList<Map<String,String>>();
			}

	    	  Map<String, String> credMap = new HashMap<String, String>();
	    	  credMap.put("serverAddr", serverAddr);
	    	  credMap.put("user", user);
	    	  credMap.put("password", pass);
	    	  credMap.put("statusMsg", "Connection to Zephyr has been validated");
	    	  
	    	  for(Map<String, String> cl: credList) {
	    		  if(cl.get("serverAddr").trim().equalsIgnoreCase(oldServerAddr.trim())) {
	    			  credList.remove(cl);
	    			  break;
	    		  }
	    	  }
	    		  credList.add(credMap);
	      
	      pluginSettings.put(PLUGIN_KEY + ".zephyrserverconfig", credList);
	      return configsMsg;
	    }
	  })
	  ).build();
	}	

	public static List<CredentialData> getServerConfig() {
		PluginSettings settings = pluginSettingsFactory
				.createGlobalSettings();
		
		List<CredentialData> configs = new ArrayList<CredentialData>();
		@SuppressWarnings("unchecked")
		List<Map<String, String>> credList=  (List<Map<String, String>>) settings.get(PLUGIN_KEY + ".zephyrserverconfig");
		
		for (Iterator<Map<String, String>> iterator = credList.iterator(); iterator.hasNext();) {
			Map<String, String> map = iterator.next();
			
			CredentialData cData = new CredentialData();

			cData.setServerAddr(map.get("serverAddr"));
			cData.setUser(map.get("user"));
			cData.setPass(map.get("password"));
			cData.setStatusMsg(map.get("statusMsg"));
			cData.setStatus(true);
			configs.add(cData);
		}

		return configs;

	}

	private static RestClient getRestclient(String serverAddr) {

		RestClient restClient = null;
		
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

					restClient = new RestClient(serverAddr, userName, password);
					
				}
			}
		}


		return restClient;
	}

	public static void closeHTTPClient(RestClient restClient) {
		if (restClient != null) {
			restClient.destroy();
		}
}
	
	public static CredentialData getCredentialData(String serverAddr) {

		CredentialData credData = null;
		List<CredentialData> serverConfig = getServerConfig();
		for (CredentialData config: serverConfig) {
			
			if(config.getServerAddr().equals(serverAddr)) {
				credData = config;
				break;
			}
			
		}

		return credData;
	}

	
	@DELETE
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response delete(final CredentialData configs, @Context HttpServletRequest request)
	{

	  return Response.ok(transactionTemplate.execute(new TransactionCallback()
	  {
	    @SuppressWarnings("unchecked")
		public Object doInTransaction()
	    {
	      PluginSettings pluginSettings = pluginSettingsFactory.createGlobalSettings();

	      List<Map<String, String>> credList = null;
	      
			credList=  (List<Map<String, String>>) pluginSettings.get(PLUGIN_KEY + ".zephyrserverconfig");
			
			if(credList == null || credList.size() == 0) {
				credList = new ArrayList<Map<String,String>>();
			}

	    	  for(Map<String, String> cl: credList) {
	    		  if(cl.get("serverAddr").trim().equalsIgnoreCase(configs.getServerAddr().trim())) {
	    			  credList.remove(cl);
	    			  break;
	    		  }
	    	  }
	    	  
	      pluginSettings.put(PLUGIN_KEY + ".zephyrserverconfig", credList);
	      return true;
	    }
	  })
	  ).build();
	}
}