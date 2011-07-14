package com.mokasocial.flicka;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;

import org.xml.sax.SAXException;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore.Images;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;

import com.aetrion.flickr.Flickr;
import com.aetrion.flickr.FlickrException;
import com.aetrion.flickr.photos.GeoData;
import com.aetrion.flickr.photos.geo.GeoInterface;
import com.aetrion.flickr.uploader.UploadMetaData;
import com.aetrion.flickr.uploader.Uploader;

/**
 * This is the controller for uploading images. At this time we upload one image
 * at a time. The upload is initiated using the URI of the image and grabbing
 * the image as a stream and uploading it using the FlickrJ lib uploader.
 * 
 */
public class ActivityUpload extends Activity {

	private Context mContext;
	private Activity mActivity;
	private Uri mRequestedUri;
	private Authorize mAuthorize;
	private UploadMetaData mMetaData;
	private InputStream mInputStream;
	private ContentResolver mContentResolver;
	private ProgressDialog mDialog;
	private String mPhotoId;

	private static final int PROGRESS_PHOTO_UPLOAD_SUCCESS = 0;
	private static final int PROGRESS_PHOTO_UPLOAD_FAILURE = 1;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mContext = this;
		mActivity = this;
		mAuthorize = Authorize.initializeAuthObj(mContext);
		mContentResolver = getContentResolver();
		mDialog = new ProgressDialog(mContext);
		mDialog.setMessage("Uploading. Please wait...");
		mDialog.setIndeterminate(true);

		// This activity assumes this file was set to the the one we want to
		// upload.

		Intent intent = mActivity.getIntent();
		Bundle extras = intent.getExtras();
		mRequestedUri = (Uri) extras.get(Flicka.INTENT_EXTRA_FILE_URI);

		// If coming from somewhere other than Flicka (eg Camera) then use that
		// method
		if (mRequestedUri == null) {
			Utilities.debugLog(this, "External intent requesting upload.");
			mRequestedUri = (Uri) extras.getParcelable(Intent.EXTRA_STREAM);
			String scheme = mRequestedUri.getScheme();
			if (scheme.equals("content")) {
				Cursor cursor = mContentResolver.query(mRequestedUri, null, null, null, null);
				cursor.moveToFirst();
				String filePath = cursor.getString(cursor.getColumnIndexOrThrow(Images.Media.DATA));
				cursor.close();
				mRequestedUri = Uri.parse(filePath);
			}
		}

		Utilities.debugLog(this, "Using path: " + mRequestedUri.getPath());
		mMetaData = new UploadMetaData();

		// Set up the various strings and view for this sub activity.
		Utilities.setupActivityView(this, R.string.upload, R.layout.view_upload);

