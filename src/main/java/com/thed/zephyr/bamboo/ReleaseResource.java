package com.thed.zephyr.bamboo;

import static com.thed.zephyr.bamboo.utils.ZeeConstants.PLUGIN_KEY;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.log4j.Logger;

import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import com.atlassian.sal.api.transaction.TransactionCallback;
import com.atlassian.sal.api.transaction.TransactionTemplate;
import com.atlassian.sal.api.user.UserManager;
import com.thed.zephyr.bamboo.utils.rest.RestClient;
import com.thed.zephyr.bamboo.utils.rest.Version;

@Path("/release")
public class ReleaseResource {
	private final UserManager userManager;
	private final TransactionTemplate transactionTemplate;
	private PluginSettingsFactory pluginSettingsFactory;
	private static final Logger log = Logger.getLogger(ReleaseResource.class);
	
	public ReleaseResource(UserManager userManager,
			PluginSettingsFactory pluginSettingsFactory,
			TransactionTemplate transactionTemplate) {
		this.userManager = userManager;
		this.pluginSettingsFactory = pluginSettingsFactory;
		this.transactionTemplate = transactionTemplate;
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response get(@Context final HttpServletRequest request) {

		return Response.ok(
				transactionTemplate.execute(new TransactionCallback() {
					public Object doInTransaction() {
						
						String serverAddress = request.getParameter("serverAddress");
						String projectKey = request.getParameter("projectKey");
						
						long projectId = 0;
						try {
							 projectId = Long.parseLong(projectKey);
						} catch (NumberFormatException e) {
							log.debug("Error parsing project Id");
						}
						RestClient restClient = null;
						Map<Long, String> releases;
						try {
					    	restClient = getRestclient(serverAddress);
							releases = Version.getVersionsByProjectID(projectId, restClient);
						} finally {
							ConfigResource.closeHTTPClient(restClient);
						}
						
						return releases;
					}
				})).build();
	}

	private RestClient getRestclient(String serverAddr) {

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

}