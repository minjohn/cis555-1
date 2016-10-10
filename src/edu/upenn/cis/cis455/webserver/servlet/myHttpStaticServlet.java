package edu.upenn.cis.cis455.webserver.servlet;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.SocketTimeoutException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import edu.upenn.cis.cis455.webserver.HttpResponseUtils;

public class myHttpStaticServlet extends HttpServlet {


	Logger log = Logger.getLogger(myHttpStaticServlet.class);

	myServletContext servlet_context;

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
	public myHttpStaticServlet(){
	}

	public void init(myServletConfig config) throws ServletException{

		// Leave the servlet Management to the HttpServlet class, handles getting initialization parameters
		//  from web.xml
		super.init(config);

		servlet_context = config.getServletContext();
	}

	/* Note: service() ass defined by the HttpServlet class will call the appropriate doXXX() method
	 *   Hence, the definitions of those methods only need to be defined. Perhaps, this makes it important
	 *   to set the HttpServletRequest's HTTP method when creating the object. 
	 *   */

	// Handle GET request
	public void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {


	}


	//Handle Post request
	public void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {

		

	}



	}
