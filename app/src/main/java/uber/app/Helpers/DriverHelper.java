package uber.app.Helpers;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.util.List;

import uber.app.Activities.MapActivity;
import uber.app.R;

import static uber.app.Helpers.FirebaseHelper.mCustomerUberRequest;
import static uber.app.Helpers.FirebaseHelper.mDriversDbRef;
import static uber.app.Helpers.FirebaseHelper.mUser;
import static uber.app.Helpers.FirebaseHelper.userIdString;

public class DriverHelper {
    private static final String TAG = "DriverHelper";

    private MapActivity mMapActivity;
    private String customerId;

    public DriverHelper( MapActivity mMapActivity ) {
        this.mMapActivity = mMapActivity;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId( String customerId ) {
        this.customerId = customerId;
    }

    public void getAssignedCustomer() {
        //if user is not a driver
        if ( mUser == null || !mUser.isDriver() )
            return;

        //if currently logged user is driver, check if he has any customer assigned
        DatabaseReference driversDbRef = mDriversDbRef.child( userIdString ).child( "customerId" );
        driversDbRef.addValueEventListener( new ValueEventListener() {
            @Override
            public void onDataChange( @NonNull DataSnapshot dataSnapshot ) {
                if ( dataSnapshot.exists() ) {
                    setCustomerId( dataSnapshot.getValue().toString() );
                    mMapActivity.changeButtonVisibility( mMapActivity.mRequestUberButton, false );
                    mMapActivity.leftDrawer.disableDriverToggleButtonState();
                    getAssignedCustomerPickupLocation( customerId );
                }
                else {
                    mMapActivity.leftDrawer.enableDriverToggleButtonState();
                    mMapActivity.resetDriverHelper();
                }
            }

            @Override
            public void onCancelled( @NonNull DatabaseError databaseError ) {
                Log.e( TAG, "onCancelled: " + "getAssignedCustomer: " + databaseError.getMessage() );
            }
        } );
    }

    private void getAssignedCustomerPickupLocation( String customerId ) {
        DatabaseReference assignedCustomerPickupLocationRef = mCustomerUberRequest.child( customerId ).child( "l" );
        assignedCustomerPickupLocationRef.addListenerForSingleValueEvent( new ValueEventListener() {
            @Override
            public void onDataChange( @NonNull DataSnapshot dataSnapshot ) {
                if ( dataSnapshot.exists() ) {
                    List<Object> customerLocation = ( List<Object> ) dataSnapshot.getValue();
                    double customerLat;
                    double customerLng;

                    //check if customer latitude and longitude in firebase is not null
                    if ( customerLocation.get( 0 ) != null && customerLocation.get( 1 ) != null ) {
                        customerLat = Double.parseDouble( customerLocation.get( 0 ).toString() );
                        customerLng = Double.parseDouble( customerLocation.get( 1 ).toString() );
                        LatLng customerLatLng = new LatLng( customerLat, customerLng );
                        String customerLocationMarker = mMapActivity.getResources().getString( R.string.customer_pickup );

                        //add customer's location marker
                        mMapActivity.addMarkerWithTitleAndIcon( customerLatLng, customerLocationMarker,
                                BitmapDescriptorFactory.fromResource( R.mipmap.ic_map_destination ));
                    }
                }
            }

            @Override
            public void onCancelled( @NonNull DatabaseError databaseError ) {
                Log.e( TAG, "onCancelled: " + "getAssignedCustomerPickupLocation: " + databaseError.getMessage() );
            }
        } );
    }
}