package cn.ac.ict.chengenbao;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;

/**
 * manage the words, be persistent to disk File
 * 
 * @author chengenbao
 *
 */
public class Buckets {
	private LinkedBlockingQueue<String> cache = null;
																						// been
																						// crawed
	private boolean stopped = false;
	private final static Logger logger = Logger.getLogger();
	private Integer crawedWordCount = 0;
	private DictFile dicFile = new DictFile();

	public Buckets(int cacheSz) {
		// TODO Auto-generated constructor stub
		cache = new LinkedBlockingQueue<String>(cacheSz);
	}

	public void persistent() {
		List<String> words = new ArrayList<String>(cache); //存储未检索单词
		dicFile.write(words); 
		cache.clear();
	}

	public void addWords(Collection<String> words) {
		if (cache.size() >= Util.BUCKET_CACHE_SIZE - Util.BATCH_SIZE) { // store
			int count = dicFile.write(cache);

			synchronized (crawedWordCount) {
				count += crawedWordCount;
			}
			if (count >= Util.TARGET_COUNT) {
				Scheduler.getInstance().stop();
				return;
			}
		}
		
		for (String word : words) {
			if (!cache.contains(word)) {
				try {
					cache.offer(word, 2, TimeUnit.SECONDS);
				} catch (InterruptedException e) {
					logger.log(e.getMessage());
				}
			}
		}
	}

	private boolean checkForTerminate() {
		int count = 0;
		
		synchronized(crawedWordCount) {
			count += crawedWordCount;
		}
		
		boolean shouldTerminated = (count >= Util.TARGET_COUNT);
		if (shouldTerminated) {
			Scheduler.getInstance().stop();
		}

		return shouldTerminated;
	}

	public void start() {
		new Thread(new Runnable() {

			@Override
			public void run() {
				while (!stopped) {
					try {
						Thread.sleep(1000);

						List<String> words = new ArrayList<String>();		
						String word = cache.poll();
						if (word == null) {
							loadFromDicFile();
						} else {
							words.add(word);
						}

						for (int i = 0; i < Util.BATCH_SIZE; ++i) {
							word = cache.poll(2, TimeUnit.SECONDS);
							if ( word != null ) {
								words.add(word);
							}
						}

						Scheduler.getInstance().getWorkQueue().addWords(words);
						System.out.println("bucket size ------------------ " + cache.size());

					} catch (InterruptedException e) {
						logger.log(e.getMessage());
					}
				}
			}

		}).start();
	}
	
	private void loadFromDicFile() {
		List<String> words = dicFile.loadRandomWords(Util.BATCH_SIZE);
		
		for(String word: words) {
			cache.offer(word);
		}
	}

	public void stop() {
		stopped = true;
		
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			logger.log(e.getMessage());
		}
		
		persistent();
		dicFile.close();
	}
	
	public static void main(String[] args) {
		DictFile dict = new DictFile();
		System.out.println(dict.find("apple4s官网"));
		dict.close();
	}
}
