package edu.upenn.cis.cis455.webserver;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import org.apache.log4j.Logger;

import edu.upenn.cis.cis455.webserver.myServer.ShutdownControl;
import edu.upenn.cis.cis455.webserver.servlet.myHttpServlet;
import edu.upenn.cis.cis455.webserver.servlet.myHttpServletRequest;
import edu.upenn.cis.cis455.webserver.servlet.myHttpServletResponse;
import edu.upenn.cis.cis455.webserver.servlet.myHttpServletSession;


public class workerThread extends Thread {
	
	Logger log = Logger.getLogger(workerThread.class);

	
	// Instance variables
	String state = "";
	String root = "";
	int id;
	ShutdownControl shutdownCtrl = null;
	boolean personalShutdownFlag = false;
	Lock shutdownLock;
	Socket requestSocket = null;
	myBlockingQueue reqQ;
	statusHandle statusHandle = null;
	HashMap<String, HttpServlet> servletMap;
	HashMap<String, String> servletURLMap;
	HashMap<String, myHttpServletSession> sessionMap;
	// End Instance variables

	
	// for communicating Persistent Connection intentions or connection close intensions
	class ConnectionPreference {

		private boolean close;


		public void setConnectionPreference(boolean keepalive){
			close = keepalive;
		}

		public boolean getConnectionPreference(){
			return close;
		}

	}

	private class StringComparator implements Comparator<String>{

		@Override
		public int compare(String s1, String s2) {
			return s1.compareTo(s2);
		}
		
	}
		
	
	public workerThread(int i, String s, myBlockingQueue q, String rootdir, ShutdownControl shutd){
		id = i;
		state = s;
		reqQ = q;
		root = rootdir;
		shutdownCtrl = shutd;
		shutdownLock = new ReentrantLock();
		log.debug("Initialized workerThread with |id: " + id + " |state: " + state + "|requestQ_ref: " + reqQ.hashCode());
	}

	//for logging pretty-printing Thread
	private String threadMessage(String msg){
		StringBuffer m = new StringBuffer();
		m.append("Thread " + id + ": ");
		m.append(msg);
		return m.toString();
	}
	
	
	// for grabbing the thread's current state for status in "Control" page
	public String getThreadState(){
		
		String currentstate_snap = "";
		synchronized(state){
			currentstate_snap = state;
		}
		
		return currentstate_snap;
		
	}
	
	/** 
	 * Set the statusHandle class for this thread. statusHandle just contains the obj refs
	 *   of all other threads in the thread pool so that this thread can query the status of
	 *   all other Threads when a control page is requested.
	 **/
	public void setStatusHandle( statusHandle sh ){
		statusHandle = sh;
	}
	

	/** 
	 * Set the setServlets Map for this thread. servlets just contains the class literals
	 *   when a servlet is ready to be invoked
	 **/
	public void setServlets( HashMap<String, HttpServlet> servlets  ){
		servletMap = servlets; 
	}
	
	/** 
	 * Set the servletURLMappings for this thread. mappings just contains the url patterns
	 *   for the servlets so that when processing the queries, we can use the regex provided
	 *   in the web.xml to match request to Servlet.
	 **/
	public void setServletURLMappings( HashMap<String, String> mappings  ){
		servletURLMap = mappings; 
	}
	
	/** 
	 * Set the sessionMap for this thread. sessionMappings just contains the key-value pairs
	 *   for the uuids to session objects. A thread could create a session and store it on the 
	 *   "server" and subsequently access it per cookie-indicated string.
	 **/
	public void setSessionMap( HashMap<String, myHttpServletSession> sessionMappings  ){
		sessionMap = sessionMappings; 
	}
	
	
	// Check if the URI specified by the request string was a URI
	private int checkifURL(String filename){
		
		String lower = filename.toLowerCase();
		
		if( lower.startsWith("http://")){
			return 1;
		}
		else if(lower.startsWith("https://") ){
			return 2;
		}
		return 0;
		
	}
	
