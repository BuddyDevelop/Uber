package uber.app.Fragments;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;


import uber.app.R;


public class BaseDialogFragment extends DialogFragment {
    public static final String TAG = "BaseDialogFragment";

    public interface IOnPositiveNegativeBtnClick {
        void onPositiveBtnClick();

        void onNegativeBtnClick();
    }

    public BaseDialogFragment() {
        // Required empty public constructor
    }

    @Override
    public void onAttach( @NonNull Context context ) {
        super.onAttach( context );
        if ( !( getActivity() instanceof IOnPositiveNegativeBtnClick ) ) {
            throw new ClassCastException( getActivity().toString() + " must implement IOnPositiveNegativeBtnClick" );
        }
    }

    public BaseDialogFragment newInstance( int title, int msg ) {
        BaseDialogFragment fragment = new BaseDialogFragment();
        Bundle args = new Bundle();
        args.putInt( "title", title );
        args.putInt( "msg", msg );

        fragment.setArguments( args );
        return fragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog( @Nullable Bundle savedInstanceState ) {
        int title = getArguments().getInt( "title" );
        int msg = getArguments().getInt( "msg" );

        return new AlertDialog.Builder( getActivity() )
                .setTitle( title )
                .setMessage( msg )
                .setPositiveButton( R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick( DialogInterface dialog, int which ) {
                        ( ( IOnPositiveNegativeBtnClick ) getActivity() ).onPositiveBtnClick();
                    }
                } )
                .setNegativeButton( R.string.cancel_str,
                        new DialogInterface.OnClickListener() {
                            public void onClick( DialogInterface dialog, int whichButton ) {
                                dismiss();
                            }
                        }
                )
                .create();
    }
}
