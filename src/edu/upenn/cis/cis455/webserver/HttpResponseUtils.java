package edu.upenn.cis.cis455.webserver;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class HttpResponseUtils {

	static String serverName = "myHTTPServer/1.0";
	static SimpleDateFormat dateFormat = new SimpleDateFormat(
	        "EEE dd MMM yyyy hh:mm:ss zzz", Locale.US);
	
	static SimpleDateFormat dateFormat2 = new SimpleDateFormat(
	        "EEEEEEEEE dd-MMM-yy hh:mm:ss zzz", Locale.US);
	
	static SimpleDateFormat dateFormat3 = new SimpleDateFormat(
	        "EEE MMM dd hh:mm:ss yyyy", Locale.US);
	
	public static SimpleDateFormat getDateFormat(){
		return dateFormat;
	}
	
	public static String getServerTime() {
	    Calendar calendar = Calendar.getInstance();
	    
	    dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
	    return dateFormat.format(calendar.getTime());
	}
	
	public static String fileToHTTPDateString(Long fileDate){
		Date lastModified = new Date(fileDate);
		
		return dateFormat.format(lastModified);
	}
	
	public static Date parseHeaderDate(String headerDate){
		
		// Need to check three forms of dates. If one of them is not detected, return null to indicate
		//   error.

		try {
			//dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
			Date date = dateFormat.parse(headerDate);
			return date;
		} catch (ParseException e) {
			
			e.printStackTrace();
			
			try {
				//dateFormat2.setTimeZone(TimeZone.getTimeZone("GMT"));
				Date date = dateFormat2.parse(headerDate);
				return date;
			}catch(ParseException e1){
				e.printStackTrace();
				try{
					//dateFormat3.setTimeZone(TimeZone.getTimeZone("GMT"));
					Date date = dateFormat3.parse(headerDate);
					return date;
				}catch(ParseException e2){
					e2.printStackTrace();
					return null;
				}
				
			}
			
			
		}
		
	}
	
	//Check if the Date String is in one of the acceptable date Formats
	public static boolean isValidDateFormat(String value) {
        Date date = null;
        try {
        	
            //try format 1
            date = dateFormat.parse(value);
            if (value.equals(dateFormat.format(date))) {
                return true;
            }
            
            //try format 2
            date = dateFormat2.parse(value);
            if (value.equals(dateFormat2.format(date))) {
                return true;
            }
            
            //try format 3
            date = dateFormat3.parse(value);
            if (!value.equals(dateFormat3.format(date))) {
                return true;	
            }
            
        } catch (ParseException ex) {
            ex.printStackTrace();
        }
        return false;
    }
	
	public String getControlPageText(String threadStatuses){
			
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
//		String statuses = statusHandle.getAllThreadStatus();

		String temp = controlPage.toString();
		String controlWithStatus = temp.replace("<!-- Include Thread Status HERE -->", threadStatuses);


		return controlWithStatus;

	}
	
	
	
	//Returns the text associated with the response code
	public static String getResponseText(int code){
		
		String text = null;
		
		switch(code){
			
			case 200:
				text = "OK";
				break;
			case 301:
				text = "Moved Permanently";
				break;
			case 302:
				text = "Moved Temporarily";
				break;
			case 303:
				text = "See Other";
				break;
			case 400:
				text = "Bad Request";
				break;
			case 403:
				text = "Access Forbidden";
				break;
			case 404:
				text = "Not Found";
				break;
			case 412:
				text = "Precondition Failed";
				break;
			case 500:
				text = "Server Error";
				break;
			case 501:
				text = "Not Implemented";
				break;
			default:
				text = "Invalid Code";
				break;
		}
		
		return text;
	}

	public static String getContinueResponse(){
		return "HTTP/1.1 100 Continue\n\n";
		
	}
	
	
	
	public static String getPreconditionFailedHTMLString(){
		
		return "<html><body>\n" +
		"<h2>412: Precondition Failed</h2>\n"+
		"Condition for retreiving resource failed\n"+
		"</body></html>";
		
	}
	
	public static String getBadRequestHTMLString(){
		
		return "<html><body>\n" +
		"<h2>400: Bad Request</h2>\n"+
		"Malformed request. Likely because HOST: header was not included\n"+
		"</body></html>";
		
	}
	
	
	public static String getAccessForbiddenHTMLString(){
		
		return "<html><body>\n" +
		"<h2>403: Access Forbidden</h2>\n"+
		"You shall not pass.\n"+
		"</body></html>";
		
	}
	
	
	public static String getFileNotFoundHTMLString(){
		
		return "<html><body>\n" +
		"<h2>404: File Not Found</h2>\n"+
		"Sorry We could not find the file.\n"+
		"</body></html>";
		
	}
	
	public static String getUnsupportedMediaFileHTMLString(){
		
		return "<html><body>\n" +
		"<h2>415: Unsupported Media File</h2>\n"+
		"Sorry we dont process that type of media.\n"+
		"</body></html>";
		
	}
	
	public static String getUnSpecifiedServerErrorHTMLString(){
		
		return "<html><body>\n" +
		"<h2>500:  Internal Server Error</h2>\n"+
		" Internal Server Error.\n"+
		"</body></html>";
		
	}
	
	public static String getNotImplementedHTMLString(){
		
		return "<html><body>\n" +
		"<h2>501:  Not Implemented</h2>\n"+
		" HTTP Method not currently implemented by Server .\n"+
		"</body></html>";
		
	}
	
	
	// for normal headers with a body for GET requests
	public static String writeResponseHeaders(int code, String contentType, String body, String httpVersion, Long lastMod, boolean connectionAlive){
		
		StringBuffer responseHeaders = new StringBuffer();
		
		responseHeaders.append(httpVersion + " " + Integer.toString(code) + " " + HttpResponseUtils.getResponseText(code)+"\n");
		
		if( code >= 200 && code < 300 ){ // 200 headers
			//responseHeaders.append("HTTP/1.1 " + Integer.toString(code) + " " + HttpResponse.getResponseText(code));
			responseHeaders.append("Date: " + HttpResponseUtils.getServerTime()+"\n");
			responseHeaders.append("Server: " + serverName +"\n");
			responseHeaders.append("Content-Type: " + contentType + "; charset=utf-8\n");
			responseHeaders.append("Content-Length: " + body.length() + "\n");
			responseHeaders.append("Last-Modified: " + fileToHTTPDateString(lastMod)+"\n");
			if(connectionAlive != false){
				responseHeaders.append("Connection: keep-alive\n");
			}
			else{
				responseHeaders.append("Connection: close\n");
			}
			responseHeaders.append("\n");
			responseHeaders.append(body);
		}
		else { //all other headers
			//responseHeaders.append("HTTP/1.1 " + Integer.toString(code) + " " + HttpResponse.getResponseText(code));
			responseHeaders.append("Date: " + HttpResponseUtils.getServerTime() + "\n");
			responseHeaders.append("Server: " + serverName +"\n");
			responseHeaders.append("Content-Type: " + contentType + "; charset=utf-8\n");
			responseHeaders.append("Content-Length: " + body.length() + "\n");
			responseHeaders.append("Last-Modified: " + fileToHTTPDateString(lastMod)+"\n");
			if(connectionAlive != false){
				responseHeaders.append("Connection: keep-alive\n");
			}
			else{
				responseHeaders.append("Connection: close\n");
			}
			responseHeaders.append("Connection: close\n");
			responseHeaders.append("\n");
			responseHeaders.append(body);
		}
		
		return responseHeaders.toString();
		
	}
	
public static String writeResponseHeaders(int code, String contentType, String body, String httpVersion, boolean connectionAlive){
		
		StringBuffer responseHeaders = new StringBuffer();
		
		responseHeaders.append(httpVersion + " " + Integer.toString(code) + " " + HttpResponseUtils.getResponseText(code)+"\n");
		
		if( code >= 200 && code < 300 ){ // 200 headers
			//responseHeaders.append("HTTP/1.1 " + Integer.toString(code) + " " + HttpResponse.getResponseText(code));
			responseHeaders.append("Date: " + HttpResponseUtils.getServerTime()+"\n");
			responseHeaders.append("Server: " + serverName +"\n");
			responseHeaders.append("Content-Type: " + contentType + "; charset=utf-8\n");
			responseHeaders.append("Content-Length: " + body.length() + "\n");
			if(connectionAlive != false){
				responseHeaders.append("Connection: keep-alive\n");
			}
			else{
				responseHeaders.append("Connection: close\n");
			}
			responseHeaders.append("\n");
			responseHeaders.append(body);
		}
		else { //all other headers
			//responseHeaders.append("HTTP/1.1 " + Integer.toString(code) + " " + HttpResponse.getResponseText(code));
			responseHeaders.append("Date: " + HttpResponseUtils.getServerTime() + "\n");
			responseHeaders.append("Server: " + serverName +"\n");
			responseHeaders.append("Content-Type: " + contentType + "; charset=utf-8\n");
			responseHeaders.append("Content-Length: " + body.length() + "\n");
			if(connectionAlive != false){
				responseHeaders.append("Connection: keep-alive\n");
			}
			else{
				responseHeaders.append("Connection: close\n");
			}
			responseHeaders.append("Connection: close\n");
			responseHeaders.append("\n");
			responseHeaders.append(body);
		}
		
		return responseHeaders.toString();
		
	}
	
	
	
	// for normal headers with a body for HEAD requests
	public static String writeHeadResponseHeaders(int code, String contentType, String body, String httpVersion, Long lastMod, boolean connectionAlive){
			
			StringBuffer responseHeaders = new StringBuffer();
			
			responseHeaders.append(httpVersion + " " + Integer.toString(code) + " " + HttpResponseUtils.getResponseText(code)+"\n");
			
			if( code >= 200 && code < 300 ){ // 200 headers
				//responseHeaders.append("HTTP/1.1 " + Integer.toString(code) + " " + HttpResponse.getResponseText(code));
				responseHeaders.append("Date: " + HttpResponseUtils.getServerTime()+"\n");
				responseHeaders.append("Server: " + serverName +"\n");
				responseHeaders.append("Content-Type: " + contentType + "; charset=utf-8\n");
				responseHeaders.append("Content-Length: " + body.length() + "\n");
				responseHeaders.append("Last-Modified: " + fileToHTTPDateString(lastMod)+"\n");
				if(connectionAlive != false){
					responseHeaders.append("Connection: keep-alive\n");
				}
				else{
					responseHeaders.append("Connection: close\n");
				}
				responseHeaders.append("\n");
				
			}
			else { //all other headers
				//responseHeaders.append("HTTP/1.1 " + Integer.toString(code) + " " + HttpResponse.getResponseText(code));
				responseHeaders.append("Date: " + HttpResponseUtils.getServerTime() + "\n");
				responseHeaders.append("Server: " + serverName +"\n");
				responseHeaders.append("Content-Type: " + contentType + "; charset=utf-8\n");
				responseHeaders.append("Content-Length: " + body.length() + "\n");
				responseHeaders.append("Last-Modified: " + fileToHTTPDateString(lastMod)+"\n");
				if(connectionAlive != false){
					responseHeaders.append("Connection: keep-alive\n");
				}
				else{
					responseHeaders.append("Connection: close\n");
				}
				responseHeaders.append("\n");
				
			}
			
			return responseHeaders.toString();

}
	
	// Typically for HTTP messages without body
	public static String writeResponseHeaders(int code, String httpVersion, boolean connectionAlive){
		StringBuffer responseHeaders = new StringBuffer();
		
		responseHeaders.append(httpVersion + " " + Integer.toString(code) + " " + HttpResponseUtils.getResponseText(code) + "\n");
		responseHeaders.append("Date: " + HttpResponseUtils.getServerTime() + "\n");
		responseHeaders.append("Server: " + serverName +"\n");
		if(connectionAlive != false){
			responseHeaders.append("Connection: keep-alive\n");
		}
		else{
			responseHeaders.append("Connection: close\n");
		}
		
		return responseHeaders.toString();
	}
	
	// For Error HTTP messages with a default error html page
	public static String writeErrorResponse(int code, String httpVersion, boolean connectionAlive){
		StringBuffer responseHeaders = new StringBuffer();
		
		String errorString =  getErrorText(code);
		
		responseHeaders.append(httpVersion + " " + Integer.toString(code) + " " + HttpResponseUtils.getResponseText(code) + "\n");
		responseHeaders.append("Server: " + serverName +"\n");
		responseHeaders.append("Content-Type: text/html charset=utf-8\n");
		responseHeaders.append("Content-Length: " + errorString.length() + "\n");
		responseHeaders.append("Date: " + HttpResponseUtils.getServerTime() + "\n");
		if(connectionAlive != false){
			responseHeaders.append("Connection: keep-alive\n");
		}
		else{
			responseHeaders.append("Connection: close\n");
		}
		responseHeaders.append("\n");
		responseHeaders.append(errorString);
		
		return responseHeaders.toString();
	}
	
	// For Error HTTP messages with a default error html page with custom text
	public static String writeErrorResponse(int code, String httpVersion, String msg, boolean connectionAlive){
		StringBuffer responseHeaders = new StringBuffer();
		
		String errorString =  getErrorText(code)+"\n"+msg;
		
		responseHeaders.append(httpVersion + " " + Integer.toString(code) + " " + HttpResponseUtils.getResponseText(code) + "\n");
		responseHeaders.append("Server: " + serverName +"\n");
		responseHeaders.append("Content-Type: text/html charset=utf-8\n");
		responseHeaders.append("Content-Length: " + errorString.length() + "\n");
		responseHeaders.append("Date: " + HttpResponseUtils.getServerTime() + "\n");
		if(connectionAlive != false){
			responseHeaders.append("Connection: keep-alive\n");
		}
		else{
			responseHeaders.append("Connection: close\n");
		}
		responseHeaders.append("\n");
		responseHeaders.append(errorString);
		
		return responseHeaders.toString();
	}
	
	
	public static String getErrorText(int code){
		String errorString = null;
		
		switch(code){
			
			case 400:
				errorString = getBadRequestHTMLString();
				break;
		
			case 403:
				errorString = getAccessForbiddenHTMLString();
				break;
				
			case 404:
				errorString = getFileNotFoundHTMLString();
				break;
				
			case 412:
				errorString = getPreconditionFailedHTMLString();
				break;
				
			case 415:
				errorString = getUnsupportedMediaFileHTMLString();
				break;
				
			case 501:
				errorString = getNotImplementedHTMLString();
				break;
				
			default:
				code = 500;
				errorString = getUnSpecifiedServerErrorHTMLString();
					
		}
		
		return errorString;
	}
	
	
	// For Error HTTP messages with a default error html page
	public static String writeHeadErrorResponse(int code, String httpVersion, boolean connectionAlive){
		StringBuffer responseHeaders = new StringBuffer();

		String errorString = null;

		switch(code){
		
		case 400:
			errorString = getBadRequestHTMLString();
			break;
	
		case 403:
			errorString = getAccessForbiddenHTMLString();
			break;
			
		case 404:
			errorString = getFileNotFoundHTMLString();
			break;
			
		case 412:
			errorString = getPreconditionFailedHTMLString();
			break;
			
		case 415:
			errorString = getUnsupportedMediaFileHTMLString();
			break;
			
		case 501:
			errorString = getNotImplementedHTMLString();
			break;
			
		default:
			code = 500;
			errorString = getUnSpecifiedServerErrorHTMLString();
				
	    }

		responseHeaders.append(httpVersion + " " + Integer.toString(code) + " " + HttpResponseUtils.getResponseText(code) + "\n");
		responseHeaders.append("Server: " + serverName +"\n");
		responseHeaders.append("Content-Type: text/html charset=utf-8\n");
		responseHeaders.append("Content-Length: " + errorString.length() + "\n");
		responseHeaders.append("Date: " + HttpResponseUtils.getServerTime() + "\n");
		if(connectionAlive != false){
			responseHeaders.append("Connection: keep-alive\n");
		}
		else{
			responseHeaders.append("Connection: close\n");
		}
		responseHeaders.append("\n");


		return responseHeaders.toString();
	}
	
	
	
	//Generates Headers For image binary data. 
	public static String writeImageResponseHeaders(int code, String contentType, String httpVersion, Long imageSize, Long lastMod, boolean connectionAlive){
		
		StringBuffer responseHeaders = new StringBuffer();
		
		responseHeaders.append(httpVersion + " " + Integer.toString(code) + " " + HttpResponseUtils.getResponseText(code)+"\n");
		
		if( code >= 200 && code < 300 ){ // 200 headers
			//responseHeaders.append("HTTP/1.1 " + Integer.toString(code) + " " + HttpResponse.getResponseText(code));
			responseHeaders.append("Date: " + HttpResponseUtils.getServerTime()+"\n");
			responseHeaders.append("Server: " + serverName +"\n");
			responseHeaders.append("Content-Type: " + contentType + "; charset=utf-8\n");
			responseHeaders.append("Content-Length: " + imageSize.toString() + "\n");
			responseHeaders.append("Last-Modified: " + fileToHTTPDateString(lastMod)+"\n");
			if(connectionAlive != false){
				responseHeaders.append("Connection: keep-alive\n");
			}
			else{
				responseHeaders.append("Connection: close\n");
			}
			responseHeaders.append("\n");
		}
		else { //all other headers
			//responseHeaders.append("HTTP/1.1 " + Integer.toString(code) + " " + HttpResponse.getResponseText(code));
			responseHeaders.append("Date: " + HttpResponseUtils.getServerTime() + "\n");
			responseHeaders.append("Server: " + serverName +"\n");
			responseHeaders.append("Content-Type: " + contentType + "; charset=utf-8\n");
			responseHeaders.append("Content-Length: " + imageSize.toString() + "\n");
			responseHeaders.append("Last-Modified: " + fileToHTTPDateString(lastMod)+"\n");
			if(connectionAlive != false){
				responseHeaders.append("Connection: keep-alive\n");
			}
			else{
				responseHeaders.append("Connection: close\n");
			}
			responseHeaders.append("\n");
		}
		
		return responseHeaders.toString();
		
	}
	
	//Generates Headers For image binary data. 
	public static String writeHeadImageResponseHeaders(int code, String contentType, String httpVersion, Long imageSize, Long lastMod, boolean connectionAlive){
			
			StringBuffer responseHeaders = new StringBuffer();
			
			responseHeaders.append(httpVersion + " " + Integer.toString(code) + " " + HttpResponseUtils.getResponseText(code)+"\n");
			
			if( code >= 200 && code < 300 ){ // 200 headers
				//responseHeaders.append("HTTP/1.1 " + Integer.toString(code) + " " + HttpResponse.getResponseText(code));
				responseHeaders.append("Date: " + HttpResponseUtils.getServerTime()+"\n");
				responseHeaders.append("Server: " + serverName +"\n");
				responseHeaders.append("Content-Type: " + contentType + "; charset=utf-8\n");
				responseHeaders.append("Content-Length: " + imageSize.toString() + "\n");
				responseHeaders.append("Last-Modified: " + fileToHTTPDateString(lastMod)+"\n");
				if(connectionAlive != false){
					responseHeaders.append("Connection: keep-alive\n");
				}
				else{
					responseHeaders.append("Connection: close\n");
				}
				responseHeaders.append("\n");
			}
			else { //all other headers
				//responseHeaders.append("HTTP/1.1 " + Integer.toString(code) + " " + HttpResponse.getResponseText(code));
				responseHeaders.append("Date: " + HttpResponseUtils.getServerTime() + "\n");
				responseHeaders.append("Server: " + serverName +"\n");
				responseHeaders.append("Content-Type: " + contentType + "; charset=utf-8\n");
				responseHeaders.append("Content-Length: " + imageSize.toString() + "\n");
				responseHeaders.append("Last-Modified: " + fileToHTTPDateString(lastMod)+"\n");
				if(connectionAlive != false){
					responseHeaders.append("Connection: keep-alive\n");
				}
				else{
					responseHeaders.append("Connection: close\n");
				}
				responseHeaders.append("\n");
			}
			
			return responseHeaders.toString();
			
	}
	
}












