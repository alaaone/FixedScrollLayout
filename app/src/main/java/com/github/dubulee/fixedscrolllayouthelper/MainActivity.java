package com.github.dubulee.fixedscrolllayouthelper;

import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.widget.RelativeLayout;

import com.github.dubulee.fixedscrolllayout.CanScrollVerticallyDelegate;
import com.github.dubulee.fixedscrolllayout.FixedScrollLayout;
import com.github.dubulee.fixedscrolllayout.OnScrollChangedListener;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends FragmentActivity {
    private static final String LAST_SCROLL_Y = "MainActivity.LastScrollY";
    private FixedScrollLayout mScrollableLayout;
    private ViewPager mViewPager;
    private TabLayout mTabLayout;
    private RelativeLayout mHeaderLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mViewPager = (ViewPager) findViewById(R.id.viewpager);
        final PagerAdapter pagerAdapter = new PagerAdapter(getSupportFragmentManager());
        pagerAdapter.addFragment(ListViewPageFragmentFixedScrollLayout.createInstance(), "List View");
        pagerAdapter.addFragment(GridViewPageFragmentFixedScrollLayout.createInstance(), "Grid View");
        mViewPager.setAdapter(pagerAdapter);

        mTabLayout = (TabLayout) findViewById(R.id.tab_layout);
        mTabLayout.setupWithViewPager(mViewPager);

        mHeaderLayout = (RelativeLayout) findViewById(R.id.layout_view);

        mScrollableLayout = (FixedScrollLayout) findViewById(R.id.scrollable_layout);
        mScrollableLayout.setDraggableView(mTabLayout);

        mScrollableLayout.setCanScrollVerticallyDelegate(new CanScrollVerticallyDelegate() {
            @Override
            public boolean canScrollVertically(int direction) {
                return pagerAdapter.canScrollVertically(mViewPager.getCurrentItem(), direction);
            }
        });

        mScrollableLayout.setOnScrollChangedListener(new OnScrollChangedListener() {
            @Override
            public void onScrollChanged(int y, int oldY, int maxY) {

                final float tabsTranslationY;
                if (y < maxY) {
                    tabsTranslationY = .0F;
                } else {
                    tabsTranslationY = y - maxY;
                }

                mTabLayout.setTranslationY(tabsTranslationY);

                mHeaderLayout.setTranslationY(y / 2);
            }
        });

        if (savedInstanceState != null) {
            final int y = savedInstanceState.getInt(LAST_SCROLL_Y);
            mScrollableLayout.post(new Runnable() {
                @Override
                public void run() {
                    mScrollableLayout.scrollTo(0, y);
                }
            });
        }
    }

    static class PagerAdapter extends FragmentPagerAdapter {
        private final List<FixedScrollLayoutBaseFragment> fragmentList = new ArrayList<>();
        private final List<String> fragmentTitleList = new ArrayList<>();

        public PagerAdapter(FragmentManager fragmentManager) {
            super(fragmentManager);
        }

        public void addFragment(FixedScrollLayoutBaseFragment fragment, String title) {
            fragmentList.add(fragment);
            fragmentTitleList.add(title);
        }

        @Override
        public FixedScrollLayoutBaseFragment getItem(int position) {
            return fragmentList.get(position);
        }

        @Override
        public int getCount() {
            return fragmentList.size();
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return fragmentTitleList.get(position);
        }

        boolean canScrollVertically(int position, int direction) {
            return getItem(position).canScrollVertically(direction);
        }
    }
}
