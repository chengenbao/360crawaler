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
	public final static int CRAWLER_WORKER_NUMBER = 5;
	public final static int BUCKET_CACHE_SIZE = 1000;
	public final static int WORK_QUEUE_SIZE = 2000;
	public final static int INDEXER_WORKER_NUMBER = 2;
	public final static int INDEXER_CACHE_SIZE = 1000;
	public final static int SLEEP_MAX_SECONDS = 10;
	public final static String SEARCH_PATTERN = "Ïà¹ØËÑË÷</th>";
	public final static int TARGET_COUNT = 100000;
	public final static int PERSISTENT_COUNT = 5000;
	public final static String WORD_SPLIT_CHAR = ",";
}