	//populate the request with parameters
	private void populateParameters(myHttpServletRequest req, String resource){
		
		String[] strings = resource.split("\\?|&|=");
		
		if(strings.length >= 2 ){
			for (int j = 1; j < strings.length - 1; j += 2) {
				req.setParameter(strings[j], strings[j+1]);
			}
		}
		else{
			//no parameters... 
			
		}
	}
	
	
	// If given a Absolute path in the form of a URL, return the portion just after the host and port
	//   number ("from / root")
	private String parseURLforResource(String filename, int http){
		
		String httpstripped = "";
		
		if(http == 1){
			httpstripped = filename.substring("http://".length());
			log.debug(threadMessage("http:// : " + httpstripped));
		}
		else if (http == 2){
			httpstripped = filename.substring("https://".length());
			log.debug(threadMessage("https:// : " + httpstripped));
		}
		
		
		// we want to keep the slash after the port number 
		String hostportstripped = httpstripped.substring(httpstripped.indexOf("/"));
		log.debug(threadMessage("url stripped : " + hostportstripped));
		
		return hostportstripped;
		
	}
	
	
	public String getControlPageText(){
		
		StringBuffer controlPage = new StringBuffer();
		File f = new File("resources/controlpage.html");
		//InputStream inputStream = workerThread.class.getResourceAsStream("resources/controlpage.html");
		
		
		String line;
		try {
			BufferedReader reader = new BufferedReader(new FileReader(f));
			
			while( (line = reader.readLine()) != null ){
				controlPage.append(line);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		// input Thread statuses in response.
		String statuses = statusHandle.getAllThreadStatus();
		
		String temp = controlPage.toString();
		String controlWithStatus = temp.replace("<!-- Include Thread Status HERE -->", statuses);
		
		
		return controlWithStatus;
		
	}
	
	// Simple check to see if the filepath contains any up-directory references, which would mean
	//   user is attempting to access outside the root directory.
	public boolean validatePath(Path filePath){
		
		//Path filePath = Paths.get(file).toAbsolutePath();
		
		log.debug("Absolute path of file/directory: " + filePath.toString());
		log.debug("Absolute path of root: " + root);
		
		if( filePath.startsWith(root) ){
			return true;
		}
		return false;
		
	}
	
	
	// Method for server to propogate shutdown to threads by set the personal_shutdownflag that 
	// each thread can check after handling a request.
	public void setShutdown(){
		shutdownLock.lock();
		personalShutdownFlag = true;
		shutdownLock.unlock();
	}
	
	
	
	// get the suffix for the file to determine the type
	private String getMimeType(String file){
		
		int dotIndex = file.lastIndexOf('.');
		if(dotIndex == -1){
			return null;
		}
		String ext = file.substring(dotIndex+1, file.length()).toLowerCase(); 
		
		log.debug(threadMessage("ext found: " + ext));
		
		
		if(ext.compareTo("html") == 0){
			return "text/html";
		}
		else if(ext.compareTo("css") == 0){
			return "text/css";
		}
		else if(ext.compareTo("txt") == 0){
			return "text/plain";
		}
		else if(ext.compareTo("jpg") == 0 || ext.compareTo("jpeg") == 0){
			return "image/jpeg";
		}
		else if(ext.compareTo("gif") == 0){
			return "image/gif";
		}
		else if(ext.compareTo("png") == 0){
			return "image/png";
		}
		else{
			return null;
		}
		
	}
	
	
	private static void sendBinaryData(FileInputStream fis, DataOutputStream outStream)
	        throws Exception
	{
	    // Construct a 1K buffer to hold bytes on their way to the socket.
	    byte[] buffer = new byte[1024];
	    int bytes = 0;

	    // Copy requested file into the socket's output stream.
	    while ((bytes = fis.read(buffer)) != -1)// read() returns minus one, indicating that the end of the file
	    {
	        outStream.write(buffer, 0, bytes);
	    }
	}

	// Handle the get request
	public void doGet( Socket clientSocket, BufferedReader reader, String[] requestParts, ConnectionPreference cp, myHttpServletSession session) throws ServletException, IOException{

		
		String filename = requestParts[1];
		String httpVersion = requestParts[2];
		
		Map<String, List<String>> headers = new HashMap<String, List<String>>();

		String nextLine;
		
		//log.debug(threadMessage("using Socket: " + clientSocket.toString()));
		
		synchronized(state){
			state = "Handling " + filename;
		}
		
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
						
						headers.get(lastHeader).add( v ); 
					} 
					else{ // this is a Single Line Header
						
						String header, value;

						
						header = nextLine.substring(0, colon).toLowerCase().trim(); // exclude the colon
						value = nextLine.substring(colon+1, nextLine.length()).trim(); 

						List<String> val = new ArrayList<String>();

						if(value.endsWith(",")){ // this is a possible multiline multivalue header, handle the headless values above
							multiLine = true;
						}

						if(header.compareTo("user-agent") != 0){ // one line comma separated multivalue header

							for( String v : Arrays.asList(value.split(","))){
								val.add(v.trim());
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


						headers.put(header, val);

						//log.debug(threadMessage("Header: " + header + 
						//		"  |  Value: " + val.toString()));

						lastHeader = header;
					}
					
					nextLine = reader.readLine();
					//log.debug(threadMessage("Next line: " + nextLine));
				}
				else{
					nextLine = null; // trigger the termination condition
				}
			}
			
			log.debug(threadMessage("Out of while loop: "));
			
			
//			if(malformedHeader == true){
//				
//				log.debug(" malformed Header");
//				
//				// Malformed request, send a 400 Bad Request.
//				int code = 400;
//				String mimeType = "text/html";
//				String response = HttpResponseUtils.writeErrorResponse(code, httpVersion);
//
//				PrintWriter out;
//				try {
//					out = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
//
//					out.write(response);
//					out.flush();
//
//					out.close();
//				} catch (IOException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}				
//			}
			
			for( String key : headers.keySet()){
				
				log.debug("key : " + key + ": " + headers.get(key).toString());
				
				
			}
			
			
		} catch (SocketTimeoutException timeout ){
			try {
				clientSocket.close();
				cp.setConnectionPreference(false);
				return;
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			synchronized(state){
				state = "ERROR in reading Headers";
			}
		}

		/** Http Servlet code here  **/
		
		myHttpServletRequest req = new myHttpServletRequest(session);
		myHttpServletResponse res = new myHttpServletResponse();
		
		// Fill in the Method, parameters?, and headers
		req.setMethod(requestParts[0]);
		req.setProtocol(httpVersion);
		
		for( String key : headers.keySet()){
			req.setHeader(key, headers.get(key));	
		}
		
		// If any parameters are provided as a part of the HTTP request add them the request object 
		populateParameters(req, filename);
		
		
		int isUrl;
		if( (isUrl = checkifURL(filename)) > 0 ){
			log.debug(threadMessage("filename is an absolute URL, parsing " ));
			String resource = parseURLforResource(filename, isUrl);
			
			filename = resource;
			
		}
		
		
		// Check if the resource requested is a servlet
		int question = filename.indexOf('?');
		String servletName; 
		
		if(question != -1){
			servletName = filename.substring(0, filename.indexOf('?'));
		}else{
			servletName = filename;
		}
		
		List<String> urls = new ArrayList<String>(servletURLMap.values());
		Collections.sort( urls, new StringComparator());
		
		String sname = null;
		for( String pattern  : urls  ){
			
			if( filename.startsWith(pattern) ){ // longest match should be caught first
				for( String key : servletURLMap.keySet() ){
					if( pattern.equals(servletURLMap.get(key)) ){
						sname = key;
					}
				}
				
				break;
			}
			
		}
		
		if( sname == null  ){ //no servlet found
			
			servletName = "default";
			
		}else{
			servletName = sname;
		}
		
		HttpServlet servlet = servletMap.get(servletName);
		
		if(servlet == null){
			// servlet not found error
		}
		else{
			servlet.service(req, res);
		}
		
		/***End Servlet Code here***/
		
		
		
		
		// check headers for HTTP 1.1 compliance
		boolean httpCompliant = false;
		
		log.debug("Checking if this is a HTTP 1.1 client");
		if( httpVersion.compareTo("HTTP/1.1") == 0){
			log.debug("This is a HTTP 1.1 client, checking compliance... (It must contain at least Host: header)");
			if(!headers.containsKey("host")){

				log.debug(" 'Host:' header not found! this is not HTTP compliant");
				// Malformed request, send a 400 Bad Request.
				int code = 400;
				String mimeType = "text/html";
				String response = HttpResponseUtils.writeErrorResponse(code, httpVersion, false);

				PrintWriter out;
				try {
					out = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream()));

					out.write(response);
					out.flush();

					//out.close();
				} 
				catch (SocketTimeoutException timeout ){
					try {
						clientSocket.close();
						cp.setConnectionPreference(false);
						return;
					} catch (IOException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
				}
				catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}				

			}
			else{
				log.debug(" 'Host:' header found ==> HTTP compliant");
				httpCompliant = true;
			}
		}else{ // HTTP 1.0 doesn't require Headers
			log.debug(" HTTP/1.0 No 'Host:' header required ==> HTTP 1.0 compliant");
			httpCompliant = true;
		}


