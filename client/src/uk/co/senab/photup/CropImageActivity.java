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
package uk.co.senab.photup;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Bundle;

import uk.co.senab.photup.base.PhotupActivity;
import uk.co.senab.photup.model.PhotoUpload;
import uk.co.senab.photup.platform.Platform;
import uk.co.senab.photup.views.CropImageView;
import uk.co.senab.photup.views.HighlightView;
import uk.co.senab.photup.views.PhotupImageView.OnPhotoLoadListener;

public class CropImageActivity extends PhotupActivity implements OnPhotoLoadListener {

    static PhotoUpload CROP_SELECTION;

    private CropImageView mCropImageView;
    private HighlightView mHighlightView;

    private PhotoUpload mPhotoUpload;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mCropImageView = new CropImageView(this, null);
        Platform.disableHardwareAcceleration(mCropImageView);

        setContentView(mCropImageView);

        // FIXME Hack
        mPhotoUpload = CROP_SELECTION;
        CROP_SELECTION = null;

        mCropImageView.requestFullSize(mPhotoUpload, false, this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getSupportMenuInflater().inflate(R.menu.menu_photo_crop, menu);

        // Remove OK Menu Item if we're not loaded yet
        if (null == mHighlightView) {
            menu.removeItem(R.id.menu_crop_ok);
        }

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_crop_cancel:
                setResult(RESULT_CANCELED);
                finish();
                return true;

            case R.id.menu_crop_ok:
                if (null != mHighlightView) {
                    mPhotoUpload.setCropValues(mHighlightView.getCropRect());
                    setResult(RESULT_OK);
                    finish();
                }
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void makeHighlight(final Bitmap bitmap) {
        final int width = bitmap.getWidth();
        final int height = bitmap.getHeight();
        final Rect imageRect = new Rect(0, 0, width, height);

        RectF cropRect;
        if (mPhotoUpload.beenCropped()) {
            cropRect = mPhotoUpload.getCropValues(width, height);
        } else {
            cropRect = getDefaultCropRect(width, height);
        }

        mHighlightView = new HighlightView(mCropImageView);
        mHighlightView.setup(mCropImageView.getImageMatrix(), imageRect, cropRect, false);

        mCropImageView.setHighlight(mHighlightView);

        // Refresh Menu so we have the OK item
        supportInvalidateOptionsMenu();
    }

    @Override
    public void onBackPressed() {
        setResult(RESULT_CANCELED);
        finish();
    }

    public void onPhotoLoadFinished(Bitmap bitmap) {
        if (null != bitmap) {
            makeHighlight(bitmap);
        }
    }

    static RectF getDefaultCropRect(final int width, final int height) {
        return new RectF(width * 0.1f, height * 0.1f, width * 0.9f, height * 0.9f);
    }
}
