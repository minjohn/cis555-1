package edu.upenn.cis.cis455.webserver.servlet;

import java.io.*;
import java.util.Map;

import javax.servlet.*;
import javax.servlet.http.*;

import org.apache.log4j.Logger;

public class TestServlet extends HttpServlet {

	Logger log = Logger.getLogger(TestServlet.class);

	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response) 
			throws java.io.IOException
	{

		log.debug("IN TEST SERVLET, doGet()");

		response.setContentType("text/html");
		PrintWriter out = response.getWriter();
		out.println("<html><head><title>Test</title></head><body>");
		out.println("RequestURL: ["+request.getRequestURL()+"]<br>");
		out.println("RequestURI: ["+request.getRequestURI()+"]<br>");
		out.println("PathInfo: ["+request.getPathInfo()+"]<br>");
		out.println("Context path: ["+request.getContextPath()+"]<br>");
		out.println("Header: ["+request.getHeader("Accept-Language")+"]<br>");

		Map params = request.getParameterMap();

		for( Object e  : params.keySet() ){

			String name = (String)(e);

			out.println(" > Param "+name+": "+request.getParameter(name)+"<br>");

		}

		out.println("</body></html>");
		out.println("\n");
		out.close();
	}

	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response) 
			throws java.io.IOException
	{

		log.debug("IN TEST SERVLET, doPost()");
		response.setContentType("text/html");
		PrintWriter out = response.getWriter();
		out.println("<html><head><title>POST</title></head><body>");
		out.println("RequestURL: ["+request.getRequestURL()+"]<br>");
//		out.println("RequestURI: ["+request.getRequestURI()+"]<br>");
//		out.println("PathInfo: ["+request.getPathInfo()+"]<br>");
//		out.println("Context path: ["+request.getContextPath()+"]<br>");
//		out.println("Header: ["+request.getHeader("Accept-Language")+"]<br>");
		out.println("</body></html>");
		out.println("\n");
		out.close();
	}



}
