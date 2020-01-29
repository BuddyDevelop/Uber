package uber.app.Fragments;


import android.os.Bundle;
import android.text.InputType;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;


import java.util.regex.Matcher;
import java.util.regex.Pattern;

import uber.app.CustomToast;
import uber.app.Helpers.FirebaseHelper;
import uber.app.R;

import static uber.app.Fragments.SignUpFragment.userAlreadyLogged;

public class LoginFragment extends Fragment implements OnClickListener {
    private static View view;

    private EditText email, password;
    private ProgressBar progressBar;
    private Button loginButton;
    private TextView forgotPassword, signUp;
    private CheckBox show_hide_password;
    private LinearLayout loginLayout;
    private static Animation shakeAnimation;
    private static FragmentManager fragmentManager;
    public static final String emailRegEx = "\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,4}\\b";

    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mFirebaseAuthListener;


    public LoginFragment() {
    }

    @Override
    public View onCreateView( LayoutInflater inflater, ViewGroup container,
                              Bundle savedInstanceState ) {
        view = inflater.inflate( R.layout.login_layout, container, false );
        initViews();
        setListeners();
        return view;
    }

    // Initiate Views
    private void initViews() {
        fragmentManager = getActivity().getSupportFragmentManager();

        email = view.findViewById( R.id.login_emailid );
        password = view.findViewById( R.id.login_password );
        loginButton = view.findViewById( R.id.loginBtn );
        forgotPassword = view.findViewById( R.id.forgot_password );
        signUp = view.findViewById( R.id.createAccount );
        show_hide_password = view
                .findViewById( R.id.show_hide_password );
        loginLayout = view.findViewById( R.id.login_layout );
        progressBar = view.findViewById( R.id.loginProgressBar );
        progressBar.setVisibility( View.GONE );

        // Load ShakeAnimation
        shakeAnimation = AnimationUtils.loadAnimation( getActivity(),
                R.anim.shake_animation );

        mAuth = FirebaseAuth.getInstance();
    }

    // Set Listeners
    private void setListeners() {
        loginButton.setOnClickListener( this );
        forgotPassword.setOnClickListener( this );
        signUp.setOnClickListener( this );

        // Set check listener over checkbox for showing and hiding password
        show_hide_password
                .setOnCheckedChangeListener( new OnCheckedChangeListener() {

                    @Override
                    public void onCheckedChanged( CompoundButton button,
                                                  boolean isChecked ) {

                        // If it is checked then show password else hide password
                        if ( isChecked ) {

                            show_hide_password.setText( R.string.hide_pswd );// change

                            password.setInputType( InputType.TYPE_CLASS_TEXT );
                            password.setTransformationMethod( HideReturnsTransformationMethod
                                    .getInstance() );// show password
                        } else {
                            show_hide_password.setText( R.string.show_pswd );// change

                            password.setInputType( InputType.TYPE_CLASS_TEXT
                                    | InputType.TYPE_TEXT_VARIATION_PASSWORD );
                            password.setTransformationMethod( PasswordTransformationMethod
                                    .getInstance() );// hide password

                        }

                    }
                } );
    }

    @Override
    public void onClick( View v ) {
        switch ( v.getId() ) {
            case R.id.loginBtn:
                checkValidation();
                break;

            case R.id.forgot_password:

                // Replace forgot password fragment with animation
                fragmentManager
                        .beginTransaction()
                        .setCustomAnimations( R.anim.right_enter_animation, R.anim.left_exit_animation )
                        .replace( R.id.frameContainer, new ForgotPasswordFragment() )
                        .commit();
                break;
            case R.id.createAccount:

                // Replace signup fragment with animation
                fragmentManager
                        .beginTransaction()
                        .setCustomAnimations( R.anim.right_enter_animation, R.anim.left_exit_animation )
                        .replace( R.id.frameContainer, new SignUpFragment() )
                        .commit();
                break;
        }

    }

    // Check Validation before login
    private void checkValidation() {
        // Get email and password
        String emailString = email.getText().toString();
        String passwordString = password.getText().toString();

        // Check patter for email
        Pattern p = Pattern.compile( emailRegEx );
        Matcher m = p.matcher( emailString );

        // Check for both field is empty or not
        if ( emailString.equals( "" ) || emailString.length() == 0
                || passwordString.equals( "" ) || passwordString.length() == 0 ) {
            loginLayout.startAnimation( shakeAnimation );
            new CustomToast().showToast( getActivity(), view,
                    getString( R.string.login_error ) );

        }
        // Check if email is valid or not
        else if ( !m.find() )
            new CustomToast().showToast( getActivity(), view,
                    getString( R.string.invalid_email ) );
            // Else do login
        else
            signIn( emailString, passwordString );

    }

    private void signIn( String email, String password ) {
        progressBar.setVisibility( View.VISIBLE );

        mAuth.signInWithEmailAndPassword( email, password )
                .addOnCompleteListener( new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete( @NonNull Task<AuthResult> task ) {
                        if ( task.isSuccessful() ) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d( "signIn: success", "signInWithEmail:success" );

                            //get currently registered user's data
                            FirebaseHelper.getUserData( getActivity() );

                            //change activity
                            userAlreadyLogged( getActivity() );
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w( "signIn: fail", "signInWithEmail:failure", task.getException() );
                            new CustomToast().showToast( getActivity(), view, getString( R.string.authentication_failed ) );
                        }
                        progressBar.setVisibility( View.GONE );
                    }
                } );
    }

    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        mFirebaseAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged( @NonNull FirebaseAuth firebaseAuth ) {
                FirebaseUser currentUser = mAuth.getCurrentUser();
                if ( currentUser != null )
                    userAlreadyLogged( getActivity() );
            }
        };
    }
}
