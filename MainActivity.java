package company.dikey.neuralnetworkcamera;


import android.app.ProgressDialog;
import android.content.Intent;
import android.gesture.GestureOverlayView;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.renderscript.RenderScript;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;

import network.CNNdroid;

import static android.graphics.Color.red;


public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener  {

    private static int nameOfFile=1;
    private File pictures;
    private File photoFile;
    private Toast resultInformation;
    private RenderScript myRenderScript;
    private CNNdroid myConv = null;
    private String[] labels;
    private boolean condition;
    private Bitmap bmpFromGesture;
    private Bitmap bmp;
    private Snackbar mSnackBar;
    private boolean mSnackBarIsShowed=false;
    private GestureOverlayView gesture;
    private Button button;
    private int sizeOfBrush=100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        button=(Button)findViewById(R.id.buttonPhoto);
        gesture= (GestureOverlayView) findViewById(R.id.gestures);

        Intent intent = getIntent();
        sizeOfBrush = intent.getIntExtra("size",100);
        gesture.setGestureStrokeWidth(sizeOfBrush);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setBackgroundTintList(getResources().getColorStateList(R.color.color_resource_file));
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!mSnackBarIsShowed) {
                    mSnackBar=Snackbar.make(view, "Понравилось ли вам моё приложение?", Snackbar.LENGTH_INDEFINITE).setAction("Да", snackBarOnClickListener);
                    mSnackBar.setActionTextColor(Color.WHITE);
                    mSnackBar.show();
                    mSnackBar.getView().setBackgroundColor(getResources().getColor(R.color.colorViolet));
                    mSnackBarIsShowed=true;
                }
                else{
                    mSnackBar.dismiss();
                    mSnackBarIsShowed=false;
                }
            }
        });
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.open, R.string.close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();
        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        gesture.setBackgroundColor(Color.BLACK);
        myRenderScript = RenderScript.create(this);
        new prepareModel().execute(myRenderScript);
    }

    View.OnClickListener snackBarOnClickListener= new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            Intent intent=new Intent(MainActivity.this,Information.class);
            startActivity(intent);
        }
    };

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
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        Fragment fragment=null;
        if (id == R.id.nav_main) {
            Intent intent=new Intent(MainActivity.this,MainActivity.class);
            startActivity(intent);
        } else if (id == R.id.nav_gallery) {
            Intent intent=new Intent(MainActivity.this,Gallery.class);
            startActivity(intent);
        } else if (id == R.id.nav_information) {
            Intent intent=new Intent(MainActivity.this,Information.class);
            startActivity(intent);
        } else if (id == R.id.nav_manage) {
            Intent intent=new Intent(MainActivity.this,Preferences.class);
            startActivity(intent);
        }
        return true;
    }

    private class prepareModel extends AsyncTask<RenderScript, Void, CNNdroid> {

        ProgressDialog progDailog;

        protected void onPreExecute() {
            progDailog = new ProgressDialog(MainActivity.this);
            progDailog.setMessage("Загрузка нейросети...(около 30 сек)");
            progDailog.setIndeterminate(false);
            progDailog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            progDailog.setCancelable(true);
            progDailog.show();
        }

        @Override
        protected CNNdroid doInBackground(RenderScript... params) {
            try {
                myConv = new CNNdroid(myRenderScript, "/sdcard/MnistNet/Mnist_def.txt");
            } catch (Exception e) {
                e.printStackTrace();
            }
            return myConv;
        }

        protected void onPostExecute(CNNdroid result) {
            condition = true;
            progDailog.dismiss();
        }
    }
    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    public void onButtonPhotoClicked(View view) {

        int answer=0;
        gesture.setDrawingCacheEnabled(true);
        bmpFromGesture = Bitmap.createBitmap(gesture.getDrawingCache());
        gesture.setDrawingCacheEnabled(false);
        gesture.cancelClearAnimation();
        gesture.clear(true);
        float[][][][] inputBatch = new float[1][3][28][28];
        bmp= Bitmap.createScaledBitmap(bmpFromGesture, 28, 28, false);

        String stringNameOfFile=Integer.toString(nameOfFile)+".png";
        pictures = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        try {
            photoFile = new File(pictures,stringNameOfFile);
            FileOutputStream fos = new FileOutputStream(photoFile);
            bmp.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.close();
        }catch (Exception e)
        {
            e.printStackTrace();
        }

        for (int j = 0; j < 28; j++)
            for (int k = 0; k < 28; k++) {
                int color = bmp.getPixel(j, k);
                if(((float) (red(color))==0))
                {
                    inputBatch[0][0][k][j] = 0;
                    inputBatch[0][1][k][j] = 0;
                    inputBatch[0][2][k][j] = 0;
                }
                else
                {
                    inputBatch[0][0][k][j] = 1;
                    inputBatch[0][1][k][j] = 1;
                    inputBatch[0][2][k][j] = 1;
                }

            }

        float[][] output = (float[][])myConv.compute(inputBatch);

        for (int i = 0; i < output[0].length; i++) {
            if(Float.isNaN(output[0][i]))
            {
                answer=i;
            }
        }

        String str="Вы написали: "+answer;
        resultInformation=Toast.makeText(MainActivity.this,str,Toast.LENGTH_SHORT);
        ViewGroup group = (ViewGroup) resultInformation.getView();
        TextView messageTextView = (TextView) group.getChildAt(0);
        messageTextView.setTextSize(45);
        resultInformation.show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
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
            Intent intent=new Intent(MainActivity.this,Information.class);
            startActivity(intent);
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }
}
