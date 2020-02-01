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

import java.util.HashMap;
import java.util.List;

import uber.app.Activities.MapActivity;
import uber.app.R;

import static uber.app.Helpers.FirebaseHelper.addWorkingDriverLocationToDB;
import static uber.app.Helpers.FirebaseHelper.deleteAvailableDriverLocationFromDB;
import static uber.app.Helpers.FirebaseHelper.deleteWorkingDriverLocationFromDB;
import static uber.app.Helpers.FirebaseHelper.mCustomerUberRequest;
import static uber.app.Helpers.FirebaseHelper.mCustomersDbRef;
import static uber.app.Helpers.FirebaseHelper.mDriversDbRef;
import static uber.app.Helpers.FirebaseHelper.mGeoFireAvailableDrivers;
import static uber.app.Helpers.FirebaseHelper.mWorkingDriversDbRef;
import static uber.app.Helpers.FirebaseHelper.userIdString;

/**
 * Class responsible for searching driver for customer
 */

public class CustomerHelper {
    private final String TAG = "CustomerHelper";
    private final int MAX_RADIUS_SEARCH = 10;
    private String customerId;
    private String foundDriverID;

    private MapActivity mMapActivity;
    private Marker foundDriverMarker;
    private int radius = 1;
    private Boolean driverFound = false;

    public CustomerHelper( MapActivity mMapActivity ) {
        this.mMapActivity = mMapActivity;
    }

    public void setCustomerId( String customerId ) {
        this.customerId = customerId;
    }

    public void setFoundDriverID( String foundDriverID ) {
        this.foundDriverID = foundDriverID;
    }

    public void setDriverFound( Boolean driverFound ) {
        this.driverFound = driverFound;
    }

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
                    addDriverToDb( customerId, foundDriverID );
//                    swapToWorkingDriverStatus( foundDriverID );
                    deleteAvailableDriverLocationFromDB( foundDriverID );
                    addWorkingDriverLocationToDB( foundDriverID, location.latitude, location.longitude );
                    //show driver on map
                    getDriverLocation( foundDriverID );
                }
            }

            @Override
            public void onKeyExited( String key ) {

            }

            @Override
            public void onKeyMoved( String key, GeoLocation location ) {

            }

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
                    mMapActivity.enableUberRequestBtn();
                    mMapActivity.clearMap();
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
                    double driverLat;
                    double driverLng;

                    //check if driver latitude and longitude in firebase is not null
                    if ( driverLocation.get( 0 ) != null && driverLocation.get( 1 ) != null ) {
                        driverLat = Double.parseDouble( driverLocation.get( 0 ).toString() );
                        driverLng = Double.parseDouble( driverLocation.get( 1 ).toString() );
                        LatLng driverLatLng = new LatLng( driverLat, driverLng );
                        String driverLocationMarker = mMapActivity.getResources().getString( R.string.your_driver );

                        //change uber request btn text
                        if ( mMapActivity != null && !mMapActivity.isFinishing() ) {
                            mMapActivity.changeRequestBtnText( R.string.driver_coming );

                            //ensure button is disabled
                            new Handler( Looper.getMainLooper() ).post( new Runnable() {
                                @Override
                                public void run() {
                                    mMapActivity.mRequestUberButton.setEnabled( false );
                                      mMapActivity.getUserInfo( foundDriverID );
                                }
                            } );

                            //remove old driver's position marker
                            removeFoundDriverMarker();

                            //add new driver's location marker
                            foundDriverMarker = mMapActivity.addMarkerWithTitleAndIcon( driverLatLng, driverLocationMarker,
                                    BitmapDescriptorFactory.fromResource( R.mipmap.ic_uber_car ) );

                            //check distance between driver and customer
                            Location customerLocation = mMapActivity.getLastLocation();
                            Location driverCurrentLocation = new Location( "" );
                            driverCurrentLocation.setLatitude( driverLat );
                            driverCurrentLocation.setLongitude( driverLng );

                            if( customerLocation != null && driverCurrentLocation != null ) {
                                float distance = customerLocation.distanceTo( driverCurrentLocation );

                                if ( distance < 100 ) {
                                    mMapActivity.changeRequestBtnText( R.string.driver_arrived );
                                }
                            }
                        }
                    }
                } else {
                    mMapActivity.hideUserInfo();
                    mMapActivity.resetCustomerHelper();
                    mMapActivity.clearMap();
                    mMapActivity.enableUberRequestBtn();
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
                    mMapActivity.enableUberRequestBtn();
                    deleteCustomerFromDb( customerId );
                    removeCustomerRequestFromDb();
                    mMapActivity.resetCustomerHelper();
                    mMapActivity.clearMap();
                }
            }

            @Override
            public void onCancelled( @NonNull DatabaseError databaseError ) {

            }
        } );
    }

    //check if customer has driver assigned to his request
    public void hasCustomerDriver( final String customerId ) {
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
                    mMapActivity.enableUberRequestBtn();
                    mMapActivity.clearMap();
                    mMapActivity.resetCustomerHelper();
                    mMapActivity.hideUserInfo();
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
            swapToAvailableDriverStatus( foundDriverID );
            setFoundDriverID( "" );
            setDriverFound( false );
        }
    }

    public void addCustomerToDb( String customerId, String foundDriverID ) {
        DatabaseReference customersFirebaseDBref = mCustomersDbRef.child( customerId );
        HashMap<String, Object> customerHashMap = new HashMap<>();
        customerHashMap.put( "foundDriverId", foundDriverID );
        customersFirebaseDBref.updateChildren( customerHashMap );
    }

    public void addDriverToDb( String customerId, String foundDriverID ) {
        DatabaseReference driversFirebaseDBref = mDriversDbRef.child( foundDriverID );
        HashMap<String, Object> customerHashMap = new HashMap<>();
        customerHashMap.put( "customerId", customerId );
        driversFirebaseDBref.updateChildren( customerHashMap );
    }

    public void deleteCustomerFromDb( String customerId ) {
        if ( customerId != null && !customerId.isEmpty() ) {
            DatabaseReference customersFirebaseDBref = mCustomersDbRef.child( customerId );
            customersFirebaseDBref.removeValue();
        }
    }

    public void deleteDriverFromDb( String foundDriverID ) {
        if ( foundDriverID != null && !foundDriverID.isEmpty() ) {
            DatabaseReference driversFirebaseDBref = mDriversDbRef.child( foundDriverID );
            driversFirebaseDBref.removeValue();
        }
    }

    private void swapToWorkingDriverStatus( String foundDriverID ) {
        deleteAvailableDriverLocationFromDB( foundDriverID );
        addWorkingDriverLocationToDB( foundDriverID, mMapActivity.mLastLocation.getLatitude(), mMapActivity.mLastLocation.getLongitude() );
    }

    private void swapToAvailableDriverStatus( String foundDriverID ) {
        deleteWorkingDriverLocationFromDB( foundDriverID );
//        addAvailableDriverLocationToDB( foundDriverID, mMapActivity.mLastLocation.getLatitude(), mMapActivity.mLastLocation.getLongitude() );
    }

    private void removeFoundDriverMarker() {
        if ( foundDriverMarker != null && mMapActivity.getGoogleMap() != null )
            foundDriverMarker.remove();
    }
}
