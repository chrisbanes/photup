/*******************************************************************************
 * Copyright 2013 Chris Banes.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.	
 *******************************************************************************/
package uk.co.senab.photup.receivers;

import uk.co.senab.photup.Flags;
import uk.co.senab.photup.PhotoUploadController;
import uk.co.senab.photup.util.Utils;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.telephony.TelephonyManager;
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

	public static boolean isConnectedViaCellular(Context context) {
		ConnectivityManager mgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo info = mgr.getActiveNetworkInfo();
		return null != info && info.isConnectedOrConnecting() && info.getType() == ConnectivityManager.TYPE_MOBILE;
	}
	
	public static boolean isConnectedViaCellularRoaming(Context context) {
		TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
		return telephonyManager.isNetworkRoaming() && isConnectedViaCellular(context);
	}

}
