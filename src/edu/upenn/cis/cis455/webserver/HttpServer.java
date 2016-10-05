package edu.upenn.cis.cis455.webserver;

import org.apache.log4j.Logger;

public class HttpServer {

	/**
	 * Logger for this particular class
	 */
	static Logger log = Logger.getLogger(HttpServer.class);
	static int num_threads = 5;
	
	private static myServer initializeServer(int p, String dir, String web){
		return new myServer(p, dir, web);
	}

	public static void main(String args[]) throws InterruptedException
	{
		log.info("Start of Http Server");
		
		/* 
		 * port = args[1]
		 * root_dir = args[2]
		 * */

		//TODO: do something with the root_dir for the server....
		
		if(args.length == 0){ 
			System.out.println("Full name: Steven Hwang\nSEAS login: stevenhw");
		}
		//else if(args.length == 2){
		else if(args.length == 3){ // for MS2
			log.info("Args:______");
			log.info(args[0]);
			log.info(args[1]);
			log.info(args[2]);
			log.info("End Args____\n");

			myServer server = initializeServer(Integer.parseInt(args[0]), args[1], args[2]);

			server.start();

			server.join();
		}else{
			System.out.println("Invalid number of args");
		}
		log.info("Http Server terminating");
	}

}
