package uk.co.senab.photup.model;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import uk.co.senab.photup.Constants;
import uk.co.senab.photup.PhotupApplication;
import uk.co.senab.photup.R;
import uk.co.senab.photup.listeners.OnFaceDetectionListener;
import uk.co.senab.photup.listeners.OnPhotoTagsChangedListener;
import uk.co.senab.photup.util.Utils;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PointF;
import android.graphics.RectF;
import android.media.FaceDetector;
import android.net.Uri;
import android.provider.MediaStore.Images.Thumbnails;
import android.text.TextUtils;
import android.util.Log;

import com.lightbox.android.photoprocessing.PhotoProcessing;
import com.lightbox.android.photoprocessing.utils.BitmapUtils;
import com.lightbox.android.photoprocessing.utils.BitmapUtils.BitmapSize;
import com.lightbox.android.photoprocessing.utils.FileUtils;

public class PhotoSelection extends PhotoUpload {

	private static final HashMap<Uri, PhotoSelection> SELECTION_CACHE = new HashMap<Uri, PhotoSelection>();

	public static final int STATE_UPLOAD_COMPLETED = 2;
	public static final int STATE_UPLOAD_ERROR = 3;
	public static final int STATE_UPLOAD_IN_PROGRESS = 1;
	public static final int STATE_WAITING = 0;

	static final String LOG_TAG = "PhotoUpload";
	static final float CROP_THRESHOLD = 0.01f; // 1%
	static final int MINI_THUMBNAIL_SIZE = 300;
	static final int MICRO_THUMBNAIL_SIZE = 96;

	private String mCaption;

	private HashSet<PhotoTag> mTags;
	private boolean mCompletedDetection;

	private int mUserRotation;
	private RectF mCropValues;
	private Filter mFilter;

	private WeakReference<OnFaceDetectionListener> mFaceDetectListener;
	private WeakReference<OnPhotoTagsChangedListener> mTagChangedListener;

	private final Uri mFullUri;

	public static PhotoSelection getSelection(Uri uri) {
		// Check whether we've already got a Selection cached
		PhotoSelection item = SELECTION_CACHE.get(uri);

		if (null == item) {
			item = new PhotoSelection(uri);
			SELECTION_CACHE.put(uri, item);
		}

		return item;
	}

	public static PhotoSelection getSelection(Uri baseUri, long id) {
		return getSelection(Uri.withAppendedPath(baseUri, String.valueOf(id)));
	}

	private PhotoSelection(Uri uri) {
		mFullUri = uri;
		reset();
	}

	public void addPhotoTag(PhotoTag tag) {
		if (null == mTags) {
			mTags = new HashSet<PhotoTag>();
		}
		mTags.add(tag);
		notifyTagListener(tag, true);
	}

	public boolean beenCropped() {
		return null != mCropValues;
	}

	public boolean beenFiltered() {
		return null != mFilter && mFilter.getId() != Filter.FILTER_ORIGINAL;
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
		if (obj instanceof PhotoSelection) {
			return getOriginalPhotoUri().equals(((PhotoSelection) obj).getOriginalPhotoUri());
		}
		return false;
	}

	public String getCaption() {
		return mCaption;
	}

	public RectF getCropValues() {
		return mCropValues;
	}

	public RectF getCropValues(final int width, final int height) {
		return new RectF(mCropValues.left * width, mCropValues.top * height, mCropValues.right * width,
				mCropValues.bottom * height);
	}

	public Bitmap getDisplayImage(Context context) {
		try {
			final int size = PhotupApplication.getApplication(context).getSmallestScreenDimension();
			Bitmap bitmap = Utils.decodeImage(context.getContentResolver(), getOriginalPhotoUri(), size);
			bitmap = Utils.rotate(bitmap, getExifRotation(context));
			return bitmap;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return null;
		}
	}

	public String getDisplayImageKey() {
		return "dsply_" + getOriginalPhotoUri();
	}

	public int getExifRotation(Context context) {
		return Utils.getOrientationFromContentUri(context.getContentResolver(), getOriginalPhotoUri());
	}

	public Filter getFilterUsed() {
		return mFilter;
	}

	public Uri getOriginalPhotoUri() {
		return mFullUri;
	}

	public List<PhotoTag> getPhotoTags() {
		if (null != mTags) {
			return new ArrayList<PhotoTag>(mTags);
		}
		return null;
	}

	public int getPhotoTagsCount() {
		return null != mTags ? mTags.size() : 0;
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

	public Bitmap getThumbnailImage(Context context) {
		if (ContentResolver.SCHEME_CONTENT.equals(mFullUri.getScheme())) {
			return getThumbnailImageFromMediaStore(context);
		}

		final Resources res = context.getResources();
		int size = res.getBoolean(R.bool.load_mini_thumbnails) ? MINI_THUMBNAIL_SIZE : MICRO_THUMBNAIL_SIZE;
		if (size == MINI_THUMBNAIL_SIZE && res.getBoolean(R.bool.sample_mini_thumbnails)) {
			size /= 2;
		}

		try {
			Bitmap bitmap = Utils.decodeImage(context.getContentResolver(), getOriginalPhotoUri(), size);
			bitmap = Utils.rotate(bitmap, getExifRotation(context));
			return bitmap;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return null;
		}
	}

	public String getThumbnailImageKey() {
		return "thumb_" + getOriginalPhotoUri();
	}

	public int getTotalRotation(Context context) {
		return (getExifRotation(context) + getUserRotation()) % 360;
	}

	public Bitmap getUploadImage(Context context, final UploadQuality quality) {
		Utils.checkPhotoProcessingThread();
		return getUploadImageNative(context, quality);
	}

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
		if (null != mTags) {
			mTags.remove(tag);
			notifyTagListener(tag, false);

			if (mTags.isEmpty()) {
				mTags = null;
			}
		}
	}

