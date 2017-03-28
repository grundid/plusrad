/**
 * Cycle Philly, Copyright 2014 Code for Philly
 *
 * @author Lloyd Emelle <lloyd@codeforamerica.org>
 * @author Christopher Le Dantec <ledantec@gatech.edu>
 * @author Anhong Guo <guoanhong15@gmail.com>
 * <p>
 * Updated/Modified for Philly's app deployment. Based on the
 * CycleTracks codebase for SFCTA and Cycle Atlanta.
 * <p>
 * CycleTracks, Copyright 2009,2010 San Francisco County Transportation Authority
 * San Francisco, CA, USA
 * @author Billy Charlton <billy.charlton@sfcta.org>
 * <p>
 * This file is part of CycleTracks.
 * <p>
 * CycleTracks is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * CycleTracks is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with CycleTracks.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.grundid.plusrad.recording;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import de.grundid.plusrad.db.TripData;

import java.util.ArrayList;
import java.util.List;

public class DbAdapter extends SQLiteOpenHelper {

	private static final int DATABASE_VERSION = 21;
	private static final String K_TRIP_ROWID = "_id";
	private static final String K_TRIP_START = "starttime";
	private static final String K_TRIP_END = "endtime";
	private static final String K_TRIP_DISTANCE = "distance";
	private static final String K_TRIP_PURPOSE = "purpose";
	private static final String K_TRIP_COMMENT = "comment";
	private static final String K_TRIP_TRIPTIME = "triptime";
	private static final String K_TRIP_STATUS = "status";
	private static final String K_TRIP_WEST = "west";
	private static final String K_TRIP_EAST = "east";
	private static final String K_TRIP_NORTH = "north";
	private static final String K_TRIP_SOUTH = "south";
	private static final String K_POINT_ROWID = "_id";
	private static final String K_POINT_TRIP_ID = "trip_id";
	private static final String K_POINT_TIME = "time";
	private static final String K_POINT_LAT = "lat";
	private static final String K_POINT_LGT = "lgt";
	private static final String K_POINT_ACC = "acc";
	private static final String K_POINT_ALT = "alt";
	private static final String K_POINT_SPEED = "speed";
	private static final String K_POINT_ACTIVITY = "activity";
	private static final String[] ALL_TRIP_ROWS = new String[] { K_TRIP_ROWID, K_TRIP_START, K_TRIP_END,
			K_TRIP_DISTANCE, K_TRIP_PURPOSE,
			K_TRIP_COMMENT, K_TRIP_TRIPTIME,
			K_TRIP_STATUS, K_TRIP_WEST, K_TRIP_EAST, K_TRIP_NORTH, K_TRIP_SOUTH };
	private static final String TABLE_CREATE_TRIPS = "create table trips "
			+ "(" + K_TRIP_ROWID + " integer primary key autoincrement, "
			+ K_TRIP_START + " integer, "
			+ K_TRIP_END + " integer, "
			+ K_TRIP_DISTANCE + " double, "
			+ K_TRIP_PURPOSE + " text, "
			+ K_TRIP_COMMENT + " text, "
			+ K_TRIP_TRIPTIME + " integer, "
			+ K_TRIP_STATUS + " integer, "
			+ K_TRIP_WEST + " double, "
			+ K_TRIP_EAST + " double, "
			+ K_TRIP_NORTH + " double, "
			+ K_TRIP_SOUTH + " double);";
	private static final String TABLE_CREATE_COORDS = "create table coords "
			+ "(" + K_POINT_ROWID + " integer primary key autoincrement, "
			+ K_POINT_TRIP_ID + " integer, "
			+ K_POINT_LAT + " double, "
			+ K_POINT_LGT + " double, "
			+ K_POINT_TIME + " integer, "
			+ K_POINT_ACC + " float, "
			+ K_POINT_ALT + " double, "
			+ K_POINT_SPEED + " float, "
			+ K_POINT_ACTIVITY + " integer);";
	private static final String DATABASE_NAME = "data";
	private static final String DATA_TABLE_TRIPS = "trips";
	private static final String DATA_TABLE_COORDS = "coords";
	public static int STATUS_INCOMPLETE = 0;
	public static int STATUS_COMPLETE = 1;
	public static int STATUS_SENT = 2;

	public DbAdapter(Context ctx) {
		super(ctx, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(TABLE_CREATE_TRIPS);
		db.execSQL(TABLE_CREATE_COORDS);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		db.execSQL("DROP TABLE IF EXISTS " + DATA_TABLE_TRIPS);
		db.execSQL("DROP TABLE IF EXISTS " + DATA_TABLE_COORDS);
		onCreate(db);
	}

	public boolean addCoordToTrip(long tripId, CyclePoint pt) {
		SQLiteDatabase db = getWritableDatabase();
		ContentValues rowValues = new ContentValues();
		rowValues.put(K_POINT_TRIP_ID, tripId);
		rowValues.put(K_POINT_LAT, pt.getCoords().latitude);
		rowValues.put(K_POINT_LGT, pt.getCoords().longitude);
		rowValues.put(K_POINT_TIME, pt.getTime());
		rowValues.put(K_POINT_ACC, pt.getAccuracy());
		rowValues.put(K_POINT_ALT, pt.getAltitude());
		rowValues.put(K_POINT_SPEED, pt.getSpeed());
		rowValues.put(K_POINT_ACTIVITY, pt.getActivityType());
		boolean success = db.insert(DATA_TABLE_COORDS, null, rowValues) > 0;
		rowValues = new ContentValues();
		rowValues.put(K_TRIP_END, pt.getTime());
		success = success && (db.update(DATA_TABLE_TRIPS, rowValues, K_TRIP_ROWID + "=" + tripId, null) > 0);
		db.close();
		return success;
	}

	public List<CyclePoint> fetchAllCoordsForTrip(long tripId) {
		List<CyclePoint> points = new ArrayList<>();
		SQLiteDatabase db = getReadableDatabase();
		Cursor cursor = db.query(true, DATA_TABLE_COORDS, new String[] {
						K_POINT_LAT, K_POINT_LGT, K_POINT_TIME,
						K_POINT_ACC, K_POINT_ALT, K_POINT_SPEED, K_POINT_ACTIVITY },
				K_POINT_TRIP_ID + "=" + tripId,
				null, null, null, K_POINT_TIME, null);
		while (cursor.moveToNext()) {
			points.add(new CyclePoint(cursor.getDouble(0), cursor.getDouble(1), cursor.getLong(2), cursor.getFloat(3),
					cursor.getDouble(4), cursor.getFloat(5), cursor.getInt(6)));
		}
		cursor.close();
		db.close();
		return points;
	}

	public TripData createTrip() {
		long tripId = createTrip(System.currentTimeMillis());
		return loadTrip(tripId);
	}

	private TripData loadTrip(long tripId) {
		TripData tripData = new TripData();
		SQLiteDatabase db = getReadableDatabase();
		Cursor cursor = db
				.query(DATA_TABLE_TRIPS,
						ALL_TRIP_ROWS,
						K_TRIP_ROWID + "=?", new String[] { "" + tripId }, null,
						null,
						null);
		try {
			if (cursor.moveToNext()) {
				mapCursorOnTripData(tripData, cursor);
			} else {
				throw new RuntimeException("Trip not found: " + tripId);
			}
		}
		finally {
			cursor.close();
			db.close();
		}
		return tripData;
	}

	private void mapCursorOnTripData(TripData tripData, Cursor cursor) {
		tripData.setTripId(cursor.getLong(cursor.getColumnIndex(K_TRIP_ROWID)));
		tripData.setStartTime(cursor.getLong(cursor.getColumnIndex(K_TRIP_START)));
		tripData.setEndTime(cursor.getLong(cursor.getColumnIndex(K_TRIP_END)));
		tripData.setComment(cursor.getString(cursor.getColumnIndex(K_TRIP_COMMENT)));
		tripData.setDistance(cursor.getDouble(cursor.getColumnIndex(K_TRIP_DISTANCE)));
		tripData.setEast(cursor.getDouble(cursor.getColumnIndex(K_TRIP_EAST)));
		tripData.setWest(cursor.getDouble(cursor.getColumnIndex(K_TRIP_WEST)));
		tripData.setNorth(cursor.getDouble(cursor.getColumnIndex(K_TRIP_NORTH)));
		tripData.setSouth(cursor.getDouble(cursor.getColumnIndex(K_TRIP_SOUTH)));
		tripData.setTripTime(cursor.getLong(cursor.getColumnIndex(K_TRIP_TRIPTIME)));
		tripData.setPurpose(cursor.getString(cursor.getColumnIndex(K_TRIP_PURPOSE)));
		tripData.setStatus(cursor.getInt(cursor.getColumnIndex(K_TRIP_STATUS)));
	}

	public long createTrip(long startTime) {
		SQLiteDatabase db = getWritableDatabase();
		ContentValues initialValues = new ContentValues();
		initialValues.put(K_TRIP_START, startTime);
		initialValues.put(K_TRIP_STATUS, STATUS_INCOMPLETE);
		long id = db.insert(DATA_TABLE_TRIPS, null, initialValues);
		db.close();
		return id;
	}

	public void deleteTrip(long tripId) {
		SQLiteDatabase db = getWritableDatabase();
		db.delete(DATA_TABLE_COORDS, K_POINT_TRIP_ID + "=" + tripId, null);
		db.delete(DATA_TABLE_TRIPS, K_TRIP_ROWID + "=" + tripId, null);
		db.close();
	}

	public List<TripData> getAllTrips() {
		SQLiteDatabase db = getReadableDatabase();
		List<TripData> trips = new ArrayList<>();
		Cursor cursor = db
				.query(DATA_TABLE_TRIPS, ALL_TRIP_ROWS,
						null, null, null,
						null, K_TRIP_END + " DESC");
		try {
			while (cursor.moveToNext()) {
				TripData tripData = new TripData();
				mapCursorOnTripData(tripData, cursor);
				trips.add(tripData);
			}
		}
		finally {
			cursor.close();
			db.close();
		}
		return trips;
	}

	public Cursor fetchUnsentTrips() {
		SQLiteDatabase db = getReadableDatabase();
		Cursor c = db.query(DATA_TABLE_TRIPS, new String[] { K_TRIP_ROWID },
				K_TRIP_STATUS + "=" + STATUS_COMPLETE,
				null, null, null, null);
		if (c != null && c.getCount() > 0) {
			c.moveToFirst();
		}
		db.close();
		return c;
	}

	public int cleanTables() {
		SQLiteDatabase db = getWritableDatabase();
		int badTrips = 0;
		Cursor c = db.query(DATA_TABLE_TRIPS, new String[]
						{ K_TRIP_ROWID, K_TRIP_STATUS },
				K_TRIP_STATUS + "=" + STATUS_INCOMPLETE,
				null, null, null, null);
		if (c != null && c.getCount() > 0) {
			c.moveToFirst();
			badTrips = c.getCount();
			while (!c.isAfterLast()) {
				long tripId = c.getInt(0);
				db.delete(DATA_TABLE_COORDS, K_POINT_TRIP_ID + "=" + tripId, null);
				c.moveToNext();
			}
			if (badTrips > 0) {
				db.delete(DATA_TABLE_TRIPS, K_TRIP_STATUS + "=" + STATUS_INCOMPLETE, null);
			}
			c.close();
		}
		db.close();
		return badTrips;
	}

	/*	public Cursor fetchTrip(long tripId) throws SQLException {
			SQLiteDatabase db = getReadableDatabase();
			Cursor mCursor = db.query(true, DATA_TABLE_TRIPS, new String[] {
							K_TRIP_ROWID, K_TRIP_PURP, K_TRIP_START, K_TRIP_FANCYSTART,
							K_TRIP_NOTE, K_TRIP_LATHI, K_TRIP_LATLO, K_TRIP_LGTHI,
							K_TRIP_LGTLO, K_TRIP_STATUS, K_TRIP_END, K_TRIP_FANCYINFO, K_TRIP_DISTANCE },
					K_TRIP_ROWID + "=" + tripId,
					null, null, null, null, null);
			if (mCursor != null) {
				mCursor.moveToFirst();
			}
			db.close();
			return mCursor;
		}*/

	public void updateTrip(TripData tripData) {
		SQLiteDatabase db = getWritableDatabase();
		ContentValues initialValues = new ContentValues();
		initialValues.put(K_TRIP_START, tripData.getStartTime());
		initialValues.put(K_TRIP_END, tripData.getEndTime());
		initialValues.put(K_TRIP_DISTANCE, tripData.getDistance());
		initialValues.put(K_TRIP_PURPOSE, tripData.getPurpose());
		initialValues.put(K_TRIP_COMMENT, tripData.getComment());
		initialValues.put(K_TRIP_TRIPTIME, tripData.getTripTime());
		initialValues.put(K_TRIP_STATUS, tripData.getStatus());
		initialValues.put(K_TRIP_WEST, tripData.getWest());
		initialValues.put(K_TRIP_EAST, tripData.getEast());
		initialValues.put(K_TRIP_NORTH, tripData.getNorth());
		initialValues.put(K_TRIP_SOUTH, tripData.getSouth());
		db.update(DATA_TABLE_TRIPS, initialValues, K_TRIP_ROWID + "="
				+ tripData.getTripId(), null);
		db.close();
	}

	public TripData getTrip(long tripId) {
		SQLiteDatabase db = getReadableDatabase();
		Cursor cursor = db
				.query(DATA_TABLE_TRIPS, ALL_TRIP_ROWS,
						K_TRIP_ROWID + "=?", new String[] { tripId + "" }, null,
						null, null);
		TripData tripData = new TripData();
		try {
			if (cursor.moveToNext()) {
				mapCursorOnTripData(tripData, cursor);
			}
		}
		finally {
			cursor.close();
			db.close();
		}
		return tripData;
	}
}
