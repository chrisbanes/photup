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

import org.json.JSONException;
import org.json.JSONObject;

import android.text.TextUtils;
import android.util.Log;

import java.util.HashMap;

import uk.co.senab.photup.Flags;

public class PhotoTag {

    private final float mX;
    private final float mY;
    private String mFriendId;
    private FbUser mFriend;

    public PhotoTag(float x, float y) {
        mX = x;
        mY = y;

        if (Flags.DEBUG) {
            Log.d("PhotoTag", "X: " + x + " Y: " + y);
        }
    }

    public PhotoTag(JSONObject object) throws JSONException {
        this((float) object.getDouble("x"), (float) object.getDouble("y"));
        mFriendId = object.getString("tag_uid");
    }

    public PhotoTag(float x, float y, float bitmapWidth, float bitmapHeight) {
        this(100 * x / bitmapWidth, 100 * y / bitmapHeight);
    }

    public PhotoTag(float x, float y, int bitmapWidth, int bitmapHeight) {
        this(x, y, (float) bitmapHeight, (float) bitmapWidth);
    }

    public float getX() {
        return mX;
    }

    public float getY() {
        return mY;
    }

    public FbUser getFriend() {
        return mFriend;
    }

    public boolean hasFriend() {
        return null != mFriend;
    }

    public void populateFromFriends(HashMap<String, FbUser> friends) {
        if (null == mFriend && !TextUtils.isEmpty(mFriendId)) {
            mFriend = friends.get(mFriendId);
        }
    }

    public void setFriend(FbUser friend) {
        mFriend = friend;
        mFriendId = null != friend ? friend.getId() : null;
    }

    public JSONObject toJsonObject() throws JSONException {
        JSONObject object = new JSONObject();
        if (hasFriend()) {
            object.put("tag_uid", mFriend.getId());
        }
        object.put("x", mX);
        object.put("y", mY);
        return object;
    }

}
