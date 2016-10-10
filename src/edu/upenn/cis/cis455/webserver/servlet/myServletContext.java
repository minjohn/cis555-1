package edu.upenn.cis.cis455.webserver.servlet;

import java.io.File;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.apache.log4j.Logger;

import edu.upenn.cis.cis455.webserver.statusHandle;

public class myServletContext implements ServletContext{
	
	Logger log = Logger.getLogger(myServletContext.class);
	
	
	private HashMap<String,Object> attributes;
	private HashMap<String,String> initParams;
	private String root_dir = null;
	private statusHandle statusHandle = null;
	

	public myServletContext (String dir){
		attributes = new HashMap<String,Object>();
		initParams = new HashMap<String,String>();
		
		root_dir = dir;
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
		// may need to handle this for extra credit where there are multiple web apps
		return this;
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
		
		// not really sure how to deal with resources in /resources/ 
		
		// also assumption is that contextpath is null given that we only have a single web app.
		
		//File f = new File(root_dir+path);
		
		URL resource = myServletContext.class.getClassLoader().getResource(path);
		
		log.debug("resource URI: " + resource);
		if(resource != null){
			try {
				Path p = Paths.get(resource.toURI()).toAbsolutePath();
				
				File f = new File(p.toString());

				if(f.exists() == true){
					//return root_dir+path;
					return p.toString();
				}

				return null;
			} catch (URISyntaxException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();

				return null;
			}
		}
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
		
		File f = new File(root_dir+path);
		
		if(f.isDirectory()){
			File[] listofFiles = f.listFiles();
			if(listofFiles.length == 0){
				return null;
			}else{
				List<File> list =  Arrays.asList(listofFiles);

				Set<String> files = new HashSet<String>();

				for(File file  : list ){
					if(file.isDirectory()){
						files.add(file.getName()+"/");
					}else{
						files.add(file.getName());
					}
				}
				
				return files;
			}
			
		}
				
		return null;
	}

	@Override
	public String getServerInfo() {
		return "My HTTP Server";
	}

	//DEPRECATED
	@Override
	public Servlet getServlet(String name) {
		return null;
	}

	@Override
	public String getServletContextName() {
		return "my Servlet Context";
	}
	
	//DEPRECATED
	@Override
	public Enumeration getServletNames() {
		return null;
	}

	//DEPRECATED
	@Override
	public Enumeration getServlets() {
		return null;
	}

	@Override
	public void log(String msg) {
		System.err.println(msg);
	}

	//DEPRECATED
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
	
	public void setThreadStatusHandle(statusHandle s){
		statusHandle = s;
	}
	
	public String getThreadStatuses(){
		return statusHandle.getAllThreadStatus();
	}
	
	public boolean validatePath(Path filePath){
				
		log.debug("Absolute path of file/directory: " + filePath.toString());
		log.debug("Absolute path of root: " + root_dir);
		
		if( filePath.startsWith(root_dir) ){
			return true;
		}
		return false;
		
	}
	
	
	

}
