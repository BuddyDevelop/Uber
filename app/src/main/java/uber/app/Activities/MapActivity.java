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
import android.view.View;
import android.view.Window;
import android.widget.Toast;

import com.firebase.geofire.GeoFire;
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
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import uber.app.Helpers.CheckNetwork;
import uber.app.Helpers.CustomerHelper;
import uber.app.Helpers.DriverHelper;
import uber.app.Helpers.FirebaseHelper;
import uber.app.Fragments.LeftDrawer;
import uber.app.OnDataReceiveCallback;
import uber.app.R;
import uber.app.SharedPref;
import uber.app.Util;

import static uber.app.Helpers.FirebaseHelper.addAvailableDriverLocationToDB;
import static uber.app.Helpers.FirebaseHelper.addCustomerLocationToDB;
import static uber.app.Helpers.FirebaseHelper.deleteAvailableDriverLocationFromDB;
import static uber.app.Helpers.FirebaseHelper.getFromFirebase;
import static uber.app.Helpers.FirebaseHelper.mAvailableDriversDbRef;
import static uber.app.Helpers.FirebaseHelper.mUser;
import static uber.app.Helpers.FirebaseHelper.mWorkingDriversDbRef;
import static uber.app.Helpers.FirebaseHelper.userIdString;
import static uber.app.SharedPref.DEFAULT_DOUBLE;
import static uber.app.Util.changeMapsMyLocationButton;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback {
    private static final int REQUEST_GPS_PERMISSIONS = 1;
    private static final float MINIMUM_DISTANCE_BETWEEN_MAP_UPDATES = 10;
    private static final long MINIMUM_TIME_INTERVAL_BETWEEN_MAP_UPDATES = 60 * 1000; //1 min
    private static final long MAXIMUM_TIME_INTERVAL_BETWEEN_MAP_UPDATES = 3 * 60 * 1000; //3 min
    private static final float MAP_ZOOM = 16;

    private FusedLocationProviderClient mFusedLocationProviderClient;
    private CustomerHelper mCustomerHelper;
    private DriverHelper mDriverHelper;
    public LeftDrawer leftDrawer;

    private GoogleMap mMap;
    private Marker locationMarker;

    public LocationRequest mLocationRequest;
    public Location mLastLocation;

    public Toolbar mToolbar;
    public MaterialButton mRequestUberButton;

    public CustomerHelper getCustomerHelper() {
        return mCustomerHelper;
    }

    public DriverHelper getDriverHelper() { return mDriverHelper; }

    public Location getLastLocation() { return mLastLocation; }

    //callback to refresh location
    public LocationCallback mLocationCallback = new LocationCallback() {
        @Override
        public void onLocationResult( LocationResult locationResult ) {
            for ( Location location : locationResult.getLocations() ) {
                if ( getApplicationContext() != null ) {

                    if ( location == null )
                        return;

                    mLastLocation = location;
                    addLocationToDatabase( location.getLatitude(), location.getLongitude() );
                    LatLng latLng = new LatLng( location.getLatitude(), location.getLongitude() );
                    mMap.moveCamera( CameraUpdateFactory.newLatLngZoom( latLng, MAP_ZOOM ) );
                }
            }
        }
    };

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_driver_map );
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = ( SupportMapFragment ) getSupportFragmentManager()
                .findFragmentById( R.id.map );
        mapFragment.getMapAsync( this );

        //get user data from firebase and save in shared pref if user is driver
        FirebaseHelper.getUserData( this );

        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient( this );

        //set toolbar
        setToolbar();
        //change location of my location button on map
        changeMapsMyLocationButton( this, mapFragment );
        //set left drawer
        leftDrawer = new LeftDrawer( this, mToolbar );
        leftDrawer.initDrawer();

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

        //do actions on data received
        getFromFirebase( new OnDataReceiveCallback() {
            public void onDataReceived() {
                if ( mUser != null && mUser.isDriver() ) {

                    if( mDriverHelper == null )
                        mDriverHelper = new DriverHelper( MapActivity.this );

                    //get driver's customer if any
                    mDriverHelper.getAssignedCustomer();
                } else{
                    leftDrawer.removeDriverToggle();
                }
            }
        } );
    }

    @Override
    protected void onStart() {
        super.onStart();
        if ( mLastLocation != null ) {
            addLocationToDatabase( mLastLocation.getLatitude(), mLastLocation.getLongitude() );
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        deleteLocationFromDatabase();
    }

    @Override
    public void onMapReady( GoogleMap googleMap ) {
        mMap = googleMap;
        //set how often update location
        initialLocationRequest();

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

    public GoogleMap getGoogleMap() {
        return mMap;
    }

    private void initializeUberRequestBtn() {
        //if logged user is driver do not initialize button
        if ( SharedPref.getBool( "isDriver" ) && mUser != null && mUser.isDriver() )
            return;

        mRequestUberButton = findViewById( R.id.request_uber );
        changeButtonVisibility( mRequestUberButton, true );

        //save customer location to db and put marker on map
        setUberRequestOnClickListener();
    }

    private void setUberRequestOnClickListener() {
        mRequestUberButton.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick( View v ) {
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
            }
        } );
    }

    public void enableUberRequestBtn() {
        mRequestUberButton.setText( R.string.request_uber );

        mRequestUberButton.postDelayed( new Runnable() {
            @Override
            public void run() {
                mRequestUberButton.setEnabled( true );
            }
        }, 3000 );


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
        mRequestUberButton.postDelayed( new Runnable() {
            @Override
            public void run() {
                mRequestUberButton.setEnabled( false );
            }
        }, 100 );

        if( pickupLat != DEFAULT_DOUBLE )
            locationMarker = addMarkerWithTitleAndIcon( pickupLatLng, pickupMarkerTitle,
                    BitmapDescriptorFactory.fromResource( R.mipmap.ic_map_destination ) );
    }

    public void addLocationToDatabase( final double latitude, final double longitude ) {
        if ( mUser != null && mUser.isDriver() ) {
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
        mToolbar = findViewById( R.id.toolbar );

        mToolbar.setBackgroundColor( Color.TRANSPARENT );
        setSupportActionBar( mToolbar );
        getSupportActionBar().setDisplayHomeAsUpEnabled( true );
        Window window = getWindow(); // get phone's window

        Util.setLayoutToFullscreen( window );
    }

    @SuppressLint( "MissingPermission" )
    private void connectDriver() {
        checkLocationPermissions();
        mFusedLocationProviderClient.requestLocationUpdates( mLocationRequest, mLocationCallback, Looper.myLooper() );
        mMap.setMyLocationEnabled( true );
    }

    private void disconnectDriver() {
        if ( mFusedLocationProviderClient != null ) {
            mFusedLocationProviderClient.removeLocationUpdates( mLocationCallback );
        }
        GeoFire geoFire = new GeoFire( FirebaseHelper.mAvailableDriversDbRef );
        geoFire.removeLocation( FirebaseHelper.userIdString );
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
                .addOnCompleteListener( this, new OnCompleteListener<Location>() {
                    @Override
                    public void onComplete( @NonNull Task<Location> task ) {
                        if ( task.isSuccessful() && task.getResult() != null ) {
                            mLastLocation = task.getResult();
                            LatLng latLng = new LatLng( mLastLocation.getLatitude(), mLastLocation.getLongitude() );

                            mMap.moveCamera( CameraUpdateFactory.newLatLngZoom( latLng, MAP_ZOOM ) );
                            mMap.animateCamera( CameraUpdateFactory.zoomTo( MAP_ZOOM ), 2000, null );
                        }
                    }
                } );
    }

    public void showMessage( final String s ) {
        runOnUiThread( new Runnable() {
            public void run() {
                Toast.makeText( MapActivity.this, s, Toast.LENGTH_LONG ).show();
            }
        } );
    }

    public void clearMap() {
        runOnUiThread( new Runnable() {
            public void run() {
                if ( getGoogleMap() != null )
                    mMap.clear();
            }
        } );
    }

    public Marker addMarkerWithTitleAndIcon( LatLng latLng, String markerTitle, BitmapDescriptor icon ) {
        if ( mMap != null ) {
            //set marker with customer pickup coordinates
            return mMap.addMarker( new MarkerOptions().position( latLng ).title( markerTitle ).icon( icon ) );
        }
        return null;
    }

    public void changeBtnText( final MaterialButton mRequestUberButton, final int stringResourceId ) {
        runOnUiThread( new Runnable() {
            public void run() {
                mRequestUberButton.setText( stringResourceId );
            }
        } );
    }

    public void changeButtonVisibility( MaterialButton mRequestUberButton, boolean makeVisible ) {
        if( mRequestUberButton == null )
            return;

        if ( makeVisible )
            Util.showButton( mRequestUberButton );
        else
            Util.hideButton( mRequestUberButton );
    }

    public void resetCustomerHelper() {
        mCustomerHelper = null;
    }

    public void resetDriverHelper(){
        mDriverHelper.setCustomerId( null );
        mDriverHelper = null;
    }
}
