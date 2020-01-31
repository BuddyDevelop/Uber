package uber.app.Activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.github.ybq.android.spinkit.style.Circle;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.UploadTask;

import java.util.HashMap;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import uber.app.CompressImage;
import uber.app.OnDataReceiveCallback;
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
    @BindView( R.id.user_profile_image )
    ImageView userProfileImage;

    private Circle mCircleUserName;
    private Circle mCircleUserSurname;
    private Circle mCircleUserEmail;

    private String mProfileImageUrl;

    @Override
    protected void onCreate( @Nullable Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_profile );

        ButterKnife.bind( this );

        initCircles();
        loadUserData();
        onProfileImageClick();
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

        mCircleUserName.setBounds( 0, 0, 50, 50 );
        mCircleUserSurname.setBounds( 0, 0, 50, 50 );
        mCircleUserEmail.setBounds( 0, 0, 50, 50 );

        mCircleUserName.setColor( Color.BLACK );
        mCircleUserSurname.setColor( Color.BLACK );
        mCircleUserEmail.setColor( Color.BLACK );

        userName.setCompoundDrawables( null, null, mCircleUserName, null );
        userSurname.setCompoundDrawables( null, null, mCircleUserSurname, null );
        userEmail.setCompoundDrawables( null, null, mCircleUserEmail, null );

        mCircleUserName.start();
        mCircleUserSurname.start();
        mCircleUserEmail.start();
    }

    //load user data and fill fields
    private void loadUserData() {
        getFromFirebase( new OnDataReceiveCallback() {
            @Override
            public void onDataReceived() {
                if ( !ProfileActivity.this.isFinishing() ) {

                    userName.setText( mUser.getName() );
                    userSurname.setText( mUser.getSurname() );
                    userEmail.setText( mUser.getEmail() );

                    mCircleUserName.stop();
                    mCircleUserSurname.stop();
                    mCircleUserEmail.stop();

                    mProfileImageUrl = mUser.getProfileImageUrl();
                    if ( mProfileImageUrl != null && !mProfileImageUrl.isEmpty() )
                        Glide.with( getApplication() ).load( mProfileImageUrl ).into( userProfileImage );
                }
            }
        } );
    }

    //open gallery to pick photo
    private void onProfileImageClick() {
        userProfileImage.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick( View v ) {
                //if user already gave storage permissions
                if( checkReadStoragePermissions() ){
                    Intent openGalleryIntent = new Intent( Intent.ACTION_PICK );
                    openGalleryIntent.setType( "image/*" );
                    startActivityForResult( openGalleryIntent, INTENT_PICKUP_PROFILE_PHOTO );
                }
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

            uploadTask.addOnSuccessListener( new OnSuccessListener<UploadTask.TaskSnapshot>() {
                //upload on storage was successfull
                @Override
                public void onSuccess( UploadTask.TaskSnapshot taskSnapshot ) {
                    //put uploaded image url
                    taskSnapshot.getMetadata().getReference().getDownloadUrl().addOnSuccessListener( new OnSuccessListener<Uri>() {
                        @Override
                        public void onSuccess( Uri uri ) {
                            Map profileImageMap = new HashMap<String, String>();
                            profileImageMap.put( "profileImageUrl", uri.toString() );
                            mUsersDbRef.child( userIdString ).updateChildren( profileImageMap );
                        }
                    } );
                }
            } );

            uploadTask.addOnFailureListener( new OnFailureListener() {
                @Override
                public void onFailure( @NonNull Exception e ) {
                    Toast.makeText( ProfileActivity.this, R.string.error_uploading_image, Toast.LENGTH_SHORT ).show();
//                    finish();
                }
            } );

        } catch ( Exception e ) {
            e.printStackTrace();
            Toast.makeText( this, R.string.error_uploading_image, Toast.LENGTH_SHORT ).show();
        }

    }
}
