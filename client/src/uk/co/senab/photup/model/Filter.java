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

public enum Filter {

    // DO NOT CHANGE ORDER DUE TO INSTANT UPLOAD FILTER PREF!
    ORIGINAL(R.string.filter_original),
    INSTAFIX(R.string.filter_instafix),
    ANSEL(R.string.filter_ansel),
    TESTINO(R.string.filter_testino),
    XPRO(R.string.filter_xpro),
    RETRO(R.string.filter_retro),
    BW(R.string.filter_bw),
    SEPIA(R.string.filter_sepia),
    CYANO(R.string.filter_cyano),
    GEORGIA(R.string.filter_georgia),
    SAHARA(R.string.filter_sahara),
    HDR(R.string.filter_hdr);

    public static Filter mapFromId(int id) {
        try {
            return values()[id];
        } catch (Exception e) {
            return null;
        }
    }

    public static Filter mapFromPref(String preference) {
        Filter returnValue;
        try {
            int id = Integer.parseInt(preference);
            returnValue = mapFromId(id);
        } catch (Exception e) {
            returnValue = ORIGINAL;
        }
        return returnValue;
    }

    private final int mLabelId;

    private Filter(int labelId) {
        mLabelId = labelId;
    }

    public int getId() {
        return ordinal();
    }

    public int getLabelId() {
        return mLabelId;
    }

    public String mapToPref() {
        return String.valueOf(getId());
    }
}
