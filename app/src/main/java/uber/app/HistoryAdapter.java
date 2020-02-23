package uber.app;


import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import uber.app.Models.History;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder> {
    private Context context;
    private ArrayList<History> historyArrayList;
    private ArrayList<String> keys;
    private OnHistoryListener onHistoryListener;

    public HistoryAdapter( Context context, ArrayList<History> historyArrayList, ArrayList<String> keys, OnHistoryListener onHistoryListener ) {
        this.context = context;
        this.historyArrayList = historyArrayList;
        this.keys = keys;
        this.onHistoryListener = onHistoryListener;
    }

    @NonNull
    @Override
    public HistoryAdapter.HistoryViewHolder onCreateViewHolder( @NonNull ViewGroup parent, int viewType ) {
        View v = LayoutInflater.from( parent.getContext() ).inflate( R.layout.history_item, parent, false );

        return new HistoryViewHolder( v, onHistoryListener );
    }

    @Override
    public void onBindViewHolder( @NonNull HistoryAdapter.HistoryViewHolder holder, int position ) {
        History historyItem = historyArrayList.get( position );

        holder.mHistoryDate.setText( historyItem.getTimestamp() );
        holder.mHistoryId.setText( keys.get( position ) );
    }

    @Override
    public int getItemCount() {
        return historyArrayList.size();
    }

    class HistoryViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private TextView mHistoryDate;
        private TextView mHistoryId;
        private OnHistoryListener onHistoryListener;

        public HistoryViewHolder( @NonNull View itemView, OnHistoryListener onHistoryListener ) {
            super( itemView );

            mHistoryDate = itemView.findViewById( R.id.history_item_date );
            mHistoryId = itemView.findViewById( R.id.history_item_id );
            this.onHistoryListener = onHistoryListener;
            itemView.setOnClickListener( this );
        }

        @Override
        public void onClick( View v ) {
            onHistoryListener.onHistoryItemClick( getAdapterPosition() );
        }
    }

    public interface OnHistoryListener{
        void onHistoryItemClick( int position );
    }
}
