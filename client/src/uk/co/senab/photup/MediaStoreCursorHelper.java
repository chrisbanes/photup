package uk.co.senab.photup;

import java.io.File;
import java.util.ArrayList;

import uk.co.senab.photup.model.MediaStorePhotoUpload;
import uk.co.senab.photup.model.PhotoSelection;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore.Images;
import android.provider.MediaStore.Images.ImageColumns;

public class MediaStoreCursorHelper {

	public static final String[] PROJECTION = { Images.Media._ID, Images.Media.MINI_THUMB_MAGIC, Images.Media.DATA };
	public static final String ORDER_BY = Images.Media.DATE_ADDED + " desc";

	public static ArrayList<PhotoSelection> cursorToSelectionList(Uri contentUri, Cursor cursor) {
		ArrayList<PhotoSelection> items = new ArrayList<PhotoSelection>(cursor.getCount());

		PhotoSelection item;
		while (cursor.moveToNext()) {
			try {
				item = cursorToSelection(contentUri, cursor);
				if (null != item) {
					items.add(item);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		return items;
	}

	public static PhotoSelection cursorToSelection(Uri contentUri, Cursor cursor) {
		PhotoSelection item = null;

		try {
			File file = new File(cursor.getString(cursor.getColumnIndexOrThrow(ImageColumns.DATA)));
			if (file.exists()) {
				item = new MediaStorePhotoUpload(contentUri, cursor.getInt(cursor
						.getColumnIndexOrThrow(ImageColumns._ID)));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return item;
	}

	public static Cursor openCursor(Context context, Uri contentUri) {
		return context.getContentResolver().query(contentUri, PROJECTION, null, null, ORDER_BY);
	}

}
