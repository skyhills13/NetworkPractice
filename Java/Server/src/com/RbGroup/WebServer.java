package com.RbGroup;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class WebServer {
	
	private final static Logger logger = Logger.getLogger(WebServer.class.getName());
	private final static String filePath = "/Users/next/Desktop/test.mp4";
	private final static int SERVER_PORT = 3000;
	private final static int MAX_CONNECTION = 100;
	private final static String SERVER_IP = "127.0.0.1";
	protected final static int BUFFER_SIZE = 1024;
	
	/*
	 * 파일데이터를 BUFFER_SIZE로 나눈후, Integer key값으로 데이터를 관리한다.
	 * 모든 Thread에서 접근할 수 있으며, 아래와 같은 Data Scheme으로 이루어져 있다.
	 * 
	 * { 0 : DATA0, 	1 : DATA3,		2 : DATA2 ...  }
	 */
	protected static HashMap<Integer, ReadData> memoryData = new HashMap<Integer, ReadData>();
	
	/*
	 * 파일 Read시 Pointer개념으로 접근할 수 있도록 해준다.
	 */
	protected static RandomAccessFile fileManager;
	
	/*
	 * memory데이터에 저장되어 있는  ReadData객체의
	 * Reference Count가 0이 되는 순간 해당 index의 데이터를
	 * memory에서 삭제한다.
	 * ReadData의 decreaseReferenceCount함수를 참고하자.
	 */
	protected static void removeReadDataFromMemory(int offset) {
		memoryData.remove(offset);
	}
	
	private static String getTime() {
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("[hh:mm:ss]");
		return simpleDateFormat.format(new Date());
	}
	
	public static void main(String[] args) {
        try {
        	
        	//설정된 IP bind를 위해 inerAddress를 선언
        	InetAddress address = InetAddress.getByName(SERVER_IP);
        	
        	// 서버소켓을 생성한다.
			ServerSocket listenSocket = new ServerSocket(SERVER_PORT, MAX_CONNECTION, address);
			logger.log(Level.INFO, "WebServer Socket Created");
			
			// 파일접근을 위한 객체를 생성한다.
			fileManager = new RandomAccessFile(filePath, "r");
			
			
	        Socket socket;
	        // 클라이언트가 연결될때까지 대기한다.
	        while ((socket = listenSocket.accept()) != null) {
	        	
	        	//접근시간과 접근 Address를 출력한다.
	        	logger.log(Level.INFO, getTime() + socket.getInetAddress());
	        	
	        	//Thread객체를 생성한후,
	        	RequestHandler requestHandler = new RequestHandler(socket);
	        	
	        	//Thread를 실행한다.
	            requestHandler.start();
	        }
			
	        if(listenSocket !=null) {
	        	logger.log(Level.INFO, "Socket close");
	        	listenSocket.close();
	        }
		} catch (IOException e) {
			logger.log(Level.SEVERE, "Create WebServer Error : " + e);
		}
	}
}
