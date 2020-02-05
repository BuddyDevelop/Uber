package uber.app;

import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.RelativeLayout;

import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.material.button.MaterialButton;

public class Util {
    //hide button
    public static void hideButton( MaterialButton mRequestUberButton ){
        mRequestUberButton.setVisibility( View.INVISIBLE );
        mRequestUberButton.setHeight( 0 );
    }

    //make button visible and change it's height
    public static void showButton( MaterialButton mRequestUberButton ){
        mRequestUberButton.setVisibility( View.VISIBLE );
        ViewGroup.LayoutParams layoutParams = mRequestUberButton.getLayoutParams();
        layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        mRequestUberButton.setLayoutParams( layoutParams );
    }

    public static void hideRelativeLayout( RelativeLayout relativeLayout ){
        if( relativeLayout != null )
            relativeLayout.setVisibility( View.INVISIBLE );
    }

    public static void showRelativeLayout( RelativeLayout relativeLayout ){
        if( relativeLayout != null )
            relativeLayout.setVisibility( View.VISIBLE );
    }

    public static void setLayoutToFullscreen( Window window ){
        //make status bar transparent so map is also visible in status bar
        if ( Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT ) {
            window.setFlags( WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS );
            window.getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN );
            window.setFlags( WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION, WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION );
            window.setFlags( WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS, WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS );
        }

        //make status bar transparent so map is also visible in status bar
        if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP ) {
//            window.setNavigationBarColor( Color.BLACK );
            window.setFlags( WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS );
            window.getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN );
            window.setStatusBarColor( Color.TRANSPARENT );
        }

        //make status bar icons grey so they has better visibility
        if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.M )
            window.getDecorView().setSystemUiVisibility( View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR );
    }

    public static void changeMapsMyLocationButton( Context context, SupportMapFragment mapFragment ) {
        int dp30ToPixels = Math.round( 30 * ( context.getResources().getDisplayMetrics().xdpi / DisplayMetrics.DENSITY_DEFAULT ) );

        //get reference to my location icon
        View locationButton = ( ( View ) mapFragment.getView().findViewById( Integer.parseInt( "1" ) ).
                getParent() ).findViewById( Integer.parseInt( "2" ) );

        if( locationButton == null )
            return;


        // and next place it at top with some margin
        RelativeLayout.LayoutParams rlp = ( RelativeLayout.LayoutParams ) locationButton.getLayoutParams();
        rlp.addRule( RelativeLayout.ALIGN_PARENT_TOP, 0 );

        //add margin to button with pixels
        rlp.setMargins( 0, dp30ToPixels, 0, 0 );
    }
}
