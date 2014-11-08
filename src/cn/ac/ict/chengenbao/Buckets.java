package cn.ac.ict.chengenbao;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * manage the words, be persistent to disk File
 * 
 * @author chengenbao
 *
 */
public class Buckets {
	private LinkedBlockingQueue<String> cache = null;
	private Set<String> crawledWords = null; // has
																						// been
																						// crawed
	private int cacheSize = 0;
	private boolean stopped = false;
	private final static Logger logger = Logger.getLogger();
	private boolean dirty = false;
	private Integer crawedWordCount = 0;

	public Buckets(int cacheSz) {
		// TODO Auto-generated constructor stub
		cache = new LinkedBlockingQueue<String>(cacheSz);
		crawledWords = new HashSet<String>();
	}

	public void persistent() {
		saveCrawledWords();

		List<String> words = new ArrayList(cache);
		cache.clear();
		crawledWords.clear();
		saveDictFile(words);
	}

	public void addWords(List<String> words) {
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
		List<String> words = null;
		int i = 0;
		synchronized(crawledWords) {
			for(String word: crawledWords) {
				words.add(word);
				crawledWords.remove(word);
				++i;
				if (i >= Util.SAVE_COUNT) {
					break;
				}
			}
		}

		saveDictFile(words);
	}

	private boolean find(String word) {
		return cache.contains(word) || crawledWords.contains(word) || findInDictFile(word);
	}

	private boolean findInDictFile(String word) {
		boolean beDirtied = false;

		synchronized (Util.SAVE_FILE_NAME) {
			beDirtied = dirty;
		}
		
		// check the file
		if (beDirtied) { // dict file has been writen
			synchronized (Util.SAVE_FILE_NAME) {
				try {
					FileInputStream fin = new FileInputStream(Util.SAVE_FILE_NAME);
					byte[] buffer = new byte[1024];
					int num = 0;
					
					while((num = fin.read()) != -1) {
						int lastPos = 0;
						
						if ( buffer[lastPos] == 44) {
							++lastPos;
						}
						
						for(int i = lastPos; i < num; ++i) { // find ',' in buffer
							int len = i - lastPos;
							if ( buffer[i] == 44 ) { // get it
								for(byte b : word.getBytes()) { // compare every byte
									if (b != buffer[lastPos]) {
										break;
									}
									
									++lastPos;
								}
								
								if (len == word.getBytes().length && lastPos == i) { // stop at ','
									return true;
								}
								
								lastPos = i + 1;
							}
						}
					}

				} catch (IOException e) {
					logger.log(e.getMessage());
				}
			}
		}
		return false;
	}

	private boolean checkForTerminate() {
		int count = 0;
		
		count += cache.size();
		synchronized(crawedWordCount) {
			count += crawedWordCount;
		}

		boolean shouldTerminated = (count >= Util.TARGET_COUNT);
		if (shouldTerminated) {
			Scheduler.getInstance().stop();
		}

		return shouldTerminated;
	}

	private void saveDictFile(List<String> words) {
		
		synchronized (Util.SAVE_FILE_NAME) {
			FileWriter writer = null;

			try {
				writer = new FileWriter(Util.SAVE_FILE_NAME, true);

				if (writer != null) {
					for (String word : words) {
						writer.write(Util.WORD_SPLIT_CHAR + word);
					}
					writer.close();
				}

			} catch (IOException e) {
				logger.log(e.getMessage());
			}
			
			dirty = true;
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
		persistent();
	}

	private void saveToCrawledWords(List<String> words) {
		synchronized (crawledWords) {
			for (String word : words) {
				crawledWords.add(word);
			}
		}
		
		synchronized(crawedWordCount) {
			crawedWordCount += words.size();
		}
		
		if (! checkForTerminate()) {
		
			boolean pers = (crawledWords.size() > Util.PERSISTENT_COUNT);
			if (pers) {
				saveCrawledWords();
			}
		}
	}
}
