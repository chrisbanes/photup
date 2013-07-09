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
package uk.co.senab.photup.views;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import uk.co.senab.photup.R;

public class UploadsActionBarView extends LinearLayout {

    public UploadsActionBarView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void updateProgress(final int progress, final int total) {
        ProgressBar pb = (ProgressBar) findViewById(R.id.pb_uploads_action);
        if (null != pb) {
            pb.setMax(100);
            pb.setProgress(Math.round(progress * 100f / total));
        }

        TextView tv = (TextView) findViewById(R.id.tv_uploads_action);
        if (null != tv) {
            String string;
            if (total > 0) {
                string = getResources()
                        .getString(R.string.action_bar_upload_progress, progress, total);
            } else {
                string = getResources().getString(R.string.tab_uploads);
            }
            tv.setText(string.toUpperCase());
        }
    }
}
