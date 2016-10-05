package edu.upenn.cis.cis455.webserver.servlet;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class myHttpServlet extends HttpServlet {

	String root_dir;
	myServletContext sc;
	
	/**
	 * Note for myself:
	 * You should never assign any request or session scoped data as an instance variable of a servlet.
	 *    It will be shared among all other requests  in other sessions. That’s thread-unsafe!
	 *  
	 *  for session stuff:
	 *  Note: Although getSession is a method of HttpServletRequest and not of HttpServletResponse it may 
	 *    modify the response header and therefore needs to be called before a ServletOutputStream or 
	 *    PrintWriter is requested.
	 *  
	 *  
	 */
	
	private static final long serialVersionUID = 4886823538838413973L;

	//default constructor
    public myHttpServlet(String root){
    	root_dir = root;
    }
    
    public void init(myServletConfig config) throws ServletException{
    	// initialize some parameters...
    	
    	// Leave the servlet Management to the HttpServlet class, handles getting initialization parameters
    	//  from web.xml
    	super.init(config);
    	
    	// Could get some init parameters here...
    	
    	//sc = config.getServletContext();
    }
    
    
    private boolean validatePath(String path){
    	
    	
    	//TODO: check if the path is trying to access a location they dont have permission to access
    	/*if(){
    		
    	}*/
    	return true;
    	
    }
    
    
    /* Note: service() ass defined by the HttpServlet class will call the appropriate doXXX() method
     *   Hence, the definitions of those methods only need to be defined. Perhaps, this makes it important
     *   to set the HttpServletRequest's HTTP method when creating the object. 
     *   */
    
    // Handle GET request
    public void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
    	// get the path and validate it does not try to access unaccessible directories
    	
    	String path = req.getPathInfo();
    	
    	if(!validatePath(path)){
    		//403 forbidden
    	}
    	else{
    		
    	}
    	
    	// get the file from root directory and write to the res output writer.
    	
    }
    
    
    //Handle Post request
    public void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
    	
    	// get the path and validate it does not try to access unaccessible directories
    	
    	String path = req.getPathInfo();
    	
    	if(!validatePath(path)){
    		//403 forbidden
    	}
    	else{
    		
    	}
    	
    	// get the file from root directory and write to the res output writer.
    	
    }
    
    
    
}
