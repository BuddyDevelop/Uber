package uber.app.Helpers;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import uber.app.R;

public class CheckNetwork {
    private static final String TAG = "CheckNetwork";

    public static boolean isInternetAvailable( Context context ){
        NetworkInfo networkInfo = (( ConnectivityManager ) context.getSystemService( Context.CONNECTIVITY_SERVICE ))
                .getActiveNetworkInfo();

        if( networkInfo == null || !networkInfo.isConnected() || !networkInfo.isAvailable() ){
            Log.d( TAG, "isInternetAvailable: " + R.string.no_network_err );
            return false;
        }

        return true;
    }
}