	public boolean requiresFaceDetectPass() {
		return !mCompletedDetection;
	}

	public boolean requiresProcessing(final boolean fullSize) {
		return getUserRotation() != 0 || beenFiltered() || (fullSize && beenCropped());
	}

	public void reset() {
		mUserRotation = 0;
		mCaption = null;
		mCropValues = null;
		mFilter = null;
		mTags = null;
		mCompletedDetection = false;
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

	public void setCropValues(RectF cropValues) {
		if (cropValues.left >= CROP_THRESHOLD || cropValues.right <= (1f - CROP_THRESHOLD)
				|| cropValues.top >= CROP_THRESHOLD || cropValues.bottom <= (1f - CROP_THRESHOLD)) {

			// TODO Remap Photo Tags using new crop values

			mCropValues = cropValues;
			if (Constants.DEBUG) {
				Log.d(LOG_TAG, "Valid Crop Values: " + cropValues.toString());
			}
		} else {
			if (Constants.DEBUG) {
				Log.d(LOG_TAG, "Invalid Crop Values: " + cropValues.toString());
			}
			mCropValues = null;
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

	private Bitmap getThumbnailImageFromMediaStore(Context context) {
		Resources res = context.getResources();

		final int kind = res.getBoolean(R.bool.load_mini_thumbnails) ? Thumbnails.MINI_KIND : Thumbnails.MICRO_KIND;

		BitmapFactory.Options opts = null;
		if (kind == Thumbnails.MINI_KIND && res.getBoolean(R.bool.sample_mini_thumbnails)) {
			opts = new BitmapFactory.Options();
			opts.inSampleSize = 2;
		}

		try {
			final long id = Long.parseLong(getOriginalPhotoUri().getLastPathSegment());

			ContentResolver cr = context.getContentResolver();
			Bitmap bitmap = Thumbnails.getThumbnail(cr, id, kind, opts);
			bitmap = Utils.rotate(bitmap, getExifRotation(context));
			return bitmap;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	private Bitmap getUploadImageNative(final Context context, final UploadQuality quality) {
		try {
			String path = Utils.getPathFromContentUri(context.getContentResolver(), getOriginalPhotoUri());
			if (null != path) {
				BitmapSize size = BitmapUtils.getBitmapSize(path);

				byte[] jpegData = FileUtils.readFileToByteArray(new File(path));
				if (Constants.DEBUG) {
					Log.d("MediaStorePhotoUpload", "getUploadImage. Read file to RAM!");
				}

				final float resizeRatio = Math.max(size.width, size.height) / (float) quality.getMaxDimension();
				size = new BitmapSize(Math.round(size.width / resizeRatio), Math.round(size.height / resizeRatio));

				PhotoProcessing.nativeInitBitmap(size.width, size.height);
				if (Constants.DEBUG) {
					Log.d("MediaStorePhotoUpload", "getUploadImage. Init " + size.width + "x" + size.height);
				}

				final int decodeResult = PhotoProcessing.nativeLoadResizedJpegBitmap(jpegData, jpegData.length,
						size.width * size.height);

				// Free the byte[]
				jpegData = null;

				if (decodeResult == 0) {
					if (Constants.DEBUG) {
						Log.d("MediaStorePhotoUpload", "getUploadImage. Native decode complete!");
					}
				} else {
					if (Constants.DEBUG) {
						Log.d("MediaStorePhotoUpload", "getUploadImage. Native decode failed. Trying Android decode");
					}

					// Just in case
					PhotoProcessing.nativeDeleteBitmap();

					// Decode in Android and send to native
					Bitmap bitmap = Utils.decodeImage(context.getContentResolver(), getOriginalPhotoUri(),
							quality.getMaxDimension());
					PhotoProcessing.sendBitmapToNative(bitmap);
					bitmap.recycle();

					// Do resize
					PhotoProcessing.nativeResizeBitmap(size.width, size.height);
				}

				/**
				 * Apply crop if needed
				 */
				if (beenCropped()) {
					RectF rect = getCropValues();
					PhotoProcessing.nativeCrop(rect.left, rect.top, rect.right, rect.bottom);
				}

				/**
				 * Apply filter if needed
				 */
				if (beenFiltered()) {
					PhotoProcessing.filterPhoto(getFilterUsed().getId());
					if (Constants.DEBUG) {
						Log.d("MediaStorePhotoUpload", "getUploadImage. Native filter complete!");
					}
				}

				/**
				 * Rotate if needed
				 */
				final int rotation = getTotalRotation(context);
				switch (rotation) {
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
				if (Constants.DEBUG) {
					Log.d("MediaStorePhotoUpload", "getUploadImage. " + rotation + " degree rotation complete!");
				}

				if (Constants.DEBUG) {
					Log.d("MediaStorePhotoUpload", "getUploadImage. Native worked!");
				}

				return PhotoProcessing.getBitmapFromNative(null);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			// Just in case...
			PhotoProcessing.nativeDeleteBitmap();
		}

		return null;
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
