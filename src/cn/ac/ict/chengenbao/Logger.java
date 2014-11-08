package cn.ac.ict.chengenbao;

import java.io.FileWriter;
import java.io.IOException;

public class Logger {
	private static Logger instance = new Logger();
	
	private Logger() {
	}
	
	public static Logger getLogger() {
		return instance;
	}
	
	public void  log(String msg) {
		System.out.println(msg);
	}
}
