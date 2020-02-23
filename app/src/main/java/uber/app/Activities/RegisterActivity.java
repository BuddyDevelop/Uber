package uber.app.Activities;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import uber.app.Fragments.SignUpFragment;
import uber.app.Fragments.LoginFragment;

import uber.app.R;
import uber.app.SharedPref;


public class RegisterActivity extends AppCompatActivity {

    private FragmentManager fragmentManager;
    public static final String LoginFragment = "Login_Fragment";
    public static final String SignUpFragment = "SignUp_Fragment";
    public static final String ForgotPasswordFragment = "ForgotPassword_Fragment";

    @Override
    protected void onCreate( @Nullable Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );
        setTheme( R.style.AppTheme );
        setContentView( R.layout.register_wrapper );

        SharedPref.initialize( getApplicationContext() );

        fragmentManager = getSupportFragmentManager();

        // If savedInstanceState is null then replace login fragment
        if ( savedInstanceState == null ) {
            fragmentManager
                    .beginTransaction()
                    .replace( R.id.frameContainer, new SignUpFragment() )
                    .commit();
        }

        // On close icon click finish activity
        findViewById( R.id.close_activity ).setOnClickListener(
                arg0 -> finish() );

    }

    // Replace Login Fragment with animation
    protected void replaceLoginFragment() {
        fragmentManager
                .beginTransaction()
                .setCustomAnimations( R.anim.left_enter_animation, R.anim.right_exit_animation )
                .replace( R.id.frameContainer, new LoginFragment() )
                .commit();
    }

    @Override
    public void onBackPressed() {

        // Find the tag of sign up and forgot password fragment
        Fragment SignUp_Fragment = fragmentManager
                .findFragmentByTag( SignUpFragment );
        Fragment ForgotPassword_Fragment = fragmentManager
                .findFragmentByTag( ForgotPasswordFragment );

        // Check if both are null or not
        // If both are not null then replace login fragment else do backpressed
        if ( SignUp_Fragment != null )
            replaceLoginFragment();
        else if ( ForgotPassword_Fragment != null )
            replaceLoginFragment();
        else
            super.onBackPressed();
    }
}
