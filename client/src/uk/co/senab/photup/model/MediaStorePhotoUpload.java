package uk.co.senab.photup.model;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import uk.co.senab.photup.Constants;
import uk.co.senab.photup.PhotupApplication;
import uk.co.senab.photup.R;
import uk.co.senab.photup.Utils;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.MediaStore;
import android.provider.MediaStore.Images.Thumbnails;
import android.util.Log;

import com.lightbox.android.photoprocessing.PhotoProcessing;
import com.lightbox.android.photoprocessing.utils.BitmapUtils;
import com.lightbox.android.photoprocessing.utils.BitmapUtils.BitmapSize;
import com.lightbox.android.photoprocessing.utils.FileUtils;

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
		
		BitmapFactory.Options opts = new BitmapFactory.Options();
		opts.inSampleSize = 2;
		
		try {
			return Thumbnails.getThumbnail(context.getContentResolver(), mId, kind, opts);
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
	public Bitmap getUploadImage(Context context, final UploadQuality quality) {
		Bitmap bitmap = null;

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

				PhotoProcessing.nativeLoadResizedJpegBitmap(jpegData, jpegData.length, size.width * size.height);
				if (Constants.DEBUG) {
					Log.d("MediaStorePhotoUpload", "getUploadImage. Native decode complete!");
				}

				// Free the byte[]
				jpegData = null;

				if (requiresProcessing()) {
					PhotoProcessing.filterPhoto(getFilterUsed().getId());
					if (Constants.DEBUG) {
						Log.d("MediaStorePhotoUpload", "getUploadImage. Native filter complete!");
					}
				}

				if (Constants.DEBUG) {
					Log.d("MediaStorePhotoUpload", "getUploadImage. Native worked!");
				}
				bitmap = PhotoProcessing.getBitmapFromNative(null);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		if (null == bitmap) {
			if (Constants.DEBUG) {
				Log.d("MediaStorePhotoUpload", "getUploadImage. Native failed, trying Java decode!");
			}
			try {
				bitmap = Utils.resizeBitmap(context.getContentResolver(), getOriginalPhotoUri(),
						quality.getMaxDimension(), true);
				if (requiresProcessing()) {
					bitmap = processBitmap(bitmap, true);
				}
			} catch (FileNotFoundException e) {
				e.printStackTrace();
				return null;
			}
		}

		return bitmap;
	}
}
