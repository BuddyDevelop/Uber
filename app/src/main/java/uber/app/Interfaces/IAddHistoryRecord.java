package uber.app.Interfaces;

import android.annotation.SuppressLint;
import android.util.Log;

import androidx.annotation.NonNull;


import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;

import static uber.app.Helpers.FirebaseHelper.mCustomerHistoryDbRef;
import static uber.app.Helpers.FirebaseHelper.mDriverHistoryDbRef;

public interface IAddHistoryRecord {
    default void addHistoryRecord( @NonNull String customerId, @NonNull String driverId, int rating ){
        String recordId = mCustomerHistoryDbRef.child( customerId ).push().getKey();

        Calendar c = Calendar.getInstance();
        @SuppressLint( "SimpleDateFormat" )
        SimpleDateFormat dateFormat = new SimpleDateFormat("E, dd MMM, yyyy HH:mm:ss z");
        String datetime = dateFormat.format( c.getTime() );

        HashMap historyRecord = new HashMap(  );
        historyRecord.put( "customerId", customerId );
        historyRecord.put( "driverId", driverId );
        historyRecord.put( "rating", rating );
        historyRecord.put( "timestamp", datetime );

        mCustomerHistoryDbRef.child( customerId ).child( recordId ).setValue( historyRecord, ( databaseError, databaseReference ) -> {
            if( databaseError != null )
                Log.e( "addHistoryRecord: ", databaseError.getMessage()  );
        } );

        mDriverHistoryDbRef.child( driverId ).child( recordId ).setValue( historyRecord, ( ( databaseError, databaseReference ) -> {
            if( databaseError != null )
                Log.e( "addHistoryRecord: ", databaseError.getMessage() );
        }) );
    }
}
