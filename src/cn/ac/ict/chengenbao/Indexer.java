package cn.ac.ict.chengenbao;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 *  analysis the the page and retrieve the words
 *  
 * @author chengenbao
 *
 */

public class Indexer {
	private ExecutorService pool = null;
	private int workerNum = 0;
	private final static Logger logger= Logger.getLogger();
	private LinkedBlockingQueue<String> queue = null;
	private boolean stopped = false;
	
	class Worker implements Runnable {

		@Override
		public void run() {
			while (!stopped) {
				
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e1) {
					logger.log(e1.getMessage());
				}
				
				String page = null;
				try {
					page = queue.take();
				} catch (InterruptedException e) {
					logger.log(e.getMessage());
				}
				
				if (page != null) {
					List<String> words = processPage(page);
					List<String> targets = new ArrayList<String>(words);
					
					for(String word: words) {
						String[] arr = word.split(" ");
						
						for(String str: arr) {
							targets.add(str);
						}
						
						targets.add(word);
					}
					
					words.clear();
					words = new ArrayList<String>(new HashSet<String>(targets));
					targets.clear();
					
					Scheduler.getInstance().getBuckets().addWords(words);
				}
			}
		}
	}

	public Indexer(int indexerWorkerNumber, int indexerCacheSize) {
		workerNum = indexerWorkerNumber;
		pool = Executors.newFixedThreadPool(workerNum);
		queue = new LinkedBlockingQueue<String>(indexerCacheSize);
	}

	public void addPage(String page) {
		if (page == null) {
			return;
		}
		
		try {
			queue.put(page);;
		} catch (InterruptedException e) {
			logger.log(e.getMessage());
		}
	}
	
	public void start() {
		for(int i = 0; i < workerNum; ++i) {
			pool.submit(new Worker());
		}
	}
	
	public void stop() {
		stopped = true;
		pool.shutdown();
	}
	
	private static List<String> processPage(String page) {
		List<String> words = new ArrayList<String>();
		
		String[] lines = page.split("\n");
		int i = 0;
		
		for(String line: lines) {
			if (line.contains(Util.SEARCH_PATTERN)) {
				break;
			}
			++i;
		}
		// next line is target line
		if ( i + 1 < lines.length ) { // found
			String target = lines[i + 1];
			String[] arr = target.split("data-type=\"0\">");
			
			for( i = 1; i < arr.length; ++i) {
				// find "<"
				int index = arr[i].indexOf("<");
				if (index != -1) {
					words.add(arr[i].substring(0, index));
				}
			}
		}
		
		return words;
	}
	
	public static void main(String[] args) {
		try {
			FileInputStream fin = new FileInputStream("output.txt");
			
			byte[] buffer = new byte[256];
			StringBuilder sb = new StringBuilder();
			int num = 0;
			
			while((num = fin.read(buffer)) != -1 ) {
				if (num > 0) {
					sb.append(new String(buffer, 0, num));
				}
			}
			
			fin.close();
			
			String page = sb.toString();
			List<String> words = processPage(page);
			
			for (String word: words) {
				System.out.println(word);
			}
		} catch (IOException e) {
			logger.log(e.getMessage());
		}
	}
}
