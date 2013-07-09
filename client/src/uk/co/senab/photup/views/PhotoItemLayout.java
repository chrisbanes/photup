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
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;

import uk.co.senab.photup.PhotoUploadController;
import uk.co.senab.photup.PhotupApplication;
import uk.co.senab.photup.R;
import uk.co.senab.photup.model.PhotoUpload;

public class PhotoItemLayout extends CheckableFrameLayout implements View.OnClickListener {

    private final PhotupImageView mImageView;
    private final CheckableImageView mButton;
    private final TextView mCaptionText;

    private PhotoUpload mSelection;

    private boolean mAnimateCheck = true;
    private boolean mShowCaption = false;

    private final PhotoUploadController mController;

    public PhotoItemLayout(Context context, AttributeSet attrs) {
        super(context, attrs);

        LayoutInflater.from(context).inflate(R.layout.item_grid_photo_internal, this);

        mController = PhotupApplication.getApplication(context).getPhotoUploadController();

        mImageView = (PhotupImageView) findViewById(R.id.iv_photo);
        mCaptionText = (TextView) findViewById(R.id.tv_photo_caption);

        mButton = (CheckableImageView) findViewById(R.id.civ_button);
        mButton.setOnClickListener(this);
    }

    public PhotupImageView getImageView() {
        return mImageView;
    }

    public void setAnimateWhenChecked(boolean animate) {
        mAnimateCheck = animate;
    }

    public void setShowCheckbox(boolean visible) {
        if (visible) {
            mButton.setVisibility(View.VISIBLE);
            mButton.setOnClickListener(this);
        } else {
            mButton.setVisibility(View.GONE);
            mButton.setOnClickListener(null);
        }
    }

    public void setShowCaption(boolean show) {
        mShowCaption = show;
    }

    public void onClick(View v) {
        if (null != mSelection) {

            // Toggle check to show new state
            toggle();

            // Update the controller
            if (isChecked()) {
                mController.addSelection(mSelection);
            } else {
                mController.removeSelection(mSelection);
            }

            // Show animate if we've been set to
            if (mAnimateCheck) {
                Animation anim = AnimationUtils
                        .loadAnimation(getContext(), isChecked() ? R.anim.photo_selection_added
                                : R.anim.photo_selection_removed);
                v.startAnimation(anim);
            }
        }
    }

    @Override
    public void setChecked(final boolean b) {
        super.setChecked(b);
        if (View.VISIBLE == mButton.getVisibility()) {
            mButton.setChecked(b);
        }
    }

    public PhotoUpload getPhotoSelection() {
        return mSelection;
    }

    public void setPhotoSelection(PhotoUpload selection) {
        if (mSelection != selection) {
            mButton.clearAnimation();
            mSelection = selection;
        }

        if (mShowCaption) {
            String caption = mSelection.getCaption();
            if (TextUtils.isEmpty(caption)) {
                mCaptionText.setVisibility(View.GONE);
            } else {
                mCaptionText.setVisibility(View.VISIBLE);
                mCaptionText.setText(caption);
            }
        }
    }

}
