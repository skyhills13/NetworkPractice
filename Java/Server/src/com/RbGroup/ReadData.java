package com.RbGroup;

public class ReadData {
	
	//현재의 offset sequence를 저장한다.
	private int sequence;
	private int reference;
	private byte[] data;
	
	//생성자 초기화
	public ReadData(int sequence, byte[] data) {
		this.sequence = sequence;
		this.data = data;
		this.reference = 1;
	}
	
	public int getReferenceCount() {
		return reference;
	}
	
	//reference count를 thread safe하기 위해 synchronize선언
	public synchronized void IncreaseReferenceCount() {
		++this.reference;
	}
	
	public synchronized void decreaseReferenceCount() {
		--this.reference;
		
		//참조가 하나도 없을경우 메모리에서 자원해제
		if ( reference == 0 )
			WebServer.removeReadDataFromMemory(sequence);
	}
	
	public byte[] getData() {
		return data;
	}
	public void setData(byte[] data) {
		this.data = data;
	}
}
