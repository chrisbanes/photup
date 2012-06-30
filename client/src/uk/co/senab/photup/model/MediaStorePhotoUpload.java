package uk.co.senab.photup.model;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.provider.MediaStore.Images.Thumbnails;

public class MediaStorePhotoUpload extends PhotoUpload {

	private final long mId;

	public MediaStorePhotoUpload(long id) {
		mId = id;
	}

	public Uri getOriginalPhotoUri() {
		return Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, String.valueOf(mId));
	}

	public Bitmap getThumbnail(Context context) {
		return Thumbnails.getThumbnail(context.getContentResolver(), mId, Thumbnails.MICRO_KIND, null);
	}

}
