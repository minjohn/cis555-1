package edu.upenn.cis.cis455.webserver;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import javax.servlet.http.HttpServlet;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.log4j.Logger;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

import edu.upenn.cis.cis455.webserver.servlet.myHttpServletSession;
import edu.upenn.cis.cis455.webserver.servlet.myServletConfig;
import edu.upenn.cis.cis455.webserver.servlet.myServletContext;

public class myServer extends Thread{
	
	Logger log = Logger.getLogger(myServer.class);
	
	/** 
	   Class to hold boolean that will be set when a workerThread receives a /shutdown request
	   it will immediately be seen accross all threads who can gracefully begin shutting down.
	   after receiving this flag, the server will also begin refusing connections while waiting
	   for threads to finish handling their requests. 
	**/
	
	class ShutdownControl {
		public volatile boolean shutdown_requested = false;
	}
	
	final ShutdownControl shutdown = new ShutdownControl();
	
	/*Instance Variables*/
	ServerSocket serverSocket;
	int port;
	String root_dir;
	String web_dir;
	
	//thread pool and socket queue
	List<workerThread> threadPool = new ArrayList<workerThread>();
	myBlockingQueue sock_q = new myBlockingQueue(50); // 50 is default backlog
	
	//Servlet Session, context and Map of Servlet Objects.
	myServletContext context;
	HashMap<String,HttpServlet> servlets;
	myHttpServletSession session;
	
	final int num_threads = 2;
	
	/* End Instance Variables*/
	
	// Public Constructor
	public myServer(int p, String r, String web){
		port = p;
		root_dir = r;
		web_dir = web;
	}
	
	//for logging pretty-printing server
	private String serverMessage(String msg){
		StringBuffer m = new StringBuffer();
		m.append("HttpServer: ");
		m.append(msg);
		return m.toString();
	}
	
	static class Handler extends DefaultHandler {
		public void startElement(String uri, String localName, String qName, Attributes attributes) {
			
			System.out.println("==> qName: " + qName );
			
			// display name
			if (qName.compareTo("servlet-name") == 0) {
				m_state = 1;
			} else if (qName.compareTo("servlet-class") == 0) {
				m_state = 2;
			} else if (qName.compareTo("context-param") == 0) {
				m_state = 3;
			} else if (qName.compareTo("init-param") == 0) {
				m_state = 4;
			} else if (qName.compareTo("url-pattern") == 0){
				m_state = 5;
			} else if (qName.compareTo("param-name") == 0) { 
				// m_state = 3, context parameter name
				// m_state != 3, just a plain old parameter name
				m_state = (m_state == 3) ? 10 : 20;
			} else if (qName.compareTo("param-value") == 0) { 
				// m_state = 10, context value for the "previous" context parameter name
				// m_state != 10, (presumably 20), plain old value for the "previous" parameter name
				m_state = (m_state == 10) ? 11 : 21;
			} else{
				System.out.println("No qname match, skipping qname: " + qName);
				m_state = 0;
			}
		}
		public void characters(char[] ch, int start, int length) {
			
			//System.out.println(" characters called with " + new String(ch));
			//System.out.println(" start: " + String.valueOf(start) + "length: " + String.valueOf(length) );
			String value = new String(ch, start, length);
			System.out.println("value: " + value.trim() + ", m_state: " + String.valueOf(m_state) );
			
			if (m_state == 1) { // servlet name
				m_servletName = value.trim();
				m_state = 0;
			} else if (m_state == 2) { // servlet class
				m_servlets.put(m_servletName, value.trim());
				m_state = 0;
			} else if (m_state == 5){ // url-pattern for servlet class
				m_servletUrlPatterns.put(m_servletName, value.trim());
				m_state = 0;
			} else if (m_state == 10 || m_state == 20) {
				m_paramName = value.trim();
			} else if (m_state == 11) {
				if (m_paramName == null) {
					System.err.println("Context parameter value '" + value.trim() + "' without name");
					System.exit(-1);
				}
				m_contextParams.put(m_paramName, value.trim());
				m_paramName = null;
				m_state = 0;
			} else if (m_state == 21) {
				if (m_paramName == null) {
					System.err.println("Servlet parameter value '" + value.trim() + "' without name");
					System.exit(-1);
				}
				HashMap<String,String> p = m_servletParams.get(m_servletName);
				if (p == null) {
					p = new HashMap<String,String>();
					m_servletParams.put(m_servletName, p);
				}
				p.put(m_paramName, value.trim());
				m_paramName = null;
				m_state = 0;
			}
		}
		private int m_state = 0;
		private String m_servletName;
		private String m_paramName;
		HashMap<String,String> m_servlets = new HashMap<String,String>();
		HashMap<String,String> m_contextParams = new HashMap<String,String>();
		HashMap<String,String> m_servletUrlPatterns = new HashMap<String,String>();
		HashMap<String,HashMap<String,String>> m_servletParams = new HashMap<String,HashMap<String,String>>();
	}
		
	private static Handler parseWebdotxml(String webdotxml) throws Exception {
		Handler h = new Handler();
		File file = new File(webdotxml);
		if (file.exists() == false) {
			System.err.println("error: cannot find " + file.getPath());
			System.exit(-1);
		}
		SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
		
		// parse and store the servlet configurations from web.xml 
		parser.parse(file, h);
		
		return h;
	}
	
