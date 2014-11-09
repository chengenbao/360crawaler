package cn.ac.ict.chengenbao;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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
	private static final char[] HEX_DIGITS = { '0', '1', '2', '3', '4', '5', 
            '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' }; 

	
	class Worker implements Runnable {

		@Override
		public void run() {
			while (!stopped) {
				
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e1) {
					logger.log(e1.getMessage());
				}
				
				String page = getPage();
				
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
			boolean success = queue.offer(page, 2, TimeUnit.SECONDS);
			
			if ( !success) {
				if (! writeToFile(page)) {
					logger.log("Write page failed!");
				}
			}
		} catch (InterruptedException e) {
			logger.log(e.getMessage());
		}
	}
	
	private static boolean  writeToFile(String page) {
		try {
			MessageDigest messageDigest = MessageDigest.getInstance("SHA1");
			messageDigest.update(page.getBytes());
			
			StringBuilder sb = new StringBuilder();
			sb.append(Util.PAGES_DIR);
			sb.append("/");
			sb.append(getFormattedText(messageDigest.digest()));
			sb.append(Util.PAGE_TMP_FILE_SUFFIX);
			
			String filename = sb.toString();
			
			FileOutputStream fos = new FileOutputStream(filename);
			fos.write(page.getBytes());
			fos.close();
			
			File tmpFile = new File(filename);
			int index = filename.indexOf(Util.PAGE_TMP_FILE_SUFFIX);
			File f = new File(filename.substring(0, index));
			return tmpFile.renameTo(f);
		} catch (NoSuchAlgorithmException | IOException e) {
			logger.log(e.getMessage());
			return false;
		}
	}

	public void start() {
		new Thread(new Worker()).start();
		
		// make sure page directory exists
		File f = new File(Util.PAGES_DIR);
		
		if ( !f.isDirectory()) {
			f.mkdir();
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
	
	private static String getFormattedText(byte[] bytes) {
		  int len = bytes.length;
		  StringBuilder buf = new StringBuilder(len * 2); 
		  for (int j = 0; j < len; j++) {
			  buf.append(HEX_DIGITS[(bytes[j] >> 4) & 0x0f]);
			  buf.append(HEX_DIGITS[bytes[j] & 0x0f]);
		  }
		  
		  return buf.toString();
	}
	
	private String getPage() {
		String page = null;

		try {
			page = queue.poll(1, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			logger.log(e.getMessage());
		}

		if (page == null) { // queue is empty£¬ enqueue
			File dir = new File(Util.PAGES_DIR);

			for (File f : dir.listFiles()) {
				if (f.isFile() && !f.getName().endsWith(Util.PAGE_TMP_FILE_SUFFIX)) { // page file
					FileInputStream fin = null;
					
					try {
						fin = new FileInputStream(f);

						// read file
						byte[] buffer = new byte[1024];
						int num = 0;
						StringBuilder sb = new StringBuilder();

						while ((num = fin.read(buffer)) != -1) {
							if (num == buffer.length) {
								sb.append(buffer);
							} else {
								for (int i = 0; i < num; ++i) {
									sb.append(buffer[i]);
								}
							}
						}// read end
						fin.close();

						page = sb.toString();
						boolean suc = queue.offer(page);
						if (!suc) { // queue is full
							break;
						} else {
							f.delete();
						}
					} catch (IOException e) {
						logger.log(e.getMessage());
					}
				}
			}
		}

		try {
			page = queue.poll(1, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			logger.log(e.getMessage());
		}

		return page;
	}
	
	public static void main(String[] args) {
		try {
			FileInputStream fin = new FileInputStream("pages/eab44583cec3441a1dfb15bc80358506ee258f08.tmp");

			byte[] buffer = new byte[256];
			StringBuilder sb = new StringBuilder();
			int num = 0;

			while ((num = fin.read(buffer)) != -1) {
				if (num > 0) {
					sb.append(new String(buffer, 0, num));
				}
			}

			fin.close();

			String page = sb.toString();
			writeToFile(page);
		} catch (IOException e) {
			logger.log(e.getMessage());
		}
	}
}
