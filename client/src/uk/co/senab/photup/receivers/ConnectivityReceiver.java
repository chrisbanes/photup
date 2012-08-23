package uk.co.senab.photup.receivers;

import uk.co.senab.photup.PhotoUploadController;
import uk.co.senab.photup.util.Utils;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class ConnectivityReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		if (isConnected(context)) {
			PhotoUploadController controller = PhotoUploadController.getFromContext(context);
			if (controller.hasWaitingUploads()) {
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
