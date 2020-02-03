package uber.app.Fragments;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Handler;
import android.os.Looper;
import android.view.View;

import androidx.appcompat.widget.Toolbar;

import com.mikepenz.materialdrawer.AccountHeader;
import com.mikepenz.materialdrawer.AccountHeaderBuilder;
import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.DrawerBuilder;
import com.mikepenz.materialdrawer.model.DividerDrawerItem;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.ProfileDrawerItem;
import com.mikepenz.materialdrawer.model.ToggleDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IProfile;

import uber.app.Activities.MapActivity;
import uber.app.Activities.HistoryActivity;
import uber.app.Activities.ProfileActivity;
import uber.app.R;
import uber.app.SharedPref;

import static uber.app.Helpers.FirebaseHelper.logoutUser;
import static uber.app.Helpers.FirebaseHelper.mFirebaseUser;
import static uber.app.Helpers.FirebaseHelper.mUser;

public class LeftDrawer implements Drawer.OnDrawerItemClickListener {
    private final int HISTORY = 1;
    private final int ISDRIVER = 2;
    private final int LOGOUT = 3;

    private MapActivity mMapActivity;
    private Toolbar mToolbar;

    private Drawer mDrawer;
    private DrawerBuilder drawerBuilder;
    private AccountHeader mDrawerHeader;

    private ProfileDrawerItem userProfile;

    private PrimaryDrawerItem historyDrawerItem;
    private ToggleDrawerItem isDriverToggleDrawerItem;
    private PrimaryDrawerItem logoutDrawerItem;

    public Drawer getDrawer() { return mDrawer; }

    public AccountHeader getDrawerHeader() { return mDrawerHeader; }

    public ProfileDrawerItem getUserProfile() { return userProfile; }

    public PrimaryDrawerItem getHistoryDrawerItem() {
        return historyDrawerItem;
    }

    public ToggleDrawerItem getIsDriverToggleDrawerItem() {
        return isDriverToggleDrawerItem;
    }

    public PrimaryDrawerItem getLogoutDrawerItem() {
        return logoutDrawerItem;
    }

    public LeftDrawer( MapActivity mMapActivity, Toolbar mToolbar ) {
        this.mMapActivity = mMapActivity;
        this.mToolbar = mToolbar;
    }

    public void initDrawer() {
        String mFirebaseUserName = mFirebaseUser.getDisplayName();
        String mFirebaseUserEmail = mFirebaseUser.getEmail();
        Drawable mUserIcon = mMapActivity.getResources().getDrawable( R.drawable.profile_image, null );

        //profile
        userProfile = new ProfileDrawerItem()
                .withName( mFirebaseUserName )
                .withEmail( mFirebaseUserEmail )
                .withIdentifier( 1 )
                .withIcon( mUserIcon );
         /*
            Drawer items
         */
        //history item
        historyDrawerItem = new PrimaryDrawerItem()
                .withName( R.string.history_menu_item )
                .withIcon( R.drawable.ic_history_black_24dp )
                .withIdentifier( HISTORY )
                .withSelectable( false );
        //toggle button to change driver to customer
        isDriverToggleDrawerItem = new ToggleDrawerItem()
                .withName( R.string.change_to_customer )
                .withIcon( R.drawable.ic_person_black_24dp )
                .withIdentifier( ISDRIVER )
                .withSelectable( false );
        isDriverToggleDrawerItem.setToggleEnabled( false );
        isDriverToggleDrawerItem.setChecked( true ); //logged driver without customer is just customer
        //logout item
        logoutDrawerItem = new PrimaryDrawerItem()
                .withName( R.string.logout )
                .withIcon( R.drawable.ic_logout_black_24dp )
                .withIdentifier( LOGOUT )
                .withSelectable( false );

        //drawer header
        mDrawerHeader = new AccountHeaderBuilder()
                .withActivity( mMapActivity )
                .withHeaderBackground( R.drawable.header_wallpaper )
                .addProfiles( userProfile )
                .withSelectionListEnabledForSingleProfile( false )
                .withOnAccountHeaderProfileImageListener( new AccountHeader.OnAccountHeaderProfileImageListener() {
                    @Override
                    public boolean onProfileImageClick( View view, IProfile profile, boolean current ) {
                        Intent intent = new Intent( mMapActivity, ProfileActivity.class );
                        mMapActivity.startActivity( intent );
//                        Log.e( "ASD", "onProfileImageClick: " + view.getId()  );
                        return false;
                    }

                    @Override
                    public boolean onProfileImageLongClick( View view, IProfile profile, boolean current ) {
                        return false;
                    }
                } )
                .build();

        //drawer content
        drawerBuilder = new DrawerBuilder()
                .withActivity( mMapActivity )
                .withToolbar( mToolbar )
                .withTranslucentStatusBar( true )
                .withFullscreen( true )
                .withAccountHeader( mDrawerHeader )
                .withSelectedItem( -1 )
                .withOnDrawerItemClickListener( this );

        drawerBuilder.addDrawerItems( historyDrawerItem );
        drawerBuilder.addDrawerItems( isDriverToggleDrawerItem );
        drawerBuilder.addDrawerItems( new DividerDrawerItem() );
        drawerBuilder.addDrawerItems( logoutDrawerItem );

        mDrawer = drawerBuilder.build();
    }

