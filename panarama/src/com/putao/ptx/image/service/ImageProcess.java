package com.putao.ptx.image.service;


public class ImageProcess {
	static {
		System.loadLibrary("image_proc");
	}
	static public native boolean cropImage(String inPath, String outPath);
	static public native boolean stitch(String[] paths, String outPath);
}