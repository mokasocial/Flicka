package com.mokasocial.flicka;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.xml.sax.SAXException;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.aetrion.flickr.FlickrException;
import com.aetrion.flickr.groups.Group;
import com.aetrion.flickr.groups.GroupsInterface;
import com.aetrion.flickr.groups.pools.PoolsInterface;
import com.aetrion.flickr.photos.Photo;
import com.aetrion.flickr.photos.PhotoList;
import com.csam.jentities.Entities;
import com.csam.jentities.HTML4Entities;

public class ActivityGroupInfo extends Activity {

	private Context mContext;
	private Activity mActivity;
	private Authorize mAuthorize;
	private Group mGroup;
	private PhotoList mPhotos;
	private PhotosAdapter mPhotosAdapter;
	private DrawableManager mDraw;

	private final static int PROGRESS_AUTH_SET_COMPLETE = 0;
	private final static int PROGRESS_GET_GROUP_COMPLETE = 1;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Set up the various strings and view for this sub activity.
		Utilities.setupActivityView(this, R.string.group_info, R.layout.view_group_info);

		// Set up some basic variables
		mContext = this;
		mActivity = this;
		mDraw = new DrawableManager(Flicka.GROUP_ICON_DIR);

		// Start progress box
		Loading.start(mActivity, Loading.ACTIVITY_LOADING_TEXT, R.string.progress_getting_group_info, Loading.ACTIVITY_LOADING_ICON);

		// Remove extra arrow in breadcrumb nav as we won't have sub-categories.
		this.findViewById(R.id.arrow_right_2).setVisibility(View.GONE);

