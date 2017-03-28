package de.grundid.plusrad.recording;

import android.os.Binder;

public class RecordingServiceBinder extends Binder implements RecordingService.IRecordService {

	private RecordingService recordingService;

	public RecordingServiceBinder(RecordingService recordingService) {
		this.recordingService = recordingService;
	}

	public int getState() {
		return recordingService.getState();
	}

	public CurrentTrip startRecording() {
		return recordingService.startRecording();
	}

	public void cancelRecording() {
		recordingService.cancelRecording();
	}

	public void pauseRecording() {
		recordingService.pauseRecording();
	}

	public void resumeRecording() {
		recordingService.resumeRecording();
	}

	public CurrentTrip finishRecording() {
		return recordingService.finishRecording();
	}

	public CurrentTrip getCurrentTrip() {
		return recordingService.getTrip();
	}

/*	public void reset() {
		recordingService.setState(RecordingService.STATE_IDLE);
	}*/

	public void setListener(RecordingService.UpdateListener updateListener) {
		recordingService.setUpdateListener(updateListener);
	}
}
