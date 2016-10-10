package edu.upenn.cis.cis455.webserver.servlet;

import java.io.PrintWriter;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

public class CalculatorServlet extends HttpServlet{
	
	Logger log = Logger.getLogger(CalculatorServlet.class);
	
	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response) 
			throws java.io.IOException
	{
		response.setContentType("text/html");
	    PrintWriter out = response.getWriter();
	    int v1 = Integer.valueOf(request.getParameter("num1")).intValue();
	    int v2 = Integer.valueOf(request.getParameter("num2")).intValue();
	    out.println("<html><head><title>Foo</title></head>");
	    out.println("<body>"+v1+"+"+v2+"="+(v1+v2)+"</body></html>");
	    
	    out.flush();
	}

}
