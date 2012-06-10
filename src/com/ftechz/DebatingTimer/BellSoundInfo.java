package com.ftechz.DebatingTimer;

/**
 * BellSoundInfo is a passive data class containing information about a bell sound.
 *
 * It is used to abstract away the mechanics of playing a bell sound away from other classes.
 * Playing a bell sound is a non-trivial exercise.  It's not necessarily just playing a single
 * sound file once, because the bell may be played multiple times.  For example, the end of
 * most speeches are signalled with a double bell, not a single one.  For some bell sounds,
 * a "double bell" may be a single bell repeated twice; for others, there may be another sound
 * file for a double bell that only needs to be played once.
 *
 * BellSoundInfo is handled by BellRepeater, and is a member of BellInfo.
 *
 * @author Chuan-Zheng Lee
 * @since  2012-05-30
 */
public class BellSoundInfo {
    protected int  mSoundResid   = R.raw.desk_bell; // default sound
    protected int  mTimesToPlay  = 1;               // default times to play
    protected long mRepeatPeriod = 500;

    public BellSoundInfo() {}

    public BellSoundInfo(int soundResid, int timesToPlay) {
        super();
        mSoundResid  = soundResid;
        mTimesToPlay = timesToPlay;
    }

    public void setSoundResid(int soundResid) {
        mSoundResid = soundResid;
    }

    public void setTimesToPlay(int timesToPlay) {
        mTimesToPlay = timesToPlay;
    }

    public void setRepeatPeriod(int repeatPeriod) {
        mRepeatPeriod = repeatPeriod;
    }

    public int getSoundResid() {
        return mSoundResid;
    }

    public int getTimesToPlay() {
        return mTimesToPlay;
    }

    public long getRepeatPeriod() {
        return mRepeatPeriod;
    }

    /**
     * @return true if this sound can be played, false if it is silent
     */
    public boolean isPlayable() {
        return mSoundResid != 0 && mTimesToPlay != 0;
    }
}