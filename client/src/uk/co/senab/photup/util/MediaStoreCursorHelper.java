package uk.co.senab.photup.util;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;

import uk.co.senab.photup.model.MediaStoreBucket;
import uk.co.senab.photup.model.PhotoUpload;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore.Images;
import android.provider.MediaStore.Images.ImageColumns;

public class MediaStoreCursorHelper {

	public static final String[] PHOTOS_PROJECTION = { Images.Media._ID, Images.Media.MINI_THUMB_MAGIC,
			Images.Media.DATA, Images.Media.BUCKET_DISPLAY_NAME, Images.Media.BUCKET_ID };
	public static final String PHOTOS_ORDER_BY = Images.Media.DATE_ADDED + " desc";

	public static final Uri MEDIA_STORE_CONTENT_URI = Images.Media.EXTERNAL_CONTENT_URI;

	public static ArrayList<PhotoUpload> photosCursorToSelectionList(Uri contentUri, Cursor cursor) {
		ArrayList<PhotoUpload> items = new ArrayList<PhotoUpload>(cursor.getCount());
		PhotoUpload item;

		if (cursor.moveToFirst()) {
			do {
				try {
					item = photosCursorToSelection(contentUri, cursor);
					if (null != item) {
						items.add(item);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			} while (cursor.moveToNext());
		}

		return items;
	}

	public static PhotoUpload photosCursorToSelection(Uri contentUri, Cursor cursor) {
		PhotoUpload item = null;

		try {
			File file = new File(cursor.getString(cursor.getColumnIndexOrThrow(ImageColumns.DATA)));
			if (file.exists()) {
				item = PhotoUpload.getSelection(contentUri,
						cursor.getInt(cursor.getColumnIndexOrThrow(ImageColumns._ID)));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return item;
	}

	public static void photosCursorToBucketList(Cursor cursor, ArrayList<MediaStoreBucket> items) {
		final HashSet<String> bucketIds = new HashSet<String>();

		final int idColumn = cursor.getColumnIndex(ImageColumns.BUCKET_ID);
		final int nameColumn = cursor.getColumnIndex(ImageColumns.BUCKET_DISPLAY_NAME);

		if (cursor.moveToFirst()) {
			do {
				try {
					final String bucketId = cursor.getString(idColumn);
					if (bucketIds.add(bucketId)) {
						items.add(new MediaStoreBucket(bucketId, cursor.getString(nameColumn)));
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			} while (cursor.moveToNext());
		}
	}

	public static Cursor openPhotosCursor(Context context, Uri contentUri) {
		return context.getContentResolver().query(contentUri, PHOTOS_PROJECTION, null, null, PHOTOS_ORDER_BY);
	}

}
