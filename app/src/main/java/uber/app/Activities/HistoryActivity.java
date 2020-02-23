package uber.app.Activities;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.viewpager.widget.ViewPager;
import androidx.fragment.app.FragmentPagerAdapter;

import android.os.Bundle;

import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import uber.app.Fragments.HistoryFragment;
import uber.app.R;

import static uber.app.Helpers.FirebaseHelper.mCustomerHistoryDbRef;
import static uber.app.Helpers.FirebaseHelper.mDriverHistoryDbRef;

public class HistoryActivity extends AppCompatActivity {
    private static final String TAG = "HistoryActivity";

    @BindView( R.id.toolbar_history )
    Toolbar mToolbar;
    @BindView( R.id.viewPager_history )
    ViewPager mViewPager;
    @BindView( R.id.tabs_history )
    TabLayout mTabLayout;

    @Override
    protected void onCreate( @Nullable Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_history );

        ButterKnife.bind( this );

        initToolbar();
        initViewPager();
        initTabLayout();
    }

    private void initTabLayout() {
        mTabLayout.setupWithViewPager( mViewPager );
        mTabLayout.addOnTabSelectedListener( new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected( TabLayout.Tab tab ) {
                mViewPager.setCurrentItem( tab.getPosition() );
                switch ( tab.getPosition() ) {
                    case 0:

                        break;
                    case 1:

                        break;
                }
            }

            @Override
            public void onTabUnselected( TabLayout.Tab tab ) {
            }

            @Override
            public void onTabReselected( TabLayout.Tab tab ) {
            }
        } );
    }

    private void initViewPager() {
        ViewPagerAdapter viewPagerAdapter = new ViewPagerAdapter( getSupportFragmentManager() );
        HistoryFragment customerHistoryFragment = new HistoryFragment( mCustomerHistoryDbRef );
        HistoryFragment driverHistoryFragment = new HistoryFragment( mDriverHistoryDbRef );
        viewPagerAdapter.addFrag( customerHistoryFragment, "Customer" );
        viewPagerAdapter.addFrag( driverHistoryFragment, "Driver" );
        mViewPager.setAdapter( viewPagerAdapter );
    }

    private void initToolbar() {
        setSupportActionBar( mToolbar );
        if ( getSupportActionBar() != null ) {
            getSupportActionBar().setDisplayHomeAsUpEnabled( true );
            getSupportActionBar().setTitle( R.string.history_menu_item );
        }
    }

    private static class ViewPagerAdapter extends FragmentPagerAdapter {
        private final List<Fragment> mFragmentList = new ArrayList<>();
        private final List<String> mFragmentTitleList = new ArrayList<>();

        public ViewPagerAdapter( FragmentManager manager ) {
            super( manager );
        }

        @Override
        public Fragment getItem( int position ) {
            return mFragmentList.get( position );
        }

        @Override
        public int getCount() {
            return mFragmentList.size();
        }

        public void addFrag( Fragment fragment, String title ) {
            mFragmentList.add( fragment );
            mFragmentTitleList.add( title );
        }

        @Override
        public CharSequence getPageTitle( int position ) {
            return mFragmentTitleList.get( position );
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return super.onSupportNavigateUp();
    }
}
