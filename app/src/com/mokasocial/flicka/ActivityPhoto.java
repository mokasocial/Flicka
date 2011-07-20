package com.mokasocial.flicka;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.xml.sax.SAXException;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.format.DateFormat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SlidingDrawer;
import android.widget.SlidingDrawer.OnDrawerOpenListener;
import android.widget.TextView;
import android.widget.Toast;

import com.aetrion.flickr.FlickrException;
import com.aetrion.flickr.favorites.FavoritesInterface;
import com.aetrion.flickr.groups.pools.PoolsInterface;
import com.aetrion.flickr.people.PeopleInterface;
import com.aetrion.flickr.photos.Extras;
import com.aetrion.flickr.photos.Photo;
import com.aetrion.flickr.photos.PhotosInterface;
import com.aetrion.flickr.photos.Size;
import com.aetrion.flickr.photos.comments.Comment;
import com.aetrion.flickr.photos.comments.CommentsInterface;
import com.csam.jentities.Entities;
import com.csam.jentities.HTML4Entities;

public class ActivityPhoto extends Activity {

	private Authorize mAuthorize;
	private Photo mPhoto;
	private List<Comment> mComments;
	private ArrayList<Comment> mCommentsArray;
	private Bitmap mBitmap;
	private String mPhotoUrl;
	private Context mContext;
	private Activity mActivity;
	private Resources mResources;
	private static Animation mFadeIn, mFadeOut;
	private RelativeLayout mImageDetails;
	private Bundle mSlideShowBundle;
	private SlideShow mSlideShow;
	private SlidingDrawer mSlidingDrawer;
	private CommentAdapter mCommentAdapter;

	final static int MENU_ID_DETAILS = 0;
	final static int MENU_ID_FAVORITE = 1;
	final static int MENU_SET_WALLPAPER = 2;
	final static int MENU_SEE_USER = 3;
	final static int MENU_SHARE_PHOTO = 4;

	private boolean mCommentsLoaded = false;

	private final static int PROGRESS_AUTH_SET_COMPLETE = 0;
	private final static int PROGRESS_GET_PHOTO_COMPLETE = 1;
	private final static int PROGRESS_LOADING_PHOTO_COMPLETE = 2;

	private final static int PROGRESS_GET_COMMENTS_INITIAL = 1;
	private final static int PROGRESS_GET_COMMENTS_CONTINUE = 2;
	private final static int PROGRESS_GET_COMMENTS_COMPLETE = 3;

	private final static int ANIMATION_TIME = 350;

	private static final int WALLPAPER_SCREEN_SIZE = 3;
	private static final String WALLPAPER_FILE_NAME = ".wallpaper.temp";
	private static final int REQUEST_CROP_IMAGE = 42;
	private UserTask<?, ?, ?> mTask;

	private static final int SWIPE_DIRECTION_LEFT = -1;
	private static final int SWIPE_DIRECTION_RIGHT = 1;

	private Animation outToLeftAnimation() {
		Animation outtoLeft = new TranslateAnimation(Animation.RELATIVE_TO_PARENT, 0.0f, Animation.RELATIVE_TO_PARENT, -1.0f, Animation.RELATIVE_TO_PARENT, 0.0f, Animation.RELATIVE_TO_PARENT, 0.0f);
		outtoLeft.setDuration(500);
		outtoLeft.setInterpolator(new AccelerateInterpolator());
		return outtoLeft;
	}

