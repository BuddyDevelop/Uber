package uber.app.Activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Objects;

import butterknife.BindView;
import butterknife.ButterKnife;
import uber.app.Fragments.HistoryFragment;
import uber.app.Models.History;
import uber.app.Models.User;
import uber.app.R;
import uber.app.Routes;
import uber.app.Util;

import static uber.app.Helpers.FirebaseHelper.mUsersDbRef;
import static uber.app.Helpers.FirebaseHelper.userIdString;

public class HistorySingleActivity extends AppCompatActivity implements OnMapReadyCallback {
    private static final String TAG = "HistorySingleActivity";
    private final String driverHistory = "driverHistory";
    private static final float MAP_ZOOM = 13;

    private GoogleMap mMap;
    private SupportMapFragment mMapFragment;
    private Routes mRoutes;

    private DatabaseReference historyUrl;
    private String historyRideId;
    private History history;

    @BindView( R.id.ride_distance )
    TextView rideDistance;
    @BindView( R.id.ride_date )
    TextView rideDate;
    @BindView( R.id.user_profile_image_history )
    ImageView userProfileImage;
    @BindView( R.id.user_name_history )
    TextView userName;
    @BindView( R.id.user_phone_history )
    TextView userPhone;
//    @BindView( R.id.rating_bar )
//    RatingBar ratingBar;

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_history_single );

        mMapFragment = ( SupportMapFragment ) getSupportFragmentManager().findFragmentById( R.id.history_map );
        mMapFragment.getMapAsync( this );

        ButterKnife.bind( this );
        initToolbar();
        getBundles();
    }

    @Override
    public void onMapReady( GoogleMap googleMap ) {
        mMap = googleMap;
        mRoutes = new Routes( this, mMap );
        getHistoryDetails();
    }

    private void getBundles(){
        Bundle dataBundle = getIntent().getExtras();
        if( dataBundle != null ){
            historyRideId = dataBundle.getString( HistoryFragment.HISTORY_RIDE_ID );
            historyUrl = FirebaseDatabase.getInstance().getReferenceFromUrl(
                    Objects.requireNonNull( dataBundle.getString( HistoryFragment.HISTORY_URL ) ) );
        }
    }

    private void getHistoryDetails(){
        historyUrl.child( userIdString ).child( historyRideId ).addListenerForSingleValueEvent( new ValueEventListener() {
            @Override
            public void onDataChange( @NonNull DataSnapshot dataSnapshot ) {
                if( dataSnapshot.exists() ){
                    history = dataSnapshot.getValue( History.class );

                    getUserDetails();
                    setHistoryDetails();
                }
            }

            @Override
            public void onCancelled( @NonNull DatabaseError databaseError ) {
                Log.e( TAG, "onCancelled: getHistoryDetails " + databaseError.getMessage()  );
            }
        } );
    }

    private void getUserDetails(){
        String userId;
        if( historyUrl.toString().contains( driverHistory ) )
            userId = history.getCustomerId();
        else
            userId = history.getDriverId();

        mUsersDbRef.child( userId ).addListenerForSingleValueEvent( new ValueEventListener() {
            @Override
            public void onDataChange( @NonNull DataSnapshot dataSnapshot ) {
                if( dataSnapshot.exists() ){
                    User user = dataSnapshot.getValue( User.class );
                    setUserDetails( user );
                }
            }

            @Override
            public void onCancelled( @NonNull DatabaseError databaseError ) {
                Log.e( TAG, "onCancelled: getUserDetails", new Throwable( databaseError.getMessage() ) );
            }
        } );
    }

    private void setHistoryDetails() {
        rideDate.setText( history.getTimestamp() );

        String fromMarkerTitle = getResources().getString( R.string.pickup_spot );
        String toMarkerTitle = getResources().getString( R.string.destination_spot );
        LatLng fromLatLng = new LatLng( history.getFromLatLng().get( 0 ), history.getFromLatLng().get( 1 ) );
        LatLng toLatLng = new LatLng( history.getToLatLng().get( 0 ), history.getToLatLng().get( 1 ) );

        mRoutes.getRouteToLocation( fromLatLng, toLatLng, false );
        addMarkerWithTitleAndIcon( fromLatLng, fromMarkerTitle, BitmapDescriptorFactory.fromResource( R.mipmap.ic_uber_car ) );
        addMarkerWithTitleAndIcon( toLatLng, toMarkerTitle, BitmapDescriptorFactory.fromResource( R.mipmap.ic_map_destination ) );

        int distance = ( int ) Util.calculateDistance( fromLatLng.latitude, fromLatLng.longitude, toLatLng.latitude, toLatLng.longitude );
        rideDistance.setText(  getString( R.string.distance_value, distance ) );

        mMap.moveCamera( CameraUpdateFactory.newLatLngZoom( fromLatLng, MAP_ZOOM ) );
    }

    private void setUserDetails( User user ) {
        if( user == null )
            return;

        String userPhoneString = user.getPhoneNumber();
        String userImage = user.getProfileImageUrl();

        userName.setText( getString( R.string.user_full_name_value, user.getName(), user.getSurname() )  );

        if( userPhoneString != null && !userPhoneString.isEmpty() )
            userPhone.setText( user.getPhoneNumber() );

        if( userImage != null && !userImage.isEmpty() )
            Glide
                    .with( getApplication() )
                    .load( user.getProfileImageUrl() )
                    .into( userProfileImage );
    }

    private void initToolbar() {
        if( getSupportActionBar() != null ){
            getSupportActionBar().setTitle( "History details" );
            getSupportActionBar().setDisplayHomeAsUpEnabled( true );
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return super.onSupportNavigateUp();
    }

    public Marker addMarkerWithTitleAndIcon( LatLng latLng, String markerTitle, BitmapDescriptor icon ) {
        if ( mMap != null ) {
            //set marker with customer pickup coordinates
            return mMap.addMarker( new MarkerOptions().position( latLng ).title( markerTitle ).icon( icon ) );
        }
        return null;
    }
}
