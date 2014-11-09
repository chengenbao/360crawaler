package cn.ac.ict.chengenbao;

import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class WorkQueue {
	private LinkedBlockingQueue<String> words = null;
	private final static Logger logger= Logger.getLogger();
	
	public WorkQueue(int workQueueSize) {
		// TODO Auto-generated constructor stub
		words = new LinkedBlockingQueue<String>(workQueueSize);
	}

	public String getWord() {
		try {
			 return words.poll(2, TimeUnit.SECONDS);	 
		} catch (InterruptedException e) {
			logger.log(e.getMessage());
			return null;
		}
	}
	
	public void addWords(List<String> l) {
		for(String word: l) {
			words.offer(word);
		}
	}
}
