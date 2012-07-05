package uk.co.senab.photup.model;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

import uk.co.senab.photup.Constants;
import uk.co.senab.photup.listeners.OnPhotoTagsChangedListener;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.PointF;
import android.media.FaceDetector;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import com.lightbox.android.photoprocessing.PhotoProcessing;

public abstract class PhotoUpload {

	static final String LOG_TAG = "PhotoUpload";

	private Filter mFilter;
	private String mCaption;

	private final HashSet<PhotoTag> mTags;
	private boolean mCompletedDetection;

	private WeakReference<OnPhotoTagsChangedListener> mTagChangedListener;

	public PhotoUpload() {
		mTags = new HashSet<PhotoTag>();
		mCompletedDetection = false;
	}

	public abstract Uri getOriginalPhotoUri();

	public abstract Bitmap getThumbnailImage(Context context);

	public abstract Bitmap getDisplayImage(Context context);

	public abstract Bitmap getUploadImage(Context context, int biggestDimension);

	public Bitmap processBitmap(Bitmap bitmap, final boolean modifyOriginal) {
		if (requiresProcessing()) {
			return PhotoProcessing.filterPhoto(bitmap, mFilter.getId(), modifyOriginal);
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

	public boolean requiresProcessing() {
		return null != mFilter && mFilter.getId() != Filter.FILTER_ORIGINAL;
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

	public void detectPhotoTags(Bitmap bitmap) {
		// If we've already done Face detection, don't do it again...
		if (mCompletedDetection) {
			return;
		}

		final int bitmapWidth = bitmap.getWidth();
		final int bitmapHeight = bitmap.getHeight();

		final FaceDetector detector = new FaceDetector(bitmapWidth, bitmapHeight, Constants.FACE_DETECTOR_MAX_FACES);
		final FaceDetector.Face[] faces = new FaceDetector.Face[Constants.FACE_DETECTOR_MAX_FACES];
		final int detectedFaces = detector.findFaces(bitmap, faces);

		if (Constants.DEBUG) {
			Log.d(LOG_TAG, "Detected Faces: " + detectedFaces);
		}

		FaceDetector.Face face;
		final PointF point = new PointF();
		for (int i = 0, z = faces.length; i < z; i++) {
			face = faces[i];
			if (null != face) {
				face.getMidPoint(point);
				addPhotoTag(new PhotoTag(point.x, point.y, bitmapWidth, bitmapWidth));
			}
		}

		/**
		 * TODO REMOVE BELOW
		 */
		Random rnd = new Random();
		addPhotoTag(new PhotoTag(null, rnd.nextFloat() * 100, rnd.nextFloat() * 100));

		mCompletedDetection = true;
	}

	public void addPhotoTag(PhotoTag tag) {
		mTags.add(tag);
		notifyTagListener();
	}

	public void removePhotoTag(PhotoTag tag) {
		mTags.remove(tag);
		notifyTagListener();
	}

	private void notifyTagListener() {
		if (null != mTagChangedListener) {
			OnPhotoTagsChangedListener listener = mTagChangedListener.get();
			if (null != listener) {
				listener.onPhotoTagsChanged();
			}
		}
	}

	public void setTagChangedListener(OnPhotoTagsChangedListener tagChangedListener) {
		mTagChangedListener = new WeakReference<OnPhotoTagsChangedListener>(tagChangedListener);
	}

	public List<PhotoTag> getPhotoTags() {
		return new ArrayList<PhotoTag>(mTags);
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

}