	private Animation outToRightAnimation() {
		Animation outtoRight = new TranslateAnimation(Animation.RELATIVE_TO_PARENT, 0.0f, Animation.RELATIVE_TO_PARENT, +1.0f, Animation.RELATIVE_TO_PARENT, 0.0f, Animation.RELATIVE_TO_PARENT, 0.0f);
		outtoRight.setDuration(500);
		outtoRight.setInterpolator(new AccelerateInterpolator());
		return outtoRight;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Set up some basic variables
		mContext = this;
		mActivity = this;
		mResources = getResources();

		// Slideshow listeners wil use these:
		// (possible: swipey, trackball, arrow key)
		// slidePhoto(mSlideShowBundle, SWIPE_DIRECTION_RIGHT);
		// slidePhoto(mSlideShowBundle, SWIPE_DIRECTION_RIGHT);

		// Initialize some preferences
		Utilities.setupActivityScreen(mActivity);
		// Set up the various strings and view for this sub activity.
		setContentView(R.layout.view_photo);

		// Now getting specifics
		mImageDetails = (RelativeLayout) findViewById(R.id.image_details);

		// Image fade attributes
		mFadeIn = new AlphaAnimation(0, 1);
		mFadeIn.setDuration(ANIMATION_TIME);
		mFadeOut = new AlphaAnimation(1, 0);
		mFadeOut.setDuration(ANIMATION_TIME);

		// Start progress box
		Loading.start(mActivity, Loading.ACTIVITY_LOADING_TEXT, R.string.progress_getting_photo, Loading.ACTIVITY_LOADING_ICON);

		initSlidingDrawer();
		mSlideShow = SlideShow.getSlideShow();

		// Start the lengthy work
		Thread photoThread = new Thread() {
			@Override
			public void run() {
				mAuthorize = Authorize.initializeAuthObj(mContext);
				photoHandler.sendEmptyMessage(PROGRESS_AUTH_SET_COMPLETE);
				try {
					mPhoto = initializePhoto();

					final PrefsMgmt prefs = new PrefsMgmt(mActivity);
					prefs.restorePreferences();

					// The user has opted to use the largest possible image
					if (prefs.isUseLargePhotosEnabled()) {
						PhotosInterface pFace = mAuthorize.flickr.getPhotosInterface();
						Collection<?> sizes = pFace.getSizes(mPhoto.getId());

						Size largestSize = null;

						// For now ignore original photos since they can be any
						// size. Memory!
						for (Iterator<?> iter = sizes.iterator(); iter.hasNext();) {
							Size size = (Size) iter.next();
							if (largestSize == null) {
								largestSize = size;
							} else if (largestSize.getLabel() < size.getLabel() && size.getLabel() != Size.ORIGINAL) {
								largestSize = size;
							}
						}

						// Something went wrong, use medium
						if (largestSize == null) {
							mPhotoUrl = mPhoto.getMediumUrl();
						} else {
							final int largestSizeLabel = largestSize.getLabel();
							switch (largestSizeLabel) {
							case Size.SMALL:
								Log.d("Photo", "Using small image");
								mPhotoUrl = mPhoto.getSmallUrl();
								break;
							case Size.MEDIUM:
								Log.d("Photo", "Using medium image");
								mPhotoUrl = mPhoto.getMediumUrl();
								break;
							case Size.LARGE:
								Log.d("Photo", "Using large image");
								mPhotoUrl = mPhoto.getLargeUrl();
								break;
							case Size.ORIGINAL:
								Log.d("Photo", "Using original image");
								mPhotoUrl = mPhoto.getOriginalUrl();
								break;
							default:
								Log.d("Photo", "Using default image");
								mPhotoUrl = mPhoto.getMediumUrl();
								break;
							}
						}
					} else {
						mPhotoUrl = mPhoto.getMediumUrl();
					}
				} catch (Exception e) {
					e.printStackTrace();
					mPhoto = null;
				}
				photoHandler.sendEmptyMessage(PROGRESS_GET_PHOTO_COMPLETE);
			}
		};
		photoThread.start();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (mTask != null && mTask.getStatus() != UserTask.Status.RUNNING) {
			mTask.cancel(true);
		}
	}

	/**
	 * This initiates the showing or hiding of the image details depending on
	 * the current state of the details.
	 * 
	 */
	private void toggleImageDetails() {
		final RelativeLayout imageDetailsView = (RelativeLayout) mActivity.findViewById(R.id.image_details);
		if (imageDetailsView.isShown()) {
			hideImageDetails();
		} else {
			showImageDetails();
		}
	}

