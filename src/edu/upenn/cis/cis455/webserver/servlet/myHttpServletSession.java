package edu.upenn.cis.cis455.webserver.servlet;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Properties;
import java.util.TimeZone;
import java.util.UUID;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionContext;

public class myHttpServletSession implements HttpSession {
	
	static SimpleDateFormat dateFormat = new SimpleDateFormat(
	        "EEE dd MMM yyyy hh:mm:ss zzz", Locale.US);
	
	
	UUID id;
	Calendar calendar = Calendar.getInstance();
	Date creationDate = null;
	Date lastAccessed = null;
    boolean persistentSession = false;
    int inactiveInterval = 0; 
    myServletContext context = null;
    
	
	// public constructor
	public myHttpServletSession(){
		
		// generate a unique uuid for the session as its identifier.
		id = UUID.randomUUID();
		
		creationDate = calendar.getTime();
	}
	
	
	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpSession#getCreationTime()
	 */
	public long getCreationTime() {
		
		return creationDate.getTime();
	}

	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpSession#getId()
	 */
	public String getId() {
		
		return id.toString();
	}

	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpSession#getLastAccessedTime()
	 */
	public long getLastAccessedTime() {
		// TODO Auto-generated method stub
		return lastAccessed.getTime();
	}

	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpSession#getServletContext()
	 */
	public ServletContext getServletContext() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpSession#setMaxInactiveInterval(int)
	 */
	public void setMaxInactiveInterval(int interval) {
		
		// Specifies the time, in seconds, between client requests before the servlet container 
		// will invalidate this session. A negative time indicates the session should never timeout.
		
		if(interval == -1){
			persistentSession = true;
		} 
		else{
			inactiveInterval = interval;
		}
		
	}

	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpSession#getMaxInactiveInterval()
	 */
	public int getMaxInactiveInterval() {
		// TODO Auto-generated method stub
		return inactiveInterval;
	}

	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpSession#getSessionContext()
	 */
	public HttpSessionContext getSessionContext() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpSession#getAttribute(java.lang.String)
	 */
	public Object getAttribute(String arg0) {
		return m_props.get(arg0);
	}

	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpSession#getValue(java.lang.String)
	 */
	public Object getValue(String arg0) {
		
		
		return m_props.get(arg0);
	}

	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpSession#getAttributeNames()
	 */
	public Enumeration getAttributeNames() {

		return m_props.keys();
	}

	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpSession#getValueNames()
	 */
	public String[] getValueNames() {
	
		return null;
	}

	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpSession#setAttribute(java.lang.String, java.lang.Object)
	 */
	public void setAttribute(String arg0, Object arg1) {
		
		m_props.put(arg0, arg1);
	}

	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpSession#putValue(java.lang.String, java.lang.Object)
	 */
	public void putValue(String arg0, Object arg1) {
		
		m_props.put(arg0, arg1);
	}

	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpSession#removeAttribute(java.lang.String)
	 */
	public void removeAttribute(String arg0) {
		m_props.remove(arg0);
	}

	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpSession#removeValue(java.lang.String)
	 */
	public void removeValue(String arg0) {
		
		m_props.remove(arg0);
	}

	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpSession#invalidate()
	 */
	public void invalidate() {
		m_valid = false;
	}

	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpSession#isNew()
	 */
	public boolean isNew() {
		// TODO Auto-generated method stub
		return false;
	}

	boolean isValid() {
		return m_valid;
	}
	
	// Properties is basically a "Hash Table" for storing name-value pairs, values would be objects
	private Properties m_props = new Properties();
	private boolean m_valid = true;
}
