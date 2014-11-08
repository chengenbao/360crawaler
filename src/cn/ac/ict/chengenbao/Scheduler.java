package cn.ac.ict.chengenbao;

import java.util.ArrayList;
import java.util.List;

public class Scheduler {
	private Crawler crawler = new Crawler(Util.CRAWLER_WORKER_NUMBER);
	private Buckets buckets = new Buckets(Util.BUCKET_CACHE_SIZE);
	private Indexer indexer = new Indexer(Util.INDEXER_WORKER_NUMBER, Util.INDEXER_CACHE_SIZE);
	private WorkQueue workQueue = new WorkQueue(Util.WORK_QUEUE_SIZE);
	private boolean stopped = false;
	private final static Scheduler instance = new Scheduler();
	
	private Scheduler() {
		
	}
	
	public Crawler getCrawler() {
		return crawler;
	}
	public Buckets getBuckets() {
		return buckets;
	}
	public Indexer getIndexer() {
		return indexer;
	}
	public WorkQueue getWorkQueue() {
		return workQueue;
	}
	
	public void Start() {
		List<String> words = new ArrayList<String>();
		words.add("hadoop");
		words.add("mapreduce");
		words.add("jquery");
		words.add("mongodb");
		words.add("bigtable");
		words.add("apple");
		
		workQueue.addWords(words);
		crawler.start();
		indexer.start();
		buckets.start();
		
		while(!stopped) {
			try {
				Thread.sleep(10000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void stop() {
		crawler.stop();
		indexer.stop();
		buckets.stop();
		
		stopped = true;
		System.out.println("------------------------------------ stop got -----------------------");
	}
	
	public static Scheduler getInstance() {
		return instance;
	}
}
