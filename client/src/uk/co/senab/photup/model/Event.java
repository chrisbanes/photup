package uk.co.senab.photup.model;

import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.Callable;

import org.json.JSONException;
import org.json.JSONObject;

import uk.co.senab.photup.DatabaseHelper;
import uk.co.senab.photup.Flags;

import android.content.Context;
import android.util.Log;

import com.j256.ormlite.android.apptools.OpenHelperManager;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.DeleteBuilder;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "event")
public class Event extends AbstractFacebookObject {
	
	static final String LOG_TAG = "Event";

	public static final String GRAPH_FIELDS = "id,name";
	
	Event() {
		// NO-Arg for Ormlite
	}

	public Event(JSONObject object, Account account) throws JSONException {
		super(object, account);
	}

	public static List<Event> getFromDatabase(Context context, Account account) {
		final DatabaseHelper helper = OpenHelperManager.getHelper(context, DatabaseHelper.class);
		final String accountId = account.getId();
		List<Event> events = null;

		try {
			final Dao<Event, String> dao = helper.getEventDao();
			events = dao.query(dao.queryBuilder().orderBy(FIELD_NAME, true).where().eq(FIELD_ACCOUNT_ID, accountId)
					.prepare());
		} catch (SQLException e) {
			if (Flags.DEBUG) {
				e.printStackTrace();
			}
		} finally {
			OpenHelperManager.releaseHelper();
		}

		return events;
	}

	public static void saveToDatabase(Context context, final List<Event> items, Account account) {
		final DatabaseHelper helper = OpenHelperManager.getHelper(context, DatabaseHelper.class);
		final String accountId = account.getId();

		try {
			final Dao<Event, String> dao = helper.getEventDao();
			dao.callBatchTasks(new Callable<Void>() {

				public Void call() throws Exception {
					// Delete all
					DeleteBuilder<Event, String> deleteBuilder = dao.deleteBuilder();
					deleteBuilder.where().eq(FIELD_ACCOUNT_ID, accountId);
					int removed = dao.delete(deleteBuilder.prepare());

					if (Flags.DEBUG) {
						Log.d(LOG_TAG, "Deleted " + removed + " from database");
					}

					for (Event item : items) {
						dao.create(item);
					}
					if (Flags.DEBUG) {
						Log.d(LOG_TAG, "Inserted " + items.size() + " into database");
					}
					return null;
				}
			});
		} catch (Exception e) {
			if (Flags.DEBUG) {
				e.printStackTrace();
			}
		} finally {
			OpenHelperManager.releaseHelper();
		}
	}

}
