package cn.ac.ict.chengenbao;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;

public class Crawler {
	private HttpRequester requester = new HttpRequester();
	private ExecutorService pool = null;
	private final static Logger logger= Logger.getLogger();
	private int workerNum = 0;
	private boolean stopped = false;
	
	class Worker implements Runnable {
		private String name = "worker";
		public Worker(String n) {
			name = n;
		}
		
		@Override
		public void run() {
			while (!stopped) {
				String word = Scheduler.getInstance().getWorkQueue().getWord();
				String url = "http://www.so.com/s?ie=utf-8&shb=1&src=360sou_newhome";
				if (word == null) {
					System.out.println("------------ Work queue is empty! ----------------\n");
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						logger.log(e.getMessage());
					}
				} else {
					System.out.println("==================="  + word + "===================");
					// random sleep
					Random rdm = new Random(System.currentTimeMillis());

					int sec = rdm.nextInt() % Util.SLEEP_MAX_SECONDS;
					if (sec < 0) {
						sec = -sec;
					}
					sec += 5;

					try {
						Thread.sleep(sec * 1000);
					} catch (InterruptedException e) {
						logger.log(e.getMessage());
					}
					Map<String, String> fields = new HashMap<String, String>();
					fields.put("q", word);

					String page = requester.get(url, fields);
					Scheduler.getInstance().getIndexer().addPage(page);
				}
			}

		}

	}
	
	public Crawler(int workernum) {
		workerNum = workernum;
		pool = Executors.newFixedThreadPool(workerNum);
	}

	public void start() {
		for (int i = 0; i < workerNum; ++i) {
//			pool.submit(new Worker("worker" + i));
			
			new Thread(new Worker("worker" + i)).start();
		}
	}

	public void stop() {
		stopped = true;
		pool.shutdown();
	}
}
