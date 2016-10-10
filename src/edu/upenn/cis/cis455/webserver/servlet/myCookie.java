package edu.upenn.cis.cis455.webserver.servlet;

import javax.servlet.http.Cookie;

public class myCookie extends Cookie {
	
	String uuid;
	
	public myCookie(String name, String value) {
		super(name, value);
		// TODO Auto-generated constructor stub
	}
	
	public void setSessionId(String id){
		uuid = id;
	}
	
	public String getSessionId(){
		return uuid;
	}

}
