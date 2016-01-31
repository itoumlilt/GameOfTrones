package com.toumlilt.gameoftrones;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

public class GameActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener,
        OnMapReadyCallback,
        LocationListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private Player player;

    private ArrayList<Sanitary> sanitaryList;
    private ArrayList<Weapon> weaponList;
    private GotDbHelper sh;
    private DownloadSanitary ds;
    private Sanitary currentSanitary;
    private Marker currentMarker;

    public final static int PROFILE_REQUEST = 1;

    public final static String EXTRA_USERNAME = "com.toumlilt.gameottrones.USERNAME";
    public final static String EXTRA_USERDESC = "com.toumlilt.gameottrones.USERDESC";

    /* Map attributes */
    private MapFragment mMapFragment;
    private GoogleMap googleMap;
    private Location mCurrentLocation = null;
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    private CircleOptions mCircleOptions;
    private Circle mCurrentCircle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);


        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                if(currentSanitary == null)
                {
                    Snackbar.make(
                            view,
                            "You must select a Sanitary first",
                            Snackbar.LENGTH_LONG
                    ).show();
                }
                else
                {
                    Location sanLocation = new Location("sanitary");

                    sanLocation.setLatitude(currentSanitary.getLatitude());
                    sanLocation.setLongitude(currentSanitary.getLongitude());

                    System.out.println("Distance : " + mCurrentLocation.distanceTo(sanLocation));

                    if(mCurrentLocation.distanceTo(sanLocation) <= (getCurrentWeapon().getScope() * 10))
                    {
                        Integer newLife = currentSanitary.getRemainingLife() - getCurrentWeapon().getPv();

                        currentSanitary.setRemainingLife(
                                newLife < 0 ? 0 : newLife
                        );

                        if (currentMarker != null) {
                            currentMarker.remove();
                        }

                        addSanitary(currentSanitary, currentSanitary.getRemainingLife() == 0);
                        sh.update(currentSanitary);

                        Snackbar.make(
                                view,
                                "Sanitary hit ! Remaining life: " + currentSanitary.getRemainingLife(),
                                Snackbar.LENGTH_LONG
                        ).show();

                        MediaPlayer.create(
                                getApplicationContext(),
                                R.raw.hit_sound
                        ).start();
                    }
                    else{
                        Snackbar.make(
                                view,
                                "Too far away !",
                                Snackbar.LENGTH_LONG
                        ).show();
                    }
                }
            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        /*** Getting intent from SignUpActivity ***/
        Intent intent = getIntent();
        String message = intent.getStringExtra(SignUpActivity.EXTRA_MESSAGE);
        String desc_msg = intent.getStringExtra(this.EXTRA_USERDESC);

        /* creating player */
        this.player = new Player(message, desc_msg);

        /* nav_view */
        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        View header = navigationView.getHeaderView(0);

        TextView usernameNavTV = (TextView) header.findViewById(R.id.usernameNavTextView);
        usernameNavTV.setText(this.player.getUsername());

        TextView userdescNavTV = (TextView) header.findViewById(R.id.userdescNavTextView);
        userdescNavTV.setText(this.player.getUserdesc());

        // Getting Google Play availability status
        int status = GooglePlayServicesUtil.isGooglePlayServicesAvailable(getBaseContext());

        // Showing status
        if (status != ConnectionResult.SUCCESS) { // Google Play Services are not available

            System.out.println("CONNECTION FAILED");

        } else { // Google Play Services are available
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
            mMapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.map);
            this.googleMap = mMapFragment.getMap();
            mMapFragment.getMapAsync(this);

            this.googleMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
                @Override
                public boolean onMarkerClick(Marker marker) {
                    currentMarker = marker;
                    currentSanitary = getSanitaryFromLatLng(marker.getPosition());
                    return false;
                }
            });
        }

        this.ds = new DownloadSanitary();
        this.sh = new GotDbHelper(this);

        this.initSanitaryList();
        this.drawSanitaryList();
        this.initWeaponList();
        this.getCurrentWeapon();
    }

    /**
     * Fait correspondre la latitude/longitude d'un marker à celle d'un Sanitary et le retourne.
     * */
    public Sanitary getSanitaryFromLatLng(LatLng l){
        for (Sanitary s:this.sanitaryList)
            if(s.getLatitude() == l.latitude && s.getLongitude() == l.longitude)
                return s;
        return null;
    }

    /**
     * Retourne l'arme courante de l'utilisateur.
     * Par défault, c'est la première de la liste.
     * La valeur est stockée dans une SharedPreference.
     * Elle est mise-à-jour par la WeaponActivity par l'utilisateur.
     * */
    public Weapon getCurrentWeapon() {
        int indCurWeapon;
        Context c = getApplicationContext();
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(c);
        indCurWeapon = sp.getInt("ind_curr_weapon", 0);
        return this.weaponList.get(indCurWeapon);
    }

    /**
     * Valeurs en dur pour créer une liste d'arme.
     * */
    private void initWeaponList() {
        this.weaponList = new ArrayList<>();
        this.weaponList.add(new DrawableWeapon("Gun", 5, 10, R.drawable.gun));
        this.weaponList.add(new DrawableWeapon("Knife", 8, 5, R.drawable.knife));
        this.weaponList.add(new DrawableWeapon("AK47", 8, 15, R.drawable.ak47));
        this.weaponList.add(new DrawableWeapon("Sword", 8, 10, R.drawable.sword));
        this.weaponList.add(new DrawableWeapon("Saber", 8, 12, R.drawable.saber));
        this.weaponList.add(new DrawableWeapon("Trowel", 8, 20, R.drawable.trowel));
    }

    /**
     * Met les sanisettes de la BDD en mémoire.
     * Si la BDD ne contient aucune sanisette :
     *      - On récupère la liste à partir du JSON en ligne
     *      - On ajoute cette liste dans la BDD
     *      - On la garde la liste en mémoire
     * */
    private boolean initSanitaryList()
    {
        ArrayList<Sanitary> tmp;

        if(this.sanitaryList == null)
        {
            //BDD vide ?
            if(this.sh.count() == 0)
            {
                try {
                    //Récupère JSON en ligne
                    tmp = this.ds.execute().get();

                    if(tmp==null){
                        Snackbar.make(
                                findViewById(R.id.fab),
                                "Impossible de se connecter à http://opendata.paris.fr",
                                Snackbar.LENGTH_LONG
                        ).show();
                    }

                    //Ajout à la BDD
                    for (Sanitary s:tmp){
                        System.out.println("--->" + this.sh.insert(s));
                    }
                }
                catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                    Snackbar.make(
                            findViewById(R.id.fab),
                            "Impossible to retrieve sanitaries",
                            Snackbar.LENGTH_LONG
                    ).show();
                    return false;
                }
            }
            //Garde la liste en mémoire
            this.sanitaryList = this.sh.getAll();
            System.out.println("--->" + this.sh.count());
        }
        return true;
    }

    /**
     * Ajoute un marqueur par sanisette.
     * */
    private void drawSanitaryList(){
        for(Sanitary s : this.sanitaryList)
            this.addSanitary(s, s.getRemainingLife() == 0);
    }


    protected void onStart() {
        mGoogleApiClient.connect();
        super.onStart();
    }

    protected void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.game, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_profile) {
            Intent intent = new Intent(this, ProfileActivity.class);
            startActivityForResult(intent, PROFILE_REQUEST);
        } else if (id == R.id.nav_map_view) {

        } else if (id == R.id.nav_weapons) {
            Intent iw = new Intent(this, WeaponsActivity.class);
            iw.putExtra("weapons", this.weaponList);
            startActivity(iw);

        } else if (id == R.id.nav_share_realm) {

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check which request we're responding to
        if (requestCode == PROFILE_REQUEST) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {
                this.player.setUsername(data.getStringExtra(EXTRA_USERNAME));
                this.player.setUserdesc(data.getStringExtra(EXTRA_USERDESC));
                this.updateNavigationViewData();

                //this.mCircleOptions.center(new LatLng(mCurrentLocation.getLatitude() + 5, mCurrentLocation.getLongitude()));

                //googleMap.addCircle(this.mCircleOptions);
                //System.out.println("HEYHEY<<----");
            }
        }
    }

    private void updateNavigationViewData() {
        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);

        View header = navigationView.getHeaderView(0);

        TextView usernameNavTV = (TextView) header.findViewById(R.id.usernameNavTextView);
        usernameNavTV.setText(this.player.getUsername());

        TextView userdescNavTV = (TextView) header.findViewById(R.id.userdescNavTextView);
        userdescNavTV.setText(this.player.getUserdesc());
    }

    /***********************************************************************************************
     * Map
     **********************************************************************************************/
    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(500);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    /**
     * Ajoute un marker à la carte.
     * La couleur du marqueur est différente si la sanisette est prise ou pas.
     * @param sanitary : le Sanitary représenté par le marker
     * @param isTaken : la vie du Sanitary est elle égale à zéro.
     * */
    public Marker addSanitary(Sanitary sanitary, Boolean isTaken)
    {
        BitmapDescriptor bdf = BitmapDescriptorFactory.defaultMarker(
                isTaken?BitmapDescriptorFactory.HUE_ORANGE:BitmapDescriptorFactory.HUE_CYAN
        );

        return googleMap.addMarker(new MarkerOptions().
                position(new LatLng(sanitary.getLatitude(), sanitary.getLongitude()))
                .title(sanitary.getRemainingLife() + "pdv")
                .icon(bdf)
        );
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.googleMap.setMyLocationEnabled(true);
        //this.googleMap.addMarker(new MarkerOptions().
        //        position(googleMap.getCameraPosition().target).title("TutorialsPoint"));

    }

    @Override
    public void onLocationChanged(Location location) {
        System.out.println("------> Location changed to " + location.toString());
        CameraPosition cameraPosition = new CameraPosition.Builder().target(
                new LatLng(location.getLatitude(), location.getLongitude()))
                .zoom(15).build();

        googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    @Override
    public void onConnected(Bundle bundle) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            System.out.println("--> PERMISSION OK");
        } else {
            System.out.println("--> PERMISSION KO");
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    1);
            return;
         }
        if(this.mCurrentLocation == null)
            this.locationSetup();
        else
            this.updateSettings();

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    private void locationSetup() {
        this.googleMap.setMyLocationEnabled(true);
        this.googleMap.getUiSettings().setMyLocationButtonEnabled(false);
        this.createLocationRequest();
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(mLocationRequest);

        mCurrentLocation = LocationServices.FusedLocationApi.getLastLocation(
                mGoogleApiClient);

        if(mCurrentLocation != null){
            System.out.println("------>" + mCurrentLocation.toString());
            CameraPosition cameraPosition = new CameraPosition.Builder().target(
                    new LatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude()))
                    .zoom(15).build();

            googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
            this.mCircleOptions = new CircleOptions()
                    .center(new LatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude()))
                    .radius(10 * getCurrentWeapon().getScope())
                    .strokeColor(Color.RED)
                    .fillColor(Color.argb(150, 168, 210, 224));
            this.mCurrentCircle = googleMap.addCircle(this.mCircleOptions);
        }
    }

    private void updateSettings() {
        this.updateCircleSettings();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 1: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    this.locationSetup();
                } else {
                    // TODO notif
                }
                return;
            }

        }
    }

    private void updateCircleSettings() {
        this.mCurrentCircle.remove();
        this.mCircleOptions = new CircleOptions()
                .center(new LatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude()))
                .radius(this.getCurrentWeapon().getScope()*10)
                .strokeColor(Color.RED)
                .fillColor(Color.argb(150, 168, 210, 224));
        this.mCurrentCircle = googleMap.addCircle(this.mCircleOptions);
    }
}
