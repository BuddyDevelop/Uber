package uber.app.Activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import uber.app.Helpers.CheckNetwork;
import uber.app.Helpers.CustomerHelper;
import uber.app.Helpers.DriverHelper;
import uber.app.Helpers.FirebaseHelper;
import uber.app.Fragments.LeftDrawer;
import uber.app.Models.User;
import uber.app.R;
import uber.app.Routes;
import uber.app.SharedPref;
import uber.app.Util;

import static uber.app.Helpers.FirebaseHelper.addAvailableDriverLocationToDB;
import static uber.app.Helpers.FirebaseHelper.addCustomerLocationToDB;
import static uber.app.Helpers.FirebaseHelper.addWorkingDriverLocationToDB;
import static uber.app.Helpers.FirebaseHelper.deleteAvailableDriverLocationFromDB;
import static uber.app.Helpers.FirebaseHelper.deleteCustomerFromDb;
import static uber.app.Helpers.FirebaseHelper.deleteCustomerRequestFromDB;
import static uber.app.Helpers.FirebaseHelper.deleteDriverFromDb;
import static uber.app.Helpers.FirebaseHelper.deleteWorkingDriverLocationFromDB;
import static uber.app.Helpers.FirebaseHelper.getFromFirebase;
import static uber.app.Helpers.FirebaseHelper.mAvailableDriversDbRef;
import static uber.app.Helpers.FirebaseHelper.mCustomerDestinationDbRef;
import static uber.app.Helpers.FirebaseHelper.mDriversDbRef;
import static uber.app.Helpers.FirebaseHelper.mUser;
import static uber.app.Helpers.FirebaseHelper.mUsersDbRef;
import static uber.app.Helpers.FirebaseHelper.mWorkingDriversDbRef;
import static uber.app.Helpers.FirebaseHelper.removeCustomerDestinationFromDb;
import static uber.app.Helpers.FirebaseHelper.userIdString;
import static uber.app.SharedPref.DEFAULT_DOUBLE;
import static uber.app.Util.changeMapsMyLocationButton;
import static uber.app.Util.showRelativeLayout;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback {
    private static final int REQUEST_GPS_PERMISSIONS = 1;
    private static final float MINIMUM_DISTANCE_BETWEEN_MAP_UPDATES = 10;
    private static final long MINIMUM_TIME_INTERVAL_BETWEEN_MAP_UPDATES = 10 * 1000; //10 sec
    private static final long MAXIMUM_TIME_INTERVAL_BETWEEN_MAP_UPDATES = 20 * 1000; //20 sec
    private static final float MAP_ZOOM = 16;

    private FusedLocationProviderClient mFusedLocationProviderClient;
    public LocationRequest mLocationRequest;
    public Location mLastLocation;

    private CustomerHelper mCustomerHelper;
    private DriverHelper mDriverHelper;
    public LeftDrawer leftDrawer;

    private SupportMapFragment mapFragment;
    private GoogleMap mMap;
    private Marker locationMarker;

    private Routes mRoutes;

    @BindView( R.id.request_uber )
    public MaterialButton mRequestUberButton;
    @BindView( R.id.toolbar )
    public Toolbar mToolbar;
    @BindView( R.id.info )
    LinearLayout mInfo;
    @BindView( R.id.user_name_value )
    TextView mUserName;
    @BindView( R.id.user_phone_number_value )
    TextView mUserPhoneNumber;
    @BindView( R.id.user_profile_image_map )
    ImageView mUserImage;
    @BindView( R.id.autocomplete_relative_layout )
    public RelativeLayout relativeLayout;
    @BindView( R.id.user_destination_value )
    TextView mUserDestination;
    @BindView( R.id.pickup_customer_btn )
    public MaterialButton mPickupCustomerBtn;

    public CustomerHelper getCustomerHelper() { return mCustomerHelper; }
    public DriverHelper getDriverHelper() { return mDriverHelper; }
    public Location getLastLocation() { return mLastLocation; }
    public GoogleMap getGoogleMap() {
        return mMap;
    }
    public Routes getRoutes() { return mRoutes; }

    //callback to refresh location
    public LocationCallback mLocationCallback = new LocationCallback() {
        @Override
        public void onLocationResult( LocationResult locationResult ) {
            for ( Location location : locationResult.getLocations() ) {
                if ( getApplicationContext() != null ) {

                    if ( location == null )
                        return;

                    mLastLocation = location;
                    LatLng latLng = new LatLng( location.getLatitude(), location.getLongitude() );
                    mMap.moveCamera( CameraUpdateFactory.newLatLngZoom( latLng, MAP_ZOOM ) );

                    //update working driver or available driver
                    if( mUser != null && mUser.isDriver() && mDriverHelper != null && mDriverHelper.getCustomerId() != null )
                        addWorkingDriverLocationToDB( userIdString, latLng.latitude, latLng.longitude );
                    else if( mUser != null && mUser.isDriver() && SharedPref.getBool( "isDriver" ) ) //if driver is not a customer
                        addAvailableDriverLocationToDB( userIdString, latLng.latitude, latLng.longitude );

                }
            }
        }
    };

    private void listenForCustomerRequest(){
        mWorkingDriversDbRef.child( userIdString ).addValueEventListener( new ValueEventListener() {
            @Override
            public void onDataChange( @NonNull DataSnapshot dataSnapshot ) {
                if( dataSnapshot.exists() ){

                    mDriversDbRef.child( userIdString ).addListenerForSingleValueEvent( new ValueEventListener() {
                        @Override
                        public void onDataChange( @NonNull DataSnapshot dataSnapshot ) {
                            if( dataSnapshot.exists() ) {
                                if ( mDriverHelper == null )
                                    mDriverHelper = new DriverHelper( MapActivity.this );

                                Map<String, Object> dataMap =  ( Map<String, Object> ) dataSnapshot.getValue();

                                if( dataMap.get( "customerId" ) != null )
                                    mDriverHelper.setCustomerId( dataMap.get( "customerId" ).toString() );
                                if( dataMap.get( "l" ) != null )
                                    mDriverHelper.setCustomerLatLng( Util.getLatLng( ( List<Object> ) dataMap.get( "l" ) ) );                            }
                        }

                        @Override
                        public void onCancelled( @NonNull DatabaseError databaseError ) { }
                    } );
                }
            }

            @Override
            public void onCancelled( @NonNull DatabaseError databaseError ) { }
        } );
    }

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_map );
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mapFragment = ( SupportMapFragment ) getSupportFragmentManager()
                .findFragmentById( R.id.map );
        mapFragment.getMapAsync( this );

        //initialize shared pref if there was no instance before
        SharedPref.initialize( getApplicationContext() );

        //get user data from firebase and save in shared pref if user is driver
        FirebaseHelper.getUserData( this );

        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient( this );

        //bind views
        ButterKnife.bind( this );

        //set toolbar
        setToolbar();
