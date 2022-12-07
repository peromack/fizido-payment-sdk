package com.pos.empressa.horizon_pos.Sunyard.bean;

public class TlvBean {

	/** 子域Tag标签 */
	private String tag;

	/** 子域取值的长度 */
	private int length;

	/** 子域取值 */
	private String value;

	public TlvBean(String tag, int length, String value) {
		this.length = length;
		this.tag = tag;
		this.value = value;
	}

	public String getTag() {
		return tag;
	}

	public void setTag(String tag) {
		this.tag = tag;
	}

	public int getLength() {
		return length;
	}

	public void setLength(int length) {
		this.length = length;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	@Override
	public String toString() {
		return "tag=[" + this.tag + "]," + "length=[" + this.length + "]," + "value=[" + this.value + "]";
	}
}