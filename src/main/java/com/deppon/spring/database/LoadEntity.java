package com.deppon.spring.database;

public class LoadEntity implements java.io.Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -9212300715723154983L;
	private String user;
	private String password;
	
	public String getUser() {
		return user;
	}
	public void setUser(String user) {
		this.user = user;
	}
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}
	
}
