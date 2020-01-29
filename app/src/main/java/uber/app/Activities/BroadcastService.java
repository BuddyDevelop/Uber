package uber.app.Activities;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

public class BroadcastService extends Service {
    private static final String TAG = "BroadcastService";
    public static final String ACTION = "BroadcastService.action";

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand( Intent intent, int flags, int startId ) {
        boolean working = false;
        working = intent.getBooleanExtra( "isWorking", false );
        if( working )
            Log.i( TAG, "onStartCommand: " + working );

        Intent intent1 = new Intent(  );
        intent1.setAction( ACTION );
        intent1.putExtra( "name", "Grzegorz" );
        intent1.putExtra( "surname", "Bury" );
        sendBroadcast( intent1 );

        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind( Intent intent ) {
        return null;
    }
}
