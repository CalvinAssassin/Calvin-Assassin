package edu.calvin.the_b_team.calvinassassingame;

//Imports for days
import android.app.AlertDialog;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.preference.PreferenceManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ImageView;
import android.net.Uri;
import android.content.SharedPreferences;
import android.widget.Spinner;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * This activity allows a user to create a profile
 */
public class ProfileViewActivity extends AppCompatActivity {

    //Initialize Drawer and Layout things
    private ListView mDrawerList;
    private ArrayAdapter<String> mAdapter;
    private DrawerLayout mDrawerLayout;
    private ActionBarDrawerToggle mDrawerToggle;

    //Initialize Widgets
    private Button choosePhotoButton;
    private Button finalizeButton;
    private EditText playerFirstNameEditable;
    private EditText playerLastNameEditable;
    private EditText playerMajorEditable;
    private EditText playerResidenceEditable;

    //Initialize State Variables
    private boolean settingsFinalized = false;
    private boolean firstRun = false;
    private ImageView profileImage;

    private SharedPreferences app_preferences;
    private AlertDialog.Builder notFinalizedAlert;
    private int notFinalizedAlertChoice;
    private int menuChoice = 0;
    private AlertDialog.Builder firstRunAlert;

    private ServerCommunication server;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ServerCommunication server = new ServerCommunication(this);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_view);
        setTitle(getResources().getText(R.string.profile_activity_title));


        //Set up the menu drawer and its items
        mDrawerList = (ListView)findViewById(R.id.navList);
        mDrawerLayout = (DrawerLayout)findViewById(R.id.drawer_layout);
        addDrawerItems();
        setupDrawer();
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_hamburger);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        //Define layout items
        profileImage = (ImageView) findViewById(R.id.profile_picture);
        choosePhotoButton = (Button) findViewById(R.id.choose_photo_button);
        finalizeButton = (Button) findViewById(R.id.finalize_profile_button);
        playerFirstNameEditable = (EditText) findViewById(R.id.player_first_name_editable);
        playerLastNameEditable = (EditText) findViewById(R.id.player_last_name_editable);
        playerMajorEditable = (EditText) findViewById(R.id.player_major_editable);
        playerResidenceEditable = (EditText) findViewById(R.id.player_residence_editable);


        app_preferences = PreferenceManager.getDefaultSharedPreferences(this);

        app_preferences = PreferenceManager.getDefaultSharedPreferences(this);
        Player player = new Player(this);

        //Load layout items from saved preferences
        playerFirstNameEditable.setText((String)player.getValue("firstName"));
        playerLastNameEditable.setText((String)player.getValue("lastName"));
        playerMajorEditable.setText((String)player.getValue("major"));
        playerResidenceEditable.setText((String)player.getValue("residence"));
        loadProfileImage(app_preferences.getString("playerPhotoPath","android.resource://edu.calvin.the_b_team.calvinassassingame/" + R.mipmap.ic_profile_placeholder));
        settingsFinalized = app_preferences.getBoolean("settingsFinalized",false);
        firstRun = app_preferences.getBoolean("firstRun", true);

        //Set up the alert that warns that settings arent finalized when users leave the activity
        notFinalizedAlert = new AlertDialog.Builder(ProfileViewActivity.this);
        notFinalizedAlert.setTitle("Profile not finalized!");
        notFinalizedAlert.setMessage("You will need to fill out and finalize your profile before you can join any games!");
        notFinalizedAlert.setNeutralButton("I'll do it later", new DialogInterface.OnClickListener() {
            //If the settings arent finalized but the user doesn't care, let them leave the page
            public void onClick (DialogInterface dialog, int id) {
                selectItem(menuChoice);
            }
        });
        notFinalizedAlert.setNegativeButton("I'll do it now", new DialogInterface.OnClickListener() {
            //If the info insn't finalized and the user decides to enter it, just close the drawer
            public void onClick (DialogInterface dialog, int id) {
                mDrawerLayout.closeDrawer(Gravity.LEFT);
            }
        });
        //Set up a greeting to firstRun users
        firstRunAlert = new AlertDialog.Builder(ProfileViewActivity.this);
        firstRunAlert.setTitle("Welcome to Calvin Assassin!");
        firstRunAlert.setMessage("Before you start playing, enter some information about yourself so other players can find you!");
        firstRunAlert.setNeutralButton("OK", new DialogInterface.OnClickListener() {
            public void onClick (DialogInterface dialog, int id) {

            }
        });

        if (firstRun){
            SharedPreferences.Editor editor = app_preferences.edit();
            firstRun = false;
            editor.putBoolean("firstRun", false);
            editor.commit(); //Store that this is no longer the first run
            firstRunAlert.show();
        }

        //Disable editing the fields if they've previously been finalized
        if (settingsFinalized == true){
            disableTextFields();
        }

        //Open the photo gallery browser to allow the user to choose a profile photo
        choosePhotoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent pickPhoto = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(pickPhoto , 1);//one can be replaced with any action code
            }
        });

        //Define the confirmation dialogue to confirm finalizing the profile info
        final AlertDialog.Builder finalizeAlert  = new AlertDialog.Builder(this);
        finalizeAlert.setMessage(R.string.finalize_message);
        finalizeAlert.setTitle("Finalize?");
        finalizeAlert.setCancelable(true);
        finalizeAlert.setPositiveButton("Finalize", new DialogInterface.OnClickListener() {
            //When the finalize button is pressed, finalize the data
            public void onClick(DialogInterface dialog, int which) {
                settingsFinalized = true;
                //Disable the info fields
                disableTextFields();
                finalizeTextFields();
                Toast.makeText(getBaseContext(), "Profile Finalized", Toast.LENGTH_SHORT).show();
            }
        });
        finalizeAlert.setNegativeButton("Not yet", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                //Dismiss confirmation message and return
            }
        });
        //Open the finalize confirmation dialogue when the FINALIZE button is pressed
        finalizeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finalizeAlert.create().show();
            }
        });
    }

    //Stop the user from editing the info on the page
    private void disableTextFields(){
        playerFirstNameEditable.setEnabled(false);
        playerLastNameEditable.setEnabled(false);
        playerMajorEditable.setEnabled(false);
        playerResidenceEditable.setEnabled(false);
        choosePhotoButton.setEnabled(false);
        finalizeButton.setEnabled(false);
        finalizeButton.setVisibility(View.GONE);
    }

    /* Save all of the profile data to a preferences file to be loaded back in when the app is re-opened
    /* A complete removal and reinstall of the app is required to make these editable again. This prevents
    /* users from editing their name/info during or between games. For obvious reasons */
    private void finalizeTextFields(){
        SharedPreferences.Editor editor = app_preferences.edit();
        Player player = new Player(this);
//        editor.putString("playerName", playerNameEditable.getText().toString());
//        editor.putInt("playerClass", playerClassEditable.getSelectedItemPosition());
//        editor.putInt("playerHome", playerHomeEditable.getSelectedItemPosition());
        editor.putBoolean("settingsFinalized", settingsFinalized);
//
        Bitmap profileBitmap = ((BitmapDrawable)profileImage.getDrawable()).getBitmap();
        editor.putString("playerPhotoPath", saveProfilePhoto(profileBitmap));
//
        editor.commit(); // Commit the changes to the preferences file

        player.save("firstName" , playerFirstNameEditable.getText().toString());
        player.save("lastName" , playerLastNameEditable.getText().toString());
        player.save( "residence",  playerResidenceEditable.getText().toString() );
        player.save( "major", playerMajorEditable.getText().toString());
        //ServerCommunication server = new ServerCommunication(this);
        //server.createUserProfile();

    }

    //Save the profile image to a file in the app's internal file system (imageDir/profile.jpg)
    private String saveProfilePhoto(Bitmap bitmapImage){
        ContextWrapper cw = new ContextWrapper(getApplicationContext());
        // path to /data/data/yourapp/app_data/imageDir
        File directory = cw.getDir("imageDir", Context.MODE_PRIVATE);
        // Create imageDir
        File mypath=new File(directory,"profile.jpg");

        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(mypath);
            // Use the compress method on the BitMap object to write image to the OutputStream
            // TODO: See if theres a way to save the photo without compressing it; for now its lossless compression
            bitmapImage.compress(Bitmap.CompressFormat.PNG, 100, fos);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            try {
                fos.close();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
        return directory.getAbsolutePath();
    }

    //Load the profile image from the hardcoded path that it was saved to.
    //Loads directly into the profileImage ImageView object
    private void loadProfileImage(String path) {
        try {
            File f=new File(path, "profile.jpg");
            Bitmap b = BitmapFactory.decodeStream(new FileInputStream(f));
            profileImage.setImageBitmap(b);
        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();
        }
    }

    //Set the profile picture after user selects it from the photo gallery
    protected void onActivityResult(int requestCode, int resultCode, Intent imageReturnedIntent) {
        super.onActivityResult(requestCode, resultCode, imageReturnedIntent);
        switch(requestCode) {
            case 0:
                if(resultCode == RESULT_OK){
                    Uri selectedImage = imageReturnedIntent.getData();
                    profileImage.setImageURI(selectedImage);
                }
                break;
            case 1:
                if(resultCode == RESULT_OK){
                    Uri selectedImage = imageReturnedIntent.getData();
                    profileImage.setImageURI(selectedImage);
                }
                break;
        }
    }

    // Beginning of menu drawer configuration

    private void addDrawerItems() {
        String[] menuPages = { "Target", "Map", "Standings", "Join a Game", "Settings" };
        mAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, menuPages);
        mDrawerList.setAdapter(mAdapter);

        mDrawerList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (!settingsFinalized) {
                    menuChoice = position;
                    if (menuChoice == 4){
                        /*Dont harass the user about filling in info if they're just trying
                          to get to the settings page. */
                        selectItem(position);
                    }
                    else {
                        notFinalizedAlert.show();
                    }
                }
                else{
                    selectItem(position);
                }
            }
        });
    }

    private void setupDrawer() {
        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, R.string.drawer_open, R.string.drawer_close) {

            /** Called when a drawer has settled in a completely open state. */
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                getSupportActionBar().setTitle("Choose one!");
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }

            /** Called when a drawer has settled in a completely closed state. */
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                getSupportActionBar().setTitle(R.string.profile_activity_title);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }
        };
        mDrawerToggle.setDrawerIndicatorEnabled(true);
        mDrawerLayout.setDrawerListener(mDrawerToggle);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.help, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent;
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }
        if (id == R.id.help_button) {
            intent = new Intent(this, HelpActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            this.startActivity(intent);
            return true;
        }
        // Activate the navigation drawer toggle
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    private void selectItem(int position) {
        // Handle Navigation Options
        Intent intent;
        switch (position) {
            case 0:
                intent = new Intent(this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                this.startActivity(intent);
                break;
            case 1:
                intent = new Intent(this, MapCompactActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                this.startActivity(intent);
                break;
            case 2:
                intent = new Intent(this, StandingsViewActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                this.startActivity(intent);
                break;
            case 3:
                intent = new Intent(this, GameSelectActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                this.startActivity(intent);
                break;
            case 4:
                intent = new Intent(this, SettingsActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                this.startActivity(intent);
                break;
        }
    }
    //End of menu drawer configuration

}
