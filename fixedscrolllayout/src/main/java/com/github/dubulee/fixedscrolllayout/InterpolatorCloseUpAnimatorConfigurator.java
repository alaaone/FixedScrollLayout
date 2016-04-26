package com.github.dubulee.fixedscrolllayout;

import android.animation.ObjectAnimator;
import android.view.animation.Interpolator;

/**
 * InterpolatorCloseUpAnimatorConfigurator.java
 * Created by mugku on 2015/12/28.
 * Copyright (c) mugku. All rights reserved.
 */
public class InterpolatorCloseUpAnimatorConfigurator implements CloseUpAnimatorConfigurator {

    private final Interpolator mInterpolator;

    public InterpolatorCloseUpAnimatorConfigurator(Interpolator interpolator) {
        this.mInterpolator = interpolator;
    }

    @Override
    public void configure(ObjectAnimator animator) {
        animator.setInterpolator(mInterpolator);
    }
}