	private static myServletContext createContext(Handler h, String root_dir) {
		myServletContext fc = new myServletContext(root_dir);
		for (String param : h.m_contextParams.keySet()) {
			fc.setInitParam(param, h.m_contextParams.get(param));
		}
		return fc;
	}
	
	private static HashMap<String,HttpServlet> createServlets(Handler h, myServletContext sc) throws Exception {
		HashMap<String,HttpServlet> servlets = new HashMap<String,HttpServlet>();
		for (String servletName : h.m_servlets.keySet()) {
			myServletConfig config = new myServletConfig(servletName, sc);
			String className = h.m_servlets.get(servletName);
			Class servletClass = Class.forName(className);
			HttpServlet servlet = (HttpServlet) servletClass.newInstance();
			HashMap<String,String> servletParams = h.m_servletParams.get(servletName);
			if (servletParams != null) {
				for (String param : servletParams.keySet()) {
					config.setInitParam(param, servletParams.get(param));
				}
			}
			servlet.init(config);
			servlets.put(servletName, servlet);
		}
		return servlets;
	}
	
	
	
	
	// run function
	public void run(){
		
		// Parse the web.xml with SAXParser
		
		Handler h;
		HashMap<String,HttpServlet> servlets = null;
		HashMap<String,String> url_mappings = null;
		HashMap<String,myHttpServletSession> session = new HashMap<String,myHttpServletSession>();
		
		statusHandle stats = new statusHandle(threadPool);
		myServletContext context;
		try {
			h = parseWebdotxml(web_dir);
			
			//log the contents of h here.
			/*
			log.debug(serverMessage("*********m_servlets**********"));
			for( String key : h.m_servlets.keySet() ){
				log.debug(serverMessage(key + ": " + h.m_servlets.get(key)));
			}
			
			log.debug(serverMessage("*********m_contextParams**********"));
			for(String key  : h.m_contextParams.keySet()){
				log.debug(serverMessage(key + ": " + h.m_contextParams.get(key)));
			}
			
			log.debug(serverMessage("*********m_servletUrlPatterns**********"));
			for(String key  : h.m_servletUrlPatterns.keySet()){
				log.debug(serverMessage(key + ": " + h.m_servletUrlPatterns.get(key)));
			}
			log.debug(serverMessage("*********m_servletParams**********"));
			for(String key  : h.m_servletParams.keySet()){
				log.debug(serverMessage(key + ": " + h.m_servletParams.get(key)));
			}
			*/
			
			context = createContext(h, root_dir);
			context.setThreadStatusHandle(stats);
			
			log.debug("Servlet context: " + context.toString());
			
			servlets = createServlets(h, context);
			url_mappings = h.m_servletUrlPatterns;
			//TODO later, when dealing with Session features
			//session = new myHttpServletSession();
			
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			
			return; // its important to correctly parse the web.xml
			
		}
		
		
		for(int i = 0; i < num_threads; i++){
			workerThread t = new workerThread(i,"INITIALIZED", sock_q, root_dir, shutdown); 
			threadPool.add(t);
		}
		
		// class that hold refs to all threads so that each individual thread can query 
		//  the status of other threads if a control page is requested of them.
		
		
		//start threads in threadPool
		for(workerThread t : threadPool){
			
			// set the shutdown status flag for the worker threads 
			t.setStatusHandle(stats);
			t.setServlets(servlets);
			t.setServletURLMappings(url_mappings);
			t.setSessionMap(session);
			t.start();
		}
		
		
		// initialize server socket
		try {
			log.debug(serverMessage("Listening for connections"));
			
			serverSocket = new ServerSocket(port);
			
			serverSocket.setSoTimeout(2000);
			
		    while (true){
		    	
		    	try{
		    		if(shutdown.shutdown_requested == false){

		    			Socket clientSocket = serverSocket.accept();
		    			log.debug(serverMessage("Got a Connection!"));
		    			
		    			clientSocket.setSoTimeout(30000);

		    			// Give socket to a thread to process the request

		    			log.debug(serverMessage("accept returned"));
		    			synchronized(sock_q){
		    				log.debug(serverMessage("Enqueuing Socket: " + clientSocket.toString()));
		    				//sock_q.add(clientSocket);
		    				try {
								sock_q.enqueue(clientSocket);
							} catch (InterruptedException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
		    				//sock_q.notify();
		    			}
		    		}else{
		    			
		    			log.debug(serverMessage("accept returned"));
		    			// propagate shutdown to all threads
		    			for(workerThread t : threadPool){
		    				t.setShutdown();
		    			}
		    			synchronized(sock_q){
		    				sock_q.notifyAll();
		    			}
		    			break;
		    		}
		    	} catch (SocketTimeoutException timeout ){ // hacky way of doing things to not block, in case there is a shutdown flag.
					// do nothing just let it continue...
					//log.debug("Server Socket accept() timed out");
		    	}
		    }		    
		} catch (SecurityException se){
			se.printStackTrace();
			
		} catch (IllegalArgumentException ie) {
			ie.printStackTrace();
			
		} catch (SocketException ske){
			ske.printStackTrace();
			
		} catch (IOException ie){
			ie.printStackTrace();
			
		}finally // close our resources
		{
			try {
				
				serverSocket.close();

			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		
		log.debug("Waiting for threads to die");
		//Wait for threads to die
		for(workerThread t : threadPool){
			try {
				t.join();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		
		
		
	}
}
