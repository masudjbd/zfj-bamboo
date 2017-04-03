package com.thed.zephyr.bamboo.utils.rest;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContextBuilder;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.apache.log4j.Logger;

import com.thed.zephyr.bamboo.model.ZephyrInstance;
import com.thed.zephyr.utils.Base64;

public class RestClient {

	private CloseableHttpClient httpclient;
	private String url;
	private String userName;
	private String password;
	private static String AUTHORIZATION_HEADER_PREFIX = "Basic ";
	private static Logger logger = Logger.getLogger(RestClient.class);

	
	public RestClient(String url, String userName, String password) {
		super();

		this.url = url;
		this.userName = userName;
		this.password = password;

		createHttpClient(userName, password);
	}

	public RestClient(ZephyrInstance zephyrServer) {
		this(zephyrServer.getServerAddress(), zephyrServer.getUsername(), zephyrServer.getPassword());
	}

	public void destroy(){
		if(httpclient != null){
			try {
				httpclient.close();
			} catch (IOException e) {
				//TODO - Log it properly
				e.printStackTrace();
			}
		}
	}

	private void createHttpClient(String userName, String password) {
		String basicAuthorizationHeaderValue = RestClient.getBasicAuthorizationHeaderValue(userName, password);
		Header header = new BasicHeader(HttpHeaders.AUTHORIZATION, basicAuthorizationHeaderValue);
		
		if (logger.isTraceEnabled()) {
			logger.trace("AUTHORIZATION : " + basicAuthorizationHeaderValue);
		}
		List<Header> headers = new ArrayList<>();
		headers.add(header);
		try {
			SSLContextBuilder builder = new SSLContextBuilder();
			builder.loadTrustMaterial(null, new TrustSelfSignedStrategy());
			SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(
					builder.build(),
					SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
			httpclient = HttpClients.custom().setSSLSocketFactory(sslsf)
					.setDefaultHeaders(headers).build();
		} catch (KeyManagementException e1) {
			e1.printStackTrace();
		} catch (NoSuchAlgorithmException e1) {
			e1.printStackTrace();
		} catch (KeyStoreException e1) {
			e1.printStackTrace();
		}
	}

	public CloseableHttpClient getHttpclient() {
		return httpclient;
	}

	public String getUrl() {
		return url;
	}

	public String getUserName() {
		return userName;
	}

	public String getPassword() {
		return password;
	}
	
	
	public static RestClient getRestclient(String serverAddr, String user, String pass) {

		RestClient restClient = new RestClient(serverAddr, user, pass);

		return restClient;
	}

	public static void closeHTTPClient(RestClient restClient) {
		if (restClient != null) {
			restClient.destroy();
		}
}

	/**
	 * @param str
	 * @return
	 */
	public static String getBasicAuthorizationHeaderValue(String userName, String password) {
		byte[]   bytesEncoded = Base64.encodeBase64((userName+":"+password).getBytes());
		String authorizationHeader = AUTHORIZATION_HEADER_PREFIX + new String(bytesEncoded );
		return authorizationHeader;
	}

}
