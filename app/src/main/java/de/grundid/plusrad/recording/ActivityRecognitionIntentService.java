package de.grundid.plusrad.recording;

import android.app.IntentService;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;

/**
 * Service that receives ActivityRecognition updates. It receives
 * updates in the background, even if the main Activity is not visible.
 */
public class ActivityRecognitionIntentService extends IntentService {

	private Intent broadcastIntent;
	private LocalBroadcastManager broadcastManager;

	public ActivityRecognitionIntentService() {
		// Set the label for the service's background thread
		super("ActivityRecognitionIntentService");
		broadcastIntent = new Intent("activityUpdate");
	}

	@Override
	public void onCreate() {
		super.onCreate();
		broadcastManager = LocalBroadcastManager.getInstance(getApplicationContext());
		Log.i("PRAD", "create ActivityRecognitionIntentService");
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		Log.i("PRAD", "destroy ActivityRecognitionIntentService");
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		if (ActivityRecognitionResult.hasResult(intent)) {
			ActivityRecognitionResult result =
					ActivityRecognitionResult.extractResult(intent);
			DetectedActivity mostProbableActivity =
					result.getMostProbableActivity();
			int confidence = mostProbableActivity.getConfidence();
			int activityType = mostProbableActivity.getType();
			String activityName = getNameFromType(activityType);
			broadcastIntent.putExtra("activityName", activityName);
			broadcastIntent.putExtra("confidence", confidence);
			broadcastIntent.putExtra("activityType", activityType);
			Log.i("PRAD", "detected activity: " + activityName + " with confidence " + Integer.toString(confidence));
			broadcastManager.sendBroadcast(broadcastIntent);
		}
	}

	private String getNameFromType(int activityType) {
		switch (activityType) {
			case DetectedActivity.IN_VEHICLE:
				return "in_vehicle";
			case DetectedActivity.ON_BICYCLE:
				return "on_bicycle";
			case DetectedActivity.ON_FOOT:
				return "on_foot";
			case DetectedActivity.STILL:
				return "still";
			case DetectedActivity.UNKNOWN:
				return "unknown";
			case DetectedActivity.TILTING:
				return "tilting";
		}
		return "unknown";
	}
}