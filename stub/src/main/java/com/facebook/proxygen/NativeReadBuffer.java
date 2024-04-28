package com.facebook.proxygen;

import java.io.*;

public class NativeReadBuffer{
	public byte[] modifiedResponse;
	public int modifiedResponseOffset;
	public java.net.URI requestURI;
	public ByteArrayOutputStream incompleteResponse;

	public int _read(byte[] bArr, int i, int i2){
		return 0;
	}

	public int _size(){
		return 0;
	}
}