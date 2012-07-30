package uk.co.senab.photup.model;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import uk.co.senab.photup.Constants;
import uk.co.senab.photup.Utils;
import uk.co.senab.photup.listeners.OnFaceDetectionListener;
import uk.co.senab.photup.listeners.OnPhotoTagsChangedListener;
import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.PointF;
import android.media.FaceDetector;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import com.lightbox.android.photoprocessing.PhotoProcessing;

public abstract class PhotoSelection extends PhotoUpload {

	static final String LOG_TAG = "PhotoUpload";

	private Filter mFilter;
	private String mCaption;

	private final HashSet<PhotoTag> mTags;
	private boolean mCompletedDetection;

	private int mUserRotation;

	private WeakReference<OnFaceDetectionListener> mFaceDetectListener;
	private WeakReference<OnPhotoTagsChangedListener> mTagChangedListener;

	public PhotoSelection() {
		mTags = new HashSet<PhotoTag>();
		mCompletedDetection = false;
		mUserRotation = 0;
	}

	public abstract Uri getOriginalPhotoUri();

	public abstract Bitmap getThumbnailImage(Context context);

	public abstract Bitmap getDisplayImage(Context context);

	public abstract Bitmap getUploadImage(Context context, UploadQuality quality);

	public Bitmap processBitmap(Bitmap bitmap, final boolean modifyOriginal) {
		Utils.checkPhotoProcessingThread();

		if (requiresProcessing()) {
			Bitmap filteredBitmap = bitmap;
			
			if (null != mFilter) {
				filteredBitmap = PhotoProcessing.filterPhoto(filteredBitmap, mFilter.getId(), modifyOriginal);
			}
			
			final int userRotation = getUserRotation();
			if (userRotation != 0) {
				filteredBitmap = Utils.rotate(filteredBitmap, userRotation);
			}
			
			return filteredBitmap;
		} else {
			return bitmap;
		}
	}

	public String getThumbnailImageKey() {
		return "thumb_" + getOriginalPhotoUri();
	}

	public String getDisplayImageKey() {
		return "dsply_" + getOriginalPhotoUri();
	}

	public void setFilterUsed(Filter filter) {
		mFilter = filter;
	}

	public Filter getFilterUsed() {
		return mFilter;
	}

	public HashSet<FbUser> getTaggedFriends() {
		HashSet<FbUser> friends = new HashSet<FbUser>();

		FbUser friend;
		for (PhotoTag tag : mTags) {
			friend = tag.getFriend();
			if (null != friend) {
				friends.add(friend);
			}
		}

		return friends;
	}

	public int getUserRotation() {
		return mUserRotation % 360;
	}

	public void rotateClockwise() {
		mUserRotation += 90;
	}

	public boolean requiresProcessing() {
		return (null != mFilter && mFilter.getId() != Filter.FILTER_ORIGINAL) || getUserRotation() != 0;
	}

	public String getCaption() {
		return mCaption;
	}

	public void setCaption(String caption) {
		if (TextUtils.isEmpty(caption)) {
			mCaption = null;
		} else {
			mCaption = caption;
		}
	}

	public boolean requiresFaceDetectPass() {
		return !mCompletedDetection;
	}

	public void setFaceDetectionListener(OnFaceDetectionListener listener) {
		// No point keeping listener if we've already done a pass
		if (!mCompletedDetection) {
			mFaceDetectListener = new WeakReference<OnFaceDetectionListener>(listener);
		}
	}

	public void detectPhotoTags(final Bitmap originalBitmap) {
		// If we've already done Face detection, don't do it again...
		if (mCompletedDetection) {
			return;
		}

		final OnFaceDetectionListener listener = mFaceDetectListener.get();
		if (null != listener) {
			listener.onFaceDetectionStarted(this);
		}

		final int bitmapWidth = originalBitmap.getWidth();
		final int bitmapHeight = originalBitmap.getHeight();

		Bitmap bitmap = originalBitmap;

		// The Face detector only accepts 565 bitmaps, so create one if needed
		if (Bitmap.Config.RGB_565 != bitmap.getConfig()) {
			bitmap = originalBitmap.copy(Bitmap.Config.RGB_565, false);
		}

		final FaceDetector detector = new FaceDetector(bitmapWidth, bitmapHeight, Constants.FACE_DETECTOR_MAX_FACES);
		final FaceDetector.Face[] faces = new FaceDetector.Face[Constants.FACE_DETECTOR_MAX_FACES];
		final int detectedFaces = detector.findFaces(bitmap, faces);

		// We must have created a converted 565 bitmap
		if (bitmap != originalBitmap) {
			bitmap.recycle();
			bitmap = null;
		}

		if (Constants.DEBUG) {
			Log.d(LOG_TAG, "Detected Faces: " + detectedFaces);
		}

		FaceDetector.Face face;
		final PointF point = new PointF();
		for (int i = 0, z = faces.length; i < z; i++) {
			face = faces[i];
			if (null != face) {
				if (Constants.DEBUG) {
					Log.d(LOG_TAG, "Detected Face with confidence: " + face.confidence());
				}
				face.getMidPoint(point);
				addPhotoTag(new PhotoTag(point.x, point.y, bitmapWidth, bitmapWidth));
			}
		}

		if (null != listener) {
			listener.onFaceDetectionFinished(this);
		}
		mFaceDetectListener = null;

		mCompletedDetection = true;
	}

	public void addPhotoTag(PhotoTag tag) {
		mTags.add(tag);
		notifyTagListener(tag, true);
	}

	public void removePhotoTag(PhotoTag tag) {
		mTags.remove(tag);
		notifyTagListener(tag, false);
	}

	private void notifyTagListener(PhotoTag tag, boolean added) {
		if (null != mTagChangedListener) {
			OnPhotoTagsChangedListener listener = mTagChangedListener.get();
			if (null != listener) {
				listener.onPhotoTagsChanged(tag, added);
			}
		}
	}

	public void setTagChangedListener(OnPhotoTagsChangedListener tagChangedListener) {
		mTagChangedListener = new WeakReference<OnPhotoTagsChangedListener>(tagChangedListener);
	}

	public List<PhotoTag> getPhotoTags() {
		return new ArrayList<PhotoTag>(mTags);
	}

	public int getPhotoTagsCount() {
		return mTags.size();
	}

	@Override
	public int hashCode() {
		return getOriginalPhotoUri().hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof MediaStorePhotoUpload) {
			return getOriginalPhotoUri().equals(((MediaStorePhotoUpload) obj).getOriginalPhotoUri());
		}
		return false;
	}

	public static PhotoSelection fromUri(Uri uri) {
		if (ContentResolver.SCHEME_CONTENT.equals(uri.getScheme())) {
			return new MediaStorePhotoUpload(uri);
		} else {
			return new FilePhotoUpload(uri);
		}
	}
}