		if(httpCompliant == true){
			try {
				// Write response (headers and body) for single response
				//if single file... (assuming we validate path and figure out if it exists prior to this...)

				File f;
				boolean isDirectory = false;
				boolean isControl = false;
				boolean expectContinue = false;

				String mimeType = "text/html";

				
				// check for the 100 Continue header
				if(headers.containsKey("expect") ){
					
					log.debug("checking expect: header value");
					for(String expects : headers.get("expect")){
						if(expects.toLowerCase().compareTo("100-continue") == 0 
								&& httpVersion.compareTo("HTTP/1.0") != 0){
							expectContinue = true;
						}
					}
					
				}
				
				
				// check for the 'Connection: ' header value
				
				if(headers.containsKey("connection")){
					
					log.debug("checking the value for Connection: header value");
					
					for(String connectionAlive : headers.get("connection")){
						if(connectionAlive.toLowerCase().compareTo("close") == 0){
							log.debug("Connection: header is close");
							cp.setConnectionPreference(false);
						}
					}

				}
				else{ // no connection header value defaults to single request then connection close
					log.debug("No Connection: header  DEFAULTING to close");
					cp.setConnectionPreference(false);
				}
				
				log.debug("Connection preference currently set to: " + ( cp.getConnectionPreference() == true ? "keep-alive" : "close" ));
				
				

				log.debug(threadMessage("filename requested: " + filename));


				// default webpage
				int http;
				if( (http = checkifURL(filename)) > 0 ){
					log.debug(threadMessage("filename is an absolute URL, parsing " ));
					String resource = parseURLforResource(filename, http);
					
					filename = resource;
					
				}
				
				
				f = new File(root+"/"+filename);
				
				
				if(filename.compareTo("/") == 0){
					//filename = "index.html";
					String explicitPath = root;  
					log.debug(threadMessage("Requested " + explicitPath));
					isDirectory = true;
					
				}
				
				//special control URLs
				else if ( filename.compareTo("/shutdown") == 0 ){
					// in case server is propogating shutdown and we are changing it too, doesn't matter
					//   b/c it is idempotent.

					personalShutdownFlag = true; // volatile boolean

					log.debug(threadMessage("Requested /SHUTDOWN"));
					
				}
				else if ( filename.compareTo("/control") == 0 ){
					log.debug(threadMessage("Requested /CONTROL"));
					isControl = true;
					
				}
				// its a directory
				else if ( f.isDirectory() == true ){
					// generate the list of files in the directory
					log.debug(threadMessage("Requested a directory!"));
					
					isDirectory = true;
				}
				// its a single file.
				else {
					log.debug(threadMessage("Requested a file!"));
					
				}

				int code = 0;
				String body = "";
				String response = "";




				if( personalShutdownFlag == true ){ // we got a shutdown requested

					code = 200;
					mimeType = "text/html";
					response = HttpResponseUtils.writeResponseHeaders(code, httpVersion, false);

					PrintWriter out = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
					//Check for 100 Continue flag
					if( expectContinue == true ){
						String continueResponse = HttpResponseUtils.getContinueResponse();
						out.write(continueResponse);
					}
					out.write(response);
					out.flush();

					//out.close();

				}

				else if(isControl){  // Return the control page
					String controlPage = getControlPageText();
					
					File controlFile = new File("resources/controlpage.html");

					code = 200;
					mimeType = "text/html";
					response = HttpResponseUtils.writeResponseHeaders(code, mimeType, controlPage, httpVersion, controlFile.lastModified(), cp.getConnectionPreference());

					PrintWriter out = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
					//Check for 100 Continue flag
					if( expectContinue == true ){
						String continueResponse = HttpResponseUtils.getContinueResponse();
						out.write(continueResponse);
					}
					out.write(response);
					out.flush();

					//out.close();

				}

				else if(!f.exists() ){ // check if file exists

					// respond with 404 file not found
					code = 404;
					log.debug(threadMessage("ERROR - File does not exist!"));
					response = HttpResponseUtils.writeErrorResponse(code, httpVersion, cp.getConnectionPreference());
					PrintWriter out = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
					out.write(response);
					out.flush();

					//out.close();

				}
				
				else{ // file or directory exists
					
					if(!f.canRead()){ //check if file is readable
						
						// respond with 403 file permission denied
						code = 403;
						log.debug(threadMessage("ERROR - File access denied!"));
						response = HttpResponseUtils.writeErrorResponse(code, httpVersion, cp.getConnectionPreference());
						PrintWriter out = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
						out.write(response);
						out.flush();

						//out.close();
					}
					else { // file is readable

						log.debug(threadMessage("File or directory exists!"));

						// validate the path...

						Path pathString =  Paths.get(root+"/"+filename).toRealPath();

						log.debug("Validating path:   "+ pathString.toString());

						boolean validPath = validatePath(pathString);

						if( validPath == false ){

							code = 403;
							log.debug(threadMessage("ERROR - File access forbidden!"));
							response = HttpResponseUtils.writeErrorResponse(code, httpVersion, cp.getConnectionPreference());
							PrintWriter out = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
							out.write(response);
							out.flush();

							//out.close();


						}
						else { // it is a valid path

							// check if the request provided the modified/unmodified-Since flag
							boolean modifiedSinceHeader = false;
							boolean unmodifiedSinceHeader = false;

							boolean preconditionMet = true;

							if(headers.containsKey("if-modified-since") && headers.containsKey("if-unmodified-since")){

								//error.... It does not make sense to have both headers.

								log.debug(" 'Host:' header not found! this is not HTTP compliant");
								// Malformed request, send a 400 Bad Request.
								code = 400;
								mimeType = "text/html";
								response = HttpResponseUtils.writeErrorResponse(code, httpVersion, cp.getConnectionPreference());

								PrintWriter out;
								try {
									out = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream()));

									out.write(response);
									out.flush();

									//out.close();
								} catch (SocketTimeoutException timeout ){
									try {
										clientSocket.close();
										cp.setConnectionPreference(false);
										return;
									} catch (IOException e1) {
										// TODO Auto-generated catch block
										e1.printStackTrace();
									}
								}catch (IOException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}				

							}
							else if(headers.containsKey("if-modified-since")){
								modifiedSinceHeader = true;	
							}
							else if(headers.containsKey("if-unmodified-since")){
								unmodifiedSinceHeader = true;
							}

							Date lastModified = new Date(f.lastModified());

							if(modifiedSinceHeader ){

								log.debug(threadMessage("checking if the file was modified since certain date"));

								Date headerModified = HttpResponseUtils.parseHeaderDate(headers.get("if-modified-since").get(0));

								if(headerModified == null ){ // ignore the malformed header
									preconditionMet = true;
								}
								else if( lastModified.before( headerModified ) || lastModified.equals( headerModified )){
									if(headerModified != null){
										log.debug(" File had not been modified since: "+ headerModified.toString());
									}
									// Malformed request, send a 400 Bad Request.
									code = 412;
									mimeType = "text/html";
									response = HttpResponseUtils.writeErrorResponse(code, httpVersion, cp.getConnectionPreference());

									PrintWriter out;
									try {
										out = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream()));

										out.write(response);
										out.flush();

										//out.close();
									} catch (SocketTimeoutException timeout ){
										try {
											clientSocket.close();
											cp.setConnectionPreference(false);
											return;
										} catch (IOException e1) {
											// TODO Auto-generated catch block
											e1.printStackTrace();
										}
									}catch (IOException e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
									}		

									preconditionMet = false;

								}

							}
							else if (unmodifiedSinceHeader ){

								log.debug(threadMessage("checking if the file was modified since certain date"));

								Date headerModified = HttpResponseUtils.parseHeaderDate(headers.get("if-unmodified-since").get(0));

								if(headerModified == null){ // ignore the malformed data
									log.debug(threadMessage("Error Parsing date, ignoring the header"));
									preconditionMet = true;
								}

								else if( lastModified.after( headerModified )){

									log.debug(" File had not been unmodified since: "+ headerModified.toString());
									// Malformed request, send a 400 Bad Request.
									code = 412;
									mimeType = "text/html";
									response = HttpResponseUtils.writeErrorResponse(code, httpVersion, cp.getConnectionPreference());

									PrintWriter out;
									try {
										out = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream()));

										out.write(response);
										out.flush();

										//out.close();
									} catch (SocketTimeoutException timeout ){
										try {
											clientSocket.close();
											cp.setConnectionPreference(false);
											return;
										} catch (IOException e1) {
											// TODO Auto-generated catch block
											e1.printStackTrace();
										}
									}catch (IOException e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
									}		

									preconditionMet = false;

								}
							}

							// either the If-Unmodified/Modified-Since headers were not included or the precondition was met
							//    either one.
							if(preconditionMet == true){ 
								
								if(isDirectory){
									log.debug(threadMessage("Got a directory request"));
									StringBuffer directoryContents = new StringBuffer();
									StringBuffer fileContents = new StringBuffer();

									// generate html page of the files in directory....

									File directoryPage = new File("resources/directory.html");

									BufferedReader directoryReader = new BufferedReader(new FileReader(directoryPage));

									String line;

									while((line = directoryReader.readLine()) != null){
										directoryContents.append(line);
									}


									File[] listofFiles = f.listFiles();

									for(File file : listofFiles){

										if (file.isFile()) {
											fileContents.append("<p> - " + file.getName()+"</p>");
										} else if (file.isDirectory()) {
											fileContents.append("<p style=\"font-weight:bold\"> - " + file.getName() +"/</p>");
										}

									}

									String directory = directoryContents.toString();
									directory = directory.replace("<!-- Include files HERE -->", fileContents.toString());
									directory = directory.replace("<!-- Directory Name -->", f.getName()+"/");

									// set the code 
									code = 200;
									body = directory.toString();
									mimeType = "text/html";

									response = HttpResponseUtils.writeResponseHeaders(code, mimeType, body, httpVersion, f.lastModified(), cp.getConnectionPreference());

									PrintWriter out = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream()));

									//Check for 100 Continue flag
									if( expectContinue == true ){
										String continueResponse = HttpResponseUtils.getContinueResponse();
										out.write(continueResponse);
									}


									out.write(response);
									out.flush();

									//out.close();

								}

								else{
									mimeType = getMimeType(filename);
									
									if(mimeType == null && f.isFile() == false ){
										//unsupported file type 415
										code = 415;

										// generate the html here...
										response = HttpResponseUtils.writeErrorResponse(code, httpVersion, cp.getConnectionPreference());

										PrintWriter out = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
										out.write(response);
										out.flush();

										//out.close();
										
									}
									else if(mimeType == null && f.isFile() == true ){ // Unsupported Media Type
										log.debug(threadMessage("file type unknown: "));
										
										
										code = 200;
										mimeType = "application/octet-stream";
										
										Path path = FileSystems.getDefault().getPath(root, filename);
										BasicFileAttributes attrs = Files.readAttributes(path , BasicFileAttributes.class);
										// re-use image byte stream headers to send data 
										response = HttpResponseUtils.writeImageResponseHeaders(code, mimeType, httpVersion, attrs.size(), f.lastModified(), cp.getConnectionPreference() );

										PrintWriter outHeaders = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream()));

										//Check for 100 Continue flag
										if( expectContinue == true ){
											String continueResponse = HttpResponseUtils.getContinueResponse();
											outHeaders.write(continueResponse);
										}

										outHeaders.write(response);
										outHeaders.flush();

										FileInputStream fileInputStm = new FileInputStream(f);
										DataOutputStream  dataOutputStm = new DataOutputStream(clientSocket.getOutputStream());

										try {
											sendBinaryData(fileInputStm, dataOutputStm);
										} catch (SocketTimeoutException timeout ){
											try {
												clientSocket.close();
												cp.setConnectionPreference(false);
												return;
											} catch (IOException e1) {
												// TODO Auto-generated catch block
												e1.printStackTrace();
											}
										}catch (Exception e) {
											// TODO Auto-generated catch block
											e.printStackTrace();
											synchronized(state){
												state = "ERROR in sending Binary Data";
											}
										}

										//fileInputStm.close();
										//dataOutputStm.close();
										//outHeaders.close();
										
									}
									else if ( mimeType.substring(0, mimeType.indexOf("/")).compareTo("image") == 0 ){ // its an image
										log.debug(threadMessage("Got image file type: "+mimeType));

										//set the code
										code = 200;
										
										Path path = FileSystems.getDefault().getPath(root, filename);
										BasicFileAttributes attrs = Files.readAttributes(path , BasicFileAttributes.class);
										response = HttpResponseUtils.writeImageResponseHeaders(code, mimeType, httpVersion, attrs.size(), f.lastModified(), cp.getConnectionPreference() );

										PrintWriter outHeaders = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream()));

										//Check for 100 Continue flag
										if( expectContinue == true ){
											String continueResponse = HttpResponseUtils.getContinueResponse();
											outHeaders.write(continueResponse);
										}

										outHeaders.write(response);
										outHeaders.flush();

										FileInputStream fileInputStm = new FileInputStream(f);
										DataOutputStream  dataOutputStm = new DataOutputStream(clientSocket.getOutputStream());

										try {
											sendBinaryData(fileInputStm, dataOutputStm);
										} catch (SocketTimeoutException timeout ){
											try {
												clientSocket.close();
												cp.setConnectionPreference(false);
												return;
											} catch (IOException e1) {
												// TODO Auto-generated catch block
												e1.printStackTrace();
											}
										}catch (Exception e) {
											// TODO Auto-generated catch block
											e.printStackTrace();
											synchronized(state){
												state = "ERROR in sending Binary Data";
											}
										}

