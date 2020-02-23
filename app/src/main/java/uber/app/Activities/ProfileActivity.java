package uber.app.Activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.github.ybq.android.spinkit.style.Circle;
import com.google.firebase.storage.UploadTask;

import java.util.HashMap;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import uber.app.CompressImage;
import uber.app.CustomToast;
import uber.app.R;

import static uber.app.Helpers.FirebaseHelper.getFromFirebase;
import static uber.app.Helpers.FirebaseHelper.mProfileImageStorageRef;
import static uber.app.Helpers.FirebaseHelper.mUser;
import static uber.app.Helpers.FirebaseHelper.mUsersDbRef;
import static uber.app.Helpers.FirebaseHelper.userIdString;

public class ProfileActivity extends AppCompatActivity {
    private final int INTENT_PICKUP_PROFILE_PHOTO = 1;
    private static final int PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 1;

    @BindView( R.id.user_profile_name )
    TextView userName;
    @BindView( R.id.user_profile_surname )
    TextView userSurname;
    @BindView( R.id.user_profile_email )
    TextView userEmail;
    @BindView( R.id.user_profile_phone_number )
    TextView userPhoneNumber;
    @BindView( R.id.user_profile_image )
    ImageView userProfileImage;
    @BindView( R.id.profile_layout_phone_number )
    LinearLayout layoutPhoneNumber;

    private Circle mCircleUserName;
    private Circle mCircleUserSurname;
    private Circle mCircleUserEmail;
    private Circle mCircleUserPhone;

    private String mProfileImageUrl;

    @Override
    protected void onCreate( @Nullable Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_profile );

        ButterKnife.bind( this );
        if( getSupportActionBar() != null ) {
            getSupportActionBar().setTitle( R.string.profile );
            getSupportActionBar().setDisplayHomeAsUpEnabled( true );
        }

