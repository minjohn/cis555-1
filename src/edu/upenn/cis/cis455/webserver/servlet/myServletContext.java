package edu.upenn.cis.cis455.webserver.servlet;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Set;
import java.util.Vector;

import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

public class myServletContext implements ServletContext{
	
	private HashMap<String,Object> attributes;
	private HashMap<String,String> initParams;

	public myServletContext (){
		attributes = new HashMap<String,Object>();
		initParams = new HashMap<String,String>();
	}
	
	@Override
	public Object getAttribute(String name) {
		return attributes.get(name);
	}

	@Override
	public Enumeration getAttributeNames() {
		Set<String> keys = attributes.keySet();
		Vector<String> atts = new Vector<String>(keys);
		return atts.elements();
	}

	@Override
	public myServletContext getContext(String name) {
		return null;
	}

	@Override
	public String getInitParameter(String name) {
		return initParams.get(name);
	}

	@Override
	public Enumeration getInitParameterNames() {
		Set<String> keys = initParams.keySet();
		Vector<String> atts = new Vector<String>(keys);
		return atts.elements();
	}

	@Override
	public int getMajorVersion() {
		return 2;
	}

	@Override
	public String getMimeType(String file) {
		return null;
	}

	@Override
	public int getMinorVersion() {
		return 4;
	}

	@Override
	public RequestDispatcher getNamedDispatcher(String name) {
		return null;
	}

	@Override
	public String getRealPath(String path) {
		return null;
	}

	@Override
	public RequestDispatcher getRequestDispatcher(String name) {
		return null;
	}

	@Override
	public URL getResource(String path) throws MalformedURLException {
		return null;
	}

	@Override
	public InputStream getResourceAsStream(String path) {
		return null;
	}

	@Override
	public Set getResourcePaths(String path) {
		return null;
	}

	@Override
	public String getServerInfo() {
		return "My HTTP Server";
	}

	@Override
	public Servlet getServlet(String name) {
		return null;
	}

	@Override
	public String getServletContextName() {
		return "my Servlet Context";
	}

	@Override
	public Enumeration getServletNames() {
		return null;
	}

	@Override
	public Enumeration getServlets() {
		return null;
	}

	@Override
	public void log(String msg) {
		System.err.println(msg);
	}

	@Override
	public void log(Exception exception, String msg) {
		log(msg, (Throwable) exception);
	}

	@Override
	public void log(String message, Throwable throwable) {
		System.err.println(message);
		throwable.printStackTrace(System.err);
	}

	@Override
	public void removeAttribute(String name) {
		attributes.remove(name);
	}

	@Override
	public void setAttribute(String name, Object object) {
		attributes.put(name, object);
	}
	
	public void setInitParam(String name, String value) {
		initParams.put(name, value);
	}

}
