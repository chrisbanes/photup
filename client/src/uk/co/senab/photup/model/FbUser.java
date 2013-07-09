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

import java.util.Comparator;

public class FbUser extends AbstractFacebookObject {

    public static final String GRAPH_FIELDS = "id,name";

    public FbUser(String id, String name, Account account) {
        super(id, name, account);
    }

    public FbUser(JSONObject object, Account account) throws JSONException {
        super(object, account);
    }

    public static Comparator<FbUser> getComparator() {
        return new Comparator<FbUser>() {
            public int compare(FbUser lhs, FbUser rhs) {
                return lhs.getName().compareTo(rhs.getName());
            }
        };
    }

    public static FbUser getMeFromAccount(Account account) {
        return new FbUser(account.getId(), account.getName(), account);
    }

}
