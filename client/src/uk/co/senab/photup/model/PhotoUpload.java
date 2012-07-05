package uk.co.senab.photup.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import uk.co.senab.photup.Constants;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.PointF;
import android.media.FaceDetector;
import android.net.Uri;
import android.text.TextUtils;

import com.lightbox.android.photoprocessing.PhotoProcessing;

public abstract class PhotoUpload {

	private Filter mFilter;
	private String mCaption;

	private final HashSet<PhotoTag> mTags;
	private boolean mCompletedDetection;

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

		final FaceDetector detector = new FaceDetector(bitmap.getWidth(), bitmap.getHeight(),
				Constants.FACE_DETECTOR_MAX_FACES);
		final FaceDetector.Face[] faces = new FaceDetector.Face[Constants.FACE_DETECTOR_MAX_FACES];

		detector.findFaces(bitmap, faces);

		FaceDetector.Face face;
		final PointF point = new PointF();
		for (int i = 0, z = faces.length; i < z; i++) {
			face = faces[i];
			if (null != face) {
				face.getMidPoint(point);
				addPhotoTag(new PhotoTag(point.x, point.y));
			}
		}

		mCompletedDetection = true;
	}

	public void addPhotoTag(PhotoTag tag) {
		mTags.add(tag);
	}

	public void removePhotoTag(PhotoTag tag) {
		mTags.remove(tag);
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
