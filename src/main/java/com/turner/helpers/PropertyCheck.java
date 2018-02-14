package com.turner.helpers;

public class PropertyCheck {
	private String fileName;
	private String property;
	private Integer lineNumber;
	public PropertyCheck(String fileName, String property) {
		super();
		this.fileName = fileName;
		this.property = property;
	}
	public PropertyCheck(String fileName, String property, Integer lineNumber) {
		super();
		this.fileName = fileName;
		this.property = property;
		this.lineNumber=lineNumber;
	}
	
	public String getFileName() {
		return fileName;
	}


	public Integer getLineNumber() {
		return lineNumber;
	}
	public String getProperty() {
		return property;
	}

	

	@Override
	public String toString() {
		return "File Name:"+this.fileName+", Property:"+this.property;
	}

}
