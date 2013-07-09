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
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import de.greenrobot.event.EventBus;
import uk.co.senab.photup.R;
import uk.co.senab.photup.events.UploadStateChangedEvent;
import uk.co.senab.photup.model.PhotoUpload;

public class UploadItemLayout extends LinearLayout {

    private PhotoUpload mSelection;

    public UploadItemLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        EventBus.getDefault().register(this);
    }

    public TextView getCaptionTextView() {
        return (TextView) findViewById(R.id.tv_photo_caption);
    }

    public PhotupImageView getImageView() {
        return (PhotupImageView) findViewById(R.id.iv_photo);
    }

    public ProgressBar getProgressBar() {
        return (ProgressBar) findViewById(R.id.pb_upload_progress);
    }

    public ImageView getResultImageView() {
        return (ImageView) findViewById(R.id.iv_upload_result);
    }

    public TextView getTagTextView() {
        return (TextView) findViewById(R.id.tv_photo_tags);
    }

    public void onEventMainThread(UploadStateChangedEvent event) {
        if (mSelection == event.getUpload()) {
            refreshUploadUi();
        }
    }

    public void refreshUploadUi() {
        if (null == mSelection) {
            return;
        }

        ProgressBar pb = getProgressBar();
        ImageView resultIv = getResultImageView();

        switch (mSelection.getUploadState()) {
            case PhotoUpload.STATE_UPLOAD_COMPLETED:
                pb.setVisibility(View.GONE);
                resultIv.setImageResource(R.drawable.ic_success);
                resultIv.setVisibility(View.VISIBLE);
                break;

            case PhotoUpload.STATE_UPLOAD_ERROR:
                pb.setVisibility(View.GONE);
                resultIv.setImageResource(R.drawable.ic_error);
                resultIv.setVisibility(View.VISIBLE);
                break;

            case PhotoUpload.STATE_UPLOAD_IN_PROGRESS:
                pb.setVisibility(View.VISIBLE);
                resultIv.setVisibility(View.GONE);

                final int progress = mSelection.getUploadProgress();
                if (progress <= 0) {
                    pb.setIndeterminate(true);
                } else {
                    pb.setIndeterminate(false);
                    pb.setProgress(progress);
                }
                break;

            case PhotoUpload.STATE_UPLOAD_WAITING:
                pb.setVisibility(View.VISIBLE);
                resultIv.setVisibility(View.GONE);
                pb.setIndeterminate(true);
                break;
        }

        requestLayout();
    }

    public void setPhotoSelection(PhotoUpload selection) {
        mSelection = selection;

        /**
         * Initial UI Update
         */
        PhotupImageView iv = getImageView();
        if (null != iv) {
            iv.requestThumbnail(mSelection, false);
        }

        TextView tv = getCaptionTextView();
        if (null != tv) {
            final String caption = mSelection.getCaption();
            if (TextUtils.isEmpty(caption)) {
                tv.setText(R.string.untitled_photo);
            } else {
                tv.setText(mSelection.getCaption());
            }
        }

        tv = getTagTextView();
        if (null != tv) {
            final int tagsCount = mSelection.getFriendPhotoTagsCount();
            if (tagsCount > 0) {
                tv.setText(getResources()
                        .getQuantityString(R.plurals.tag_summary_photo, tagsCount, tagsCount));
                tv.setVisibility(View.VISIBLE);
            } else {
                tv.setVisibility(View.GONE);
            }
        }

        /**
         * Refresh Progress Bar
         */
        refreshUploadUi();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        EventBus.getDefault().unregister(this);
    }

}
