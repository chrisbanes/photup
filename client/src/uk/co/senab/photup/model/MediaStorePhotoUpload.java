package uk.co.senab.photup.model;

import java.io.FileNotFoundException;

import uk.co.senab.photup.Constants;
import uk.co.senab.photup.PhotupApplication;
import uk.co.senab.photup.R;
import uk.co.senab.photup.Utils;
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

	public Bitmap getThumbnailImage(Context context) {
		final int kind = context.getResources().getBoolean(R.bool.load_mini_thumbnails) ? Thumbnails.MINI_KIND
				: Thumbnails.MICRO_KIND;
		try {
			return Thumbnails.getThumbnail(context.getContentResolver(), mId, kind, null);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public Bitmap getDisplayImage(Context context) {
		try {
			final int size = Math.min(PhotupApplication.getApplication(context).getLargestScreenDimension(),
					Constants.DISPLAY_PHOTO_SIZE);
			return Utils.resizeBitmap(context.getContentResolver(), getOriginalPhotoUri(), size, false);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public Bitmap getUploadImage(Context context, UploadQuality quality) {
		try {
			return Utils.resizeBitmap(context.getContentResolver(), getOriginalPhotoUri(), quality.getMaxDimension(),
					true);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return null;
		}
	}

}
