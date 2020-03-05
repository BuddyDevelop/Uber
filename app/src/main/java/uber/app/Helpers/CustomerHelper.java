package uber.app.Helpers;

import android.location.Location;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import uber.app.Activities.MapActivity;
import uber.app.R;
import uber.app.Util;

import static uber.app.Helpers.FirebaseHelper.addCustomerToDb;
import static uber.app.Helpers.FirebaseHelper.addDriverToDb;
import static uber.app.Helpers.FirebaseHelper.addWorkingDriverLocationToDB;
import static uber.app.Helpers.FirebaseHelper.deleteAvailableDriverLocationFromDB;
import static uber.app.Helpers.FirebaseHelper.deleteCustomerFromDb;
import static uber.app.Helpers.FirebaseHelper.deleteWorkingDriverLocationFromDB;
import static uber.app.Helpers.FirebaseHelper.mCustomerDestinationDbRef;
import static uber.app.Helpers.FirebaseHelper.mCustomerUberRequest;
import static uber.app.Helpers.FirebaseHelper.mCustomersDbRef;
import static uber.app.Helpers.FirebaseHelper.mGeoFireAvailableDrivers;
import static uber.app.Helpers.FirebaseHelper.mWorkingDriversDbRef;
import static uber.app.Helpers.FirebaseHelper.removeCustomerDestinationFromDb;
import static uber.app.Helpers.FirebaseHelper.userIdString;

/**
 * Class responsible for searching driver for customer
 */

public class CustomerHelper {
    private final String TAG = "CustomerHelper";
    public static final int MIN_DISTANCE_TO_INFORM_CUSTOMER = 200;

    private int radius = 1;
    private final int MAX_RADIUS_SEARCH = 10;

    private String customerId;
    private Boolean driverFound = false;
    private String foundDriverID;

    public static String destination;
    public static LatLng destinationLatLng;

    private MapActivity mMapActivity;
    private Marker foundDriverMarker;

    public CustomerHelper( MapActivity mMapActivity ) {
        this.mMapActivity = mMapActivity;
    }
    public void setCustomerId( String customerId ) {
        this.customerId = customerId;
    }
    public void setFoundDriverID( String foundDriverID ) { this.foundDriverID = foundDriverID; }
    public void setDriverFound( Boolean driverFound ) { this.driverFound = driverFound; }
    public Boolean getDriverFound() {
        return driverFound;
    }
    public String getFoundDriverID() {
        return foundDriverID;
    }
    public String getCustomerId() {
        return customerId;
    }

    public void getClosestDriverInRadius( final double latitude, final double longitude ) {
        //query to look for driver within radius
        GeoQuery geoQuery = mGeoFireAvailableDrivers.queryAtLocation( new GeoLocation( latitude, longitude ), radius );
        //remove all listeners cuz listener is applied each time radius change
        geoQuery.removeAllListeners();

        geoQuery.addGeoQueryEventListener( new GeoQueryEventListener() {
            //when driver is found this method is called
            @Override
            public void onKeyEntered( String key, GeoLocation location ) {
                //customer cannot be driver at same time
                if( key.equals( userIdString ) || mMapActivity.getCustomerHelper() == null ){
                    mMapActivity.enableUberRequestBtn();
                }
                //this if is necessary cuz if there is multiple drivers found
                //then it will change driver id continuously
                else if ( !driverFound ) {
                    driverFound = true;
                    foundDriverID = key;

                    if ( mMapActivity != null && !mMapActivity.isFinishing() ) {
                        mMapActivity.changeRequestBtnText( R.string.getting_driver_location );
                    }

                    customerId = FirebaseHelper.userIdString;
                    addCustomerToDb( customerId, foundDriverID );
                    addDriverToDb( customerId, foundDriverID, latitude, longitude );

                    addCustomerDestinationToDb();
                    deleteAvailableDriverLocationFromDB( foundDriverID );
                    addWorkingDriverLocationToDB( foundDriverID, location.latitude, location.longitude );
                    //show driver on map
                    getDriverLocation( foundDriverID );
                }
            }

            @Override
            public void onKeyExited( String key ) { }

            @Override
            public void onKeyMoved( String key, GeoLocation location ) { }

            @Override
            public void onGeoQueryReady() {
                //if driver was not found search larger area
                if ( !driverFound && radius < MAX_RADIUS_SEARCH ) {
                    radius++;
                    getClosestDriverInRadius( latitude, longitude );
                } else if ( radius >= MAX_RADIUS_SEARCH ) {
                    mMapActivity.showMessage( mMapActivity.getResources().getString( R.string.no_driver_nearby ) );
                    removeCustomerRequestFromDb();
                    deleteCustomerFromDb( customerId );
                    removeCustomerDestinationFromDb( customerId );
                    mMapActivity.resetCustomerHelper();
                }
            }

            @Override
            public void onGeoQueryError( DatabaseError error ) {
                Log.e( TAG, "onGeoQueryError: " + "getClosestDriverInRadius: " + error.getMessage() );
            }
        } );
    }

