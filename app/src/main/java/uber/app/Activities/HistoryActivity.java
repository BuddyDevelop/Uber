package uber.app.Activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;


import java.lang.ref.WeakReference;

import uber.app.R;

public class HistoryActivity extends AppCompatActivity {
    private static final String TAG = "HistoryActivity";
    private TextView name, surname, email, isDriver;
    private ProgressBar progressBar;
    private myAsyncTask task;
    private Intent intent;

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_history );

        progressBar = findViewById( R.id.progressBar );

        task = new myAsyncTask( this );
        task.execute( 5 );

        intent = new Intent( this, BroadcastService.class );
        intent.setAction( BroadcastService.ACTION );
        intent.putExtra( "isWorking", true );

    }

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive( Context context, Intent intent ) {
            if ( intent.getAction().equals( BroadcastService.ACTION ) ) {
                String name = intent.getStringExtra( "name" );
                String surname = intent.getStringExtra( "surname" );
                TextView textView = findViewById( R.id.userName );
                textView.setText( name );

                TextView textView2 = findViewById( R.id.userSurname );
                textView2.setText( surname );
            }
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        startService( intent );
        registerReceiver( broadcastReceiver, new IntentFilter( BroadcastService.ACTION ) );
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver( broadcastReceiver );
        stopService( intent );
        if ( task != null && task.getStatus().equals( AsyncTask.Status.RUNNING ) )
            task.cancel( true );
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private static class myAsyncTask extends AsyncTask<Integer, Integer, String> {
        private WeakReference<HistoryActivity> historyActivityWeakReference;

        public myAsyncTask( HistoryActivity activity ) {
            historyActivityWeakReference = new WeakReference<HistoryActivity>( activity );
        }

        @Override
        protected void onPreExecute() {

            HistoryActivity historyActivity = historyActivityWeakReference.get();
            if ( historyActivity == null || historyActivity.isFinishing() )
                return;

            historyActivity.progressBar.setVisibility( View.VISIBLE );
            super.onPreExecute();
        }


        @Override
        protected String doInBackground( Integer... integers ) {
            for ( int i = 0; i <= integers[ 0 ]; i++ ) {
                if ( isCancelled() )
                    break;

                publishProgress( ( i * 100 ) / integers[ 0 ] );
                try {
                    Thread.sleep( 1000 );
                } catch ( InterruptedException e ) {
                    e.printStackTrace();
                }
            }

            return "Finished!";
        }


        @Override
        protected void onProgressUpdate( Integer... values ) {
            super.onProgressUpdate( values );

            HistoryActivity historyActivity = historyActivityWeakReference.get();
            if ( historyActivity == null || historyActivity.isFinishing() )
                return;
            historyActivity.progressBar.setProgress( values[ 0 ] );
        }


        @Override
        protected void onPostExecute( String s ) {
            super.onPostExecute( s );

            HistoryActivity historyActivity = historyActivityWeakReference.get();
            if ( historyActivity == null || historyActivity.isFinishing() )
                return;

            Toast.makeText( historyActivity, s, Toast.LENGTH_SHORT ).show();
        }

    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if ( task != null && task.getStatus().equals( AsyncTask.Status.RUNNING ) ) {
            task.cancel( true );
        }
    }
}
