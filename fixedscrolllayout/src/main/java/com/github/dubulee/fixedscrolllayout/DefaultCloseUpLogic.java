package com.github.dubulee.fixedscrolllayout;

/**
 * DefaultCloseUpLogic.java
 * Created by mugku on 2015/12/28.
 * Copyright (c) mugku. All rights reserved.
 */
public class DefaultCloseUpLogic implements CloseUpLogic {

    @Override
    public int getFlingFinalY(FixedScrollLayout layout, boolean isScrollingBottom, int nowY, int suggestedY, int maxY) {
        return isScrollingBottom ? 0 : maxY;
    }

    @Override
    public int getIdleFinalY(FixedScrollLayout layout, int nowY, int maxY) {
        final boolean shouldScrollToTop = nowY < (maxY / 2);
        return shouldScrollToTop ? 0 : maxY;
    }
}
