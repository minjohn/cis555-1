package edu.upenn.cis.cis455.webserver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import javax.servlet.http.HttpServlet;

import org.apache.log4j.Logger;

import javax.servlet.http.Cookie;
import edu.upenn.cis.cis455.webserver.servlet.myHttpServletSession;

public class HttpRequestParser {

	Logger log = Logger.getLogger(HttpRequestParser.class);

	public Socket socket = null;

	public Properties m_params = new Properties();
	public Properties m_props = new Properties();
	public HashMap<String, List<String>>m_headers = new HashMap<String,List<String>>();
	public myHttpServletSession m_session = null;
	public String m_method = null;
	public String http_protocol = null;
	public String character_encoding = null;
	public Locale locale = null;
	public String query_string = null;
	public String resource_uri = null;
	public String host = null;
	public int port = 0;
	public String url_protocol = null;
	public Cookie[] cookies;
	public String requested_session_id = null;
	public String m_servlet_path = null;
	public String pattern_matched = null;
	public String path_info = null;
	public HashMap<String, HttpServlet> servletMap;
	public HashMap<String, String> servletURLMap;
	public HashMap<String, myHttpServletSession> sessionMap;
	public HttpServlet m_servlet;
	public boolean keepalive = false;
	public Long resource_last_modified = 0L;
	

	//constructor.
	public HttpRequestParser(){

	}

	public void setServletMaps( HashMap<String, HttpServlet> sMap,
								HashMap<String, String> sURLMap,	
								HashMap<String, myHttpServletSession> sessMap){
		servletMap = sMap;
		servletURLMap = sURLMap;
		sessionMap = sessMap;
	}
	
	
	
