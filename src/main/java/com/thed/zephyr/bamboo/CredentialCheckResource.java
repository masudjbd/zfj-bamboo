package com.thed.zephyr.bamboo;


import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import com.atlassian.sal.api.transaction.TransactionCallback;
import com.atlassian.sal.api.transaction.TransactionTemplate;
import com.atlassian.sal.api.user.UserManager;
import com.thed.zephyr.bamboo.utils.URLValidator;
import com.thed.zephyr.bamboo.utils.rest.RestClient;
import com.thed.zephyr.bamboo.utils.rest.ServerInfo;

@Path("/CredCheck")
public class CredentialCheckResource {
	private final UserManager userManager;
	private final TransactionTemplate transactionTemplate;

	public CredentialCheckResource(UserManager userManager,
			PluginSettingsFactory pluginSettingsFactory,
			TransactionTemplate transactionTemplate) {
		this.userManager = userManager;
		this.transactionTemplate = transactionTemplate;
	}

	@POST
	@Produces(MediaType.APPLICATION_JSON)
	public Response post(final List<Config> configs, @Context HttpServletRequest request) {

		return Response.ok(
				transactionTemplate.execute(new TransactionCallback() {
					public Object doInTransaction() {
						Config config = configs.get(0);
						config.setStatus(false);
						config.setStatusMsg("fail");
						
						String serverAddr = config.getServerAddr();
						String user = config.getUser();
						String pass = config.getPass();
						
						if (!(serverAddr.trim().startsWith("https://") || serverAddr
								.trim().startsWith("http://"))) {
							config.setStatusMsg("Incorrect server address format");
							return config;
						}

						String zephyrURL = URLValidator.validateURL(serverAddr);
						boolean credentialValidationResultMap;
						RestClient restClient = null;
						try {
					    	restClient = getRestclient(serverAddr, user, pass);

							if (!zephyrURL.startsWith("http")) {
								config.setStatusMsg(zephyrURL);
								return config;
				            }

							if (!ServerInfo.findServerAddressIsValidZephyrURL(restClient)) {
								config.setStatusMsg("This is not a valid Zephyr Server");
								return config;
				            }

							credentialValidationResultMap = ServerInfo
				                    .validateCredentials(restClient);
							
							if (credentialValidationResultMap && !ServerInfo.findUserHasBrowseProjectPermission(restClient)) {
									config.setStatusMsg("User has no browse project permissions");
									return config;
								}
						} finally {
							closeHTTPClient(restClient);
						}
						if (!credentialValidationResultMap) {
							config.setStatusMsg("Validation failed");
							return config;
						}

						config.setStatus(true);
						config.setStatusMsg("Connection to Zephyr has been validated");

						return config;
					}
				})).build();
	}

	
	private RestClient getRestclient(String serverAddr, String user, String pass) {

			RestClient restClient = new RestClient(serverAddr, user, pass);
			
			return restClient;
	}
	
	private void closeHTTPClient(RestClient restClient) {
		if(restClient != null) {
			restClient.destroy();
		}
	}

	
	@XmlRootElement
	@XmlAccessorType(XmlAccessType.FIELD)
	public static final class Config {
		@XmlElement
		private String serverAddr;
		@XmlElement
		private String user;
		@XmlElement
		private String pass;
		@XmlElement
		private boolean status;
		@XmlElement
		private String statusMsg;

		public String getServerAddr() {
			return serverAddr;
		}

		public void setServerAddr(String serverAddr) {
			this.serverAddr = serverAddr;
		}

		public String getUser() {
			return user;
		}

		public void setUser(String user) {
			this.user = user;
		}

		public String getPass() {
			return pass;
		}

		public void setPass(String pass) {
			this.pass = pass;
		}

		public boolean isStatus() {
			return status;
		}

		public void setStatus(boolean status) {
			this.status = status;
		}

		public String getStatusMsg() {
			return statusMsg;
		}

		public void setStatusMsg(String statusMsg) {
			this.statusMsg = statusMsg;
		}

	}

}