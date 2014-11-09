package cn.ac.ict.chengenbao;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 
 * Constant and utility function
 * @author chengenbao
 *
 */
public class Util {
	public  static  String SAVE_FILE_NAME = "words.txt";
	public  static int CRAWLER_WORKER_NUMBER = 10;
	public  static int BUCKET_CACHE_SIZE = 1000;
	public  static int WORK_QUEUE_SIZE = 1000;
	public  static int INDEXER_WORKER_NUMBER = 6;
	public  static int INDEXER_CACHE_SIZE = 100;
	public  static int SLEEP_MAX_SECONDS = 30;
	public  static String SEARCH_PATTERN = "相关搜索</th>";
	public  static int TARGET_COUNT = 100000;
	public  static int PERSISTENT_COUNT = 1000;
	public  static int BATCH_SIZE = 50;
	public  static int SAVE_COUNT = 500;
	public  static byte WORD_SPLIT_CHAR = 44;
	public  static String PAGES_DIR = "pages";
	public  static String PAGE_TMP_FILE_SUFFIX = ".tmp";
	public  static int BUFFER_SIZE = 2048;
	public static List<String> SEED_WORDS = new ArrayList<String>();
	
	private static void loadParams() {
		SEED_WORDS.add("hadoop");
		SEED_WORDS.add("mapreduce");
		SEED_WORDS.add("jquery");
		SEED_WORDS.add("mongodb");
		
		SEED_WORDS.add("bigtable");
		SEED_WORDS.add("apple");
		SEED_WORDS.add("bootstrap");
		SEED_WORDS.add("linux");
		
		// 读取配置
		try {
			FileInputStream fin = new FileInputStream("config.ini");
			
			byte[] buffer = new byte[1024];
			int num = 0;
			StringBuilder sb = new StringBuilder();
			
			while( (num = fin.read(buffer)) != -1 ) {
				sb.append(new String(buffer, 0, num));
			}
			
			fin.close();
			
			String config = sb.toString();
			for(String line: config.split("\n")) {
				String[] arr = line.split("=");
				if ( arr.length < 2) {
					continue;
				}
				String key = arr[0].trim();
				String value = arr[1].trim();
				
				if (key.equals("workqueue.size")) {
					Util.WORK_QUEUE_SIZE = Integer.parseInt(value);
				} else if (key.equals("bucket.cache_size")) {
					Util.BUCKET_CACHE_SIZE = Integer.parseInt(value);
				} else if (key.equals("crawler.number")) {
					Util.CRAWLER_WORKER_NUMBER = Integer.parseInt(value);
				} else if (key.equals("indexer.number")) {
					Util.INDEXER_WORKER_NUMBER = Integer.parseInt(value);
				} else if (key.equals("indexer.cache_size")) {
					Util.INDEXER_CACHE_SIZE = Integer.parseInt(value);
				} else if (key.equals("crawler.max_sleep_time")) {
					Util.SLEEP_MAX_SECONDS = Integer.parseInt(value);
				} else if (key.equals("bucket.persistent_count")) {
					Util.PERSISTENT_COUNT = Integer.parseInt(value);
				} else if (key.equals("bucket.batch_size")) {
					Util.BATCH_SIZE = Integer.parseInt(value);
				} else if (key.equals("bucket.save_count")) {
					Util.SAVE_COUNT = Integer.parseInt(value);
				} else if (key.equals("buffer.size")) {
					Util.BUFFER_SIZE = Integer.parseInt(value);
				} else if (key.equals("bucket.word_split_char")) {
					Util.WORD_SPLIT_CHAR = Byte.parseByte(value);
				} else if (key.equals("indexer.pages_dir")) {
					Util.PAGES_DIR = value;
				} else if (key.equals("indexer.page_tmp_file_suffix")) {
					Util.PAGE_TMP_FILE_SUFFIX = value;
				}
			}
		} catch (IOException e) {
			System.out.println("未找到配置文件");
		}
	}
	
	static{
		loadParams();
	}
}
