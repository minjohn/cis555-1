package edu.upenn.cis.cis455.webserver.servlet;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.IllegalFormatException;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import javax.servlet.ServletResponse;

import edu.upenn.cis.cis455.webserver.HttpResponseUtils;

public class myHttpServletResponse implements HttpServletResponse {

	Logger log = Logger.getLogger(myHttpServletResponse.class);
	
	private Socket requestSocket = null;
	
	private String character_encoding = null;
	private String content_type = null;
	private HashMap<String, List<String>>m_headers = new HashMap<String,List<String>>(); 
	private int status_code = 0;
	private int content_length = 0;
	private String http_protocol = null;
	private boolean keep_alive = false;
	private boolean committed = false;
	
	private int numbytes = 0;
	private StringBuffer strBuffer = new StringBuffer();
	private Byte[] bytebuffer = null;
	private int buffer_size = 0; // unbounded by default
	private PrintWriter outWriter = null;
	private OutputStreamWriter streamWriter = null;
	private Locale locale = null;
	private Long lastMod = null;
	private ByteArrayOutputStream buffer = null;
	private boolean bufferSizeSet = false;
	private boolean fixedBufferedOption = false;
	
	
	private class myServletResponseWriter extends PrintWriter{
		
		OutputStreamWriter outwriter = null;
		
