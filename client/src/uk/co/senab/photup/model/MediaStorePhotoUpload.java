package uk.co.senab.photup.model;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import uk.co.senab.photup.Constants;
import uk.co.senab.photup.PhotupApplication;
import uk.co.senab.photup.R;
import uk.co.senab.photup.Utils;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.MediaStore.Images.Thumbnails;
import android.util.Log;

import com.lightbox.android.photoprocessing.PhotoProcessing;
import com.lightbox.android.photoprocessing.utils.BitmapUtils;
import com.lightbox.android.photoprocessing.utils.BitmapUtils.BitmapSize;
import com.lightbox.android.photoprocessing.utils.FileUtils;

public class MediaStorePhotoUpload extends PhotoSelection {

	private final long mId;
	private final Uri mFullUri;

	public MediaStorePhotoUpload(Uri contentUri, long id) {
		mFullUri = Uri.withAppendedPath(contentUri, String.valueOf(id));
		mId = id;
	}
	
	public MediaStorePhotoUpload(Uri uri) {
		mFullUri = uri;
		mId = Long.parseLong(uri.getLastPathSegment());
	}

	public Uri getOriginalPhotoUri() {
		return mFullUri;
	}

	public Bitmap getThumbnailImage(Context context) {
		Resources res = context.getResources();

		final int kind = res.getBoolean(R.bool.load_mini_thumbnails) ? Thumbnails.MINI_KIND : Thumbnails.MICRO_KIND;

		BitmapFactory.Options opts = null;
		if (kind == Thumbnails.MINI_KIND && res.getBoolean(R.bool.sample_mini_thumbnails)) {
			opts = new BitmapFactory.Options();
			opts.inSampleSize = 2;
		}

		try {
			ContentResolver cr = context.getContentResolver();
			Bitmap bitmap = Thumbnails.getThumbnail(cr, mId, kind, opts);

			final int orientation = Utils.getOrientationFromContentUri(cr, getOriginalPhotoUri());
			if (orientation != 0) {
				if (Constants.DEBUG) {
					Log.d(LOG_TAG, "Orientation: " + orientation);
				}
				bitmap = Utils.rotate(bitmap, orientation);
			}

			return bitmap;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public Bitmap getDisplayImage(Context context) {
		try {
			final int size = PhotupApplication.getApplication(context).getSmallestScreenDimension();
			return Utils.decodeImage(context.getContentResolver(), getOriginalPhotoUri(), size, true);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public Bitmap getUploadImage(Context context, final UploadQuality quality) {
		Utils.checkPhotoProcessingThread();
		return getUploadImageNative(context, quality);
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
							quality.getMaxDimension(), false);
					PhotoProcessing.sendBitmapToNative(bitmap);
					bitmap.recycle();

					// Do resize
					PhotoProcessing.nativeResizeBitmap(size.width, size.height);
				}

				/**
				 * Apply filter if needed
				 */
				if (requiresProcessing()) {
					PhotoProcessing.filterPhoto(getFilterUsed().getId());
					if (Constants.DEBUG) {
						Log.d("MediaStorePhotoUpload", "getUploadImage. Native filter complete!");
					}
				}

				/**
				 * Rotate if needed
				 */
				final int orientation = Utils.getOrientationFromContentUri(context.getContentResolver(),
						getOriginalPhotoUri());
				if (orientation != 0) {
					switch (orientation) {
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
						Log.d("MediaStorePhotoUpload", "getUploadImage. " + orientation + " degree rotation complete!");
					}
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
}