	/** Custom method to read HTTP request and extract the appropriate information  
	 * @throws IOException **/
	public int extract(Socket requestSocket) throws IOException {

		socket = requestSocket;
		
		port = requestSocket.getLocalPort();
		host = requestSocket.getLocalAddress().getHostName();
		
		BufferedReader reader = new BufferedReader(new InputStreamReader(requestSocket.getInputStream()));

		try{
			query_string = reader.readLine();
		}catch (SocketTimeoutException timeout ){
			return -1;
		}

		String filename = null;
		String httpVersion = null;
//		String method = null;

		log.debug("requestline Text: " + query_string);


		if(query_string != null){

			String[] requestParts = query_string.split("\\s+");


			if(requestParts.length != 3){
				//send 400 bad request

				log.debug(" Request does not have all of the parts!");
				log.debug("ERROR - Imcomplete Request!");

				//TODO should create and enum for these values
				return 400;
			}
			//GET/HEAD/POST Request
			else if (requestParts[0].toLowerCase().compareTo("get") == 0 || requestParts[0].toLowerCase().compareTo("head") == 0
																		 || requestParts[0].toLowerCase().compareTo("post") == 0){
				log.debug( "Got a GET or POST request. ");

				m_method = requestParts[0].toUpperCase();
				resource_uri = requestParts[1];
				http_protocol = requestParts[2];

				// extra checking for parsing difference cases of resource_uri

				// Absolute URL paths
				if( resource_uri.toLowerCase().startsWith("http://") == true ){
					
					url_protocol = "http://";
					log.debug("filename is an absolute URL, parsing " );

					resource_uri = resource_uri.substring("http://".length());

					resource_uri = resource_uri.substring(resource_uri.indexOf("/"));
					
				} else if(resource_uri.toLowerCase().startsWith("https://") == true){
					
					url_protocol = "https://";
					log.debug("filename is an absolute URL, parsing " );

					resource_uri = resource_uri.substring("https://".length());
					resource_uri = resource_uri.substring(resource_uri.indexOf("/"));
				}


				// check for query string if the request was a Posted GET request
				int question = resource_uri.indexOf('?');

				if( question != -1 ){

					log.debug("question index: " + String.valueOf(question));
					m_servlet_path = resource_uri.substring(0, question);
					
					log.debug("m_servlet_path: " + m_servlet_path);
					
					// get correct URI in the servlet case
					
					log.debug("resource_uri: " + resource_uri + " length:" + resource_uri.length());
					query_string = resource_uri.substring(question+1, resource_uri.length());
					
					resource_uri = m_servlet_path;
					// Check for and parse parameters in the resourece URI 
					String[] params = query_string.split("\\?|&|=");

					if(params.length >= 2 ){
						if(params.length % 2 ==0 ){
							log.debug("params have been parsed");
							for (int j = 0; j < params.length - 1; j += 2) {
								log.debug("key: " + params[j] + " value: " + params[j+1]);

								m_params.setProperty(params[j], params[j+1]);
							}
						}else{
							//malformed error 
							log.debug("ERROR! params list is malformed!");
							return 400;
						}
					}
					else{
						//no parameters... 
						
					}

				} else { 
					// not a Posted GET, could be just a regular file, or a call to a servlet with no parameters. 
					//   we should check either way.
					
					m_servlet_path = resource_uri;
				}




			}else{  // Unsupported Method

				//invalid method
				log.debug(" Got another kind of request: " + requestParts[0]);
				log.debug("ERROR - Method not Implemented!");

				//TODO should create and enum for these values
				return 501;
			}

		}
		else{ // nothing to read from the stream its probably indicative to close the stream


			//TODO should create and enum for these values
			return -1;
		}

		// Helper class to store the parsed information from the HttpRequest Headers
		class Headers{

			Map<String, List<String>> headerContents = new HashMap<String, List<String>>();
			List<Cookie> cookies = new ArrayList<Cookie>();

			public void addHeader(String name, List<String> val){
				if(headerContents.containsKey(name) == true){
					headerContents.get(name).add(val.get(0));
				}else{
					headerContents.put(name,val);	
				}
				
			}
			public List<String> getHeader(String name){
				if(headerContents.containsKey(name)){
					return headerContents.get(name);
				}
				return null;
			}
			public void addCookie(Cookie c){
				cookies.add(c);
			}
			public Map<String, List<String>> getHeaders(){
				return headerContents;
			}
			public List<Cookie> getCookies(){
				return cookies;
			}
			public void updateHeader(String name, String val){
				headerContents.get(name).add(val);
			}


		};


		Headers headers = new Headers();

		String nextLine;

		try {
			nextLine = reader.readLine();
			//log.debug(threadMessage("First line: " + nextLine));
			boolean run = true;
			boolean malformedHeader = false;
			boolean multiLine = false;
			String lastHeader = null; // for multiLine headers
			while( nextLine != null && nextLine.compareTo("\n") != 0 && nextLine.compareTo("\r\n") != 0  ){

				// to skip the last new line character in the HTTPRequest ( Last line of HttpRequests
				//   is a newline character, but BufferedReader will return the contents of the line without
				//   the line-terminating characters (\n ,\r\n). Hence it will return an empty string?
				if(!nextLine.isEmpty()){ 


					//find the whitespace between header and value, if there is none, then its a malformed request

					int colon = nextLine.indexOf(':');	
					if( colon == -1 &&  multiLine == false ){

						//ignore this header, malformed header
					}
					else if(colon == -1 &&  multiLine == true ){ // possible multiple line header 

						// add to the value array of header key
						//log.debug(threadMessage("last header:" + lastHeader));

						String v = nextLine.trim();

						//log.debug(threadMessage("v: " + v));

						if(v.endsWith(",") == true){

							v = v.substring(0, v.length()-1);
							//log.debug(threadMessage("without comma v: " + v));
							multiLine = true;
						}else{
							multiLine = false;
						}

						headers.updateHeader(lastHeader, v ); 
					} 
					else{ // this is a Single Line Header

						String header, value;


						header = nextLine.substring(0, colon).toLowerCase().trim(); // exclude the colon
						value = nextLine.substring(colon+1, nextLine.length()).trim(); 

						List<String> val = new ArrayList<String>();

						if(value.endsWith(",")){ // this is a possible multiline multivalue header, handle the headless values above
							multiLine = true;
						}

						if(header.compareTo("user-agent") != 0){ // one line comma separated multivalue header or just regular header
							
							// cookies usually have name=value pair and a session token
							if(header.compareTo("cookie") == 0 ){

								log.debug("got a cookie header ");
								
								String [] cookieattrs = value.split(";");
								String sessionToken = null;
								String name = null;
								String cookieval = null;

								for(String attr  : Arrays.asList(cookieattrs) ){
									
									
									
									String[] nameval =  attr.trim().split("=");
//									if( nameval[0].trim().toLowerCase().compareTo("sessiontoken") == 0 ){
//										sessionToken = nameval[0].trim();
//										requested_session_id = sessionToken; // set to the last one?
//									}else{
										name = nameval[0];
										cookieval = nameval[1];
//									}
										log.debug("COOKIE name:" + name + " value:"+cookieval);
								}

								Cookie cookie = new Cookie(name, cookieval);
								//cookie.setSessionId(sessionToken);
								
								log.debug("Adding COOKIE HEADER: name:"+header+" value:"+value);
								//List<String> cval = new ArrayList();
								val.add(value);
								
								//headers.addHeader(header, cval);
								
								// save in an object for easy handling later
								headers.addCookie(cookie);

							}
							else{
								// single value header
								for( String v : Arrays.asList(value.split(","))){
									val.add(v.trim());
								}
							}

						}
						else{ // parse user-agent values specially because they can have comments in brackets (...)

							int pos = 0;

							//log.debug("parsing user-agent");

							while( value != null ){

								//log.debug("value: " + value);

								// its a comment, add it to the previous user-agent info
								if(value.startsWith("(")){

									int endparen = value.indexOf(')');
									if(endparen > 0){
										log.debug("pos: " + pos);
										int prev = pos-1;
										log.debug("prev: " + prev);
										if( prev >= 0){ //there is a valid user agent to apply comment to
											val.set(prev, val.get(prev) + " " + value.substring(0, endparen+1) );
										}
									}

									value = ( value.substring(endparen+1).trim() );
									pos = pos-1; // since we move the potential element to the previous element 

								}
								else {
									int nextSpace = value.indexOf(' ');
									if(nextSpace < 0 ){ // last agent thing
										val.add(value.trim());
										value = null;
									}
									else{
										val.add(value.substring(0, nextSpace));
										value = value.substring(nextSpace).trim();
									}

								}
								pos ++;
							}

						}


						headers.addHeader(header, val);

						//log.debug(threadMessage("Header: " + header + 
						//		"  |  Value: " + val.toString()));

						lastHeader = header;
					}

					nextLine = reader.readLine();
					log.debug("Next line: " + nextLine);
				}
				else{
					nextLine = null; // trigger the termination condition
				}
			}

			log.debug("Out of while loop: ");
			
			log.debug("m_method: " + m_method);
			
			// Extract the body of a POST request, assuming there is one
			if(m_method.toLowerCase().compareTo("post") == 0){
				
				log.debug("Trying to read post body");
//				int c = reader.read();
//				log.debug("Read a single character? value: " + String.valueOf(c));
//				c = reader.read();
//				log.debug("Read a single character? value: " + String.valueOf(c));
//				String body = reader.readLine();
				
				int content_length = Integer.parseInt(headers.getHeader("content-length").get(0));
				StringBuffer data = new StringBuffer();
				
				for(int i = 0; i < content_length; i++){
					int c = reader.read();
					log.debug("c: " + (char)c); 
					data.append((char) c);
				}
				
				String post_data = data.toString();
				log.debug("body: " + post_data);
				
				String[] params = post_data.split("\\?|&|=");

				if(params.length >= 2 ){
					if(params.length % 2 ==0 ){
						log.debug("params have been parsed");
						for (int j = 0; j < params.length - 1; j += 2) {
							log.debug("key: " + params[j] + " value: " + params[j+1]);

							m_params.setProperty(params[j], params[j+1]);
						}
					}else{
						//malformed error 
						log.debug("ERROR! params list is malformed!");
						return 400;
					}
				}
				
//				if( body != null ){ // end of the stream is not reached
//					
//					while( body != null && body.isEmpty() == false ){

//						if(!body.isEmpty()){
//							
//						}
//						
//						body = reader.readLine();
//						
//					}
					
				}
				
//			}
			
			
			
			m_headers = (HashMap<String, List<String>>) headers.getHeaders();
			
			//update connection closed flag for persistent connections
			if(m_headers.containsKey("connection") == true){
				keepalive = ( m_headers.get("connection").get(0).compareTo("keep-alive") == 0 ? true:false );
			}
			

			log.debug("Header (Cookies): " + headers.getCookies().toString());
			
			
			
			Object [] objs = (headers.getCookies()).toArray();
			cookies = new Cookie[objs.length];
			int i = 0;
			for (Object o : objs) {
			   cookies[i++] = (Cookie) o;
			}
			
			//cookies = (Cookie[])((headers.getCookies()).toArray());
			


			Map<String,List<String>> stringHeaders = headers.getHeaders();
			for( String key : stringHeaders.keySet()){

				log.debug("key : " + key + ": " + stringHeaders.get(key).toString());

			}
			
			
			
//			if (cookies != null){
//				// find the session matching it
//				
//				List <Cookie> cookieList = Arrays.asList(cookies); 
//				
//				for(Cookie c : cookieList  ){
//					//String sid = c.getSessionId();
//					if ( sessionMap.containsKey(sid) ){
//						m_session = sessionMap.get(sid);
//						break; // for now, not sure how to deal with multi cookie things...
//					}
//					else{ // session got invalidated??? Probably need to renew.
//						m_session = new myHttpServletSession();
//						
//						synchronized(sessionMap){
//							sessionMap.put(m_session.getId(), m_session);
//						}
//						break; // for now, not sure how to deal with multi cookie things...
//					}
//				}
//				
//
//			}
//			else{ // this is probably the first time a client connected for this session....
//				//new session
//				m_session = new myHttpServletSession();
//				
//				synchronized(sessionMap){
//					sessionMap.put(m_session.getId(), m_session);
//				}
//			}
			
			
			String servletName = null;
			if( m_servlet_path != null ){ // servlet name is detected
				
				
				
				List<String> urlpatterns = new ArrayList<String>(servletURLMap.values());
				Collections.sort( urlpatterns, new Comparator<String>()
					{
						public int compare(String s1, String s2) {
							return s2.length() - s1.length();
						}
						
					});
				
				String sname = null;
				pattern_matched = null;
				
				log.debug("checking for servlets with the Servlet path: " + m_servlet_path);
				
				String servlet_path_exact = m_servlet_path+"/";
				
				for( String pattern  : urlpatterns  ){
					
					log.debug("checking Servlet URL pattern: " + pattern);
					
					if(servlet_path_exact.compareTo(pattern) == 0 ){
						log.debug("matched Servlet URL pattern: " + pattern);
						pattern_matched = pattern;
						for( String key : servletURLMap.keySet() ){
							if( pattern.equals(servletURLMap.get(key)) ){
								sname = key;
							}
						}
						
						
						path_info = servlet_path_exact.substring(pattern.length());

												
						break;
					}
					else if(pattern.endsWith("/*")){
						//get rid of the astrix for matching purposes
						String adjusted_pattern = pattern.substring(0, pattern.length()-1);
						
						if( servlet_path_exact.startsWith(adjusted_pattern) ){ // longest match should be caught first
							log.debug("matched Servlet URL pattern: " + pattern);
							pattern_matched = pattern;
							for( String key : servletURLMap.keySet() ){
								if( pattern.equals(servletURLMap.get(key)) ){
									sname = key;
								}
							}
							
							//get any path info following the servlet path
							
							path_info = servlet_path_exact.substring(adjusted_pattern.length());
							break;
						}
					}
					
				}
				
				if( sname == null  ){ //no servlet found
					
					// throw a 404 error
					log.debug("no Servlet patterns matched... ");
					
				}else{
					servletName = sname;
					
					log.debug("Matched servlet pattern for " + sname +  "!");
					
					m_servlet = servletMap.get(servletName);
					
					if(m_servlet == null){
						log.debug("Could not find servlet in URL map...");
						// servlet not found error 404?
					}
										
				}
				
			} else{ //static page servlet?
				//HttpServlet servlet = servletMap.get("staticServlet");
				log.debug("servlet Path not Detected!...");
				
				m_servlet = null;
//				if(servlet == null){
//					// servlet not found error 404?
//				}
				
			}
			

		} catch (SocketTimeoutException timeout ){

			//cp.setConnectionPreference(false);

			//TODO should create and enum for these values
			return -1;

		}catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			//synchronized(state){
			//	state = "ERROR in reading Headers";
			//}
			return -2;
		}


		// all is fine
		return 0;

	}


}


