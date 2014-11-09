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
	private LinkedBlockingQueue<String> crawledWords = null; // has
																						// been
																						// crawed
	private boolean stopped = false;
	private final static Logger logger = Logger.getLogger();
	private Integer crawedWordCount = 0;
	private DictFile dicFile = new DictFile();

	public Buckets(int cacheSz) {
		// TODO Auto-generated constructor stub
		cache = new LinkedBlockingQueue<String>(cacheSz);
		crawledWords = new LinkedBlockingQueue<String>(Util.PERSISTENT_COUNT + Util.SAVE_COUNT);
	}

	public void persistent() {
		List<String> words = new ArrayList<String>(cache); //存储未检索单词
		saveDictFile(words); 
		cache.clear();
		
		saveCrawledWords(); // 存储已经搜索的单词
		crawledWords.clear();
	}

	public void addWords(Collection<String> words) {
		for (String word : words) {
			if (!find(word)) {
				try {
					cache.put(word);
				} catch (InterruptedException e) {
					logger.log(e.getMessage());
				}
			}
		}
	}

	private void saveCrawledWords() {
		List<String> words = new ArrayList<String>();
		int i = 0;
		int size = crawledWords.size();
		
		while( i < size) {
			String word;
			try {
				word = crawledWords.poll(1, TimeUnit.SECONDS);
				if (word != null) {
					words.add(word);
				}
			} catch (InterruptedException e) {
				logger.log(e.getMessage());
			}	
			++i;
		}
		
		saveDictFile(words);
	}

	private boolean find(String word) {
		Set<String> tmp = null;
		tmp = new HashSet<String>(crawledWords);
		
		return cache.contains(word) || tmp.contains(word);
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

	/**
	 * 写入dict文件， 保证没有重复
	 * @param words
	 */
	private void saveDictFile(List<String> words) {
		synchronized(crawedWordCount) {
			crawedWordCount += dicFile.write(words);
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

						for (int i = 0; i < Util.BATCH_SIZE; ++i) {
							String word = cache.poll(2, TimeUnit.SECONDS);
							if ( word != null ) {
								words.add(word);
							}
						}

						Scheduler.getInstance().getWorkQueue().addWords(words);
						saveToCrawledWords(words);
						
						System.out.println("bucket size ------------------ " + cache.size());

					} catch (InterruptedException e) {
						logger.log(e.getMessage());
					}
				}
			}

		}).start();
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

	private void saveToCrawledWords(List<String> words) {
		for (String word : words) {
			crawledWords.add(word);
		}
		
		int count  = crawledWords.size();
		System.out.println("================ words count " + count + " ===========================\n");
		
		
		if (! checkForTerminate()) {		
			boolean pers = (count >= Util.PERSISTENT_COUNT);
			if (pers) {
				saveCrawledWords();
			}
		}
	}
	
	public static void main(String[] args) {
		DictFile dict = new DictFile();
		System.out.println(dict.find("apple4s官网"));
		dict.close();
	}
}
