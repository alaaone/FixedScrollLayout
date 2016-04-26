package com.github.dubulee.fixedscrolllayout;

/**
 * CloseUpLogic.java
 * Created by mugku on 2015/12/28.
 * Copyright (c) mugku. All rights reserved.
 */
public interface CloseUpLogic {
    int getFlingFinalY(FixedScrollLayout layout, boolean isScrollingBottom, int nowY, int suggestedY, int maxY);

    int getIdleFinalY(FixedScrollLayout layout, int nowY, int maxY);
}
