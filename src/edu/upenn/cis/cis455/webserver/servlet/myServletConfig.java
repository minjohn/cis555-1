package edu.upenn.cis.cis455.webserver.servlet;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Set;
import java.util.Vector;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;

public class myServletConfig implements ServletConfig {

	private String name;
	private myServletContext context;
	private HashMap<String,String> initParams;
	
	public myServletConfig(String name, myServletContext context){
		this.name = name;
		this.context = context;
		initParams = new HashMap<String,String>();
	}
	
	
	@Override
	public String getInitParameter(String arg0) {
		return initParams.get(name);
	}

	@Override
	public Enumeration getInitParameterNames() {
		Set<String> keys = initParams.keySet();
		Vector<String> atts = new Vector<String>(keys);
		return atts.elements();
	}

	@Override
	public myServletContext getServletContext() {
		return context;
	}

	@Override
	public String getServletName() {
		return name;
	}
	
	public void setInitParam(String name, String value) {
		initParams.put(name, value);
	}

}
