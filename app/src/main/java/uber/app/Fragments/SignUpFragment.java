package uber.app.Fragments;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.FirebaseDatabase;


import java.util.regex.Matcher;
import java.util.regex.Pattern;

import uber.app.Activities.MapActivity;
import uber.app.CustomToast;
import uber.app.Helpers.FirebaseHelper;
import uber.app.Models.User;
import uber.app.R;

import static uber.app.Helpers.FirebaseHelper.mUsersDbRef;

public class SignUpFragment extends Fragment implements OnClickListener {
    private View view;
    private EditText userName, email, userSurname,
            password, confirmPassword;
    private TextView login;
    private Button signUpButton;
    private CheckBox driver;
    private ProgressBar progressBar;
    private User user;

    private FirebaseAuth mAuth;
    private FirebaseDatabase mFirebaseDatabase;

    public SignUpFragment() {
    }

    @Override
    public View onCreateView( LayoutInflater inflater, ViewGroup container,
                              Bundle savedInstanceState ) {
        view = inflater.inflate( R.layout.signup_layout, container, false );
        initViews();
        setListeners();
        return view;
    }

    // Initialize all views
    private void initViews() {
        userName = view.findViewById( R.id.name );
        email = view.findViewById( R.id.userEmailId );
        userSurname = view.findViewById( R.id.surname );
        password = view.findViewById( R.id.password );
        confirmPassword = view.findViewById( R.id.confirmPassword );
        signUpButton = view.findViewById( R.id.signUpBtn );
        login = view.findViewById( R.id.already_user );
        driver = view.findViewById( R.id.driver );
        progressBar = view.findViewById( R.id.progressBar );
        progressBar.setVisibility( View.GONE );

        mAuth = FirebaseAuth.getInstance();
        mFirebaseDatabase = FirebaseDatabase.getInstance();
    }

    @Override
    public void onStart() {
        super.onStart();

        if ( mAuth.getCurrentUser() != null ) {
            userAlreadyLogged( getActivity() );
        }
    }

    public static void userAlreadyLogged( Context context ) {
        Intent intent = new Intent( context, MapActivity.class );
        context.startActivity( intent );
        intent.addFlags( Intent.FLAG_ACTIVITY_CLEAR_TOP );
        ( ( Activity ) context ).finish();
    }

    // Set Listeners
    private void setListeners() {
        signUpButton.setOnClickListener( this );
        login.setOnClickListener( this );
    }

    @Override
    public void onClick( View v ) {
        switch ( v.getId() ) {
            case R.id.signUpBtn:

                checkValidation();
                break;

            case R.id.already_user:

                // Replace login fragment
                getActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .setCustomAnimations( R.anim.left_enter_animation, R.anim.right_exit_animation )
                        .replace( R.id.frameContainer, new LoginFragment() )
                        .commit();

                break;
        }

    }

    // validate fields
    private void checkValidation() {
        // Get all editText texts
        String userName = this.userName.getText().toString().trim();
        String emailString = email.getText().toString().trim();
        String userSurname = this.userSurname.getText().toString().trim();
        String passwordString = password.getText().toString();
        String confirmPasswordString = confirmPassword.getText().toString();


        // Pattern match for email
        Pattern emailPattern = Pattern.compile( LoginFragment.emailRegEx );
        Matcher emailMatcher = emailPattern.matcher( emailString );

        // Check if all strings are null or not
        if ( userName.equals( "" ) || userName.length() == 0
                || userSurname.equals( "" ) || userSurname.length() == 0
                || emailString.equals( "" ) || emailString.length() == 0
                || passwordString.equals( "" ) || passwordString.length() == 0
                || confirmPasswordString.equals( "" )
                || confirmPasswordString.length() == 0 )

            new CustomToast().showToast( getActivity(), view,
                    getString( R.string.all_fields_required_err ) );

        else if ( userName.length() < 4 )
            new CustomToast().showToast( getActivity(), view,
                    getString( R.string.name_err ) );

            // Check if email id valid or not
        else if ( !emailMatcher.find() )
            new CustomToast().showToast( getActivity(), view,
                    getString( R.string.invalid_email ) );

        else if ( userSurname.length() < 4 )
            new CustomToast().showToast( getActivity(), view,
                    getString( R.string.surname_err ) );

        else if ( passwordString.length() < 6 )
            new CustomToast().showToast( getActivity(), view,
                    getString( R.string.password_length_err ) );

            // Check if both password should be equal
        else if ( !confirmPasswordString.equals( passwordString ) )
            new CustomToast().showToast( getActivity(), view,
                    getString( R.string.passwords_not_match_err ) );

//            // Make sure user should check Terms and Conditions checkbox
//        else if ( !driver.isChecked() )
//            new CustomToast().showToast( getActivity(), view,
//                    getString( R.string.terms_err ) );

            // Else do signup
        else {
            progressBar.setVisibility( View.VISIBLE );
            boolean isDriver = driver.isChecked();
            RegisterUser( emailString, passwordString, userName, userSurname, isDriver );
        }
    }

    private void RegisterUser( final String email, String password, final String name, final String surname, final boolean isDriver ) {
        mAuth.createUserWithEmailAndPassword( email, password )
                .addOnCompleteListener( task -> {
                    if ( task.isSuccessful() ) {

                        user = new User(
                                name,
                                email,
                                surname,
                                isDriver
                        );

                        //save data to database
                        mUsersDbRef
                                .child( FirebaseAuth.getInstance().getCurrentUser().getUid() )
                                .setValue( user )
                                .addOnCompleteListener( task1 -> {
                                    progressBar.setVisibility( View.GONE );
                                    if ( task1.isSuccessful() ) {
                                        setUserName( name, surname );

                                        //get currently registered user's data
                                        FirebaseHelper.getUserData( getActivity() );

                                        Toast.makeText( getActivity(), R.string.registration_successful, Toast.LENGTH_SHORT ).show();
                                        userAlreadyLogged( getContext() );
                                    } else
                                        new CustomToast().showToast( getActivity(), view,
                                                getString( R.string.sth_went_wrong_err ) );
                                } );

                    } else
                        new CustomToast().showToast( getActivity(), view,
                                task.getException().getMessage() );
                    progressBar.setVisibility( View.GONE );
                } );
    }

    private void setUserName( @NonNull String name, @NonNull String surname ){
        String userName = name + " " + surname;
        //make user profile with name
        FirebaseUser user = mAuth.getCurrentUser();

        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder().setDisplayName( userName ).build();
        user.updateProfile( profileUpdates ).addOnCompleteListener( task -> {
            if( !task.isSuccessful() && task.getException().getMessage() != null )
                Log.e("setUserName: ", task.getException().getMessage() );
        } );
    }
}
