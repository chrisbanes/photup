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

import com.j256.ormlite.field.DatabaseField;

import org.json.JSONException;
import org.json.JSONObject;

public abstract class AbstractFacebookObject {

    public static final String FIELD_ID = "_id";
    public static final String FIELD_NAME = "name";
    public static final String FIELD_ACCOUNT_ID = "acc_id";

    @DatabaseField(columnName = FIELD_ID)
    String mId;
    @DatabaseField(columnName = FIELD_NAME)
    String mName;
    @DatabaseField(columnName = FIELD_ACCOUNT_ID)
    String mAccountId;

    AbstractFacebookObject() {
        // NO-Arg for Ormlite
    }

    public AbstractFacebookObject(String id, String name, Account account) {
        mId = id;
        mName = name;

        if (null != account) {
            mAccountId = account.getId();
        }
    }

    public AbstractFacebookObject(JSONObject object, Account account) throws JSONException {
        mId = object.getString("id");
        mName = object.getString("name");

        if (null != account) {
            mAccountId = account.getId();
        }
    }

    public String getId() {
        return mId;
    }

    @Override
    public String toString() {
        return mName;
    }

    public String getName() {
        return mName;
    }

    public String getAvatarUrl() {
        return new StringBuffer("http://graph.facebook.com/").append(mId).append("/picture")
                .toString();
    }
}
