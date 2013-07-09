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
package uk.co.senab.photup.model;

import uk.co.senab.photup.R;

public enum UploadQuality {

    LOW(640, 75), MEDIUM(1024, 80), HIGH(2048, 85), ORIGINAL(Integer.MAX_VALUE, 90);

    private final int mMaxDimension, mJpegQuality;

    private UploadQuality(int maxDimension, int jpegQuality) {
        mMaxDimension = maxDimension;
        mJpegQuality = jpegQuality;
    }

    public int getMaxDimension() {
        return mMaxDimension;
    }

    public int getJpegQuality() {
        return mJpegQuality;
    }

    public boolean requiresResizing() {
        return mMaxDimension < Integer.MAX_VALUE;
    }

    public static UploadQuality mapFromButtonId(int buttonId) {
        switch (buttonId) {
            case R.id.rb_quality_low:
                return UploadQuality.LOW;
            case R.id.rb_quality_medium:
                return UploadQuality.MEDIUM;
            default:
            case R.id.rb_quality_high:
                return UploadQuality.HIGH;
            case R.id.rb_quality_max:
                return UploadQuality.ORIGINAL;
        }
    }

    public static UploadQuality mapFromPreference(String value) {
        UploadQuality returnValue = MEDIUM;

        if ("0".equals(value)) {
            returnValue = LOW;
        } else if ("1".equals(value)) {
            returnValue = MEDIUM;
        } else if ("2".equals(value)) {
            returnValue = HIGH;
        } else if ("3".equals(value)) {
            returnValue = ORIGINAL;
        }

        return returnValue;
    }
}