//        //change location of my location button on map
        //set left drawer
        leftDrawer = new LeftDrawer( this, mToolbar );
        leftDrawer.initDrawer();

        //set customer or driver
        if ( mUser != null && mUser.isDriver() ) {
            mDriverHelper = new DriverHelper( this );
            //get driver's customer if any
            mDriverHelper.getAssignedCustomer();
        } else {
            mCustomerHelper = new CustomerHelper( this );
            //if user is not a driver create button to request for a ride
            initializeUberRequestBtn();
            if ( userIdString != null && !userIdString.isEmpty() )
                mCustomerHelper.hasCustomerRequest( userIdString );
        }

        updateUIOnDataReceived();
        //google autocomplete API
        initializeAutocompleteFragment();
    }

    private void updateUIOnDataReceived() {
        getFromFirebase( () -> {
            if ( mUser != null && mUser.isDriver() ) {

                if( mDriverHelper == null )
                    mDriverHelper = new DriverHelper( MapActivity.this );

                //get driver's customer if any
                mDriverHelper.getAssignedCustomer();
                listenForCustomerRequest();
            } else{
                leftDrawer.removeDriverToggle();
            }

            //load user's profile image
            String userProfileImageUrl = mUser.getProfileImageUrl();
            if( userProfileImageUrl != null && !userProfileImageUrl.isEmpty() ) {
                //profile image button id cuz anything else does not work
                @SuppressLint( "ResourceType" )
                ImageView imageView = findViewById( 2131230907 );
                if( imageView != null )
                    Glide
                        .with( imageView.getContext() )
                        .load( userProfileImageUrl )
                        .into( imageView );
            }
        } );
    }

    @Override
    protected void onStart() {
        super.onStart();
        if ( mLastLocation != null ) {
            addLocationToDatabase( mLastLocation.getLatitude(), mLastLocation.getLongitude() );
        }
        //refresh location callback
        if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.M )
            if ( ContextCompat.checkSelfPermission( this, Manifest.permission.ACCESS_FINE_LOCATION ) == PackageManager.PERMISSION_GRANTED )
                mFusedLocationProviderClient.requestLocationUpdates( mLocationRequest, mLocationCallback, Looper.myLooper() );
    }

    @Override
    protected void onStop() {
        super.onStop();
        deleteLocationFromDatabase();
        //remove location callback
        mFusedLocationProviderClient.removeLocationUpdates( mLocationCallback );
    }

    @Override
    public void onMapReady( GoogleMap googleMap ) {
        mMap = googleMap;
        mRoutes = new Routes( this, mMap );
        //set how often update location
        initialLocationRequest();
        //change location of my location button on map
        changeMapsMyLocationButton( this, mapFragment );

        if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ) {
            if ( ContextCompat.checkSelfPermission( this, Manifest.permission.ACCESS_FINE_LOCATION ) != PackageManager.PERMISSION_GRANTED ) {
                checkLocationPermissions();
            } else {
                mFusedLocationProviderClient.requestLocationUpdates( mLocationRequest, mLocationCallback, Looper.myLooper() );
                mMap.setMyLocationEnabled( true );

                //show last known location
                getLastKnownLocation();
            }
        }
    }

    private void initializeAutocompleteFragment() {
        //initialize places API
        Places.initialize( getApplicationContext(), getString( R.string.google_api_key ) );

        // Initialize the AutocompleteSupportFragment.
        AutocompleteSupportFragment autocompleteFragment = ( AutocompleteSupportFragment )
                getSupportFragmentManager().findFragmentById( R.id.autocomplete_fragment );

        // Specify the types of place data to return.
        autocompleteFragment.setPlaceFields( Arrays.asList( Place.Field.NAME, Place.Field.LAT_LNG ) );

        // Set up a PlaceSelectionListener to handle the response.
        autocompleteFragment.setOnPlaceSelectedListener( new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected( @NonNull Place place ) {
                if( place.getName() != null ) {
                    CustomerHelper.destination = place.getName();
                    CustomerHelper.destinationLatLng = place.getLatLng();
                }
            }

            @Override
            public void onError( Status status ) {
                Log.e( "onPlaceSelected", "An error occurred: " + status );
            }
        });
    }

    private void initializeUberRequestBtn() {
        showRelativeLayout( relativeLayout );

        //save customer location to db and put marker on map
        setUberRequestOnClickListener();
    }

    private void setUberRequestOnClickListener() {
        mRequestUberButton.setOnClickListener( v -> {
            //no last location available
            if ( mLastLocation == null ) {
                Toast.makeText( MapActivity.this, R.string.no_location_err, Toast.LENGTH_SHORT ).show();
                return;
            }

            //no internet connection
            if ( !CheckNetwork.isInternetAvailable( MapActivity.this ) ) {
                Toast.makeText( MapActivity.this, R.string.no_network_err, Toast.LENGTH_SHORT ).show();
                return;
            }

            //customer id is assigned so customer already has requested for uber
            if ( mCustomerHelper != null && mCustomerHelper.getCustomerId() != null && !mCustomerHelper.getCustomerId().isEmpty() )
                return;

            final String pickupMarkerTitle = getResources().getString( R.string.pickup_marker_title );
            final double pickupLat = mLastLocation.getLatitude();
            final double pickupLong = mLastLocation.getLongitude();
            final LatLng pickupLatLng = new LatLng( pickupLat, pickupLong );

            //save customer pickup location
            SharedPref.setDouble( "pickupLat", pickupLat );
            SharedPref.setDouble( "pickupLong", pickupLong );

            //save customer location
            addCustomerLocationToDB( pickupLat, pickupLong );
            //put marker on map
            locationMarker = addMarkerWithTitleAndIcon( pickupLatLng, pickupMarkerTitle,
                    BitmapDescriptorFactory.fromResource( R.mipmap.ic_map_destination ) );

            //disable button
            disableUberRequestBtn();

            if ( mCustomerHelper == null )
                mCustomerHelper = new CustomerHelper( MapActivity.this );
            mCustomerHelper.setCustomerId( userIdString );
            mCustomerHelper.getClosestDriverInRadius( pickupLat, pickupLong );
        } );
    }

    public void enableUberRequestBtn() {
        mRequestUberButton.setText( R.string.request_uber );

        mRequestUberButton.postDelayed( () -> mRequestUberButton.setEnabled( true ), 3000 );


        SharedPref.setBool( "buttonDisabled", false );
        SharedPref.setDouble( "pickupLat", DEFAULT_DOUBLE );
        SharedPref.setDouble( "pickupLong", DEFAULT_DOUBLE );
    }

    public void disableUberRequestBtn() {
        SharedPref.setBool( "buttonDisabled", true );
        //get customer pickup coordinates saved in shared prefs
        final String pickupMarkerTitle = getResources().getString( R.string.pickup_marker_title );
        double pickupLat = SharedPref.getDouble( "pickupLat" );
        double pickupLong = SharedPref.getDouble( "pickupLong" );
        LatLng pickupLatLng = new LatLng( pickupLat, pickupLong );

        //change button text
        mRequestUberButton.setText( R.string.pickup_pending );
        //disable button
        mRequestUberButton.setEnabled( false );

        if( pickupLat != DEFAULT_DOUBLE )
            locationMarker = addMarkerWithTitleAndIcon( pickupLatLng, pickupMarkerTitle,
                    BitmapDescriptorFactory.fromResource( R.mipmap.ic_map_destination ) );
    }

    public void addLocationToDatabase( final double latitude, final double longitude ) {
        if ( mUser != null && mUser.isDriver() && SharedPref.getBool( "isDriver" ) ) {
            mWorkingDriversDbRef.child( userIdString ).addListenerForSingleValueEvent( new ValueEventListener() {
                @Override
                public void onDataChange( @NonNull DataSnapshot dataSnapshot ) {
                    if ( !dataSnapshot.exists() )
                        addAvailableDriverLocationToDB( userIdString, latitude, longitude );
                }

                @Override
                public void onCancelled( @NonNull DatabaseError databaseError ) {

                }
            } );
        }
    }

    public void deleteLocationFromDatabase() {
        if ( mUser != null && mUser.isDriver() ) {
            mAvailableDriversDbRef.child( userIdString ).addListenerForSingleValueEvent( new ValueEventListener() {
                @Override
                public void onDataChange( @NonNull DataSnapshot dataSnapshot ) {
                    if ( dataSnapshot.exists() )
                        deleteAvailableDriverLocationFromDB( userIdString );
                }

                @Override
                public void onCancelled( @NonNull DatabaseError databaseError ) {

                }
            } );
        }
    }

    private void setToolbar() {
        mToolbar.setBackgroundColor( Color.TRANSPARENT );
        setSupportActionBar( mToolbar );
        getSupportActionBar().setDisplayHomeAsUpEnabled( true );
        Window window = getWindow(); // get phone's window

        Util.setLayoutToFullscreen( window );
    }

    //ask user for permissions
    private void checkLocationPermissions() {
        if ( ActivityCompat.checkSelfPermission( this, Manifest.permission.ACCESS_FINE_LOCATION ) != PackageManager.PERMISSION_GRANTED ) {
            ActivityCompat.requestPermissions( MapActivity.this, new String[]{ Manifest.permission.ACCESS_FINE_LOCATION }, REQUEST_GPS_PERMISSIONS );
        } else {
            getLastKnownLocation();
        }
    }

    @Override
    public void onRequestPermissionsResult( int requestCode, @NonNull String[] permissions,
                                            @NonNull int[] grantResults ) {
        super.onRequestPermissionsResult( requestCode, permissions, grantResults );

        switch ( requestCode ) {
            case REQUEST_GPS_PERMISSIONS:
                if ( grantResults.length > 0 && grantResults[ 0 ] == PackageManager.PERMISSION_GRANTED ) {

                    if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ) {
                        if ( ContextCompat.checkSelfPermission( this, Manifest.permission.ACCESS_FINE_LOCATION ) == PackageManager.PERMISSION_GRANTED ) {
                            //get location updates
                            mFusedLocationProviderClient.requestLocationUpdates( mLocationRequest, mLocationCallback, Looper.myLooper() );
                            mMap.setMyLocationEnabled( true );

                            //show last known location
                            getLastKnownLocation();
                        }
                    }
                } else {
                    Toast.makeText( this, R.string.GPS_permissions_err, Toast.LENGTH_SHORT ).show();
                }
                return;
            default:
                break;
        }
    }

    //set how often check for location updates
    public void initialLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setSmallestDisplacement( MINIMUM_DISTANCE_BETWEEN_MAP_UPDATES );
        mLocationRequest.setInterval( MINIMUM_TIME_INTERVAL_BETWEEN_MAP_UPDATES );
        mLocationRequest.setFastestInterval( MAXIMUM_TIME_INTERVAL_BETWEEN_MAP_UPDATES );
        mLocationRequest.setPriority( LocationRequest.PRIORITY_HIGH_ACCURACY );
    }

    @SuppressWarnings( "MissingPermission" )
    private void getLastKnownLocation() {
        mFusedLocationProviderClient.getLastLocation()
                .addOnCompleteListener( this, task -> {
                    if ( task.isSuccessful() && task.getResult() != null ) {
                        mLastLocation = task.getResult();
                        LatLng latLng = new LatLng( mLastLocation.getLatitude(), mLastLocation.getLongitude() );

                        mMap.moveCamera( CameraUpdateFactory.newLatLngZoom( latLng, MAP_ZOOM ) );
                        mMap.animateCamera( CameraUpdateFactory.zoomTo( MAP_ZOOM ), 2000, null );
                    }
                } );
    }

    public void showMessage( final String s ) {
        runOnUiThread( () -> Toast.makeText( MapActivity.this, s, Toast.LENGTH_LONG ).show() );
    }

    public void clearMap() {
        runOnUiThread( () -> {
            if ( getGoogleMap() != null )
                mMap.clear();
        } );
    }

    public Marker addMarkerWithTitleAndIcon( LatLng latLng, String markerTitle, BitmapDescriptor icon ) {
        if ( mMap != null ) {
            //set marker with customer pickup coordinates
            return mMap.addMarker( new MarkerOptions().position( latLng ).title( markerTitle ).icon( icon ) );
        }
        return null;
    }

    public void changeRequestBtnText( final int stringResourceId ) {
        runOnUiThread( () -> mRequestUberButton.setText( stringResourceId ) );
    }

    public void resetCustomerHelper() {
        setUserDestination( getResources().getString( R.string.no_specified ) );
        hideUserInfo();
        clearMap();
        enableUberRequestBtn();
        mCustomerHelper = null;
    }

    public void resetDriverHelper(){
        leftDrawer.enableDriverToggleButtonState();
        setUserDestination( getResources().getString( R.string.no_specified ) );
        hideUserInfo();
        Util.showRelativeLayout( relativeLayout );
        clearMap();
        mDriverHelper = null;
    }

    public void showUserInfo() {
        mInfo.setVisibility( View.VISIBLE );
    }

    public void hideUserInfo(){
        mInfo.setVisibility( View.GONE );
    }

    public void setUserName( String name ){
        runOnUiThread( () -> mUserName.setText( name ) );
    }

    public void setUserPhone( String phone ){
        runOnUiThread( () -> mUserPhoneNumber.setText( phone ) );
    }

    public void setUserImage( String userImage ){
        runOnUiThread( () -> Glide.with( getApplicationContext() ).load( userImage ).into( mUserImage ) );
    }

    public void setUserDestination( String destination ){
        runOnUiThread( () -> mUserDestination.setText( destination ) );
    }

    //display user info on screen
    public void getUserInfo( String foundDriverID ){
        mUsersDbRef.child( foundDriverID ).addListenerForSingleValueEvent( new ValueEventListener() {
            @Override
            public void onDataChange( @NonNull DataSnapshot dataSnapshot ) {
                if( dataSnapshot.exists() ){
                    showUserInfo();
                    User user = dataSnapshot.getValue( User.class );
                    setUserName( getString( R.string.user_full_name_value, user.getName(), user.getSurname() ) );
                    setUserPhone( user.getPhoneNumber() );

                    String userImage = user.getProfileImageUrl();
                    if( userImage != null && !userImage.isEmpty() )
                        setUserImage( user.getProfileImageUrl() );
                }
            }

            @Override
            public void onCancelled( @NonNull DatabaseError databaseError ) {
                Log.e( "getUserInfo", "getDriverInfo: " + databaseError.getMessage()  );
            }
        } );
    }

    public void getCustomerDestination( String customerId ){
        mCustomerDestinationDbRef.child( customerId ).child( "destination" ).addListenerForSingleValueEvent( new ValueEventListener() {
            @Override
            public void onDataChange( @NonNull DataSnapshot dataSnapshot ) {
                if( dataSnapshot.exists() )
                    mUserDestination.setText( dataSnapshot.getValue().toString() );
                else
                    mUserDestination.setText( getResources().getString( R.string.no_specified ) );
            }

            @Override
            public void onCancelled( @NonNull DatabaseError databaseError ) {

            }
        } );
    }

    //on driver screen btn to pickup customer and end ride
    @OnClick( R.id.pickup_customer_btn )
    public void changeRideStatus(){
        String btnText = mPickupCustomerBtn.getText().toString();

        //pick up customer
        if( btnText.equals( getResources().getString( R.string.pick_customer ) ) )
            pickUpCustomer();
        else
            finishRide();
    }

    private void pickUpCustomer(){
        if( mDriverHelper != null ){
            clearMap();

            //if customer have destination draw route
            String customerId = mDriverHelper.getCustomerId();
            if( customerId != null ){
                mCustomerDestinationDbRef.child( customerId ).child( "l" ).addListenerForSingleValueEvent( new ValueEventListener() {
                    @Override
                    public void onDataChange( @NonNull DataSnapshot dataSnapshot ) {
                        if( dataSnapshot.exists() ){
                            List<Object> customerDestination = ( List<Object> ) dataSnapshot.getValue();
                            LatLng customerLatLngDestination = Util.getLatLng( customerDestination );

                            //create route to customers destination
                            if( customerLatLngDestination != null && mLastLocation != null ){
                                mRoutes.getRouteToLocation( new LatLng( mLastLocation.getLatitude(), mLastLocation.getLongitude() ),
                                        customerLatLngDestination, true );

                                addMarkerWithTitleAndIcon( customerLatLngDestination, getResources().getString( R.string.customer_destination ),
                                        BitmapDescriptorFactory.fromResource( R.mipmap.ic_map_destination ) );
                            }
                        }
                    }

                    @Override
                    public void onCancelled( @NonNull DatabaseError databaseError ) {
                        Log.e( "err", "pickUpCustomer: " + databaseError.getMessage()  );
                    }
                } );
            }

            mPickupCustomerBtn.setText( R.string.finish_ride );
        }
    }

    private void finishRide(){
        //set this button text to initial
        mPickupCustomerBtn.setText( R.string.pick_customer );
        Util.changeButtonVisibility( mPickupCustomerBtn, false );

        //prepare data to save in db
        if( mDriverHelper != null && mDriverHelper.getCustomerId() != null ) {
            String customerId = mDriverHelper.getCustomerId();
            LatLng customerPickupLocation = mDriverHelper.getCustomerLatLng();

            mDriverHelper.addHistoryRecord( customerId, userIdString, 0, customerPickupLocation, mLastLocation );

            //delete from database
            deleteCustomerFromDb( customerId );
            deleteCustomerRequestFromDB( customerId );
            removeCustomerDestinationFromDb( customerId );
        }

        deleteWorkingDriverLocationFromDB( userIdString );
        deleteDriverFromDb( userIdString );
        resetDriverHelper();
    }
}
