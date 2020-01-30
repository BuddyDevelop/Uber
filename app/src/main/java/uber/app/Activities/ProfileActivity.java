package uber.app.Activities;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.github.ybq.android.spinkit.style.Circle;

import butterknife.BindView;
import butterknife.ButterKnife;
import uber.app.OnDataReceiveCallback;
import uber.app.R;

import static uber.app.Helpers.FirebaseHelper.getFromFirebase;
import static uber.app.Helpers.FirebaseHelper.mUser;

public class ProfileActivity extends AppCompatActivity {
    @BindView( R.id.user_profile_name ) TextView userName;
    @BindView( R.id.user_profile_surname ) TextView userSurname;
    @BindView( R.id.user_profile_email ) TextView userEmail;
//    @BindView( R.id.user_profile_phone_number ) TextView userPhoneNumber;

    private Circle mCircleUserName;
    private Circle mCircleUserSurname;
    private Circle mCircleUserEmail;
//    private Circle mCircleUserPhoneNumber;


    @Override
    protected void onCreate( @Nullable Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_profile );

        ButterKnife.bind( this );

        initCircles();
        loadUserData();
    }

    private void loadUserData() {
        getFromFirebase( new OnDataReceiveCallback() {
            @Override
            public void onDataReceived() {
                if( !ProfileActivity.this.isFinishing() ) {

                    userName.setText( mUser.getName() );
                    userSurname.setText( mUser.getSurname() );
                    userEmail.setText( mUser.getEmail() );

                    mCircleUserName.stop();
                    mCircleUserSurname.stop();
                    mCircleUserEmail.stop();
                }
            }
        } );
    }


    private void initCircles(){
        mCircleUserName = new Circle();
        mCircleUserSurname = new Circle();
        mCircleUserEmail = new Circle();

        mCircleUserName.setBounds(0, 0, 50, 50);
        mCircleUserSurname.setBounds(0, 0, 50, 50);
        mCircleUserEmail.setBounds(0, 0, 50, 50);

        mCircleUserName.setColor(Color.BLACK);
        mCircleUserSurname.setColor(Color.BLACK);
        mCircleUserEmail.setColor(Color.BLACK);

        userName.setCompoundDrawables(null, null, mCircleUserName, null);
        userSurname.setCompoundDrawables(null, null, mCircleUserSurname, null);
        userEmail.setCompoundDrawables(null, null, mCircleUserEmail, null);

        mCircleUserName.start();
        mCircleUserSurname.start();
        mCircleUserEmail.start();
    }
}
