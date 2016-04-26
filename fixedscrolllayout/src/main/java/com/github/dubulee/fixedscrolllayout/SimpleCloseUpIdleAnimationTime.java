package com.github.dubulee.fixedscrolllayout;


public class SimpleCloseUpIdleAnimationTime implements CloseUpIdleAnimationTime {

    private final long mDuration;

    public SimpleCloseUpIdleAnimationTime(long duration) {
        this.mDuration = duration;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long compute(FixedScrollLayout layout, int nowY, int endY, int maxY) {
        return mDuration;
    }

    public long getDuration() {
        return mDuration;
    }
}
