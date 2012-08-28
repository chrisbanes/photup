package uk.co.senab.photup.receivers;

import uk.co.senab.photup.Flags;
import uk.co.senab.photup.PhotoUploadController;
import uk.co.senab.photup.util.Utils;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

public class ConnectivityReceiver extends BroadcastReceiver {

	static final String LOG_TAG = "ConnectivityReceiver";

	@Override
	public void onReceive(Context context, Intent intent) {
		if (isConnected(context)) {
			if (Flags.DEBUG) {
				Log.d(LOG_TAG, "onReceive - We're connected!");
			}

			PhotoUploadController controller = PhotoUploadController.getFromContext(context);
			if (controller.hasWaitingUploads()) {
				if (Flags.DEBUG) {
					Log.d(LOG_TAG, "onReceive - Have waiting uploads, starting service!");
				}
				context.startService(Utils.getUploadAllIntent(context));
			}
		}
	}

	public static boolean isConnected(Context context) {
		ConnectivityManager mgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo info = mgr.getActiveNetworkInfo();
		return null != info && info.isConnectedOrConnecting();
	}

}
