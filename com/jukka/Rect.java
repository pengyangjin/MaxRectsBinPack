package com.jukka;

public class Rect {
	public int x = 0;
	public int y = 0;
	public int width = 0;
	public int height = 0;

	public Rect() {

	}

	public Rect(int x, int y, int width, int height) {
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;
	}

	public Rect clone() {
		return new Rect(x, y, width, height);
	}

	public String toString() {
		return "x:" + x + ",y:" + y + ",width:" + width + ",height:" + height;
	}
}
