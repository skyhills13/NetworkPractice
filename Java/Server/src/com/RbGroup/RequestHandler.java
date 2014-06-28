package com.RbGroup;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RequestHandler extends Thread {

	private final static Logger logger = Logger.getLogger(RequestHandler.class.getName());
	private Socket socketConnection;
	
	//현재 Thread에서 다운로드 받아야 하는 Offset정보를 담고있다.
	private int currentOffset;
	
	public RequestHandler(Socket socket) {
		this.socketConnection = socket;
		
		//초기설정은 0 (자동으로 초기화되나 명시적으로 써주었다.)
		this.currentOffset = 0;
	}
	
	/*
	 * 현재 currentOffset에 맞춰서 데이터를 리턴해준다.
	 */
	public byte[] readOffsetData() throws IOException {
		
		//이전에 참조하고 있던 reference는 더이상 필요하지 않으므로,
		//memory상에 점유하던 이전의 reference count를 낮춘다. 
		//Decrease Previous Reference count
		ReadData previousOffsetData = WebServer.memoryData.get(currentOffset-1);
		if (previousOffsetData != null) {
			previousOffsetData.decreaseReferenceCount();
		}
		
		//필요한 데이터정보를 전역 메모리객체로부터 가져온다.
		ReadData currentOffsetData  = WebServer.memoryData.get(currentOffset);
		
		//만약 메모리에 필요로하는 데이터가 없다면
		if (currentOffsetData  == null) {
			
			//파일에서 필요한 데이터의 position 시작위치로 이동한다.
			WebServer.fileManager.seek(currentOffset * WebServer.BUFFER_SIZE);
			
			byte[] bytes = new byte[WebServer.BUFFER_SIZE];
			
			//위에 선언한 bytes객체에 원하는 데이터를 저장한다.
			WebServer.fileManager.read(bytes, 0, WebServer.BUFFER_SIZE);
			
			//메모리에 새로 load한 데이터를 저장한다.
			WebServer.memoryData.put(currentOffset, new ReadData(currentOffset, bytes));
			
			++this.currentOffset;
			return bytes;
		} else {
			//Increase Current Reference count
			currentOffsetData.IncreaseReferenceCount();
			++this.currentOffset;
			return currentOffsetData .getData();	
		}
	}
	
	@Override
	public void run() {
		logger.log(Level.INFO, "WebServer Thread Created!");
		
		OutputStream responseStream = null;
		DataOutputStream dataOutputStream = null;

		try {
			responseStream = socketConnection.getOutputStream();
			dataOutputStream = new DataOutputStream(responseStream);
			
			//전송할 파일의 총 크기
			long fileLength = WebServer.fileManager.length();
			
			//총 수행해야할 for loop 횟수를 계산
			int opperationCount = (int) (fileLength / WebServer.BUFFER_SIZE); 
			
			//Header 데이터 전송
			dataOutputStream.writeBytes("HTTP/1.1 200 Document Follows \r\n");
			dataOutputStream.writeBytes("Content-Type: application/octet-stream\r\n");
			dataOutputStream.writeBytes("Content-Disposition: attachment;filename=\"test.mp4\"\r\n");
			dataOutputStream.writeBytes("Content-Length: " + fileLength + "\r\n");
			dataOutputStream.writeBytes("\r\n");

			//Body 데이터 전송
			//연산과정중, 나머지로 인해 opperationCount보다 1회 더 수행되어야 한다.
			for (int index = 0 ; index <= opperationCount+1 ; ++index) {
				//currentOffset을 기준으로 데이터를 읽어온다.
				byte[] reaData = readOffsetData();
				dataOutputStream.write(reaData, 0, reaData.length);
			}
		} catch (IOException e) {
			logger.log(Level.SEVERE, "getStream Error : " + e);
		} finally {
			//자원해제
			try {

				if (socketConnection != null) {
					socketConnection.close();
				}

				if (dataOutputStream != null) {
					dataOutputStream.close();
				}

				if (responseStream != null) {
					responseStream.close();
				}

			} catch (IOException e) {
				logger.log(Level.SEVERE, "Close Exception : " + e);
			}
		}
	}
}
