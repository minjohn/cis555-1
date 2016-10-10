package edu.upenn.cis.cis455.webserver.servlet;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

public class LoginServlet extends HttpServlet{

	//myServletContext servlet_context;
	
	Logger log = Logger.getLogger(LoginServlet.class);
	
	@Override
	public void init(ServletConfig config) throws ServletException{

		// Leave the servlet Management to the HttpServlet class, handles getting initialization parameters
		//  from web.xml
		
		log.debug("called init from Login");
		
		super.init(config);

		//servlet_context = config.getServletContext();
	}
	
	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response) 
			throws java.io.IOException
	{
		
		// return the login page
//		String realpath = getServletContext().getRealPath("resources/testform.html");
		ServletContext sc = getServletContext();
		String realpath = sc.getRealPath("resources/testform.html");
		log.debug("servletcontext??:: " + sc);
		
		log.debug("Testing servlet context, testform.html real path: [" + realpath + "]" );
		
		PrintWriter p = response.getWriter();
		
		File f = new File("resources/testform.html");
		
		StringBuffer loginPage = new StringBuffer();
		
		String line;
		try {
			BufferedReader reader = new BufferedReader(new FileReader(f));
			
			while( (line = reader.readLine()) != null ){
				loginPage.append(line);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		p.println(loginPage.toString());
		
		
		
	}
	
	
	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response) 
			throws java.io.IOException
	{
		log.debug("In Post method of Login!");
		
		String name = (String)request.getParameter("realname");
		String pass = (String)request.getParameter("mypassword");
		
		PrintWriter p = response.getWriter();
		if( (name != null && pass != null) && name.compareTo("dan") == 0 && pass.compareTo("dan") == 0 ){
			p.println("LOGIN! ");
		}else{
			p.println(" ACCESS denied! ");
		}
		//p.flush();
		
	}
	
	
}
