package com.mokasocial.flicka;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.Log;

/**
 * An icon management class which uses the url and local save paths to fetch,
 * save, and load icons and returning input streams.
 * 
 * @author Michael Hradek mhradek@gmail.com, mhradek@mokasocial.com,
 *         mhradek@flicka.mobi
 * @date 2010.01.12
 */
public class ImageMgmt {

	final static float USER_ICON_CORNER_RADIUS = 4;

	final static int CHUNKSIZE = 8192; // Size of fixed chunks
	final static int IO_BUFFER_SIZE = 1024; // Size of reading buffer

	public ImageMgmt() {
	}

	/**
	 * Download a contact icon given the url and return the inputStream
	 * representation of it. Any errors like malformed URLs, connection,
	 * DataInputStreamIO, and so on will log and the function will return null.
	 * 
	 * @param urlString
	 * @return
	 */

	public static InputStream fetchImage(String urlString) {
		ImageMgmt thisObj = ImageMgmt.createIconMgmtObj();

		InputStream in;
		try {
			in = new BufferedInputStream(new URL(urlString).openStream(), IO_BUFFER_SIZE);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}

		int bytesRead = 0;
		byte[] buffer = new byte[IO_BUFFER_SIZE];
		byte[] fixedChunk = new byte[CHUNKSIZE];
		ArrayList<byte[]> BufferChunkList = new ArrayList<byte[]>();
		int spaceLeft = CHUNKSIZE;
		int chunkIndex = 0;

		try {
			// Loop until the DataInputStream is completed.
			while ((bytesRead = in.read(buffer)) != -1) {
				if (bytesRead > spaceLeft) {
					// Copy to end of the current chunk
					System.arraycopy(buffer, 0, fixedChunk, chunkIndex, spaceLeft);
					BufferChunkList.add(fixedChunk);

					// Create a new chunk, and fill in the leftover.
					fixedChunk = new byte[CHUNKSIZE];
					chunkIndex = bytesRead - spaceLeft;
					System.arraycopy(buffer, spaceLeft, fixedChunk, 0, chunkIndex);
				} else {
					// Plenty of space, just copy it in
					System.arraycopy(buffer, 0, fixedChunk, chunkIndex, bytesRead);
					chunkIndex = chunkIndex + bytesRead;
				}
				spaceLeft = CHUNKSIZE - chunkIndex;
			}
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}

		if (in != null) {
			try {
				in.close();
			} catch (IOException e) {
				e.printStackTrace();
				return null;
			}
		}

		// Copy it all into one big array; the final return value
		int responseSize = (BufferChunkList.size() * CHUNKSIZE) + chunkIndex;

		byte[] responseBody = new byte[responseSize];
		int index = 0;
		for (byte[] b : BufferChunkList) {
			System.arraycopy(b, 0, responseBody, index, CHUNKSIZE);
			index = index + CHUNKSIZE;
		}

		System.arraycopy(fixedChunk, 0, responseBody, index, chunkIndex);
		return new BufferedInputStream(new ByteArrayInputStream(responseBody), IO_BUFFER_SIZE);
	}

