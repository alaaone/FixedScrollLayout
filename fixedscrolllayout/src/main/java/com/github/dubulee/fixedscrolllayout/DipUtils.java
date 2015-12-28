package com.github.dubulee.fixedscrolllayout;

import android.content.Context;
import android.content.res.Resources;

/**
 * DipUtils.java
 * Created by DUBULEE on 2015/12/28.
 * Copyright (c) DUBULEE. All rights reserved.
 */
class DipUtils {

    private DipUtils() {}

    static int dipToPx(Context context, int dip) {
        final Resources r = context.getResources();
        final float scale = r.getDisplayMetrics().density;
        return (int) (dip * scale + .5F);
    }
}
