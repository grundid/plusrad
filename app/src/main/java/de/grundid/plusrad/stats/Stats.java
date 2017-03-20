package de.grundid.plusrad.stats;

import de.grundid.plusrad.db.TripData;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class Stats {

	private Map<ValueAndYear, CountAndDistance> weekData = new HashMap<>();
	private Map<ValueAndYear, CountAndDistance> weekdayData = new HashMap<>();
	private Map<ValueAndYear, CountAndDistance> monthData = new HashMap<>();

	public void addTrip(TripData tripData) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(tripData.getStartTime());
		int weekNumber = calendar.get(Calendar.WEEK_OF_YEAR);
		int month = calendar.get(Calendar.MONTH);
		int weekday = calendar.get(Calendar.DAY_OF_WEEK);
		int year = calendar.get(Calendar.YEAR);
		ValueAndYear weekKey = new ValueAndYear(weekNumber, year);
		ValueAndYear weekdayKey = new ValueAndYear(weekday, year);
		ValueAndYear monthKey = new ValueAndYear(month, year);
		handleWeekStats(tripData, weekKey, weekData);
		handleWeekStats(tripData, weekdayKey, weekdayData);
		handleWeekStats(tripData, monthKey, monthData);
	}

	private void handleWeekStats(TripData tripData, ValueAndYear weekKey, Map<ValueAndYear, CountAndDistance> data) {
		CountAndDistance countAndDistance = data.get(weekKey);
		if (countAndDistance == null) {
			countAndDistance = new CountAndDistance();
			data.put(weekKey, countAndDistance);
		}
		countAndDistance.incCount();
		countAndDistance.addDistance(tripData.getDistance());
	}
}
