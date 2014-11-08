package cn.ac.ict.chengenbao;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class DictFile {
	private final static Logger logger = Logger.getLogger();
	private FileOutputStream fos = null;
	private FileChannel outChannel = null;
	private FileInputStream fin = null;
	private FileChannel inChannel = null;
	
	public DictFile() {
		try {
			fos = new FileOutputStream(Util.SAVE_FILE_NAME, true);
			outChannel = fos.getChannel();
			fin = new FileInputStream(Util.SAVE_FILE_NAME);
			inChannel = fin.getChannel();
		} catch (FileNotFoundException e) {
			logger.log(e.getMessage());
		}
	}
	
	public  boolean find(String word) {
		try {
			byte[] buffer = new byte[1024];
			long num = 0;

			while ((num = fin.read(buffer)) != -1) {
				int lastPos = 0;
				if (buffer[lastPos] == 44) {
					++lastPos;
				}

				for (int i = lastPos; i < num; ++i) { // find ',' in buffer
					int len = i - lastPos;
					if (buffer[i] == 44) { // get it
						for (byte b : word.getBytes()) { // compare every byte
							if (b != buffer[lastPos]) {
								break;
							}
							++lastPos;
						}

						if (len == word.getBytes().length && lastPos == i) {
							return true;
						}

						lastPos = i + 1;
					}
				}
			} // while end
			
			fin.reset();
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
	public int write(List<String> words) {
		FileLock lock = null;
		try {
			lock = outChannel.lock();
		} catch (IOException e) {
			logger.log(e.getMessage());
		}
		
		int size = 0;
		
		if (lock != null) {
			for(String word: words) {
				if (!find(word)) {
					try {
						fos.write(word.getBytes());
						++size;
					} catch (IOException e) {
						logger.log(e.getMessage());
					}
				}
			}
			
			try {
				lock.release();
			} catch (IOException e) {
				logger.log(e.getMessage());
			}
		}
		return size;
	}
	
	public void close() {
		try {
			fos.close();
			fin.close();
		} catch (IOException e) {
			logger.log(e.getMessage());
		}
	}
}