	/**
	 * Save the contact icon stream to a local cache. This will overwrite any
	 * local copy of the cached file.
	 */
	public static void saveImage(InputStream userIconStream, String urlString, File directory) {
		String iconFilename = Utilities.extractFilenameFromUrl(urlString);
		ImageMgmt thisObj = ImageMgmt.createIconMgmtObj();

		Log.d("Flicka", "Attempting to save '" + iconFilename + "' locally.");

		// If this isn't a contact icon file, we need the full URL path to
		// create the directory.
		if (iconFilename.contains("@") == false) {
			String tempPath = directory.getPath() + File.separator + Utilities.extractDirectoryFromUrl(urlString);
			Log.d("Flicka", "PATH: " + tempPath);
			directory = new File(tempPath);
		}

		try {
			// The directory doesn't exist; create it.
			if (!directory.exists()) {
				directory.mkdirs();
			}

			File iconFile = new File(directory.getPath() + File.separator + iconFilename);

			// The file doesn't exist; create a 'handle' for it.
			if (!iconFile.exists()) {
				iconFile.createNewFile();
			}

			// Write file contents to the 'handle'.
			FileOutputStream out = new FileOutputStream(iconFile);
			Bitmap bmp = BitmapFactory.decodeStream(userIconStream);
			bmp.compress(Bitmap.CompressFormat.JPEG, 100, out);
			out.flush();
			out.close();
			clearBitmap(bmp);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Load the cached icon from the local storage area. If there is any
	 * failure, the directory or file don't exist, or the file seems corrupted
	 * (only checking for the size of the file for now) then return null.
	 * 
	 * @param urlString
	 * @return
	 */
	public static InputStream loadImage(String urlString, File directory) {
		String iconFilename = Utilities.extractFilenameFromUrl(urlString);
		ImageMgmt thisObj = ImageMgmt.createIconMgmtObj();

		// If this isn't a contact icon file, we need the full URL path to
		// create the directory.
		if (iconFilename.contains("@") == false) {
			String tempPath = directory.getPath() + File.separator + Utilities.extractDirectoryFromUrl(urlString);
			Log.d("Flicka", "PATH: " + tempPath);
			directory = new File(tempPath);
		}

		InputStream in = null;
		try {
			// The directory doesn't exist so either the save function never ran
			// or there is no SD card
			if (!directory.exists()) {
				Log.d("Flicka", "Directory " + directory.getPath() + " does not exist.");
				return null;
			}

			// The file doesn't exist so it's new
			File iconFile = new File(directory.getPath() + File.separator + iconFilename);
			if (!iconFile.exists()) {
				Log.d("Flicka", "File path " + directory.getPath() + File.separator + iconFilename + " does not exist.");
				return null;
			}

			// The file wasn't written correctly so let's assume it's not here
			if (iconFile.length() == 0) {
				return null;
			}

			in = new BufferedInputStream(new FileInputStream(iconFile), IO_BUFFER_SIZE);
			Log.d("Flicka", "File " + iconFilename + " exists and returning stream.");

		} catch (Exception e) {
			e.printStackTrace();
		}

		return in;
	}

	/**
	 * Return a Bitmap with rounded corners specified by the radius.
	 * 
	 * @param bitmap
	 * @param roundPx
	 * @return
	 */
	public static Bitmap getRndedCornerBtmp(Bitmap bitmap, float roundPx) {
		final int width = bitmap.getWidth();
		final int height = bitmap.getHeight();

		Bitmap output = Bitmap.createBitmap(width, height, Config.ARGB_8888);
		Canvas canvas = new Canvas(output);

		final int color = 0xff424242;
		final Paint paint = new Paint();
		final Rect rect = new Rect(0, 0, width, height);
		final RectF rectF = new RectF(rect);

		paint.setAntiAlias(true);
		canvas.drawARGB(0, 0, 0, 0);
		paint.setColor(color);
		canvas.drawRoundRect(rectF, roundPx, roundPx, paint);

		paint.setXfermode(new PorterDuffXfermode(Mode.SRC_IN));
		canvas.drawBitmap(bitmap, rect, rect, paint);

		return output;
	}

	/**
	 * Free up a bitmap and ask the garbage collector to grab resources.
	 * 
	 * @param bitmap
	 */
	public static void clearBitmap(Bitmap bitmap) {
		bitmap.recycle();
		bitmap = null;
		System.gc();
	}

	/**
	 * Resize the image to the given dimensions, scaled.
	 * 
	 * @param bitmap
	 * @param targetWidth
	 * @param targetHeight
	 * @return
	 */
	public static Bitmap resizeBitmap(final Bitmap bitmap, final int targetWidth, final int targetHeight) {

		final int width = bitmap.getWidth();
		final int height = bitmap.getHeight();

		int newWidth = 0, newHeight = 0;

		final double scaledByWidthRatio = ((float) targetWidth) / width;
		final double scaledByHeightRatio = ((float) targetHeight) / height;

		if (height * scaledByWidthRatio <= targetHeight) {
			newWidth = targetWidth;
			newHeight = (int) (height * scaledByWidthRatio);
		} else {
			newWidth = (int) (width * scaledByHeightRatio);
			newHeight = targetHeight;
		}

		Log.d("Flicka", "Resizing to WxH: " + newWidth + "x" + newHeight);

		return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
	}

	/**
	 * A simple static object constructor used in static functions to give
	 * 'this' references something.
	 * 
	 * @return
	 */
	private static ImageMgmt createIconMgmtObj() {
		return new ImageMgmt();
	}
}
