package cn.ac.ict.chengenbao;

import java.util.ArrayList;
import java.util.List;

/**
 * 
 * Constant and utility function
 * @author chengenbao
 *
 */
public class Util {
	public final static  String SAVE_FILE_NAME = "words.txt";
	public final static int CRAWLER_WORKER_NUMBER = 10;
	public final static int BUCKET_CACHE_SIZE = 1000;
	public final static int WORK_QUEUE_SIZE = 2000;
	public final static int INDEXER_WORKER_NUMBER = 6;
	public final static int INDEXER_CACHE_SIZE = 1000;
	public final static int SLEEP_MAX_SECONDS = 30;
	public final static String SEARCH_PATTERN = "Ïà¹ØËÑË÷</th>";
	public final static int TARGET_COUNT = 100000;
	public final static int PERSISTENT_COUNT = 1000;
	public final static int BATCH_SIZE = 50;
	public final static int SAVE_COUNT = 500;
	public final static String WORD_SPLIT_CHAR = ",";
	public final static String PAGES_DIR = "pages";
	public final static String PAGE_TMP_FILE_SUFFIX = ".tmp";
	public static List<String> SEED_WORDS = new ArrayList<String>();
	
	static{
		SEED_WORDS.add("hadoop");
		SEED_WORDS.add("mapreduce");
		SEED_WORDS.add("jquery");
		SEED_WORDS.add("mongodb");
		
		SEED_WORDS.add("bigtable");
		SEED_WORDS.add("apple");
		SEED_WORDS.add("bootstrap");
		SEED_WORDS.add("linux");
	}
}