		public myServletResponseWriter(OutputStreamWriter out){
			super(out);
			outwriter = out;
		}
		
		
		public void flush(){
			try {
//				flushBuffer();
				out.flush();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		@Override
		public void close(){
			try{
				out.flush();
				//flushBuffer();
			} catch(IOException e){
				e.printStackTrace();
			}
			super.close();
		}
		
		public void write(String s){
			
			if(fixedBufferedOption == true){
				if( s.length() + buffer.size() >  buffer_size ){

					try {
						//out.flush();
						flushBuffer();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
			else{ 
				super.write(s);
			}

		}
		
	}
	
	
	
	
	//default constructor
	public myHttpServletResponse(Socket s, String protocol, boolean alive){
		requestSocket = s;
		http_protocol = protocol;
		keep_alive = alive;
		
	}

		
	//safe to assume we already have an outwriter?
	@Override
	public void flushBuffer() throws IOException {
		// TODO Auto-generated method stub
		

		if(!isCommitted()){
			
			//String body = strBuffer.toString();
			StringBuffer response = new StringBuffer();
			
			outWriter.flush();
			
			//Status line
			response.append(http_protocol).append(" ").append(Integer.toString(200)).append(" ").append(HttpResponseUtils.getResponseText(200)).append("\n");
			
			// Headers
			response.append("Date: " + HttpResponseUtils.getServerTime()+"\n");
			if(getContentType() != null ){
				response.append("Content-Type: ").append(getContentType()).append("; charset=utf-8\n");
			}
			if (getBufferSize() != -1){
				response.append("Content-Length: ").append(getBufferSize()).append("\n");
			}
					
			
			for(String name : m_headers.keySet()   ){
				
				if( name.compareTo("content-type") != 0 || name.compareTo("content-length") != 0 ){
					if(m_headers.get(name).size() > 1){
						if(name.toLowerCase().compareTo("set-cookie") == 0){
							for(String c_str : m_headers.get(name)){
								response.append(name).append(": ").append(c_str).append("\n");
							}
							
						}else{
							int size = 0;
							size = m_headers.get(name).size();
							response.append(name).append(": ").append(m_headers.get(name).get(0));
							for(int i = 1; i < size; i++ ){
								response.append("; ").append(name).append(": ").append(m_headers.get(name).get(i));
							}
						}
						
					}else{
						response.append(name).append(": ").append(m_headers.get(name).get(0)).append("\n");
					}
					
				}
				
			}
			
			response.append("\n");
			
			log.debug("byte array size for content: " + buffer.size());
			
			response.append(buffer.toString());
			
			log.debug(response.toString());
			
			PrintWriter headersPrintWriter = new PrintWriter(requestSocket.getOutputStream(), true);
			
			headersPrintWriter.println(response.toString());
			
			log.debug("flushed print writer");
			
			committed = true;
		}else{
			throw new IllegalStateException();
		}
		
		
//		OutputStreamWriter outStream = new OutputStreamWriter(requestSocket.getOutputStream());
//		
//		byte[] stage_buffer = new byte[1024];
//		byte[] last_buffer;
//		
//		int index = 0;
//		int length = 0;
//		while( index < buffer.length){
//			
//			length = ( index + 1024 <= buffer.length   )? 1024 : buffer.length -index;
//			if(length != 1024){
//				stage_buffer = new byte[length];
//			}
//			System.arraycopy( buffer, index, stage_buffer, 0, length);
//			
//			outStream.write(stage_buffer, 0, length);
//			index += length;
//		}
	}

	@Override
	public int getBufferSize() {
		
		return buffer.size();
		
	}

	@Override
	public String getCharacterEncoding() {
		if(character_encoding != null){
		  return character_encoding;
		}
		return "ISO-8859-1";
	}

	@Override
	public String getContentType() {
		
		if( content_type != null ){
			return content_type;
		}
		
		return "text/html";
	}

	@Override
	public Locale getLocale() {
		return locale;
	}

	@Override
	public ServletOutputStream getOutputStream() throws IOException {
		return null;
	}

	@Override
	public PrintWriter getWriter() throws IOException {
		if(isCommitted() == false){
			
			if(this.outWriter == null){
				
				// flag to indicate that from now on the buffer size cannot be changed
				bufferSizeSet = true;
				//PrintWriter p = new PrintWriter(requestSocket.getOutputStream());
				
				if(buffer_size > 0){ // user specified buffer size
					fixedBufferedOption = true;
					buffer = new ByteArrayOutputStream(buffer_size);
					
				}else{ // unlimited buffer size
					
					buffer = new ByteArrayOutputStream();
				}
				this.streamWriter = new OutputStreamWriter(buffer, getCharacterEncoding());
				PrintWriter p =  new myServletResponseWriter(this.streamWriter);
				this.outWriter = p;
				return p;
			}
			else{
				return this.outWriter;
			}
			
		}else{
			throw new IllegalStateException();
		}
	}

	@Override
	public boolean isCommitted() {
		
		return committed;
	}

	@Override
	public void reset() {
		
		if(isCommitted() == false){
			
			resetBuffer();
			m_headers.clear();
			status_code = 200;
			
			//reset committed flag
			committed = false;
		}else{
			
			throw new IllegalStateException();
			
		}
		
	}

	@Override
	public void resetBuffer() {
		
		if(isCommitted() == false){
			
//			if(bytebuffer != null){
//
//				int len = bytebuffer.length;
//				bytebuffer[0] = (byte)0;
//				for(int i = 1; i< len; i+=i ){
//					System.arraycopy(bytebuffer, 0, bytebuffer, i, ((len - i) < i) ? (len - i) : i);
//				}
//
//			}
//			
//			//reset string buffer ref
//			strBuffer = new StringBuffer();				
//			
//			buffer_size = -1; // unbounded
			
			buffer.reset();
			
		}else{
			
			throw new IllegalStateException();
			
		}
		
	}

	
	@Override
	public void setBufferSize(int size) {
		
		if(isCommitted()){
			throw new IllegalStateException();
		}
		else if(buffer.size() > 0){ // content was written prior to this call
			throw new IllegalStateException();
		}
		else if(bufferSizeSet == true){
			throw new IllegalStateException();
		}
		else{
			fixedBufferedOption = true;
			buffer_size = size;
		}
		
	}

	@Override
	public void setCharacterEncoding(String encoding) {

		character_encoding = encoding;

	}

	@Override
	public void setContentLength(int len) {
		List<String> list = new ArrayList<String>();
		list.add(String.valueOf(len));
		m_headers.put("content-length", list);
	}

	@Override
	public void setContentType(String type) {
		//content_type = type;
		List<String> list = new ArrayList<String>();
		list.add(type);
		m_headers.put("content-type", list);
	}

	@Override
	public void setLocale(Locale l) {
		
		locale = l;
		
		
	}

	@Override
	public void addCookie(Cookie c) {
		
		if(c != null){
			String name = c.getName();
			StringBuffer value = new StringBuffer();
			
			String cvalue = c.getValue();
			
//			Pattern p = Pattern.compile(".*\\W+.*");
			Pattern p = Pattern.compile("^[a-zA-Z0-9]+$");
			Matcher m_name = p.matcher(name);
			Matcher m_value = p.matcher(cvalue);
			if( !m_name.find()){
				throw new IllegalArgumentException();
			}
			
			// if value has whitespace, put double quotes around it
			if(cvalue.contains(" ")){
				
				// put quotes around value with version less than 1.
				//   version puts quotes around strings with space.
				if(c.getVersion() < 1){
				
					if( (cvalue.startsWith("\"") && cvalue.endsWith("\"")) == false ){
						log.debug(cvalue + "has quotes");
						cvalue = "\"" + cvalue + "\"";	
					}
				}
				
			}
			
			value.append(name).append("=").append(cvalue);

			if(c.getMaxAge() != -1){
				Calendar calendar = Calendar.getInstance();
				Long d = calendar.getTime().getTime();
				d += c.getMaxAge();
				SimpleDateFormat dateFormat = new SimpleDateFormat(
				        "EEE dd MMM yyyy hh:mm:ss zzz", Locale.US);
				String date_str = dateFormat.format(new Date(d));
				value.append("; ").append("expires=").append(date_str);
			}else if(c.getVersion() != -1){
				value.append("; ").append("Version=").append(c.getVersion());
			}else if(c.getComment() != null  ){
				value.append("; ").append("Comment=").append("\"").append(c.getComment()).append("\"");
			}else if(c.getDomain() != null){
				if(c.getDomain().contains(" ")){
					throw new IllegalArgumentException();
				}
				value.append("; ").append("Domain=").append(c.getDomain());
			}else if(c.getPath() != null){
				if(c.getPath().contains(" ")){
					throw new IllegalArgumentException();
				}
				value.append("; ").append("Path=").append(c.getPath());
			}else if(c.getSecure() == true){
				value.append("; ").append("Secure=").append(c.getSecure());
			}
			
			List<String> cookieHeader = m_headers.get("Set-Cookie");
			if(cookieHeader == null){
				List<String> cookieVal = new ArrayList<String>();
				cookieVal.add(value.toString());
				m_headers.put("Set-Cookie", cookieVal);
			}else{
				m_headers.get("Set-Cookie").add(value.toString());
			}
			
		}
				
	}

	@Override
	public void addDateHeader(String name, long date) {
		
		String adjust_name = name.toLowerCase();
		if(containsHeader(adjust_name)){
			Date d = new Date(date);
			m_headers.get(adjust_name).add(d.toString());
		}
		else{
			List<String> newlist = new ArrayList<String>();
			Date d = new Date(date);
			newlist.add(d.toString());
			m_headers.put(adjust_name, newlist);
		}

	}

	
	@Override
	public void addHeader(String name, String value) {
		
		String adjust_name = name.toLowerCase();
		if(containsHeader(adjust_name)){
			m_headers.get(adjust_name).add(value);
		}
		else{
			List<String> newlist = new ArrayList<String>();
			newlist.add(value);
			m_headers.put(adjust_name, newlist);
		}
		
	}

	
	@Override
	public void addIntHeader(String name, int value) {
		
		String adjust_name = name.toLowerCase();
		if(containsHeader(adjust_name)){
			m_headers.get(adjust_name).add( String.valueOf(value) );
		}
		else{
			List<String> newlist = new ArrayList<String>();
			newlist.add( String.valueOf(value));
			m_headers.put(adjust_name.toLowerCase(), newlist);
		}

	}

	
	@Override
	public boolean containsHeader(String name) {
		
		return m_headers.containsKey(name.toLowerCase());
	}

	@Override
	public String encodeRedirectURL(String url) {
		
		String encoded;
		
		try{
			encoded = URLEncoder.encode(url,"UTF-8");
		}catch(UnsupportedEncodingException ee){
			
			encoded = null;
		}
		
		return encoded;
	}

	//DEPRECATED
	public String encodeRedirectUrl(String arg0) {
		return null;
	}

	@Override
	public String encodeURL(String url) {
		String encoded;
		
		try{
			encoded = URLEncoder.encode(url,"UTF-8");
		}catch(UnsupportedEncodingException ee){
			
			encoded = null;
		}
		
		return encoded;
	}

	//DEPRECATED
	public String encodeUrl(String arg0) {
		return null;
	}

	@Override
	public void sendError(int code) throws IOException {

		if(committed == true){
			throw new IllegalStateException();
		}
		else{
			String errorString = HttpResponseUtils.writeErrorResponse(code, http_protocol, keep_alive);
			PrintWriter out = new PrintWriter(requestSocket.getOutputStream());
			out.write(errorString);
			out.flush();

			// clear the buffer
			reset();
			// set to "Committed" state ? throw IllegalStateException if its already "committed"
			committed = true;
		}
	}

	@Override
	public void sendError(int code, String msg) throws IOException {
	
		if(committed == true){
			throw new IllegalStateException();
		}
		else{
			String errorString = HttpResponseUtils.writeErrorResponse(code, http_protocol, msg, keep_alive);

			PrintWriter out = new PrintWriter(requestSocket.getOutputStream());
			out.write(errorString);
			out.flush();

			// clear the buffer
			reset();
			// set to "Committed" state ? throw IllegalStateException if its already "committed"
			committed = true;
		}
		
	}

	@Override
	public void sendRedirect(String arg0) throws IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setDateHeader(String name, long time) {
		
		String adjust_name = name.toLowerCase();
		Date d = new Date(time);
		List<String> newdate = new ArrayList<String>();
		newdate.add(d.toString());
		m_headers.put(adjust_name,newdate);


	}

	@Override
	public void setHeader(String name, String val) {
		String adjust_name = name.toLowerCase();
		List<String> list = new ArrayList<String>();
		list.add(val);
		m_headers.put(adjust_name, list);
		
	}

	@Override
	public void setIntHeader(String name, int val) {
		String adjust_name = name.toLowerCase();
		List<String> list = new ArrayList<String>();
		list.add(String.valueOf(val));
		m_headers.put(adjust_name, list);
		
	}

	
	/**
	 * This method is used to set the return status code when there is no error. 
	 * 
	 * If this method is used to set an error code, then the container's error page mechanism will 
	 *   not be triggered. If there is an error and the caller wishes to invoke an error page defined 
	 *   in the web application, then sendError(int, java.lang.String) must be used instead.
	 */
	
	@Override
	public void setStatus(int code) {	

		status_code = code;

	}

	//DEPRECATED
	@Override
	public void setStatus(int arg0, String arg1) {
		
	}
	

}
