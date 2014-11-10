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
	private LinkedBlockingQueue<String> dictFileCache = new LinkedBlockingQueue<String>(Util.BUCKET_CACHE_SIZE);

	public Buckets(int cacheSz) {
		// TODO Auto-generated constructor stub
		cache = new LinkedBlockingQueue<String>(cacheSz);
	}

	public void persistent() {
		dicFile.write(dictFileCache); 
		dictFileCache.clear();
	}

	public void addWords(Collection<String> words) {
		
		for (String word: words) {
			boolean suc = dictFileCache.offer(word);
			if (!suc) {
				int count = dicFile.write(dictFileCache);
				dictFileCache.clear();
			}
			dictFileCache.offer(word);
		}
		
		for (String word : words) {
			if (!cache.contains(word)) {
				cache.offer(word);
			}
		}
	}

	private void checkForTerminate() {
		int count = dicFile.count();
		
		System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>> dict file words count " + count);
		if (count >= Util.TARGET_COUNT) {
			Scheduler.getInstance().stop();
		}
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
		
		// check for terminat
		new Thread(new Runnable() {
			@Override
			public void run() {	
				while (!stopped) {
					checkForTerminate();
					try {
						Thread.sleep(Util.CHECK_TERMINATE_INTERVAL * 1000);
					} catch (InterruptedException e) {
						logger.log(e.getMessage());
					}
				}
			}
			
		}).start();;
	}
	
	private void loadFromDicFile() {
		int size = Util.BATCH_SIZE * 2;
		// 先从dictFileCache中加载
		for(int i = 0; i < size; ++i) {
			String e = null;
			try {
				e = dictFileCache.poll(1, TimeUnit.SECONDS);
			} catch (InterruptedException e1) {
				logger.log(e1.getMessage());
			}
			if (e != null) {
				cache.offer(e);
			}
		}
		
		if (cache.size() == 0) {
			List<String> words = dicFile.loadRandomWords(size);
			for (String word : words) {
				cache.offer(word);
			}
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
