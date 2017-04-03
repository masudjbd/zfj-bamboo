package com.thed.zephyr.bamboo;
import java.io.Serializable;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

	@XmlRootElement
	@XmlAccessorType(XmlAccessType.FIELD)
	public class CredentialData implements Serializable {

		/**
		 * 
		 */
		private static final long serialVersionUID = 3872278817271308887L;

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

