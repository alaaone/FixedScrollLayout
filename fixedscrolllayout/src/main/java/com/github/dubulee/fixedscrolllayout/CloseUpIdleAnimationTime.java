package com.github.dubulee.fixedscrolllayout;

/**
 * CloseUpIdleAnimationTime.java
 * Created by DUBULEE on 2015/12/28.
 * Copyright (c) DUBULEE. All rights reserved.
 */
public interface CloseUpIdleAnimationTime {
    long compute(FixedScrollLayout layout, int nowY, int endY, int maxY);
}
