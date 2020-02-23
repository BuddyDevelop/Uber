package uber.app.Fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.ybq.android.spinkit.sprite.Sprite;
import com.github.ybq.android.spinkit.style.ThreeBounce;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import uber.app.HistoryAdapter;
import uber.app.Models.History;
import uber.app.R;

import static uber.app.Helpers.FirebaseHelper.userIdString;

public class HistoryFragment extends Fragment implements HistoryAdapter.OnHistoryListener {
    private HistoryAdapter mHistoryAdapter;
    private ArrayList<History> mHistoryArrayList = new ArrayList<>();
    private ArrayList<String> keys = new ArrayList<>();

    private DatabaseReference historyPath;

    @BindView( R.id.recycler_history )
    RecyclerView mRecyclerView;
    @BindView( R.id.spin_kit )
    ProgressBar mProgressBar;
    @BindView( R.id.no_data )
    TextView mNoDataTV;

    public HistoryFragment() { }

    public HistoryFragment( DatabaseReference historyPath ) {
        this.historyPath = historyPath;
    }

    @Nullable
    @Override
    public View onCreateView( @NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState ) {
        View view = inflater.inflate( R.layout.fragment_customer_history, container, false );

        ButterKnife.bind( this, view );
        mRecyclerView.setLayoutManager( new LinearLayoutManager( getActivity().getBaseContext() ) );
        mRecyclerView.setHasFixedSize( true );

        Sprite threeBounce = new ThreeBounce();
        mProgressBar.setIndeterminateDrawable( threeBounce );

        loadCustomerHistory( historyPath );
        return view;
    }

    private void loadCustomerHistory( DatabaseReference historyPath ) {
        if( userIdString == null || historyPath == null )
            return;

        historyPath.child( userIdString ).addValueEventListener( new ValueEventListener() {
            @Override
            public void onDataChange( @NonNull DataSnapshot dataSnapshot ) {
                mProgressBar.setVisibility( View.GONE );

                if ( dataSnapshot.exists() ) {
                    mHistoryArrayList.clear();
                    History history;

                    for ( DataSnapshot historyRecord : dataSnapshot.getChildren() ) {
                        keys.add( historyRecord.getKey() );
                        history = historyRecord.getValue( History.class );
                        mHistoryArrayList.add( history );
                    }
                    mHistoryAdapter = new HistoryAdapter( getContext(), mHistoryArrayList, keys, HistoryFragment.this );
                    mRecyclerView.setAdapter( mHistoryAdapter );
                }else{
                    if( mHistoryArrayList.isEmpty() )
                        mNoDataTV.setVisibility( View.VISIBLE );
                    else
                        mNoDataTV.setVisibility( View.GONE );
                }
            }

            @Override
            public void onCancelled( @NonNull DatabaseError databaseError ) {
                Log.e( "loadCustomerHistory: ", databaseError.getMessage()  );
            }
        } );
    }

    @Override
    public void onHistoryItemClick( int position ) {
        Toast.makeText( getContext(), keys.get( position ) , Toast.LENGTH_SHORT ).show();
    }
}
