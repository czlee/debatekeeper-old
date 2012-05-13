package com.ftechz.DebatingTimer;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.ftechz.DebatingTimer.AlarmChain.Event;

/**
 * DebatingActivity The first Activity shown when application is started... for
 * now
 *
 */
public class DebatingActivity extends Activity {
	private TextView mStateText;
	private TextView mStageText;
	private TextView mCurrentTimeText;
	private TextView mNextTimeText;
	private TextView mFinalTimeText;

	// The buttons are allocated as follows:
	// When at startOfSpeaker: [Start] [Next Speaker]
	// When running:           [Stop]
	// When stopped by user:   [Resume] [Restart] [Next Speaker]
	// When stopped by alarm:  [Resume]
	private Button leftControlButton;
	private Button centreControlButton;
	private Button rightControlButton;

	private Debate mDebate;

	static final int DIALOG_SETTINGS_NOT_IMPLEMENTED = 0;

	// TODO This is a temporary mechanism to switch between real-world and test modes
	// (It just changes the speech times.)
	private int mTestMode = 0;

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.debating_activity_menu, menu);
	    return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    switch (item.getItemId()) {
	    case R.id.restartDebate:
	        mDebate.resetDebate();
	        updateGui();
	        return true;
	    case R.id.switchMode:
	        mDebate.release();
	        mDebate = null;
	        mDebate = mBinder.createDebate();
	        mTestMode = (mTestMode == 0) ? 1 : 0;
	        setupDefaultDebate(mDebate, mTestMode);
	        mDebate.resetSpeaker();
	        updateGui();
	        return true;
	    case R.id.settings:
	        showDialog(DIALOG_SETTINGS_NOT_IMPLEMENTED);
	        return true;
        default:
            return super.onOptionsItemSelected(item);
	    }
	}

	@Override
	protected Dialog onCreateDialog(int id) {
        Dialog dialog;
        switch (id) {
        case DIALOG_SETTINGS_NOT_IMPLEMENTED:
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Sorry!")
                   .setMessage("I haven't implemented the settings part yet.")
                   .setCancelable(true)
                   .setNegativeButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    });
            dialog = builder.create();
            break;
        default:
            dialog = null;
        }
        return dialog;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.debate_activity);

		mStateText = (TextView) findViewById(R.id.stateText);
		mStageText = (TextView) findViewById(R.id.titleText);
		mCurrentTimeText = (TextView) findViewById(R.id.currentTime);
		mNextTimeText = (TextView) findViewById(R.id.nextTime);
		mFinalTimeText = (TextView) findViewById(R.id.finalTime);
		leftControlButton = (Button) findViewById(R.id.leftControlButton);
		centreControlButton = (Button) findViewById(R.id.centreControlButton);
		rightControlButton = (Button) findViewById(R.id.rightControlButton);

		leftControlButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View pV) {
				switch (mDebate.getDebateStatus()) {
				case StartOfSpeaker:
					mDebate.start();
					break;
				case TimerRunning:
					mDebate.stop();
					break;
				case TimerStoppedByAlarm:
				case TimerStoppedByUser:
					mDebate.resume();
					break;
				default:
					break;
				}
				updateGui();
			}
		});

		centreControlButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View pV) {
                switch (mDebate.getDebateStatus()) {
                case TimerStoppedByUser:
                    mDebate.resetSpeaker();
                    break;
                default:
                    break;
                }
                updateGui();
            }
        });

		rightControlButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View pV) {
			    switch (mDebate.getDebateStatus()) {
			    case StartOfSpeaker:
			    case TimerStoppedByUser:
			        mDebate.prepareNextSpeaker();
	                break;
                default:
                    break;
			    }
                updateGui();
            }
		});

		Intent intent = new Intent(this, DebatingTimerService.class);
		startService(intent);

		bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
	}

	// Updates the buttons according to the current status of the debate
	private void updateButtons() {
		switch (mDebate.getDebateStatus()) {
		case StartOfSpeaker:
		    setButtons(R.string.startTimer, R.string.nullButtonText, R.string.nextSpeaker);
			break;
		case TimerRunning:
		    setButtons(R.string.stopTimer, R.string.nullButtonText, R.string.nullButtonText);
			break;
		case TimerStoppedByAlarm:
		    setButtons(R.string.resumeTimerAfterAlarm, R.string.nullButtonText, R.string.nullButtonText);
			break;
		case TimerStoppedByUser:
		    setButtons(R.string.resumeTimerAfterUserStop, R.string.resetTimer, R.string.nextSpeaker);
			break;
		case EndOfDebate:
			break;
		default:
			break;
		}
	}

	// Sets the text and visibility of all buttons
	private void setButtons(int leftResid, int centreResid, int rightResid) {
	    setButton(leftControlButton, leftResid);
	    setButton(centreControlButton, centreResid);
	    setButton(rightControlButton, rightResid);
	}

	// Sets the text and visibility of a single button
	private void setButton(Button button, int resid) {
	    button.setText(resid);
	    int visibility = (resid == R.string.nullButtonText) ? View.GONE : View.VISIBLE;
	    button.setVisibility(visibility);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		unbindService(mConnection);
		Intent intent = new Intent(this, DebatingTimerService.class);
		stopService(intent);
	}

	private String secsToMinuteSecText(long time) {
		return String.format("%02d:%02d", time / 60, time % 60);
	}

	public void updateGui() {
		if (mDebate != null) {
			mStateText.setText(mDebate.getStageStateText());
			mStageText.setText(mDebate.getStageName());
			mStateText.setBackgroundColor(mDebate.getStageBackgroundColor());
			mStageText.setBackgroundColor(mDebate.getStageBackgroundColor());
			mCurrentTimeText.setText(secsToMinuteSecText(mDebate.getStageCurrentTime()));
			mNextTimeText.setText(String.format(
		        this.getString(R.string.nextBell),
		        secsToMinuteSecText(mDebate.getStageNextTime())
	        ));
			mFinalTimeText.setText(String.format(
                this.getString(R.string.speechLength),
                secsToMinuteSecText(mDebate.getStageFinalTime())
            ));

			updateButtons();
		}
	}

	// Second tick broadcast
	private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			updateGui();
		}
	};

	@Override
	public void onResume() {
		super.onResume();
		registerReceiver(broadcastReceiver, new IntentFilter(
				DebatingTimerService.BROADCAST_ACTION));
	}

	@Override
	public void onPause() {
		super.onPause();
		unregisterReceiver(broadcastReceiver);
	}

	private DebatingTimerService.DebatingTimerServiceBinder mBinder;

	/** Defines callbacks for service binding, passed to bindService() */
	private final ServiceConnection mConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName className, IBinder service) {

			mBinder = (DebatingTimerService.DebatingTimerServiceBinder) service;
			mDebate = mBinder.getDebate();
			if (mDebate == null) {
				mDebate = mBinder.createDebate();
				setupDefaultDebate(mDebate, mTestMode);
				mDebate.resetSpeaker();
			}
		}

		@Override
		public void onServiceDisconnected(ComponentName arg0) {
			mDebate = null;
		}
	};

	// TODO: Remove this from ConfigActivity (it is called setupDebate() there)
	public void setupDefaultDebate(Debate debate, int testMode) {
	    Event[] prepAlerts;
	    Event[] substantiveSpeechAlerts;
	    Event[] replySpeechAlerts;

	    switch (testMode) {
        case 1:
            // This is a special test mode
            prepAlerts = new AlarmChain.Event[] {
                    new SpeakerTimer.Event(5, 1, "Choose moot"),
                    new SpeakerTimer.Event(10, 1, "Choose side"),
                    new SpeakerTimer.Event(15, 2, "Prepare debate") };
            substantiveSpeechAlerts = new AlarmChain.Event[] {
                    new SpeakerTimer.Event(5, 1, "Points of information allowed", 0x7200ff00),
                    new SpeakerTimer.Event(10, 1, "Warning bell rung", 0x72ff9900),
                    new SpeakerTimer.Event(15, 2, "Overtime", 0x72ff0000),
                    new SpeakerTimer.RepeatedEvent(20, 3, 3) };

            replySpeechAlerts = new AlarmChain.Event[] {
                    new SpeakerTimer.Event(3, 1, "Warning bell rung", 0x72ff9900),
                    new SpeakerTimer.Event(6, 2, "Overtime", 0x72ff0000),
                    new SpeakerTimer.RepeatedEvent(9, 3, 3) };
            // Add in the alarm sets
            debate.addAlarmSet("prep", prepAlerts, 15);
            debate.addAlarmSet("substantiveSpeech", substantiveSpeechAlerts, 15);
            debate.addAlarmSet("replySpeech", replySpeechAlerts, 6);
            break;
        case 0:
        default:
            // prepAlerts isn't actually used (it's only used in NZ Easters;
            // the format described below is Thropy).
            prepAlerts = new AlarmChain.Event[] {
                    new SpeakerTimer.Event(1*60, 1, "Choose moot"),
                    new SpeakerTimer.Event(2*60, 1, "Choose side"),
                    new SpeakerTimer.Event(7*60, 2, "Prepare debate") };
            substantiveSpeechAlerts = new AlarmChain.Event[] {
                    new SpeakerTimer.Event(1*60, 1, "Points of information allowed", 0x7200ff00),
                    new SpeakerTimer.Event(5*60, 1, "Warning bell rung", 0x72ff9900),
                    new SpeakerTimer.Event(6*60, 2, "Overtime", 0x72ff0000),
                    new SpeakerTimer.RepeatedEvent(6*60+20, 20, 3) };

            replySpeechAlerts = new AlarmChain.Event[] {
                    new SpeakerTimer.Event(2*60, 1, "Warning bell rung", 0x72ff9900),
                    new SpeakerTimer.Event(3*60, 2, "Overtime", 0x72ff0000),
                    new SpeakerTimer.RepeatedEvent(2*60+20, 20, 3) };
            // Add in the alarm sets
            debate.addAlarmSet("prep", prepAlerts, 7*60);
            debate.addAlarmSet("substantiveSpeech", substantiveSpeechAlerts, 6*60);
            debate.addAlarmSet("replySpeech", replySpeechAlerts, 3*60);
            break;
	    }


		// Set up speakers
		Team team1 = new Team();
		team1.addMember(new Speaker("1st Affirmative"), true);
		team1.addMember(new Speaker("2nd Affirmative"), false);
		team1.addMember(new Speaker("3rd Affirmative"), false);

		Team team2 = new Team();
		team2.addMember(new Speaker("1st Negative"), true);
		team2.addMember(new Speaker("2nd Negative"), false);
		team2.addMember(new Speaker("3rd Negative"), false);

		int team1Index = debate.addTeam(team1);
		int team2Index = debate.addTeam(team2);

		debate.setSide(team1Index, TeamsManager.SpeakerSide.Affirmative);
		debate.setSide(team2Index, TeamsManager.SpeakerSide.Negative);

		// Add in the stages
		// debate.addStage(new PrepTimer("Preparation"), "prep");
		debate.addStage(new SpeakerTimer("1st Affirmative",
				TeamsManager.SpeakerSide.Affirmative, 1), "substantiveSpeech");
		debate.addStage(new SpeakerTimer("1st Negative",
				TeamsManager.SpeakerSide.Negative, 1), "substantiveSpeech");
		debate.addStage(new SpeakerTimer("2nd Affirmative",
				TeamsManager.SpeakerSide.Affirmative, 2), "substantiveSpeech");
		debate.addStage(new SpeakerTimer("2nd Negative",
				TeamsManager.SpeakerSide.Negative, 2), "substantiveSpeech");
		debate.addStage(new SpeakerTimer("3nd Affirmative",
				TeamsManager.SpeakerSide.Affirmative, 3), "substantiveSpeech");
		debate.addStage(new SpeakerTimer("3nd Negative",
				TeamsManager.SpeakerSide.Negative, 3), "substantiveSpeech");
		debate.addStage(new SpeakerTimer("Negative Leader's Reply",
				TeamsManager.SpeakerSide.Negative, 0), "replySpeech");
		debate.addStage(new SpeakerTimer("Affirmative Leader's Reply",
				TeamsManager.SpeakerSide.Affirmative, 0), "replySpeech");
	}

}
