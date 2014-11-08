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
	private LinkedBlockingQueue<String> allWords = new LinkedBlockingQueue<String>(); // has
																						// been
																						// crawed
	private int cacheSize = 0;
	private boolean stopped = false;
	private final static Logger logger = Logger.getLogger();
	private boolean dirty = false;

	public Buckets(int cacheSz) {
		// TODO Auto-generated constructor stub
		cache = new LinkedBlockingQueue<String>(cacheSz);
		cacheSize = cacheSz;
	}

	public void persistent() {
		saveAllWords();

		List<String> words = new ArrayList(cache);
		cache.clear();
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

	private void saveAllWords() {
		List<String> words = null;

		words = new ArrayList(allWords);
		allWords.clear();

		saveDictFile(words);
	}

	private boolean find(String word) {
		boolean find = false;
		
		// check cache
		find = cache.contains(word);
		if (find) {
			return find;
		}

		// check allwords
		find = allWords.contains(word);
		if (find) {
			return find;
		}
		
		boolean beDirtied = false;
		synchronized(Util.SAVE_FILE_NAME) {
			beDirtied = dirty;
		}
		
		find = false;
		// check the file
		if (beDirtied) {
			String content = readDictFile();
			String arr[] = content.split(Util.WORD_SPLIT_CHAR);
			
			for (String str: arr) {
				if (str == word) {
					find = true;
					break;
				}
			}
		}
		
		
		return find;
	}

	private boolean checkForTerminate() {
		int count = 0;
		
		boolean beDirtied = false;
		synchronized(Util.SAVE_FILE_NAME) {
			beDirtied = dirty;
		}
		
		if (beDirtied) {
			String content = readDictFile();
			String[] arr = content.split(Util.WORD_SPLIT_CHAR);
			for (String str : arr) {
				if (str.length() > 0) {
					++count;
				}
			}
		}
		
		count += cache.size();
		count += allWords.size();

		boolean shouldTerminated = (count >= Util.TARGET_COUNT);
		if (shouldTerminated) {
			Scheduler.getInstance().stop();
		}

		return shouldTerminated;
	}


	private String readDictFile() {
		String content = "";

		synchronized (Util.SAVE_FILE_NAME) {
			try {
				FileInputStream fin = new FileInputStream(Util.SAVE_FILE_NAME);

				byte[] buffer = new byte[256];
				StringBuilder sb = new StringBuilder();
				int num = 0;

				while ((num = fin.read(buffer)) != -1) {
					if (num > 0) {
						sb.append(new String(buffer, 0, num));
					}
				}

				fin.close();
				content = sb.toString();
			} catch (IOException e) {
				logger.log(e.getMessage());
			}
		}

		return content;
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
						Thread.sleep(3000);

						List<String> words = new ArrayList<String>();

						for (int i = 0; i < 5; ++i) {
							String word = cache.take();
							if ( word != null ) {
								words.add(word);
							}
						}

						Scheduler.getInstance().getWorkQueue().addWords(words);
						saveToAllWords(words);
						
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

	private void saveToAllWords(List<String> words) {
		for (String word : words) {
			allWords.add(word);
		}
		
		if (! checkForTerminate()) {
		
			boolean pers = (allWords.size() > Util.PERSISTENT_COUNT);
			if (pers) {
				saveAllWords();
			}
		}
	}
}
