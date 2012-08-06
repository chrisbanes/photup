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
import android.graphics.RectF;
import android.media.FaceDetector;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import com.lightbox.android.photoprocessing.PhotoProcessing;

public abstract class PhotoSelection extends PhotoUpload {

	static final String LOG_TAG = "PhotoUpload";
	static final float CROP_THRESHOLD = 1.0f;

	public static PhotoSelection fromUri(Uri uri) {
		if (ContentResolver.SCHEME_CONTENT.equals(uri.getScheme())) {
			return new MediaStorePhotoUpload(uri);
		} else {
			return new FilePhotoUpload(uri);
		}
	}

	private String mCaption;
	private final HashSet<PhotoTag> mTags;

	private boolean mCompletedDetection;

	private int mUserRotation;
	private RectF mCropValues;
	private Filter mFilter;

	private WeakReference<OnFaceDetectionListener> mFaceDetectListener;
	private WeakReference<OnPhotoTagsChangedListener> mTagChangedListener;

	public PhotoSelection() {
		mTags = new HashSet<PhotoTag>();
		mCompletedDetection = false;
		mUserRotation = 0;
	}

	public void addPhotoTag(PhotoTag tag) {
		mTags.add(tag);
		notifyTagListener(tag, true);
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

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof MediaStorePhotoUpload) {
			return getOriginalPhotoUri().equals(((MediaStorePhotoUpload) obj).getOriginalPhotoUri());
		}
		return false;
	}

	public String getCaption() {
		return mCaption;
	}

	public RectF getCropValues() {
		return mCropValues;
	}

	public abstract Bitmap getDisplayImage(Context context);

	public String getDisplayImageKey() {
		return "dsply_" + getOriginalPhotoUri();
	}

	public abstract int getExifRotation(Context context);

	public Filter getFilterUsed() {
		return mFilter;
	}

	public abstract Uri getOriginalPhotoUri();

	public List<PhotoTag> getPhotoTags() {
		return new ArrayList<PhotoTag>(mTags);
	}

	public int getPhotoTagsCount() {
		return mTags.size();
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

	public abstract Bitmap getThumbnailImage(Context context);

	public String getThumbnailImageKey() {
		return "thumb_" + getOriginalPhotoUri();
	}

	public int getTotalRotation(Context context) {
		return (getExifRotation(context) + getUserRotation()) % 360;
	}

	public abstract Bitmap getUploadImage(Context context, UploadQuality quality);

	public int getUserRotation() {
		return mUserRotation % 360;
	}

	@Override
	public int hashCode() {
		return getOriginalPhotoUri().hashCode();
	}

	public Bitmap processBitmap(Bitmap bitmap, final boolean fullSize, final boolean modifyOriginal) {
		if (requiresProcessing(fullSize)) {
			return processBitmapUsingFilter(bitmap, mFilter, fullSize, modifyOriginal);
		} else {
			return bitmap;
		}
	}

	public Bitmap processBitmapUsingFilter(final Bitmap bitmap, final Filter filter, final boolean fullSize,
			final boolean modifyOriginal) {
		Utils.checkPhotoProcessingThread();

		PhotoProcessing.sendBitmapToNative(bitmap);
		if (modifyOriginal) {
			bitmap.recycle();
		}

		if (fullSize && beenCropped()) {
			RectF rect = getCropValues();
			PhotoProcessing.nativeCrop(rect.left, rect.top, rect.right, rect.bottom);
		}

		if (null != filter) {
			PhotoProcessing.filterPhoto(filter.getId());
		}

		switch (getUserRotation()) {
			case 90:
				PhotoProcessing.nativeRotate90();
				break;
			case 180:
				PhotoProcessing.nativeRotate180();
				break;
			case 270:
				PhotoProcessing.nativeRotate180();
				PhotoProcessing.nativeRotate90();
				break;
		}

		return PhotoProcessing.getBitmapFromNative(null);
	}

	public void removePhotoTag(PhotoTag tag) {
		mTags.remove(tag);
		notifyTagListener(tag, false);
	}

	public boolean requiresFaceDetectPass() {
		return !mCompletedDetection;
	}

	public boolean requiresProcessing(final boolean fullSize) {
		return getUserRotation() != 0 || beenFiltered() || (fullSize && beenCropped());
	}

	public void rotateClockwise() {
		mUserRotation += 90;
	}

	public void setCaption(String caption) {
		if (TextUtils.isEmpty(caption)) {
			mCaption = null;
		} else {
			mCaption = caption;
		}
	}

	public void setFaceDetectionListener(OnFaceDetectionListener listener) {
		// No point keeping listener if we've already done a pass
		if (!mCompletedDetection) {
			mFaceDetectListener = new WeakReference<OnFaceDetectionListener>(listener);
		}
	}

	public void setFilterUsed(Filter filter) {
		mFilter = filter;
	}

	public void setTagChangedListener(OnPhotoTagsChangedListener tagChangedListener) {
		mTagChangedListener = new WeakReference<OnPhotoTagsChangedListener>(tagChangedListener);
	}

	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		String caption = getCaption();
		if (null != caption) {
			sb.append(caption).append(" ");
		}

		Place place = getPlace();
		if (null != place) {
			sb.append("(").append(place.getName()).append(")");
		}

		return sb.toString();
	}

	protected boolean beenCropped() {
		if (null != mCropValues) {
			return mCropValues.left >= CROP_THRESHOLD || mCropValues.right >= CROP_THRESHOLD
					|| mCropValues.top >= CROP_THRESHOLD || mCropValues.bottom >= CROP_THRESHOLD;
		}
		return false;
	}

	protected boolean beenFiltered() {
		return null != mFilter && mFilter.getId() != Filter.FILTER_ORIGINAL;
	}

	private void notifyTagListener(PhotoTag tag, boolean added) {
		if (null != mTagChangedListener) {
			OnPhotoTagsChangedListener listener = mTagChangedListener.get();
			if (null != listener) {
				listener.onPhotoTagsChanged(tag, added);
			}
		}
	}
}
