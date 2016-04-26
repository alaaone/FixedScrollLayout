package com.github.dubulee.fixedscrolllayout;

import android.content.Context;
import android.content.res.Resources;

/**
 * DipUtils.java
 * Created by mugku on 2015/12/28.
 * Copyright (c) mugku. All rights reserved.
 */
class DipUtils {

    private DipUtils() {}

    static int dipToPx(Context context, int dip) {
        final Resources r = context.getResources();
        final float scale = r.getDisplayMetrics().density;
        return (int) (dip * scale + .5F);
    }
}