		// Start the lengthy work
		Thread userThread = new Thread() {
			@Override
			public void run() {
				mAuthorize = Authorize.initializeAuthObj(mContext);
				groupHandler.sendEmptyMessage(PROGRESS_AUTH_SET_COMPLETE);
				try {
					mGroup = ActivityGroupInfo.initializeGroup(mActivity, mAuthorize, Flicka.INTENT_EXTRA_ACTIVITY_NSID);
				} catch (Exception e) {
					e.printStackTrace();
				}
				mPhotos = getRecentPhotos();
				groupHandler.sendEmptyMessage(PROGRESS_GET_GROUP_COMPLETE);
			}
		};
		userThread.start();
	}

	private final Handler groupHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case PROGRESS_AUTH_SET_COMPLETE:
				final TextView loadingTextView = (TextView) findViewById(R.id.activity_loading_text);
				loadingTextView.setText(getString(R.string.progress_loading_group_info));
				break;
			case PROGRESS_GET_GROUP_COMPLETE:
				renderViewGroup();
				break;
			}
		}
	};

	private void renderViewGroup() {
		try {
			populateGroupDetails(mGroup);
			Loading.dismiss(mActivity, Loading.ACTIVITY_LOADING_PARENT, Loading.ACTIVITY_LOADING_TARGET);
		} catch (Exception e) {
			e.printStackTrace();
			Loading.failed(mActivity, Loading.ACTIVITY_LOADING_LAYOUT, Loading.ACTIVITY_FAILED_LOAD);
		}
	}

	public static Group initializeGroup(Activity activity, Authorize authorize, String extrasName) throws IOException, SAXException, FlickrException {
		// Grab the passed NSID.
		Intent intent = activity.getIntent();
		Bundle extras = intent.getExtras();
		String nsid = extras.getString(extrasName);

		// Try DB first.
		Database db = new Database(activity);
		long lastUpdate = db.getGroupUpdateTime(nsid);

		// Return the cached group if its within the accepted time limit.
		if (lastUpdate > (System.currentTimeMillis() - Flicka.CACHED_GROUP_LIMIT)) {
			Group cachedGroup = db.getGroup(nsid);
			return cachedGroup;
		}

		// Grab the user from Flickr, cache, and return.
		GroupsInterface iface = authorize.flickr.getGroupsInterface();
		Group freshGroup = iface.getInfo(nsid);
		db.addGroup(freshGroup);
		return freshGroup;

	}

	/**
	 * This is a basic crap function to demonstrate what date is available.
	 * Images will need to get loaded, etc. It should probably remain but I
	 * imagine it will be much cleaner.
	 * 
	 * @param user
	 * @throws IOException
	 */
	private void populateGroupDetails(Group group) throws IOException {
		final TextView groupNameTextView = (TextView) findViewById(R.id.details_group_name);
		final TextView groupMemberCount = (TextView) findViewById(R.id.details_group_member_count);
		final ImageView groupIconView = (ImageView) findViewById(R.id.details_group_icon);
		final TextView groupDescriptionTextView = (TextView) findViewById(R.id.group_description);

		// Populate the group name
		groupNameTextView.setText(group.getName());

		// Populate the group description
		String groupDescription = group.getDescription();
		if (groupDescription != null && groupDescription != "") {
			// This needs work. It should replace some formatting like <br> and
			// <p> with \n type stuff.
			Entities entities = new HTML4Entities();
			groupDescriptionTextView.setText(entities.parseText(groupDescription.replaceAll("\\<.*?\\>", "")));
			final ScrollView descriptionScroller = (ScrollView) findViewById(R.id.group_description_scroller);
			descriptionScroller.setVisibility(TextView.VISIBLE);
		}

		// Load image from cache
		InputStream is = ImageMgmt.loadImage(group.getBuddyIconUrl(), new File(Flicka.GROUP_ICON_DIR));

		// If it fails (missing SDCard)
		if (is == null) {
			is = ImageMgmt.fetchImage(group.getBuddyIconUrl());
			ImageMgmt.saveImage(is, group.getBuddyIconUrl(), new File(Flicka.GROUP_ICON_DIR));
			is = ImageMgmt.loadImage(group.getBuddyIconUrl(), new File(Flicka.GROUP_ICON_DIR));
		}

		// Load into Bitmap, let's add some rounded corners
		Bitmap userIcon;
		if (is != null) {
			userIcon = ImageMgmt.getRndedCornerBtmp(BitmapFactory.decodeStream(is), ImageMgmt.USER_ICON_CORNER_RADIUS);
			try {
				is.close();
			} catch (IOException e) {
			}
		} else {
			userIcon = ImageMgmt.getRndedCornerBtmp(BitmapFactory.decodeResource(this.getResources(), R.drawable.loading_user_icon), ImageMgmt.USER_ICON_CORNER_RADIUS);
		}

		// Populate the group icon
		groupIconView.setImageBitmap(userIcon);

		// Populate the pool if there is one
		if (mPhotos != null && mPhotos.size() > 0) {
			populateGroupRecentPhotos();
		}

		// Populate the member count
		groupMemberCount.setText(Integer.toString(group.getMembers()));
	}

	private void populateGroupRecentPhotos() {
		final RelativeLayout recentPhotosLayout = (RelativeLayout) findViewById(R.id.group_photos);
		recentPhotosLayout.setVisibility(RelativeLayout.VISIBLE);
		final GridView photosGridView = (GridView) findViewById(R.id.photos_grid_view);
		mPhotosAdapter = new PhotosAdapter(this);
		photosGridView.setAdapter(mPhotosAdapter);

		photosGridView.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
				Photo photo = (Photo) mPhotos.get(position);
				Intent intent = new Intent(ActivityGroupInfo.this, ActivityPhoto.class);
				intent.putExtra(Flicka.INTENT_EXTRA_VIEWPHOTO_PHOTOID, photo.getId());

				// Send the details so we can do a slideshow if the user wants
				Bundle slideShow = new Bundle();
				slideShow.putInt(SlideShow.CURRENT_ITEM, position + 1);
				slideShow.putInt(SlideShow.CURRENT_STREAM, SlideShow.STREAM_GROUP_PHOTOS);
				slideShow.putString(SlideShow.CURRENT_IDENTIFIER, mGroup.getId());
				intent.putExtra(Flicka.INTENT_EXTRA_SLIDESHOW, slideShow);

				startActivity(intent);
			}
		});
	}

	private class PhotosAdapter extends BaseAdapter {
		private final Context mContext;

		public PhotosAdapter(Context context) {
			mContext = context;
		}

		public int getCount() {
			return mPhotos.size();
		}

		public Object getItem(int position) {
			return mPhotos.get(position);
		}

		public long getItemId(int position) {
			return 0;
		}

		// Create a new ImageView for each item referenced by the Adapter
		public View getView(int position, View convertView, ViewGroup parent) {

			ImageView imageView;
			// If it's not recycled, initialize some attributes
			if (convertView == null) {
				imageView = new ImageView(mContext);
				imageView.setLayoutParams(new GridView.LayoutParams(65, 65));
				imageView.setBackgroundResource(R.drawable.opacity_25_green);
				imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
				imageView.setPadding(4, 4, 4, 4);
			} else {
				imageView = (ImageView) convertView;
			}

			Photo photo = (Photo) getItem(position);
			mDraw.fetchDrawableOnThread(photo.getSmallSquareUrl(), imageView);

			return imageView;
		}
	}

	public void viewPhotos(View view) {
		Intent intent = new Intent(ActivityGroupInfo.this, ActivityPhotoStream.class);
		intent.putExtra(Flicka.INTENT_EXTRA_ACTIVITY_NSID, mGroup.getId());
		intent.putExtra(Flicka.INTENT_EXTRA_PHOTO_STREAM_TYPE, ActivityPhotoStream.PHOTO_STREAM_TYPE_GROUP);
		startActivity(intent);
	}

	private PhotoList getRecentPhotos() {
		try {
			PoolsInterface iFace = mAuthorize.flickr.getPoolsInterface();
			return iFace.getPhotos(mGroup.getId(), null, 10, 1);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
}