	private final Handler photoHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case PROGRESS_AUTH_SET_COMPLETE:
				Loading.update(mActivity, Loading.ACTIVITY_LOADING_TEXT, R.string.progress_loading_photo);
				break;
			case PROGRESS_GET_PHOTO_COMPLETE:
				renderViewPhoto();
				break;
			case PROGRESS_LOADING_PHOTO_COMPLETE:
				showImageDetails();
				// now safe to show sliding drawer
				final SlidingDrawer drawer = (SlidingDrawer) findViewById(R.id.photo_sliding_drawer);
				drawer.setVisibility(View.VISIBLE);
				break;
			}
		}
	};

	private void renderViewPhoto() {
		try {
			populatePhotoView();
			Loading.dismiss(mActivity, Loading.ACTIVITY_LOADING_PARENT, Loading.ACTIVITY_LOADING_TARGET);
		} catch (Exception e) {
			e.printStackTrace();
			Loading.failed(mActivity, Loading.ACTIVITY_LOADING_LAYOUT, Loading.ACTIVITY_FAILED_LOAD);
		}
	}

	private void showImageDetails() {
		mImageDetails.startAnimation(mFadeIn);
		mImageDetails.setVisibility(View.VISIBLE);
	}

	private void hideImageDetails() {
		mImageDetails.startAnimation(mFadeOut);
		mImageDetails.setVisibility(View.GONE);
	}

	private void populatePhotoView() throws IOException {
		final ImageView imageView = (ImageView) findViewById(R.id.view_photo_small);
		final TextView imageName = (TextView) findViewById(R.id.image_name);
		final java.text.DateFormat dateformat = DateFormat.getMediumDateFormat(mContext);

		if (mPhoto.getOwner() != null) {
			imageName.setText("\"" + mPhoto.getTitle() + "\" by " + mPhoto.getOwner().getUsername());
		} else {
			imageName.setText(mPhoto.getTitle());
		}

		String separator = mResources.getString(R.string.key_val) + " ";
		// some of these too
		if (mPhoto.getDescription() != null && mPhoto.getDescription() != "") {
			Entities entities = new HTML4Entities();
			final TextView description = (TextView) findViewById(R.id.image_description);
			description.setText(entities.parseText(mPhoto.getDescription()));
			description.setVisibility(TextView.VISIBLE);
		}
		if (mPhoto.getDateAdded() != null && mPhoto.getDateAdded().toString() != "") {
			final TextView dateAdded = (TextView) findViewById(R.id.image_date_added);
			dateAdded.setText(mResources.getString(R.string.date_added) + separator + dateformat.format(mPhoto.getDateAdded()));
			dateAdded.setVisibility(TextView.VISIBLE);
		}
		if (mPhoto.getDatePosted() != null && mPhoto.getDatePosted().toString() != "") {
			final TextView datePosted = (TextView) findViewById(R.id.image_date_posted);
			datePosted.setText(mResources.getString(R.string.date_posted) + separator + dateformat.format(mPhoto.getDatePosted()));
			datePosted.setVisibility(TextView.VISIBLE);
		}
		if (mPhoto.getDateTaken() != null && mPhoto.getDateTaken().toString() != "") {
			final TextView dateTaken = (TextView) findViewById(R.id.image_date_taken);
			dateTaken.setText(mResources.getString(R.string.date_taken) + separator + dateformat.format(mPhoto.getDateTaken()));
			dateTaken.setVisibility(TextView.VISIBLE);
		}

		if (mPhoto.getViews() >= 0) {
			final TextView viewCount = (TextView) findViewById(R.id.image_view_count);
			viewCount.setText(mResources.getString(R.string.view_count) + separator + mPhoto.getViews());
			viewCount.setVisibility(TextView.VISIBLE);
		}

		toggleFaveIcon(mPhoto.isFavorite());
		registerForContextMenu(imageView);

		mBitmap = getBitmap(mPhotoUrl);
		imageView.setImageBitmap(mBitmap);
		photoHandler.sendEmptyMessage(PROGRESS_LOADING_PHOTO_COMPLETE);
	}

	private Bitmap getBitmap(String photoUrl) {
		// Check cache and return that
		Bitmap photo = mSlideShow.getBitmap(photoUrl);
		if (photo != null) {
			return photo;
		}

		InputStream is = ImageMgmt.loadImage(photoUrl, new File(Flicka.PHOTO_CACHE_DIR));
		if (is == null) {
			is = ImageMgmt.fetchImage(photoUrl);
			ImageMgmt.saveImage(is, photoUrl, new File(Flicka.PHOTO_CACHE_DIR));
			is = ImageMgmt.loadImage(photoUrl, new File(Flicka.PHOTO_CACHE_DIR));
		}

		photo = BitmapFactory.decodeStream(is);

		if (is != null) {
			try {
				is.close();
			} catch (IOException e) {
				e.printStackTrace();
				return null;
			}
		}

		// Place into cache
		mSlideShow.setBitmap(photoUrl, photo);
		return photo;
	}

	public void saveComment(View submitButton) {
		// Grab the basic parameters
		final EditText commentText = (EditText) findViewById(R.id.comment_box);
		CommentsInterface cFace = mAuthorize.flickr.getCommentsInterface();

		if (commentText.getText().toString().trim() == "") {
			// blank comment? no way
			return;
		}

		try {
			cFace.addComment(mPhoto.getId(), commentText.getText().toString());
			// oh and clear the text
			commentText.setText("");
		} catch (Exception e) {
			e.printStackTrace();
			Utilities.alertUser(mContext, "Sorry, couldn't save your comment.", null);
		}

		// now refresh them
		initComments();
	}

	private void cropOrFullWallpaperDialog() {
		try {
			setWallpaper();
			showWallpaperSuccess();
		} catch (IOException e) {
			showWallpaperError();
		}
	}

	private void setWallpaper() throws IOException {
		DisplayMetrics metrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(metrics);

		final int targetWidth = metrics.widthPixels * WALLPAPER_SCREEN_SIZE;
		final int targetHeight = metrics.heightPixels;

		Bitmap resizedBitmap = ImageMgmt.resizeBitmap(mBitmap, targetWidth, targetHeight);
		mContext.setWallpaper(resizedBitmap);
		ImageMgmt.clearBitmap(resizedBitmap);
	}

	private Photo initializePhoto() throws IOException, SAXException, FlickrException {
		// Grab the passed photo id.
		Intent intent = getIntent();
		Bundle extras = intent.getExtras();
		String photoId = extras.getString(Flicka.INTENT_EXTRA_VIEWPHOTO_PHOTOID);
		Log.d("Photo", "Populated photo object with id: " + photoId);
		mSlideShowBundle = extras.getBundle(Flicka.INTENT_EXTRA_SLIDESHOW);

		Photo photo = mSlideShow.getPhoto(photoId);
		if (photo == null) {
			PhotosInterface iFace = mAuthorize.flickr.getPhotosInterface();
			photo = iFace.getInfo(photoId, null);
			mSlideShow.setPhoto(photoId, photo);
			return photo;
		}

		return photo;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// Spawns a new task to set the wallpaper in a background thread when/if
		// we receive a successful result from the image cropper.
		if (requestCode == REQUEST_CROP_IMAGE) {
			if (resultCode == RESULT_OK) {
				mTask = new SetWallpaperTask().execute();
			} else {
				cleanupWallpaper();
				showWallpaperError();
			}
		}
	}

	private void showWallpaperError() {
		Toast.makeText(ActivityPhoto.this, mResources.getString(R.string.photo_set_fail), Toast.LENGTH_SHORT).show();
	}

	private void showWallpaperSuccess() {
		Toast.makeText(ActivityPhoto.this, mResources.getString(R.string.photo_set_ok), Toast.LENGTH_SHORT).show();
	}

	private void cleanupWallpaper() {
		// deleteFile(WALLPAPER_FILE_NAME);
	}

	/**
	 * Background task to crop a large version of the image. The cropped result
	 * will be set as a wallpaper. The tasks starts by showing the progress bar,
	 * then downloads the large version of the photo into a temporary file and
	 * ends by sending an intent to the Camera application to crop the image.
	 */
	@SuppressWarnings("unused")
	private class CropWallpaperTask extends UserTask<Photo, Void, Boolean> {
		private File mPhotoFile;

		@Override
		public void onPreExecute() {
			File directory = mContext.getFilesDir();
			File file = new File(WALLPAPER_FILE_NAME);

			if (!directory.exists()) {
				directory.mkdirs();
			}

			// The file doesn't exist; create a 'handle' for it.
			mPhotoFile = new File(directory.getPath() + File.separator + file.getName());
			if (!mPhotoFile.exists()) {
				try {
					mPhotoFile.createNewFile();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		@Override
		public Boolean doInBackground(Photo... params) {
			boolean success = false;
			FileOutputStream stream = null;

			try {
				stream = openFileOutput(mPhotoFile.getName(), MODE_WORLD_READABLE | MODE_WORLD_WRITEABLE);
				mBitmap.compress(CompressFormat.JPEG, 100, stream);

				success = true;
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				if (stream != null) {
					try {
						stream.close();
					} catch (IOException e) {
						success = false;
					}
				}
			}

			return success;
		}

		@Override
		public void onPostExecute(Boolean result) {
			if (!result) {
				cleanupWallpaper();
				showWallpaperError();
			} else {
				final int width = getWallpaperDesiredMinimumWidth();
				final int height = getWallpaperDesiredMinimumHeight();

				final Intent intent = new Intent("com.android.camera.action.CROP");
				intent.setClassName("com.android.camera", "com.android.camera.CropImage");
				final Uri fileUri = Uri.fromFile(mPhotoFile);
				Log.d("Photo", "Attempting to crop image: " + fileUri.toString());
				intent.setDataAndType(fileUri, "image/*");
				intent.putExtra("outputX", width);
				intent.putExtra("outputY", height);
				intent.putExtra("aspectX", width);
				intent.putExtra("aspectY", height);
				intent.putExtra("scale", true);
				intent.putExtra("noFaceDetection", true);
				intent.putExtra("output", fileUri);

				startActivityForResult(intent, REQUEST_CROP_IMAGE);
			}

			mTask = null;
		}
	}

	/**
	 * Background task to set the cropped image as the wallpaper. The task
	 * simply open the temporary file and sets it as the new wallpaper. The task
	 * ends by deleting the temporary file and display a message to the user.
	 */
	private class SetWallpaperTask extends UserTask<Void, Void, Boolean> {
		@Override
		public Boolean doInBackground(Void... params) {
			boolean success = false;
			InputStream in = null;
			try {
				in = new BufferedInputStream(openFileInput(WALLPAPER_FILE_NAME));
				setWallpaper(in);
				success = true;
			} catch (IOException e) {
				success = false;
			} finally {
				if (in != null) {
					try {
						in.close();
					} catch (IOException e) {
						success = false;
					}
				}
			}
			return success;
		}

		@Override
		public void onPostExecute(Boolean result) {
			cleanupWallpaper();

			if (!result) {
				showWallpaperError();
			} else {
				showWallpaperSuccess();
			}

			mTask = null;
		}
	}

	public void toggleFaveIcon(boolean isFavorite) {
		final Button isFaveIcon = (Button) findViewById(R.id.image_fave_icon);

		if (mPhoto.getOwner().toString().equals(mAuthorize.authObj.getUser().toString())) {
			// don't show if this user owns the photo
			isFaveIcon.setBackgroundDrawable(null);
			isFaveIcon.setVisibility(Button.GONE);
		} else {
			// show away! this gets toggled later
			isFaveIcon.setVisibility(Button.VISIBLE);
			if (isFavorite == false) {
				isFaveIcon.setBackgroundDrawable(mResources.getDrawable(R.drawable.icon_favorite_false));
			} else {
				isFaveIcon.setBackgroundDrawable(mResources.getDrawable(R.drawable.icon_favorite_true));
			}
		}
	}

	/**
	 * The layout onClick required version.
	 * 
	 * @see updateFaveStatus()
	 * @param view
	 */
	public void updateFaveStatus(View view) {
		updateFaveStatus();
	}

	/**
	 * Update the fave status of this photo depending on what it is now.
	 * 
	 */
	public void updateFaveStatus() {
		try {
			FavoritesInterface iFace = mAuthorize.flickr.getFavoritesInterface();
			Log.d("Photo", "This image is a favorite: " + mPhoto.isFavorite() + ". Photo id: " + mPhoto.getId());
			if (mPhoto.isFavorite() == true) {
				iFace.remove(mPhoto.getId());
				mPhoto.setFavorite(false);
				toggleFaveIcon(false);
			} else {
				iFace.add(mPhoto.getId());
				mPhoto.setFavorite(true);
				toggleFaveIcon(true);
			}
		} catch (Exception e) {
			e.printStackTrace();
			Utilities.alertUser(this, "Favorite status update failed.", e);
		}
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		menu.add(Menu.NONE, MENU_ID_DETAILS, 1, R.string.menu_info);
		menu.add(Menu.NONE, MENU_ID_FAVORITE, 2, R.string.menu_add_to_faves);
		menu.add(Menu.NONE, MENU_SET_WALLPAPER, 3, mResources.getString(R.string.photo_menu_set_wallpaper));
		menu.add(Menu.NONE, MENU_SEE_USER, 4, mResources.getString(R.string.photo_menu_see_owner));
		menu.add(Menu.NONE, MENU_SHARE_PHOTO, 5, mResources.getString(R.string.photo_menu_share));
		super.onCreateContextMenu(menu, v, menuInfo);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		// This needs some logic help. The user may click on home before
		// authenticate is finished.
		case MENU_ID_DETAILS:
			toggleImageDetails();
			return true;
		case MENU_ID_FAVORITE:
			updateFaveStatus();
			return true;
		case MENU_SET_WALLPAPER:
			cropOrFullWallpaperDialog();
			return true;
		case MENU_SEE_USER:
			Intent intent = new Intent(ActivityPhoto.this, ActivityUser.class);
			intent.putExtra(Flicka.INTENT_EXTRA_ACTIVITY_NSID, mPhoto.getOwner().getId());
			startActivity(intent);
			return true;
		case MENU_SHARE_PHOTO:
			Intent share = new Intent(Intent.ACTION_SEND);
			share.setType("text/plain");
			share.putExtra(Intent.EXTRA_TEXT, mPhoto.getUrl());
			share.putExtra(Intent.EXTRA_SUBJECT, mPhoto.getTitle());
			startActivity(Intent.createChooser(share, mResources.getString(R.string.photo_menu_share)));
		default:
			return true;
		}
	}

	public void runSlideShow() {
		Log.d("Photo", "RunSlideShow executed!");
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			e.printStackTrace();
			mSlideShowBundle.putBoolean(SlideShow.AUTO_SLIDE_SHOW_MODE, false);
			return;
		}

		mSlideShowBundle.putBoolean(SlideShow.AUTO_SLIDE_SHOW_MODE, true);
		slidePhoto(mSlideShowBundle, SWIPE_DIRECTION_RIGHT);
	}

	public void slidePhoto(Bundle slideShow, int direction) {
		final String identifier = slideShow.getString(SlideShow.CURRENT_IDENTIFIER);
		final boolean autoMode = slideShow.getBoolean(SlideShow.AUTO_SLIDE_SHOW_MODE);
		int position = slideShow.getInt(SlideShow.CURRENT_ITEM);
		switch (slideShow.getInt(SlideShow.CURRENT_STREAM)) {
		case SlideShow.STREAM_FAVORITES:
			try {
				FavoritesInterface fInt = mAuthorize.flickr.getFavoritesInterface();
				int newPosition = position + direction;

				if (newPosition < 1) {
					throw new Exception("Flickr uses a 1 based numbering system.");
				}

				Log.d("Photo", "Currently: " + position + " Requested: " + direction + " Result: " + newPosition);
				Object[] photoList = fInt.getList(identifier, 1, newPosition, null).toArray();
				Photo currentPhoto = (Photo) photoList[0];

				// Start pre fetch
				final int prefetchPosition = newPosition + direction;
				Thread prefetchThread = new Thread() {
					@Override
					public void run() {
						try {
							Authorize tAuthorize = Authorize.initializeAuthObj(mContext);
							FavoritesInterface fInt = tAuthorize.flickr.getFavoritesInterface();
							Object[] photoList = fInt.getList(identifier, 1, prefetchPosition, null).toArray();
							Photo prefetchPhoto = (Photo) photoList[0];
							String url = prefetchPhoto.getMediumUrl();
							File cacheDir = new File(Flicka.PHOTO_CACHE_DIR);

							InputStream is = ImageMgmt.loadImage(url, cacheDir);
							if (is == null) {
								is = ImageMgmt.fetchImage(url);
								ImageMgmt.saveImage(is, url, cacheDir);
							}

							mSlideShow.setPhoto(prefetchPhoto.getId(), prefetchPhoto);
							mSlideShow.setBitmap(url, BitmapFactory.decodeStream(is));
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				};
				prefetchThread.start();

				Intent intent = new Intent(ActivityPhoto.this, ActivityPhoto.class);
				if (android.os.Build.VERSION.SDK_INT >= 5) {
					intent.addFlags(65536); // Intent.FLAG_ACTIVITY_NO_ANIMATION
				}
				intent.putExtra(Flicka.INTENT_EXTRA_VIEWPHOTO_PHOTOID, currentPhoto.getId());

				// Send the details so we can do a slideshow if the user wants
				slideShow = new Bundle();
				slideShow.putInt(SlideShow.CURRENT_ITEM, newPosition);
				slideShow.putInt(SlideShow.CURRENT_STREAM, SlideShow.STREAM_FAVORITES);
				slideShow.putString(SlideShow.CURRENT_IDENTIFIER, identifier);
				slideShow.putBoolean(SlideShow.AUTO_SLIDE_SHOW_MODE, autoMode);
				intent.putExtra(Flicka.INTENT_EXTRA_SLIDESHOW, slideShow);

				// Animation stuff. Hopefully the above code will exception if
				// this ain't possible.
				final FrameLayout currentLayout = (FrameLayout) findViewById(R.id.main_activity_container);
				if (direction == SWIPE_DIRECTION_LEFT) {
					currentLayout.startAnimation(outToRightAnimation());
				} else if (direction == SWIPE_DIRECTION_RIGHT) {
					currentLayout.startAnimation(outToLeftAnimation());
				}

				startActivity(intent);
			} catch (Exception e) {
				mSlideShowBundle.putBoolean(SlideShow.AUTO_SLIDE_SHOW_MODE, false);
				e.printStackTrace();
			}
			break;
		case SlideShow.STREAM_GROUP_PHOTOS:
			try {
				PoolsInterface iFace = mAuthorize.flickr.getPoolsInterface();
				int newPosition = position + direction;
				if (newPosition < 1) {
					throw new Exception("Flickr uses a 1 based numbering system.");
				}

				Log.d("Photo", "Currently: " + position + " Requested: " + direction + " Result: " + newPosition);
				Object[] photoList = iFace.getPhotos(identifier, null, 1, newPosition).toArray();
				Photo photo = (Photo) photoList[0];

				// Start pre fetch
				final int prefetchPosition = newPosition + direction;
				Thread prefetchThread = new Thread() {
					@Override
					public void run() {
						try {
							Authorize tAuthorize = Authorize.initializeAuthObj(mContext);
							PoolsInterface iFace = tAuthorize.flickr.getPoolsInterface();
							Object[] photoList = iFace.getPhotos(identifier, null, 1, prefetchPosition).toArray();
							Photo prefetchPhoto = (Photo) photoList[0];
							String url = prefetchPhoto.getMediumUrl();
							File cacheDir = new File(Flicka.PHOTO_CACHE_DIR);

							InputStream is = ImageMgmt.loadImage(url, cacheDir);
							if (is == null) {
								is = ImageMgmt.fetchImage(url);
								ImageMgmt.saveImage(is, url, cacheDir);
							}

							mSlideShow.setPhoto(prefetchPhoto.getId(), prefetchPhoto);
							mSlideShow.setBitmap(url, BitmapFactory.decodeStream(is));
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				};
				prefetchThread.start();

				Intent intent = new Intent(ActivityPhoto.this, ActivityPhoto.class);
				if (android.os.Build.VERSION.SDK_INT >= 5) {
					intent.addFlags(65536); // Intent.FLAG_ACTIVITY_NO_ANIMATION
				}
				intent.putExtra(Flicka.INTENT_EXTRA_VIEWPHOTO_PHOTOID, photo.getId());

				// Send the details so we can do a slideshow if the user wants
				slideShow = new Bundle();
				slideShow.putInt(SlideShow.CURRENT_ITEM, newPosition);
				slideShow.putInt(SlideShow.CURRENT_STREAM, SlideShow.STREAM_GROUP_PHOTOS);
				slideShow.putString(SlideShow.CURRENT_IDENTIFIER, identifier);
				slideShow.putBoolean(SlideShow.AUTO_SLIDE_SHOW_MODE, autoMode);
				intent.putExtra(Flicka.INTENT_EXTRA_SLIDESHOW, slideShow);

				// Animation stuff. Hopefully the above code will exception if
				// this ain't possible.
				final FrameLayout currentLayout = (FrameLayout) findViewById(R.id.main_activity_container);
				if (direction == SWIPE_DIRECTION_LEFT) {
					currentLayout.startAnimation(outToRightAnimation());
				} else if (direction == SWIPE_DIRECTION_RIGHT) {
					currentLayout.startAnimation(outToLeftAnimation());
				}

				startActivity(intent);
			} catch (Exception e) {
				mSlideShowBundle.putBoolean(SlideShow.AUTO_SLIDE_SHOW_MODE, false);
				e.printStackTrace();
			}
			break;
		case SlideShow.STREAM_USER_PHOTOS:
			try {
				PeopleInterface iFace = mAuthorize.flickr.getPeopleInterface();
				int newPosition = position + direction;
				if (newPosition < 1) {
					throw new Exception("Flickr uses a 1 based numbering system.");
				}

				Log.d("Photo", "Currently: " + position + " Requested: " + direction + " Result: " + newPosition);
				Object[] photoList = iFace.getPhotos(identifier, 1, newPosition, Extras.MIN_EXTRAS).toArray();
				Photo photo = (Photo) photoList[0];

				// Start pre fetch
				final int prefetchPosition = newPosition + direction;
				Thread prefetchThread = new Thread() {
					@Override
					public void run() {
						try {
							Authorize tAuthorize = Authorize.initializeAuthObj(mContext);
							PeopleInterface iFace = tAuthorize.flickr.getPeopleInterface();
							Object[] photoList = iFace.getPhotos(identifier, 1, prefetchPosition, Extras.MIN_EXTRAS).toArray();
							Photo prefetchPhoto = (Photo) photoList[0];
							String url = prefetchPhoto.getMediumUrl();
							File cacheDir = new File(Flicka.PHOTO_CACHE_DIR);

							InputStream is = ImageMgmt.loadImage(url, cacheDir);
							if (is == null) {
								is = ImageMgmt.fetchImage(url);
								ImageMgmt.saveImage(is, url, cacheDir);
							}

							mSlideShow.setPhoto(prefetchPhoto.getId(), prefetchPhoto);
							mSlideShow.setBitmap(url, BitmapFactory.decodeStream(is));
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				};
				prefetchThread.start();

				Intent intent = new Intent(ActivityPhoto.this, ActivityPhoto.class);
				if (android.os.Build.VERSION.SDK_INT >= 5) {
					intent.addFlags(65536); // Intent.FLAG_ACTIVITY_NO_ANIMATION
				}
				intent.putExtra(Flicka.INTENT_EXTRA_VIEWPHOTO_PHOTOID, photo.getId());

				// Send the details so we can do a slideshow if the user wants
				slideShow = new Bundle();
				slideShow.putInt(SlideShow.CURRENT_ITEM, newPosition);
				slideShow.putInt(SlideShow.CURRENT_STREAM, SlideShow.STREAM_USER_PHOTOS);
				slideShow.putString(SlideShow.CURRENT_IDENTIFIER, identifier);
				slideShow.putBoolean(SlideShow.AUTO_SLIDE_SHOW_MODE, autoMode);
				intent.putExtra(Flicka.INTENT_EXTRA_SLIDESHOW, slideShow);

				// Animation stuff. Hopefully the above code will exception if
				// this ain't possible.
				final FrameLayout currentLayout = (FrameLayout) findViewById(R.id.main_activity_container);
				if (direction == SWIPE_DIRECTION_LEFT) {
					currentLayout.startAnimation(outToRightAnimation());
				} else if (direction == SWIPE_DIRECTION_RIGHT) {
					currentLayout.startAnimation(outToLeftAnimation());
				}

				startActivity(intent);
			} catch (Exception e) {
				mSlideShowBundle.putBoolean(SlideShow.AUTO_SLIDE_SHOW_MODE, false);
				e.printStackTrace();
			}
			break;
		default:
			break;
		}
	}

	private void initComments() {
		mComments = loadComments(true);
		// convert the collection to the array
		// @todo clean this up of course
		mCommentsArray = new ArrayList<Comment>();
		try {
			if (mComments != null) {
				Iterator<Comment> commentsIterator = mComments.iterator();
				while (commentsIterator.hasNext()) {
					Comment comment = commentsIterator.next();
					mCommentsArray.add(comment);
				}

				if (mCommentAdapter != null) {
					mCommentAdapter.clear();
					for (int i = 0; i < mCommentsArray.size(); i++) {
						mCommentAdapter.add(mCommentsArray.get(i));
					}
				}
			}
			Log.d("Photo", "Built array. Num objects: " + mCommentsArray.size());
		} catch (Exception e) {
			e.printStackTrace();
		}

		if (mCommentAdapter != null) {
			mCommentAdapter.notifyDataSetChanged();
		}
	}

	/**
	 * This handle expects messages from the thread from within it is called.
	 * 
	 */
	private final Handler commentsHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case PROGRESS_AUTH_SET_COMPLETE:
				Loading.update(mActivity, Loading.ACTIVITY_LOADING_TEXT, R.string.progress_loading_favorites);
				break;
			case PROGRESS_GET_COMMENTS_INITIAL:
				Loading.update(mActivity, Loading.DRAWER_LOADING_TEXT, R.string.progress_loading_comments);
				break;
			case PROGRESS_GET_COMMENTS_CONTINUE:
				if (mComments != null) {
					Log.d("Photo", "Continued getting comments. Total: " + mComments.size());
				}
				break;
			case PROGRESS_GET_COMMENTS_COMPLETE:
				mCommentsLoaded = true;
				if (mComments != null) {
					Log.d("Photo", "Finished getting comments. Total: " + mComments.size());
				}
				if (mComments != null && mComments.size() > 0) {
					final ListView commentList = (ListView) findViewById(R.id.image_comments);
					mCommentAdapter = new CommentAdapter(mContext, R.layout.row_comment, mCommentsArray);
					commentList.setAdapter(mCommentAdapter);
					Loading.dismiss(mActivity, Loading.DRAWER_LOADING_PARENT, Loading.DRAWER_LOADING_TARGET);
				} else {
					Loading.noDisplay(mActivity, Loading.DRAWER_LOADING_LAYOUT, Loading.DRAWER_NO_DISPLAY);
				}
				break;
			}
		}
	};

	private void initSlidingDrawer() {
		// When the drawer opens it, populate it
		Loading.start(mActivity, Loading.DRAWER_LOADING_TEXT, R.string.progress_getting_comments, Loading.DRAWER_LOADING_ICON);

		mSlidingDrawer = (SlidingDrawer) findViewById(R.id.photo_sliding_drawer);
		mSlidingDrawer.setOnDrawerOpenListener(new OnDrawerOpenListener() {
			public void onDrawerOpened() {

				if (mCommentsLoaded == false) {
					// Start the lengthy work
					Thread commentsThread = new Thread() {
						@Override
						public void run() {
							mAuthorize = Authorize.initializeAuthObj(mContext);
							// crickets
							commentsHandler.sendEmptyMessage(PROGRESS_GET_COMMENTS_INITIAL);
							initComments();
							commentsHandler.sendEmptyMessage(PROGRESS_GET_COMMENTS_COMPLETE);
						}
					};
					commentsThread.start();
				}
			}
		});
	}

	/**
	 * This is an adapter class that adds certain view functionalities to an
	 * array list.
	 */
	private class CommentAdapter extends ArrayAdapter<Comment> {

		public ArrayList<Comment> mItems;

		/**
		 * The constructor. Note the reference to super.
		 * 
		 * @param Context
		 *            context
		 * @param int textViewResourceId
		 * @param ArrayList
		 *            <Comment> items
		 */
		public CommentAdapter(Context context, int textViewResourceId, ArrayList<Comment> items) {
			super(context, textViewResourceId, items);
			mItems = items;
		}

		@Override
		public int getCount() {
			return mItems.size();
		}

		@Override
		public Comment getItem(int position) {
			return mItems.get(position);
		}

		@Override
		public int getPosition(Comment item) {
			return mItems.indexOf(item);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View view = convertView;
			if (view == null) {
				LayoutInflater viewInflator = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				view = viewInflator.inflate(R.layout.row_comment, null);
			}

			Comment comment = mItems.get(position);
			if (comment != null) {
				final TextView usernameTextView = (TextView) view.findViewById(R.id.username);
				final TextView commentTextView = (TextView) view.findViewById(R.id.comment);

				usernameTextView.setText(comment.getAuthorName());
				commentTextView.setText(comment.getText().replaceAll("\\<.*?\\>", ""));
			} else {
				Log.d("Photo", "Comment should not be null! Position: " + position);
			}

			return view;
		}
	}

	/**
	 * Load a collection of Comment objects for the authorized user. We suppress
	 * the casting warning in the line assigning the comments variable because
	 * this warning is irrelevant.
	 * 
	 * @return Collection<Comment> or null
	 */
	@SuppressWarnings("unchecked")
	public List<Comment> loadComments(boolean refreshComments) {
		try {
			CommentsInterface cFace = mAuthorize.flickr.getCommentsInterface();
			mComments = cFace.getList(mPhoto.getId());
		} catch (Exception e) {
			e.printStackTrace();
		}
		return mComments;
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			if (mSlidingDrawer != null && mSlidingDrawer.isOpened()) {
				mSlidingDrawer.animateClose();
				return true;
			}
		}

		return super.onKeyDown(keyCode, event);
	}
}