//										fileInputStm.close();
//										dataOutputStm.close();
//										outHeaders.close();

									}else { // its text file

										log.debug(threadMessage("Got text file type: "+mimeType));

										BufferedReader filereader = new BufferedReader(new FileReader(f));

										StringBuffer contents = new StringBuffer();
										String line;
										while( (line = filereader.readLine()) != null ){
											contents.append(line);
										}

										log.debug(threadMessage("file contents: " + contents.toString()));

										//set the code
										code = 200;
										body = contents.toString();

										response = HttpResponseUtils.writeResponseHeaders(code, mimeType, body, httpVersion, f.lastModified(), cp.getConnectionPreference());

										PrintWriter out = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream()));

										//Check for 100 Continue flag
										if( expectContinue == true ){
											String continueResponse = HttpResponseUtils.getContinueResponse();
											out.write(continueResponse);
										}

										out.write(response);
										out.flush();

										//out.close();
									}
								}

							}
						}
					}
				}
				//clientSocket.close();

			} catch (SocketTimeoutException timeout ){
				try {
					clientSocket.close();
					cp.setConnectionPreference(false);
					return;
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				synchronized(state){
					state = "ERROR in retreiving File";
				}
			}

		}
	}

		
	
	/****
	 *   MS2 code using servlets
	 * 
	 * ****/
	
	public void processRequest( Socket requestSocket, ConnectionPreference cp  ) 
			throws SocketTimeoutException, IOException, ServletException
	{
		
		
		BufferedReader reader = new BufferedReader(new InputStreamReader(requestSocket.getInputStream()));
		
		String requestLine = reader.readLine();
		String filename = null ;
		String httpVersion = null;
		String method = null;

		log.debug(threadMessage("requestline Text: " + requestLine));


		if(requestLine != null){

			String[] requestParts = requestLine.split("\\s+");


			if(requestParts.length != 3){
				//send 400 bad request

				log.debug(threadMessage(" Request does not have all of the parts!"));
				int code = 400;
				log.debug(threadMessage("ERROR - Imcomplete Request!"));
				/*for (String part : requestParts){

				if(   ){

				}
			    }*/ //some regex to check if there is a match for HTTP request
				String response = HttpResponseUtils.writeErrorResponse(code, "HTTP/1.1", cp.getConnectionPreference());
				PrintWriter out = new PrintWriter(new OutputStreamWriter(requestSocket.getOutputStream()));
				out.write(response);
				out.flush();
				cp.setConnectionPreference(false);
				return; 

			}
			//GET Request
			else if (requestParts[0].compareToIgnoreCase("GET") == 0 || requestParts[0].compareToIgnoreCase("POST") == 0){
				log.debug(threadMessage(" Got a GET or POST request. "));
				
				method = requestParts[0];
				filename = requestParts[1];
				httpVersion = requestParts[2];
				
				
			}else{  // Unsupported Method

				//invalid method

				log.debug(threadMessage(" Got another kind of request: " + requestParts[0]));
				int code = 501;
				log.debug(threadMessage("ERROR - Method not Implemented!"));
				String response = HttpResponseUtils.writeErrorResponse(code, requestParts[2], cp.getConnectionPreference());
				PrintWriter out = new PrintWriter(new OutputStreamWriter(requestSocket.getOutputStream()));
				out.write(response);
				out.flush();
				
				cp.setConnectionPreference(false);
				return;
				// probably need to flush til the next request line if there is one...
			}

		}
		else{ // nothing to read from the stream its probably indicative to close the stream
			
			requestSocket.close();
			cp.setConnectionPreference(false);
			return;
		}
		
			
		Map<String, List<String>> headers = new HashMap<String, List<String>>();

		String nextLine;
		
		// update state for the /control page
		synchronized(state){
			state = "Handling " + filename;
		}
		
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
						
						headers.get(lastHeader).add( v ); 
					} 
					else{ // this is a Single Line Header
						
						String header, value;

						
						header = nextLine.substring(0, colon).toLowerCase().trim(); // exclude the colon
						value = nextLine.substring(colon+1, nextLine.length()).trim(); 

						List<String> val = new ArrayList<String>();

						if(value.endsWith(",")){ // this is a possible multiline multivalue header, handle the headless values above
							multiLine = true;
						}

						if(header.compareTo("user-agent") != 0){ // one line comma separated multivalue header

							for( String v : Arrays.asList(value.split(","))){
								val.add(v.trim());
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


						headers.put(header, val);

						//log.debug(threadMessage("Header: " + header + 
						//		"  |  Value: " + val.toString()));

						lastHeader = header;
					}
					
					nextLine = reader.readLine();
					//log.debug(threadMessage("Next line: " + nextLine));
				}
				else{
					nextLine = null; // trigger the termination condition
				}
			}
			
			log.debug(threadMessage("Out of while loop: "));
			
			
			for( String key : headers.keySet()){
				
				log.debug("key : " + key + ": " + headers.get(key).toString());
				
				
			}
			
			
		} catch (SocketTimeoutException timeout ){
			try {
				cp.setConnectionPreference(false);
				return;
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			synchronized(state){
				state = "ERROR in reading Headers";
			}
		}

		/** Http Servlet code here  **/
		
		myHttpServletRequest req = new myHttpServletRequest(session);
		myHttpServletResponse res = new myHttpServletResponse();
		
		// Fill in the Method, parameters?, and headers
		req.setMethod(method);
		req.setProtocol(httpVersion);
		
		for( String key : headers.keySet()){
			req.setHeader(key, headers.get(key));	
		}
		
		// If any parameters are provided as a part of the HTTP request add them the request object 
		populateParameters(req, filename);
		
		
		int isUrl;
		if( (isUrl = checkifURL(filename)) > 0 ){
			log.debug(threadMessage("filename is an absolute URL, parsing " ));
			String resource = parseURLforResource(filename, isUrl);
			
			filename = resource;
			
		}
		
		
		// Check if the resource requested is a servlet
		int question = filename.indexOf('?');
		String servletName; 
		
		if(question != -1){
			servletName = filename.substring(0, filename.indexOf('?'));
		}else{
			servletName = filename;
		}
		
		List<String> urls = new ArrayList<String>(servletURLMap.values());
		Collections.sort( urls, new StringComparator());
		
		String sname = null;
		for( String pattern  : urls  ){
			
			if( filename.startsWith(pattern) ){ // longest match should be caught first
				for( String key : servletURLMap.keySet() ){
					if( pattern.equals(servletURLMap.get(key)) ){
						sname = key;
					}
				}
				
				break;
			}
			
		}
		
		if( sname == null  ){ //no servlet found
			
			servletName = "default";
			
		}else{
			servletName = sname;
		}
		
		HttpServlet servlet = servletMap.get(servletName);
		
		if(servlet == null){
			// servlet not found error
		}
		else{
			servlet.service(req, res);
		}
		
		/***End Servlet Code here***/
		
		
	}
	
	
	@Override
	public void run(){
		
		ConnectionPreference cp = new ConnectionPreference();
		
		myHttpServletSession session = new myHttpServletSession();
		
		
		log.debug(threadMessage("grabbing lock..."));
		while (true){
			
			
			cp.setConnectionPreference(true);
			//reset socket each time
			requestSocket = null;
			
			// if we get a shutdown signal, we update the shared shutdown flag so that
			//  server can see it and propagate it, then we exit.
			shutdownLock.lock();
			if(personalShutdownFlag == true){
				shutdownLock.unlock();
				shutdownCtrl.shutdown_requested = true;
				try {
					reqQ.setShutdown();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				break;
			}
			shutdownLock.unlock(); // unlock if we don't go into if statement.
			
			log.debug(threadMessage("beginning of loop..."));
			
			log.debug(threadMessage("attempting to dequeue..."));
			synchronized(state){
				state = "WAITING";
			}
			try {
				requestSocket = reqQ.dequeue();
			} catch (InterruptedException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			
						
			// got a socket, lets handle the request.
			if(requestSocket != null){
				
				
				while (cp.getConnectionPreference() == true){
					synchronized(state){
						state = "WORKING";
					}

					log.debug(threadMessage("Got a request Socket!"));

					// do work.



					try {
						
						processRequest(requestSocket, cp);

					} catch (SocketTimeoutException timeout ){
						try {
							requestSocket.close();
							cp.setConnectionPreference(false);
						} catch (IOException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
					}catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						try {
							requestSocket.close();
							cp.setConnectionPreference(false);
						} catch (IOException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
					} catch (ServletException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				
				try {
					requestSocket.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
			}
			
			// after doing some work, close the socket, assume that for persistent connections, 
			//   we handle in the above while loop
			

		}
		
		log.debug(threadMessage("Shutting Down..."));

	}




}
