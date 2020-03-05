package uber.app;

import android.content.Context;
import android.util.Log;

import com.directions.route.AbstractRouting;
import com.directions.route.Route;
import com.directions.route.RouteException;
import com.directions.route.Routing;
import com.directions.route.RoutingListener;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.List;

public class Routes implements RoutingListener {
    private Context context;

    private GoogleMap mMap;
    private List< Polyline > polylines = new ArrayList<>(  );
    private static final int[] COLORS = new int[]{ R.color.primary_dark, R.color.md_amber_50 };

    public Routes() {}

    public Routes( Context context, GoogleMap map ){
        this.context = context;
        this.mMap = map;
    }
    /*
    Route drawing
 */
    @Override
    public void onRoutingFailure( RouteException e ) {
        if( e != null && e.getMessage() != null ){
            Log.e( "onRoutingFailure: ", e.getMessage() );
        }
    }

    @Override
    public void onRoutingStart() {  }

    @Override
    public void onRoutingSuccess( ArrayList<Route> route, int shortestRouteIndex ) {
        if( polylines.size() > 0 ) {
            for ( Polyline poly : polylines ) {
                poly.remove();
            }
        }

        polylines = new ArrayList<>();
        //add route(s) to the map.
        for ( int i = 0; i < route.size(); i++ ) {
            int colorIndex;
            colorIndex = i == 0 ? i : 1;


            PolylineOptions polyOptions = new PolylineOptions();
            polyOptions.color( context.getResources().getColor( COLORS[ colorIndex ] ) );
            if( i == 0 )
                polyOptions.width( 22 );
            else
                polyOptions.width( 14 );
            polyOptions.addAll( route.get( i ).getPoints() );
            Polyline polyline = mMap.addPolyline( polyOptions );
            this.polylines.add( polyline );
        }
    }

    @Override
    public void onRoutingCancelled() {  }

    public void getRouteToLocation( LatLng currentLatLng, LatLng destinationLatLng, boolean alternativeRoutes ){
        Routing routing = new Routing.Builder()
                .travelMode( AbstractRouting.TravelMode.DRIVING )
                .withListener( this )
                .alternativeRoutes( alternativeRoutes )
                .waypoints( currentLatLng, destinationLatLng )
                .key( context.getResources().getString( R.string.google_api_key ) )
                .build();
        routing.execute();
    }

      /*
        END Route drawing
     */
}