    public void getDriverLocation( String foundDriverID ) {
        DatabaseReference workingDriverLocationDBRef = mWorkingDriversDbRef.child( foundDriverID ).child( "l" );
        workingDriverLocationDBRef.addValueEventListener( new ValueEventListener() {
            @Override
            public void onDataChange( @NonNull DataSnapshot dataSnapshot ) {
                if ( dataSnapshot.exists() ) {
                    List<Object> driverLocation = ( List<Object> ) dataSnapshot.getValue();
                    LatLng driverLatLng =  Util.getLatLng( driverLocation );

                    //check if driver latitude and longitude in firebase is not null
                    if ( driverLatLng != null ) {
                        String driverLocationMarker = mMapActivity.getResources().getString( R.string.your_driver );

                        //change uber request btn text
                        if ( mMapActivity != null && !mMapActivity.isFinishing() ) {
                            mMapActivity.changeRequestBtnText( R.string.driver_coming );

                            //ensure button is disabled
                            new Handler( Looper.getMainLooper() ).post( () -> {
                                mMapActivity.mRequestUberButton.setEnabled( false );
                                mMapActivity.getUserInfo( foundDriverID );
                                mMapActivity.getCustomerDestination( customerId );
                            } );

                            //remove old driver's position marker
                            removeFoundDriverMarker();

                            //add new driver's location marker
                            foundDriverMarker = mMapActivity.addMarkerWithTitleAndIcon( driverLatLng, driverLocationMarker,
                                    BitmapDescriptorFactory.fromResource( R.mipmap.ic_uber_car ) );

                            /*
                            TODO calculate distance between driver's location and customer pickup location
                            lines under calculates customer's and driver's current location
                            but it should calculate driver's location with customer's pick up location
                            */
                            //check distance between driver and customer
                            Location customerLocation = mMapActivity.getLastLocation();
                            if( customerLocation != null ) {
                                float distance = Util.calculateDistance( customerLocation.getLatitude(), customerLocation.getLongitude(),
                                        driverLatLng.latitude, driverLatLng.longitude );

                                if( distance < MIN_DISTANCE_TO_INFORM_CUSTOMER )
                                    mMapActivity.changeRequestBtnText( R.string.driver_arrived );
                            }
                        }
                    }
                } else {
                    mMapActivity.resetCustomerHelper();
                }
            }

            @Override
            public void onCancelled( @NonNull DatabaseError databaseError ) {
                Log.e( TAG, "onCancelled: " + databaseError.getMessage() );
            }
        } );
    }

    //check if customer has requested uber
    public void hasCustomerRequest( final String customerId ) {
        mCustomerUberRequest.child( customerId ).addListenerForSingleValueEvent( new ValueEventListener() {
            @Override
            public void onDataChange( @NonNull DataSnapshot dataSnapshot ) {
                if ( dataSnapshot.exists() ) {
                    setCustomerId( customerId );
                    hasCustomerDriver( customerId );

                    //update UI
                    if ( mMapActivity != null && !mMapActivity.isFinishing() ) {
                        mMapActivity.disableUberRequestBtn();
                        mMapActivity.changeRequestBtnText( R.string.getting_driver_location );
                    }
                } else {
                    deleteCustomerFromDb( customerId );
                    removeCustomerRequestFromDb();
                    mMapActivity.resetCustomerHelper();
                }
            }

            @Override
            public void onCancelled( @NonNull DatabaseError databaseError ) {

            }
        } );
    }

    //check if customer has driver assigned to his request
    private void hasCustomerDriver( final String customerId ) {
        mCustomersDbRef.child( customerId ).child( "foundDriverId" ).addListenerForSingleValueEvent( new ValueEventListener() {
            @Override
            public void onDataChange( @NonNull DataSnapshot dataSnapshot ) {
                //user has assigned driver
                if ( dataSnapshot.exists() ) {
                    setFoundDriverID( dataSnapshot.getValue().toString() );
                    getDriverLocation( foundDriverID );

                    mMapActivity.getUserInfo( foundDriverID );
                }
                //user has NOT assigned driver
                else {
                    mMapActivity.showMessage( mMapActivity.getResources().getString( R.string.no_driver_nearby ) );
                    removeCustomerRequestFromDb();
                    deleteCustomerFromDb( customerId );
                    mMapActivity.resetCustomerHelper();
                }
            }

            @Override
            public void onCancelled( @NonNull DatabaseError databaseError ) {
                Log.e( TAG, "hasCustomerDriver: " + databaseError.getMessage()  );
            }
        } );
    }

    private void removeCustomerRequestFromDb() {
        if ( customerId != null && !customerId.isEmpty() )
            mCustomerUberRequest.child( customerId ).removeValue();

        if ( foundDriverID != null && !foundDriverID.isEmpty() ) {
            deleteWorkingDriverLocationFromDB( foundDriverID );
            setFoundDriverID( "" );
            setDriverFound( false );
        }
    }

    private void removeFoundDriverMarker() {
        if ( foundDriverMarker != null && mMapActivity.getGoogleMap() != null )
            foundDriverMarker.remove();
    }

    private void addCustomerDestinationToDb(){
        if( destination != null && !destination.isEmpty() ){
            Map<String, Object> map = new HashMap<>(  );
            map.put( "destination", destination );
            if( destinationLatLng != null )
                map.put( "l", Arrays.asList( destinationLatLng.latitude, destinationLatLng.longitude ) );

            mCustomerDestinationDbRef.child( customerId ).updateChildren( map, ( databaseError, databaseReference ) -> {
                if( databaseError != null )
                    Log.e( TAG, "addCustomerDestinationToDb: " + databaseError.getMessage()  );
            } );
        }
    }
}