        initCircles();
        loadUserData();
        onProfileImageClick();
        onPhoneNumberClick();
    }

    private boolean checkReadStoragePermissions() {
        if ( ContextCompat.checkSelfPermission( this, Manifest.permission.READ_EXTERNAL_STORAGE ) != PackageManager.PERMISSION_GRANTED ) {
            if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ) {
                if ( shouldShowRequestPermissionRationale( Manifest.permission.READ_EXTERNAL_STORAGE ) ) {
                    // Explain to the user why the need to read the contacts
                }
            }

            if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ) {
                ActivityCompat.requestPermissions( ProfileActivity.this, new String[]{ Manifest.permission.READ_EXTERNAL_STORAGE },
                        PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE );
            }
            return false;
        } else {
            return true;
        }
    }

    private void initCircles() {
        mCircleUserName = new Circle();
        mCircleUserSurname = new Circle();
        mCircleUserEmail = new Circle();
        mCircleUserPhone = new Circle();

        mCircleUserName.setBounds( 0, 0, 50, 50 );
        mCircleUserSurname.setBounds( 0, 0, 50, 50 );
        mCircleUserEmail.setBounds( 0, 0, 50, 50 );
        mCircleUserPhone.setBounds( 0, 0, 50, 50 );

        mCircleUserName.setColor( Color.BLACK );
        mCircleUserSurname.setColor( Color.BLACK );
        mCircleUserEmail.setColor( Color.BLACK );
        mCircleUserPhone.setColor( Color.BLACK );

        userName.setCompoundDrawables( null, null, mCircleUserName, null );
        userSurname.setCompoundDrawables( null, null, mCircleUserSurname, null );
        userEmail.setCompoundDrawables( null, null, mCircleUserEmail, null );
        userPhoneNumber.setCompoundDrawables( null, null, mCircleUserPhone, null );

        mCircleUserName.start();
        mCircleUserSurname.start();
        mCircleUserEmail.start();
        mCircleUserPhone.start();
    }

    //load user data and fill fields
    private void loadUserData() {
        getFromFirebase( () -> {
            if ( !ProfileActivity.this.isFinishing() ) {

                userName.setText( mUser.getName() );
                userSurname.setText( mUser.getSurname() );
                userEmail.setText( mUser.getEmail() );

                mCircleUserName.stop();
                mCircleUserSurname.stop();
                mCircleUserEmail.stop();
                mCircleUserPhone.stop();

                mProfileImageUrl = mUser.getProfileImageUrl();
                if ( mProfileImageUrl != null && !mProfileImageUrl.isEmpty() )
                    Glide.with( getApplication() ).load( mProfileImageUrl ).into( userProfileImage );

                String phoneNumber = mUser.getPhoneNumber();
                if( phoneNumber != null && !phoneNumber.isEmpty() )
                    userPhoneNumber.setText( phoneNumber );
            }
        } );
    }

    //open gallery to pick photo
    private void onProfileImageClick() {
        userProfileImage.setOnClickListener( v -> {
            //if user already gave storage permissions
            if( checkReadStoragePermissions() ){
                Intent openGalleryIntent = new Intent( Intent.ACTION_PICK );
                openGalleryIntent.setType( "image/*" );
                startActivityForResult( openGalleryIntent, INTENT_PICKUP_PROFILE_PHOTO );
            }
        } );
    }

    @Override
    public void onRequestPermissionsResult( int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults ) {
        super.onRequestPermissionsResult( requestCode, permissions, grantResults );

        if( requestCode == PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE ){
            if( grantResults.length > 0 && grantResults[ 0 ] == PackageManager.PERMISSION_GRANTED ){
                Intent openGalleryIntent = new Intent( Intent.ACTION_PICK );
                openGalleryIntent.setType( "image/*" );
                startActivityForResult( openGalleryIntent, INTENT_PICKUP_PROFILE_PHOTO );
            } else{
                Toast.makeText( this, R.string.storage_permissions_denied, Toast.LENGTH_SHORT ).show();
            }
        }
    }

    @Override
    protected void onActivityResult( int requestCode, int resultCode, @Nullable Intent data ) {
        super.onActivityResult( requestCode, resultCode, data );

        if ( requestCode == INTENT_PICKUP_PROFILE_PHOTO && resultCode == RESULT_OK ) {
            final Uri imageUri = data.getData();
            userProfileImage.setImageURI( imageUri );

            saveProfileImageToFirebase( imageUri );
        }
    }

    private void saveProfileImageToFirebase( Uri imageUri ) {
        Bitmap bitmap = null;

        try {
            UploadTask uploadTask = mProfileImageStorageRef.child( userIdString )
                    .putBytes( CompressImage.compressImage( this, imageUri ).toByteArray() );

            //upload on storage was successfull
            uploadTask.addOnSuccessListener( taskSnapshot -> {
                //put uploaded image url
                taskSnapshot.getMetadata().getReference().getDownloadUrl().addOnSuccessListener( uri -> {
                    Map profileImageMap = new HashMap<String, String>();
                    profileImageMap.put( "profileImageUrl", uri.toString() );
                    mUsersDbRef.child( userIdString ).updateChildren( profileImageMap );
                } );
            } );

            uploadTask.addOnFailureListener( e -> {
                Toast.makeText( ProfileActivity.this, R.string.error_uploading_image, Toast.LENGTH_SHORT ).show();
//                    finish();
            } );

        } catch ( Exception e ) {
            e.printStackTrace();
            Toast.makeText( this, R.string.error_uploading_image, Toast.LENGTH_SHORT ).show();
        }
    }


    private void onPhoneNumberClick() {
        layoutPhoneNumber.setOnClickListener( v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder( ProfileActivity.this );
            EditText editText = new EditText( ProfileActivity.this );
            editText.setInputType( InputType.TYPE_CLASS_PHONE );

            builder
                    .setTitle( R.string.phone_number )
                    .setView( editText )
                    .setPositiveButton( R.string.ok, ( dialogInterface, i ) -> {
                    //Do nothing here because we override this button later to change the close behaviour.
                    } )
                    .setNegativeButton("Cancel", null );

            final AlertDialog dialog = builder.create();
            dialog.show();

            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener( v1 -> {
                Boolean wantToCloseDialog = false;
                String editTextInput = editText.getText().toString();

                if( editTextInput.length() < 8 )
                    new CustomToast().showToast( ProfileActivity.this, v1, getResources().getString( R.string.phone_number_error ) );
                else
                    wantToCloseDialog = true;

                Map phoneNumberMap = new HashMap<String, String>();
                phoneNumberMap.put( "phoneNumber", editTextInput );
                mUsersDbRef.child( userIdString ).updateChildren( phoneNumberMap );

                if( wantToCloseDialog )
                    dialog.dismiss();
            } );
        } );
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return super.onSupportNavigateUp();
    }
}
