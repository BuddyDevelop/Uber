package uber.app.Helpers;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;

import uber.app.Activities.RegisterActivity;
import uber.app.Cache;
import uber.app.Models.User;
import uber.app.OnDataReceiveCallback;
import uber.app.R;
import uber.app.SharedPref;


public class FirebaseHelper {
    private static final String TAG = "FirebaseHelper";

    public static User mUser;
    public static String userIdString;
    public static FirebaseUser mFirebaseUser;

    public static final DatabaseReference mUsersDbRef = FirebaseDatabase.getInstance().getReference( "users" );
    public static final DatabaseReference mDriversDbRef = FirebaseDatabase.getInstance().getReference( "users" ).child( "Drivers" );
    public static final DatabaseReference mCustomersDbRef = FirebaseDatabase.getInstance().getReference( "users" ).child( "Customers" );
    public static final DatabaseReference mAvailableDriversDbRef = FirebaseDatabase.getInstance().getReference( "driversAvailable" );
    public static final DatabaseReference mWorkingDriversDbRef = FirebaseDatabase.getInstance().getReference( "driversWorking" );
    public static final DatabaseReference mCustomerUberRequest = FirebaseDatabase.getInstance().getReference( "customerRequest" );
    public static final DatabaseReference mCustomerDestinationDbRef = FirebaseDatabase.getInstance().getReference( "customerDestination" );

    public static final GeoFire mGeoFireWorkingDrivers = new GeoFire( mWorkingDriversDbRef );
    public static final GeoFire mGeoFireAvailableDrivers = new GeoFire( mAvailableDriversDbRef );
    public static final GeoFire mGeoFireCustomerUberRequest = new GeoFire( mCustomerUberRequest );

    public static final StorageReference mProfileImageStorageRef = FirebaseStorage.getInstance().getReference().child( "profileImages" );

    public static void getUserData( final Context context ){
        userIdString = FirebaseAuth.getInstance().getUid();
        mFirebaseUser = FirebaseAuth.getInstance().getCurrentUser();

        mUsersDbRef.child( userIdString ).addValueEventListener( new ValueEventListener() {
            @Override
            public void onDataChange( @NonNull DataSnapshot dataSnapshot ) {
                if( dataSnapshot.exists() ){
                    mUser = dataSnapshot.getValue( User.class );
                }

            }

            @Override
            public void onCancelled( @NonNull DatabaseError databaseError ) {
                Log.e( TAG, "onCancelled: " + "getUserData: " + databaseError.getMessage() );
            }
        } );
    }

    public static void getFromFirebase( final OnDataReceiveCallback callback ){
        mUsersDbRef.child( userIdString ).addValueEventListener( new ValueEventListener() {
            @Override
            public void onDataChange( @NonNull DataSnapshot dataSnapshot ){
                mUser = dataSnapshot.getValue( User.class );

                callback.onDataReceived();
            }

            @Override
            public void onCancelled( @NonNull DatabaseError databaseError ){
            }
        });
    }


    public static void logoutUser( Activity activity ) {
        deleteAvailableDriverLocationFromDB( userIdString );

        FirebaseAuth.getInstance().signOut();

        SharedPref.setBool( "isDriver", false );
        mUser = null;
        userIdString = null;
        mFirebaseUser = null;

        Cache.deleteCache( activity );

        Intent intent = new Intent( activity, RegisterActivity.class );
        //clear back stack so on logout we exit app instead showing last visible account
        intent.addFlags( Intent.FLAG_ACTIVITY_CLEAR_TOP |
                         Intent.FLAG_ACTIVITY_CLEAR_TASK |
                         Intent.FLAG_ACTIVITY_NEW_TASK );

        activity.finish();
        activity.startActivity( intent );
    }

