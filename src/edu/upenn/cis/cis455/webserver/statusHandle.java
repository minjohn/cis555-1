package edu.upenn.cis.cis455.webserver;

import java.util.ArrayList;
import java.util.List;

public class statusHandle {

	List<workerThread> threadPool = null;
	
	public statusHandle( List<workerThread> tPool  ) {
		threadPool = tPool;
	}
	
	public String getAllThreadStatus(){
		
		StringBuffer status = new StringBuffer();
		
		for( workerThread t : threadPool){
			status.append("<p>Thread " + t.id + ": " + t.getThreadState()+"</p>");
		}
		return status.toString();
	}
	
	
	
	
}
