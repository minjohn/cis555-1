package edu.upenn.cis.cis455.webserver;

import java.net.Socket;
import java.util.LinkedList;
import java.util.List;

public class myBlockingQueue {

	private List<Socket> queue = new LinkedList<Socket>();
	private int  limit = 10;
	private boolean shutdown_flag = false; // for purposes of shutting down
	

	public myBlockingQueue(int limit){
		this.limit = limit;
	}
	
	public synchronized void enqueue(Socket item)
			throws InterruptedException  {
		while(this.queue.size() == this.limit) {
			wait();
			if(shutdown_flag == true){
				notifyAll();
				return;
			}
		}
		//if(this.queue.size() == 0) {
		//  notifyAll();
		//}
		this.queue.add(item);
		notify();
	}


	public synchronized Socket dequeue()
			throws InterruptedException{
		while(this.queue.size() == 0){
			wait();
			if(shutdown_flag == true){
				notifyAll();
				return null;
			}
		}
		//if(this.queue.size() == this.limit){
		//  notifyAll();
		//}

		return this.queue.remove(0);
	}

	public synchronized boolean isEmpty()
			throws InterruptedException{

		return this.queue.size() ==0 ? true : false; 

	}

	public synchronized int size()
			throws InterruptedException{

		return this.queue.size(); 

	}
	
	public synchronized void setShutdown()
		throws InterruptedException{
		
		shutdown_flag = true;
		notifyAll();
		
	}



}
