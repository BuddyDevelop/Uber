package uber.app;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.Nullable;

public class SharedPref {
    private static SharedPreferences mSharedPref;

    public static final double DEFAULT_DOUBLE = 5000;
    private static final String SHARED_PREF = "SharedPref";

    //prevents any other class from instantiating
    private SharedPref() {
    }

    public static void initialize( Context context ){
        if( mSharedPref == null )
            mSharedPref = context.getSharedPreferences( SHARED_PREF, Context.MODE_PRIVATE );
    }

    public static void setStr( String key, String value ){
        SharedPreferences.Editor editor = mSharedPref.edit();
        editor.putString( key, value );
        editor.apply();
    }

    public static String getStr( String key, @Nullable String defaultValue ){
        return mSharedPref.getString( key, defaultValue );
    }

    public static void setInt( String key, int value ){
        SharedPreferences.Editor editor = mSharedPref.edit();
        editor.putInt( key, value );
        editor.apply();
    }

    public static int getInt( String key, int defaultValue ){
        return mSharedPref.getInt( key, defaultValue );
    }

    public static void setBool( String key, boolean value ){
        SharedPreferences.Editor editor = mSharedPref.edit();
        editor.putBoolean( key, value );
        editor.apply();
    }

    public static boolean getBool( String key ){
        return mSharedPref.getBoolean( key, false );
    }

    public static void setLong( String key, long value ){
        SharedPreferences.Editor editor = mSharedPref.edit();
        editor.putLong( key, value );
        editor.apply();
    }

    public static long getLong( String key ){
        return mSharedPref.getLong( key, 0 );
    }

    public static void setDouble( String key, double value ){
        SharedPreferences.Editor editor = mSharedPref.edit();
        editor.putLong( key, Double.doubleToLongBits( value ) );
        editor.apply();
    }

    public static double getDouble( String key ){
        return Double.longBitsToDouble( mSharedPref.getLong( key, Double.doubleToLongBits( DEFAULT_DOUBLE ) ) );
    }
}
