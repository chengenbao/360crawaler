package cn.ac.ict.chengenbao;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

public class Tester {

	public static void main(String[] args) {
		DictFile dicFile = new DictFile();
		
		System.out.println(dicFile.find("影视大全手"));
		
		List<String> words = dicFile.loadRandomWords(Util.BATCH_SIZE);
		for(String word: words) {
			System.out.println(word);
		}
	}
}