    @Override
    public boolean onItemClick( View view, int position, IDrawerItem drawerItem ) {
        int itemId = ( int ) drawerItem.getIdentifier();

        switch ( itemId ) {
            case HISTORY:
                Intent historyIntent = new Intent( mMapActivity, HistoryActivity.class );
                mMapActivity.startActivity( historyIntent );
                break;
            case LOGOUT:
                logoutUser( mMapActivity );
                break;
            case ISDRIVER:
                //if user has no connection button is visible by customer
                if( mUser == null || !mUser.isDriver() ){
                    mMapActivity.showMessage( mMapActivity.getResources().getString( R.string.driver_toggle_btn_not_allowed ) );
                    return false;
                }

                if( mMapActivity.getDriverHelper() == null || mMapActivity.getDriverHelper().getCustomerId() == null ) {
                    if ( SharedPref.getBool( "isDriver" ) ) {
                        SharedPref.setBool( "isDriver", false );
                        mMapActivity.deleteLocationFromDatabase();

                        isDriverToggleDrawerItem.setChecked( true );
                        mMapActivity.changeButtonVisibility( mMapActivity.mRequestUberButton, true );

                    } else {
                        SharedPref.setBool( "isDriver", true );

                        Location driverLocation = mMapActivity.getLastLocation();
                        double driverLong = driverLocation.getLongitude();
                        double driverLat = driverLocation.getLatitude();
                        mMapActivity.addLocationToDatabase( driverLong, driverLat );

                        isDriverToggleDrawerItem.setChecked( false );
                        mMapActivity.changeButtonVisibility( mMapActivity.mRequestUberButton, false );
                    }
                }else{
                    isDriverToggleDrawerItem.setChecked( false );
                    isDriverToggleDrawerItem.setToggleEnabled( false );
                    mMapActivity.showMessage( mMapActivity.getResources().getString( R.string.cannot_switch_to_customer ) );
                }
                break;
        }

        return false;
    }

    public void disableDriverToggleButtonState() {
        SharedPref.setBool( "isDriver", true );
        new Handler( Looper.getMainLooper() ).post( new Runnable() {
            @Override
            public void run() {
                isDriverToggleDrawerItem.setChecked( false );
            }
        } );
    }

    public void enableDriverToggleButtonState() {
        SharedPref.setBool( "isDriver", false ); //false
        new Handler( Looper.getMainLooper() ).post( new Runnable() {
            @Override
            public void run() {
                isDriverToggleDrawerItem.setChecked( true );
            }
        } );
    }

    public void removeDriverToggle() {
        new Handler( Looper.getMainLooper() ).post( new Runnable() {
            @Override
            public void run() {
                mDrawer.removeItem( ISDRIVER );
            }
        } );
    }

    public void addItem( IDrawerItem drawerItem ) {
        drawerBuilder.addDrawerItems( drawerItem );
    }
}
