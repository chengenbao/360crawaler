package cn.ac.ict.chengenbao;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HttpRequester {
	private final static Logger logger= Logger.getLogger();
	
	// METHOD
	public final static String GET = "GET";
	public final static String POST = "POST";
	
	public String get(String url) {
		return get(url, null);
	}

	public String get(String url,  Map<String, String> field) {
		String[] group = retrieveHostAndPort(url);
		if (group == null) {
			return null;
		}
		
		String hostIp = dnsResolve(group[0]);
		int port = Integer.parseInt(group[1]);
		String uri = group[2];
		
		String header = buildHeader(GET, uri, group[0], port, field);
		
		byte[] content = retrieveContent(hostIp, port, url, header);
		
		return processData(content);
	}
	
	public static String buildHeader(String method, String uri, String host,
			int port, Map<String, String> field) {
		String query = null;
		if (field != null) {
			int i = 0;
			
			for (Entry<String, String> item : field.entrySet()) {
				try {
					String k = URLEncoder.encode(item.getKey(), "UTF8");
					String v= URLEncoder.encode(item.getValue(), "UTF8");
					
					if (i > 0) {
						query += "&";
						query += k + "=" + v; 
					} else {
						query = k + "=" + v; 
					}
				} catch (UnsupportedEncodingException e) {
					logger.log(e.getMessage());
					return null;
				}
				++i;
			}
		}
		
		method = method.toUpperCase();
		if (method.equals("GET")) { //modify uri
			int index = uri.indexOf("?");
			if (index == -1 && query != null) { // no query fields in uri
				uri += "?";
			} else if(query != null){
				uri += "&";
			}
			
			if (query != null) {
				uri += query;
			}
		}
		
		StringBuilder sb = new StringBuilder();
		sb.append(method + " " + uri + " HTTP/1.1\r\n");
		sb.append("Accept: html/text\r\n");
		
		// host
		sb.append("Host: " + host);
		if (port != 80) {
			sb.append(":" + port);
		}
		sb.append("\r\n");
		
		// connection type
		sb.append("Connection: close\r\n");
		
		// send to server
		sb.append("\r\n");
		
		System.out.println(uri);
		return sb.toString();
	}
	
	private byte[] retrieveContent(String ip, int port, String url, String header) {
		List<Byte> tmp = new ArrayList<Byte>();
		
		
		try {
			Socket client = new Socket(ip, port);

			client.setSoTimeout(5000);
			
			// send to server
			OutputStream out = client.getOutputStream();
			out.write(header.getBytes());
			out.flush();
			
			
			InputStream in = client.getInputStream();
			byte[] buffer = new byte[256];
			int num = 0;
			while ((num = in.read(buffer)) != -1) {
				for (int i = 0; i < num; ++i) {
					tmp.add(buffer[i]);
				}
			}
			
			out.close();
			in.close();
			
			byte[] content = new byte[tmp.size()];
			for(int i = 0; i < content.length; ++i) {
				content[i] = tmp.get(i);
			}
			
			return content;
			
		} catch (UnknownHostException e) {
			logger.log(e.getMessage());
		} catch (IOException e) {
			logger.log(e.getMessage());
		}
		return null;
	}
	
	public static String[] retrieveHostAndPort(String url) {
		Pattern pattern = Pattern.compile("[a-z0-9]+\\.[\\w]+\\.[a-z]+");
		Matcher matcher = pattern.matcher(url);
		boolean rs = matcher.find();
		if (!rs) {
			return null;
		}
		String[] group = new String[3];
		group[0] = matcher.group(0);
		
		// retrieve port
		group[1] = "80"; //default 80
		int index = url.indexOf(group[0]);
		String suffix = url.substring(index + group[0].length());
		
		group[2] = "/"; // uri
		
		if (suffix.startsWith("/")) {
			group[2] = suffix;
			
			return group;
		} else if (suffix.startsWith(":")) {
			index = suffix.indexOf("/");
			if (index != -1) {
				group[1] = suffix.substring(1, index);
				group[2] = suffix.substring(index);
			}
			try {
				Integer.parseInt(group[1]);
			} catch (NumberFormatException e) {
				return null;
			}		
			return group;
		}
		
		return group;
	}
	
	public static String dnsResolve(String host) {
		if (host == null) {
			return null;
		}
		
		host = host.toLowerCase();
		
		// remove the protocol prefix if exists
		int index = host.indexOf("://");
		if (index != -1 ) { // found
			host = host.substring(index + 3);
		}
		
		// check if the host is valid
		Pattern pattern = Pattern.compile("^[a-z0-9]+\\.[\\w]+\\.[a-z]+$");
		boolean valid = pattern.matcher(host).matches();
		if (!valid) {
			logger.log("invalid host name\n" );
			return null;
		}
		
		try {
			InetAddress inetHost =  InetAddress.getByName(host);
			return inetHost.getHostAddress();
		} catch (UnknownHostException e) {
			logger.log("Unrecognized host\n");
		}
		
		return null;
	}
	
	/**
	 * 
	 * @param content the socket receive byte array
	 * @return the clean html page
	 */
	private static String processData(byte[] content) {
		if (content == null) { 
			return null;
		}
		
		int i = 0;
		boolean chunked = false;
		int lastPos = 0;
		
		// remove header
		while (true) {
			while (content[i] != 10) {
				++i;
			}
			String line = new String(content, lastPos, i - lastPos - 1); // -1 for trim '\r'
			if (line.toLowerCase().contains("chunked")) {
				chunked = true;
			}
			lastPos = i + 1;
			if (  content[i - 2] ==  10) {
				break;
			}
			++i;
		}
		
		List<Integer[]> positions = new ArrayList<Integer[]>(); // every item is a Integer[2]
		if (!chunked) {
			Integer[] pos = new Integer[2];
			pos[0] = i + 1;
			pos[1] = content.length - pos[0];
			positions.add(pos);
		} else {
			++i; // skip first '\n'
			int chunkSize = 0;
			do {
				int start = i;
				chunkSize = 0;
				while (content[i] != 13) { // search for '\r'

					int value = content[i];
					if (value >= 48 && value <= 57) {
						chunkSize *= 16;
						chunkSize += value - 48;
					} else if (value >= 65 && value <= 70) {
						chunkSize *= 16;
						chunkSize += value - 55;
					} else if (value >= 97 && value <= 102) {
						chunkSize *= 16;
						chunkSize += value - 87;
					} else {
						break;
					}

					++i;
				}
				start = i + 2; // skip for \n
				Integer[] pos = new Integer[2];
				pos[0] = start;
				pos[1] = chunkSize;
				positions.add(pos);
				
				i = start + chunkSize + 2; // skip for CRLF
			} while (chunkSize > 0 && i < content.length);
		}
		
		int len = 0;
		
		// move chunk
		for(Integer[] pos : positions) {
			int start = pos[0];
			int size = pos[1];
			
			for (i = 0; i < size; ++i) {
				content[len++] = content[start + i];
			}
		}
		
		try {
			return new String(content, 0, len, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			logger.log(e.getMessage());
			return null;
		}
	}
	
	public static void main(String[] args) {
		Map<String, String> fields = new HashMap<String, String>();
		fields.put("name", "chengenbao");
		fields.put("age", "27");
		fields.put("home address", "beijing daxing");
		
		//System.out.println(buildHeader("get", "/index.php", "www.baidu.com", 80, null));
		
		HttpRequester requester = new HttpRequester();
		String content = requester.get("http://www.so.com/s?ie=utf-8&shb=1&src=360sou_newhome&q=hadoop");
		
		try {
			if (content != null) {
				FileOutputStream fos = new FileOutputStream("output.txt");
				fos.write(content.getBytes());
				fos.close();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			logger.log(e.getMessage());
		}
	}

}
