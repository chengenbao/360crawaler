package cn.ac.ict.chengenbao;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class DictFile {
	private final static Logger logger = Logger.getLogger();
	private FileOutputStream lockFos = null;
	
	public DictFile() {
		try {
			lockFos = new FileOutputStream("lock");
		} catch (FileNotFoundException e) {
			logger.log(e.getMessage());
		}
	}
	
	public  boolean find(String word) {
		FileLock lock = null;
		try {
			byte[] buffer = new byte[Util.BUFFER_SIZE];
			int num = 0;
			int start = 0;
			FileInputStream fin = new FileInputStream(Util.SAVE_FILE_NAME);
			lock = lockFos.getChannel().lock(0, Integer.MAX_VALUE, false);
			
			while ((num = fin.read(buffer, start, buffer.length - start)) != -1) {
				boolean suc = findInBuffer(word, buffer, 0, num);
				if (suc) {
					return true;
				}
				start = moveBuffer(buffer, 0, start + num);
			} // while end
			
			lock.release();
			fin.close();
		} catch (IOException e) {
			logger.log(e.getMessage());
		} 
		
		return false;
	}
	
	/**
	 * write to dict file, remvoe duplicates
	 * 
	 * @param words
	 * @return the
	 */
	public int write(Collection<String> words) {
		int size = 0;
		LinkedBlockingQueue<String> wordSet = new LinkedBlockingQueue<String>();
		for(String word: words) {
			if (!wordSet.contains(word)) {
				wordSet.add(word);
			}
		}
		
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(Util.SAVE_FILE_NAME, true);
			wordSet = filterWords(wordSet);
			FileLock lock = lockFos.getChannel().lock(0, Integer.MAX_VALUE, false);

			for (String word : wordSet) {
				fos.write(Util.WORD_SPLIT_CHAR);
				fos.write(word.getBytes());
				fos.flush();
				++size;
			}

			lock.release();
			fos.close();
		} catch (IOException e) {
			logger.log("can not write dict file\n");
		}

		return size;
	}
	
	private  LinkedBlockingQueue<String> filterWords(LinkedBlockingQueue<String> wordSet) {
		try {
			byte[] buffer = new byte[Util.BUFFER_SIZE];
			int num = 0;
			int start = 0;
			FileInputStream fin = new FileInputStream(Util.SAVE_FILE_NAME);
			FileLock lock = lockFos.getChannel().lock(0, Integer.MAX_VALUE, false);
			while ((num = fin.read(buffer, start, buffer.length - start)) != -1) {
				for(String word: wordSet) {
					boolean find = findInBuffer(word, buffer, 0, start + num);
					if (find) {
						wordSet.remove(word);
					}	
				}
				start = moveBuffer(buffer, 0, start + num);
			} // while end
			
			lock.release();
			fin.close();
		} catch (IOException e) {
			logger.log(e.getMessage());
		}
		
		return wordSet;	
	}
	
	private static int moveBuffer(byte[] buffer, int start, int end) {
		// 整理buffer， 将最后一个分隔符后面的字符保留
		// 找到第一个分隔符
		int i = end - 1;
		while(buffer[i] != Util.WORD_SPLIT_CHAR && i >= start) {
			--i;
		}
		
		if (i > start) { // 很幸运，找到了
			int flag = i; // 标记分隔符位置
			while (i < end) { //移动分隔符后面的字符
				buffer[start + i - flag] = buffer[i];
				++i;
			}
			start = start + i - flag; // 计算缓冲区有效起始位置
		} 
		
		return start;
	}
	
	/**
	 * 
	 * @param word
	 * @param buffer
	 * @param start
	 * @param end
	 * @return
	 */
	private static boolean findInBuffer(String word, byte[] buffer, int start, int end) {
		if (end <= start || word.length() > end - start) {
			return false;
		}
		
		if (word.length() == end - start) { // completely match
			return word.equals(new String(buffer, start, end));
		}

		int lastPos = start;
		if (buffer[lastPos] == Util.WORD_SPLIT_CHAR) {
			++lastPos;
		}

		for (int i = lastPos; i < end; ++i) { // find ',' in buffer
			if (buffer[i] == Util.WORD_SPLIT_CHAR) { // get it
				if (i - lastPos == word.getBytes().length) { // length match
					for (byte b : word.getBytes()) { // compare every byte
						if (b != buffer[lastPos]) {
							break;
						}
						++lastPos;
					}

					if (lastPos == i) {
						return true;
					}
				}
				lastPos = i + 1; //next charator
			}
		}
		
		// find in the end
		if ( lastPos < end && end - lastPos == word.getBytes().length) {
			for (byte b : word.getBytes()) { // compare every byte
				if (b != buffer[lastPos]) {
					break;
				}
				++lastPos;
			}
			
			if (lastPos == end) {
				return true;
			}
		}

		return false;
	}
	
	/**
	 *  load n random words from dict file 
	 * @param count the count of words to load
	 * @return
	 */
	public List<String> loadRandomWords(int count) {
		byte[] buffer = new byte[Util.BUFFER_SIZE];
		int num = 0;
		List<String> words = new ArrayList<String>();
		
		try {
			FileInputStream fin = new FileInputStream(Util.SAVE_FILE_NAME);
			FileLock lock = lockFos.getChannel().lock();
			
			int filesize = fin.available();
			int byteCount = count * 8; // 每个单词最多（估计值）占8个字节
			int skipTime = 0;
			
			int maxSkipTime = (filesize - byteCount) / buffer.length; // 最多可以跳过次数
			
			if (maxSkipTime > 1) {
				Random rdm = new Random(System.currentTimeMillis());
				skipTime = rdm.nextInt() % maxSkipTime;
				if (skipTime < 0) {
					skipTime = -skipTime;
				}
			}
			
			int i = 0;
			int start = 0;
			
			while((num = fin.read(buffer, start, buffer.length - start)) != -1 && i < count) {
				if (skipTime > 0) { //skip
					--skipTime;
					continue;
				}
				
				int lastPos = 0;
				
				for (int j = lastPos; j < start + num; ++j) {
					if (buffer[j] == Util.WORD_SPLIT_CHAR && buffer[lastPos] == Util.WORD_SPLIT_CHAR 
							&& j > lastPos + 1) {
						String str = new String(buffer, lastPos + 1, j - lastPos - 1, "UTF8");
						words.add(str);
						lastPos = j;
						++i;
					}
				}
				
				start = moveBuffer(buffer, 0, start + num);
			}
			
			lock.release();
			fin.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			logger.log(e.getMessage());
		}
		
		return words;
	}
	
	public void close() {
		try {
			lockFos.close();
		} catch (IOException e) {
			logger.log(e.getMessage());
		}
	}
	
	public int count() {
		int size = 0;
		try {
			FileInputStream fin = new FileInputStream(Util.SAVE_FILE_NAME);
			FileLock lock = lockFos.getChannel().lock();
			byte[] buffer = new byte[Util.BUFFER_SIZE];
			int num = 0;
			
			while( (num = fin.read(buffer)) != -1) {
				for(int i = 0; i < num; ++i) {
					if (buffer[i] == Util.WORD_SPLIT_CHAR ) {
						++size;
					}
				}
			}
			
			lock.release();
			fin.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			logger.log(e.getMessage());
		}
		
		return size;	
	}
}
