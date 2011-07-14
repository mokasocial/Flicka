package com.aetrion.flickr.photos;

/**
 * Quick and dirty class that only contains the four values of a Rectangle. Android doesn't support 
 * most of the java.awt.* libraries.
 * 
 * @author Michael Hradek <mhradek@gmail.com>, <mhradek@mokasocial.com>, <mhradek@flicka.mobi>
 * @date 2009.11.04
 */
public class RectangleCustom {
	public int x, y, width, height;
	
	public RectangleCustom (int paramX, int paramY, int paramWidth, int paramHeight) {
		x = paramX;
		y = paramY;
		width = paramWidth;
		height = paramHeight;
	}
}