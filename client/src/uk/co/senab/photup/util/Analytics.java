/*
 * Copyright 2013 Chris Banes
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.co.senab.photup.util;

import android.content.Context;

public class Analytics {

    public static final String EVENT_PHOTO_RESET = "photo_viewer_reset";
    public static final String EVENT_PHOTO_FILTERS = "photo_viewer_filters";
    public static final String EVENT_PHOTO_CROP = "photo_viewer_crop";
    public static final String EVENT_PHOTO_ROTATE = "photo_viewer_rotate";
    public static final String EVENT_PHOTO_CAPTION = "photo_viewer_caption";
    public static final String EVENT_PHOTO_PLACE = "photo_viewer_place";

    public static void onStartSession(Context context) {
        //
    }

    public static void onEndSession(Context context) {
        //
    }

    public static void logEvent(final String event) {
        //
    }

}