		mInputStream = showImagePreview();
	}

	private InputStream showImagePreview() {
		// final ImageView imageView = (ImageView)
		// findViewById(R.id.upload_image);
		InputStream inputStream;
		try {
			inputStream = mContentResolver.openInputStream(mRequestedUri);
		} catch (FileNotFoundException e) {
			// Try opening the file using the path instead of the content
			// resolver
			try {
				inputStream = new BufferedInputStream(new FileInputStream(new File(mRequestedUri.getPath())), ImageMgmt.IO_BUFFER_SIZE);
			} catch (FileNotFoundException e1) {
				Utilities.errorOccurred(this, "Couldn't show preview", e1);
				return null;
			}
		} catch (Exception e) {
			Utilities.errorOccurred(this, "Couldn't show preview", e);
			return null;
		}

		// imageView.setImageBitmap(ImageMgmt.resizeBitmap(BitmapFactory.decodeStream(inputStream),
		// 100, 200));
		// imageView.setBackgroundResource(R.drawable.opacity_25);
		// imageView.setPadding(8, 8, 8, 8);

		/*
		 * This is ugly but what happens is that the content resolver's stream
		 * doesn't close after reading from it whereas the usual method
		 * (FileInputStream) is a one time use. Now for the ugly: If input
		 * stream has 0 available, assume its the FileInputStream method and
		 * reload. There is a better way to do this. For now...
		 */
		try {
			if (inputStream.available() < 1) {
				inputStream = new BufferedInputStream(new FileInputStream(new File(mRequestedUri.getPath())), ImageMgmt.IO_BUFFER_SIZE);
			}
		} catch (Exception e) {
			Utilities.errorOccurred(mContext, "Non contentResolver inputStream reinitization failure", e);
		}

		return inputStream;
	}

	/**
	 * A UI accessible function call for processUpload. For use with OnClick.
	 * 
	 * @param view
	 */
	public void processUpload(View view) {
		processUpload();
	}

	private void processUpload() {
		mDialog.show();

		// Start the retrieval of contacts thread.
		Thread uploadThread = new Thread(null, rUploadPhoto, Flicka.FLICKA_THREAD_NAME);
		uploadThread.start();
	}

	private final Runnable rUploadPhoto = new Runnable() {
		public void run() {
			try {
				gatherMeta();
				mPhotoId = uploadPhoto();
				uploadHandler.sendEmptyMessage(PROGRESS_PHOTO_UPLOAD_SUCCESS);
			} catch (Exception e) {
				Utilities.errorOccurred(mContext, "Upload failed", e);
				uploadHandler.sendEmptyMessage(PROGRESS_PHOTO_UPLOAD_FAILURE);
			}
		}
	};

	private void uploadSuccess() {
		AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
		builder.setMessage("Your photo has been uploaded").setCancelable(false).setPositiveButton(getString(R.string.option_okay), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				finish();
			}
		});
		AlertDialog alert = builder.create();
		alert.show();
	}

	private void uploadFailure() {
		AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
		builder.setMessage("There was a problem uploading your photo").setCancelable(false).setIcon(android.R.drawable.ic_dialog_alert).setPositiveButton(getString(R.string.option_okay), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				finish();
			}
		});
		AlertDialog alert = builder.create();
		alert.show();
	}

	/**
	 * This handle expects messages from the thread from within it is called.
	 * 
	 */
	private final Handler uploadHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case PROGRESS_PHOTO_UPLOAD_SUCCESS:
				processGeoLocation(mPhotoId);
				mDialog.dismiss();
				uploadSuccess();
				break;
			case PROGRESS_PHOTO_UPLOAD_FAILURE:
				mDialog.dismiss();
				uploadFailure();
				break;
			}
		}
	};

	private void gatherMeta() {
		// Ready and grab data from various fields on view
		final EditText titleBox = (EditText) findViewById(R.id.image_title);
		final EditText descriptionBox = (EditText) findViewById(R.id.image_description);
		final EditText tagsBox = (EditText) findViewById(R.id.image_tags);
		final CheckBox publicCheckBox = (CheckBox) findViewById(R.id.image_public_cb);

		// Convert strings within tagsbox into collection
		String arrayTags[] = tagsBox.getText().toString().split(" ");
		Collection<String> tags = Arrays.asList(arrayTags);

		// Put those details into meta object
		mMetaData.setTitle(titleBox.getText().toString());
		mMetaData.setDescription(descriptionBox.getText().toString());
		mMetaData.setTags(tags);
		mMetaData.setPublicFlag(publicCheckBox.isChecked());
	}

	private void processGeoLocation(String photoId) {

		final CheckBox addGeoLocation = (CheckBox) findViewById(R.id.image_geo_location_cb);

		if (addGeoLocation.isChecked() == false) {
			return;
		}

		LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

		Criteria criteria = new Criteria();
		criteria.setAccuracy(Criteria.ACCURACY_FINE);
		criteria.setAltitudeRequired(false);
		criteria.setBearingRequired(false);

		// Try the best provider first
		String provider = locationManager.getBestProvider(criteria, true);

		if (provider == null) {
			return;
		}

		Location location = locationManager.getLastKnownLocation(provider);

		// If the location is still null, let's skip this step. We can let the
		// user know.
		if (location == null) {
			Utilities.debugLog(mContext, "Unable to determine geo location. Skipping location tag");
			return;
		}

		GeoData geoData = new GeoData();
		geoData.setLatitude((float) location.getLatitude());
		geoData.setLongitude((float) location.getLongitude());
		geoData.setAccuracy(Flickr.ACCURACY_CITY);

		GeoInterface geoFace = mAuthorize.flickr.getGeoInterface();
		try {
			geoFace.setLocation(photoId, geoData);
		} catch (FlickrException e) {
			if (e.getErrorCode() == "7") {
				Utilities.errorOccurred(this, "Unable to set geo location of photo. Privacy settings update required", e);
			}
		} catch (Exception e) {
			Utilities.errorOccurred(this, "Geo location setting failed", e);
		}
	}

	private String uploadPhoto() throws IOException, FlickrException, SAXException {
		Uploader uploader = null;
		String photoId = null;
		Authorize authorize = Authorize.initializeAuthObj(mContext);
		uploader = authorize.flickr.getUploader();
		photoId = uploader.upload(mInputStream, mMetaData);
		Utilities.debugLog(mContext, "Upload success: " + photoId);
		mInputStream.close();

		return photoId;
	}
}