    public static void addAvailableDriverLocationToDB( String userIdString, double latitude, double longitude ){
        mGeoFireAvailableDrivers.setLocation( userIdString, new GeoLocation( latitude, longitude ), new GeoFire.CompletionListener() {
            @Override
            public void onComplete( String key, DatabaseError error ) {
                if ( error != null ) {
                    Log.e( TAG, "addAvailableDriverLocationToDatabase: " + R.string.geofire_saving_err + " " + error );
                }
            }
        } );
    }

    public static void deleteAvailableDriverLocationFromDB( String userIdString ) {
        mGeoFireAvailableDrivers.removeLocation( userIdString, new GeoFire.CompletionListener() {
            @Override
            public void onComplete( String key, DatabaseError error ) {
                if ( error != null ) {
                    Log.e( TAG, "deleteAvailableDriverLocationFromDB: " + R.string.geofire_removing_err + " " + error );
                }
            }
        } );
    }

    public static void addWorkingDriverLocationToDB( String userIdString, double latitude, double longitude ){
        mGeoFireWorkingDrivers.setLocation( userIdString, new GeoLocation( latitude, longitude ), new GeoFire.CompletionListener() {
            @Override
            public void onComplete( String key, DatabaseError error ) {
                if ( error != null ) {
                    Log.e( TAG, "addWorkingDriverLocationToDatabase: " + R.string.geofire_saving_err + " " + error );
                }
            }
        } );
    }

    public static void deleteWorkingDriverLocationFromDB( String userIdString ) {
        mGeoFireWorkingDrivers.removeLocation( userIdString, new GeoFire.CompletionListener() {
            @Override
            public void onComplete( String key, DatabaseError error ) {
                if ( error != null ) {
                    Log.e( TAG, "deleteWorkingDriverLocationFromDB: " + R.string.geofire_removing_err + " " +  error );
                }
            }
        } );
    }

    public static void addCustomerLocationToDB( double latitude, double longitude ){
        mGeoFireCustomerUberRequest.setLocation( userIdString, new GeoLocation( latitude, longitude ), new GeoFire.CompletionListener() {
            @Override
            public void onComplete( String key, DatabaseError error ) {
                if( error != null ){
                    Log.e( TAG, "addCustomerLocationToDB: " + R.string.geofire_saving_err + " " +  error );
                }
            }
        } );
    }

    public static void deleteCustomerRequestFromDB( String customerId ){
        mGeoFireCustomerUberRequest.removeLocation( customerId, new GeoFire.CompletionListener() {
            @Override
            public void onComplete( String key, DatabaseError error ) {
                if ( error != null ) {
                    Log.e( TAG, "deleteCustomerRequestFromDB: " + R.string.geofire_removing_err + " " + error );
                }
            }
        } );
    }

    public static void addCustomerToDb( String customerId, String foundDriverID ) {
        DatabaseReference customersFirebaseDBref = mCustomersDbRef.child( customerId );
        HashMap<String, Object> customerHashMap = new HashMap<>();
        customerHashMap.put( "foundDriverId", foundDriverID );
        customersFirebaseDBref.updateChildren( customerHashMap );
    }

    public static void deleteCustomerFromDb( String customerId ) {
        if ( customerId != null && !customerId.isEmpty() ) {
            DatabaseReference customersFirebaseDBref = mCustomersDbRef.child( customerId );
            customersFirebaseDBref.removeValue();
        }
    }

    public static void addDriverToDb( String customerId, String foundDriverID ) {
        DatabaseReference driversFirebaseDBref = mDriversDbRef.child( foundDriverID );
        HashMap<String, Object> customerHashMap = new HashMap<>();
        customerHashMap.put( "customerId", customerId );
        driversFirebaseDBref.updateChildren( customerHashMap );
    }

    public static void deleteDriverFromDb( String driverId ) {
        if ( driverId != null && !driverId.isEmpty() ) {
            DatabaseReference driversFirebaseDBref = mDriversDbRef.child( driverId );
            driversFirebaseDBref.removeValue();
        }
    }

    public static void removeCustomerDestinationFromDb( String customerId ){
        mCustomerDestinationDbRef.child( customerId ).removeValue();
    }
}
