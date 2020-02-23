package uber.app.Helpers;

import android.location.Location;
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
import uber.app.Interfaces.IAddHistoryRecord;
import uber.app.R;
import uber.app.Util;

import static uber.app.Helpers.FirebaseHelper.mCustomerUberRequest;
import static uber.app.Helpers.FirebaseHelper.mDriversDbRef;
import static uber.app.Helpers.FirebaseHelper.mUser;
import static uber.app.Helpers.FirebaseHelper.userIdString;

public class DriverHelper implements IAddHistoryRecord {
    private static final String TAG = "DriverHelper";

    private MapActivity mMapActivity;
    private String customerId;

    public DriverHelper( MapActivity mMapActivity ) {
        this.mMapActivity = mMapActivity;
    }
    public String getCustomerId() { return customerId; }
    public void setCustomerId( String customerId ) { this.customerId = customerId; }

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

                    Util.hideRelativeLayout( mMapActivity.relativeLayout );
                    Util.changeButtonVisibility( mMapActivity.mPickupCustomerBtn, true );
                    mMapActivity.leftDrawer.disableDriverToggleButtonState();
                    getAssignedCustomerPickupLocation( customerId );
                    mMapActivity.getUserInfo( customerId );
                    mMapActivity.getCustomerDestination( customerId );
                }
                else {
                    Util.changeButtonVisibility( mMapActivity.mPickupCustomerBtn, false );
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
                    LatLng customerLatLng = Util.getLatLng( customerLocation );

                    if( customerLatLng != null ) {
                        String customerLocationMarker = mMapActivity.getResources().getString( R.string.customer_pickup );
                        //add customer's location marker
                        mMapActivity.addMarkerWithTitleAndIcon( customerLatLng, customerLocationMarker,
                                BitmapDescriptorFactory.fromResource( R.mipmap.ic_map_destination ) );

                        Location driverLocation = mMapActivity.getLastLocation();
                        if ( driverLocation != null ) {
                            LatLng driverLatLng = new LatLng( driverLocation.getLatitude(), driverLocation.getLongitude() );
                            //create route to customer
                            mMapActivity.getRouteToLocation( driverLatLng, customerLatLng );
                        }
                    }
                }
                else {
                    mMapActivity.hideUserInfo();
                }
            }

            @Override
            public void onCancelled( @NonNull DatabaseError databaseError ) {
                Log.e( TAG, "getAssignedCustomerPickupLocation: " + databaseError.getMessage() );
            }
        } );
    }
}
