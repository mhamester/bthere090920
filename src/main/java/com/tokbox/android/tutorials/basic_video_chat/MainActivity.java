package com.tokbox.android.tutorials.basic_video_chat;

import com.tokbox.android.tutorials.basic_video_chat.Joystick;
import com.tokbox.android.tutorials.basic_video_chat.JoystickListener;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.opengl.GLSurfaceView;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.support.annotation.NonNull;
import android.Manifest;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.FrameLayout;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import java.util.Timer;
import java.util.TimerTask;
import android.widget.ProgressBar;

import org.json.JSONException;
import org.json.JSONObject;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.opentok.android.Session;
import com.opentok.android.Session.SignalListener;
import com.opentok.android.Stream;
import com.opentok.android.Connection;
import com.opentok.android.OpentokError;
import com.opentok.android.Publisher;
import com.opentok.android.PublisherKit;
import com.opentok.android.Subscriber;
import com.opentok.android.BaseVideoRenderer;
import com.opentok.android.OpentokError;
import com.opentok.android.SubscriberKit;
import com.tokbox.android.tutorials.basicvideochat.R;

import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ToggleButton;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.InputStreamReader;

import android.bluetooth.BluetoothAdapter;
import android.widget.ToggleButton;


import java.util.List;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.AppSettingsDialog;
import pub.devrel.easypermissions.EasyPermissions;


import com.opentok.android.Connection;

import static com.tokbox.android.tutorials.basic_video_chat.OpenTokConfig.API_KEY;
import static com.tokbox.android.tutorials.basic_video_chat.OpenTokConfig.SESSION_ID;
import static com.tokbox.android.tutorials.basic_video_chat.OpenTokConfig.TOKEN;
import static java.lang.Math.cos;
import static java.lang.Math.incrementExact;
import static java.lang.Math.sin;

import com.tokbox.android.tutorials.basic_video_chat.Joystick;
import com.tokbox.android.tutorials.basic_video_chat.JoystickListener;

public class MainActivity extends AppCompatActivity
                            implements EasyPermissions.PermissionCallbacks,
                                        WebServiceCoordinator.Listener,
                                        Session.SessionListener,
                                        PublisherKit.PublisherListener,
                                        SubscriberKit.SubscriberListener,
                                        SignalListener,OnClickListener{

    Timer timer;
    private BluetoothAdapter mBluetoothAdapter = null;
    private BtInterface bt = null;
    private boolean btremoteconnected;
    private TextView logview;
    private ProgressBar BatteryBar;
    private TextView BatteryText;
    private EditText roomNameText;
    private EditText textChatText;
    public static String message=null; // made static so can be referenced from BtInterface
    public static final String SIGNAL_TYPE = "text-signal"; // this is for bthere signalling data
    public static final String MSG_TYPE = "msg"; // this is for text chat
    private static final String LOG_TAG = MainActivity.class.getSimpleName();
    private static final int RC_SETTINGS_SCREEN_PERM = 123;
    private static final int RC_VIDEO_APP_PERM = 124;
    //define buttons
    private Button connect, toggle_light, play_sound, leftProx, midProx, rightProx, rearProx, otConnect, otDisConnect, sndTxtMsg;
    private ImageView forwardArrow, backArrow, rightArrow, leftArrow, stop;
    // Suppressing this warning. mWebServiceCoordinator will get GarbageCollected if it is local.
    @SuppressWarnings("FieldCanBeLocal")
    private WebServiceCoordinator mWebServiceCoordinator;
    public boolean connected = false;
    private Session mSession;
    private Publisher mPublisher;
    private Subscriber mSubscriber;
    private String[] logArray = null;
    private FrameLayout mPublisherViewContainer;
    private FrameLayout mSubscriberViewContainer;
    Handler updateConversationHandler;
    static final String TAG = "Chihuahua";
    static final int REQUEST_ENABLE_BT = 3;
    int delay = 0;
    int repeat = 0;

    public static int LU = 0;
    public static int CU = 0;
    public static int RU = 0;
    public static int BU = 0;

    public int status;



    private Timer btread;

    public void btread() {
        this.btread = new Timer();
        btread.schedule( new TimerTask(){

                             @Override
                             public void run() {

                                 runOnUiThread(new Runnable() {

                                     @Override
                                     public void run() {

                                         message = BtInterface.btSignalled; // change the signaling message to be the received bt data e.g. sonars
                                         if (message != null)  sendMessage(); // send the received bt data over signalling
                                         BtInterface.btSignalled = ""; //clear this

                                     }

                                 });

                             }

                         },
                // Set how long before to start calling the TimerTask (in milliseconds)
                0,(100));} // timer milliseconds. ok at 1sec, flaky at .5


    //This handler listens to messages from the bluetooth interface and adds them to the log
    final Handler handler = new Handler() {
        public void handleMessage(Message msg) {
            String msgdata = msg.getData().getString("receivedData");
            addToLog(msgdata);
        }
    };


    //this handler is dedicated to the status of the bluetooth connection
    final Handler handlerStatus = new Handler() {
        @SuppressLint("HandlerLeak")
        public void handleMessage(Message msg) {
           status = msg.arg1;
            if(status == BtInterface.CONNECTED) {
                addToLog("Connected");


            } else if(status == BtInterface.DISCONNECTED) {
                addToLog("Disconnected");

            }
        }
    };


    //handles the log view modification
    //only the most recent messages are shown
    private void addToLog(String message){
        for (int i = 1; i < logArray.length; i++){
            logArray[i-1] = logArray[i];
        }
        logArray[logArray.length - 1] = message;

        logview.setText("");
        for (int i = 0; i < logArray.length; i++){
            if (logArray[i] != null){
                logview.append(logArray[i] + "\n");
            }
        }
    }
    public void fetchSessionConnectionData() {
        RequestQueue reqQueue = Volley.newRequestQueue(this);
        reqQueue.add(new JsonObjectRequest(Request.Method.GET,
                //"https://b-there.herokuapp.com" + "/session",
                "https://b-there.herokuapp.com" + "//room/" + roomNameText.getText(),
                null, new Response.Listener<JSONObject>() {

            @Override
            public void onResponse(JSONObject response) {
                try {
                    API_KEY = response.getString("apiKey");
                    SESSION_ID = response.getString("sessionId");
                    TOKEN = response.getString("token");

                    Log.i(LOG_TAG, "API_KEY: " + API_KEY);
                    Log.i(LOG_TAG, "SESSION_ID: " + SESSION_ID);
                    Log.i(LOG_TAG, "TOKEN: " + TOKEN);

                    mSession = new Session.Builder(MainActivity.this, API_KEY, SESSION_ID).build();
                    mSession.setSessionListener(MainActivity.this);
                    mSession.connect(TOKEN);


                } catch (JSONException error) {
                    Log.e(LOG_TAG, "Web Service error: " + error.getMessage());
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(LOG_TAG, "Web Service error: " + error.getMessage());
            }
        }));
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        Log.d(LOG_TAG, "onCreate");

        super.onCreate(savedInstanceState);

        updateConversationHandler = new Handler();


        setContentView(R.layout.activity_main);

        // initialize view objects from your layout
        mPublisherViewContainer = (FrameLayout) findViewById(R.id.publisher_container);
        mSubscriberViewContainer = (FrameLayout) findViewById(R.id.subscriber_container);

        final TextView angleView = (TextView) findViewById(R.id.tv_angle);
        final TextView offsetView = (TextView) findViewById(R.id.tv_offset);
        BatteryText = findViewById(R.id.BatteryText);
        BatteryBar = findViewById(R.id.BatteryBar);

        final String angleNoneString = getString(R.string.angle_value_none);
        final String angleValueString = getString(R.string.angle_value);
        final String offsetNoneString = getString(R.string.offset_value_none);
        final String offsetValueString = getString(R.string.offset_value);

        logview = (TextView)findViewById(R.id.logview);
        //I chose to display only the last 3 messages
        logArray = new String[3];
        roomNameText = (EditText)findViewById(R.id.roomName);
        textChatText = (EditText)findViewById(R.id.textChat);
        otConnect = (Button) findViewById(R.id.otConnect);
        otDisConnect = (Button) findViewById(R.id.otDisConnect);
        leftProx = (Button) findViewById(R.id.leftProx);
        rightProx = (Button) findViewById(R.id.rightProx);
        midProx = (Button) findViewById(R.id.midProx);
        rearProx = (Button) findViewById(R.id.rearProx);
        connect = (Button) findViewById(R.id.connect);
        sndTxtMsg = (Button) findViewById(R.id.sndTxtMsg);
        connect.setOnClickListener(this);
        otConnect.setOnClickListener(this);
        otDisConnect.setOnClickListener(this);
        sndTxtMsg.setOnClickListener(this);

        final ToggleButton toggleAudio = (ToggleButton) findViewById(R.id.toggleAudio);
        toggleAudio.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

          public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
             if (mPublisher == null) {
               return;
                 }
                  if (isChecked) {
                      mPublisher.setPublishAudio(true);
                   } else {
                    mPublisher.setPublishAudio(false);
                   }
                   }});

        toggle_light = (Button) findViewById(R.id.toggle_light);
        toggle_light.setOnClickListener(this);

        play_sound = (Button) findViewById(R.id.play_sound);
        play_sound.setOnClickListener(this);
        Joystick joystick = (Joystick) findViewById(R.id.joystick);
        joystick.setJoystickListener(new JoystickListener() {
            @Override
            public void onDown() {
              // sendMessage();
            }

            @Override

              public void onDrag(float degrees, float offset) {

                //showing these next to the joystick view
                angleView.setText(String.format(angleValueString, degrees));
                offsetView.setText(String.format(offsetValueString, offset));


                // send drive message to remote system
                //convert to int
                int Xcoordint = (int)Joystick.Xcoord;
                int Ycoordint = (int)Joystick.Ycoord;
               //scale to -512 to +512 same as blynk app
               // Log.d(LOG_TAG, "Xcoord " + (Xcoordint));
               // Log.d(LOG_TAG, "Ycoord " + (Ycoordint));

                Xcoordint = (Xcoordint*4)+512;
                Ycoordint = (Ycoordint*4)+512;
                Log.d(LOG_TAG, "Xcoord" + (Xcoordint));
                Log.d(LOG_TAG, "Ycoord" + (Ycoordint));
                message= "hbd " + (String.valueOf(Xcoordint) + " " + (String.valueOf(Ycoordint)));

                sendMessage(); //send drive command over signaling link
            }

            @Override
            public void onUp() {
                angleView.setText(angleNoneString);
                offsetView.setText(offsetNoneString);

                //  bugView.setVelocity(0, 0);

                //on up send motor stop values
                message = "hbd " + "512" + " " + "512"; // stop values

                for (repeat = 1; repeat < 20; repeat++) { //we really want the stop to get through!
                    sendMessage();
                }
                repeat = 0;
            }
              // repeat = 0;
                // send 10 times to ensure we send the stop!

           // }
        });
        //it is better to handle bluetooth connection in onResume (ie able to reset when changing screens)
        requestPermissions();

        //define imageview section
        /*
        forwardArrow = (ImageView)findViewById(R.id.forward_arrow);
        forwardArrow.setOnClickListener(this);
        backArrow = (ImageView)findViewById(R.id.back_arrow);
        backArrow.setOnClickListener(this);
        rightArrow = (ImageView)findViewById(R.id.right_arrow);
        rightArrow.setOnClickListener(this);
        leftArrow = (ImageView)findViewById(R.id.left_arrow);
        leftArrow.setOnClickListener(this);
        stop = (ImageView)findViewById(R.id.stop);
        stop.setOnClickListener(this);*/

        leftProx.setOnClickListener(this);
        rightProx.setOnClickListener(this);
        midProx.setOnClickListener(this);
        rearProx.setOnClickListener(this);



    }

     /* Activity lifecycle methods */

    @Override
    protected void onPause() {

        Log.d(LOG_TAG, "onPause");

        super.onPause();

        if (mSession != null) {
            mSession.onPause();
        }

    }

    @Override
    protected void onResume() {

        Log.d(LOG_TAG, "onResume");

        super.onResume();

        if (mSession != null) {
            mSession.onResume();
        }
        //first of all, we check if there is bluetooth on the phone
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            // Device does not support Bluetooth
            Log.v(TAG, "Device does not support Bluetooth");
        }
        else{
            //Device supports BT
            if (!mBluetoothAdapter.isEnabled()){
                //if Bluetooth not activated, then request it
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
            else{
                //BT activated, then initiate the BtInterface object to handle all BT communication
                bt = new BtInterface(handlerStatus, handler);
            }
        }
        btread();//start the btread timer
    }
    //called only if the BT is not already activated, in order to activate it
    protected void onActivityResult(int requestCode, int resultCode, Intent moreData){
        if (requestCode == REQUEST_ENABLE_BT){
            if (resultCode == this.RESULT_OK){
                //BT activated, then initiate the BtInterface object to handle all BT communication
                bt = new BtInterface(handlerStatus, handler);
            }
            else if (resultCode == this.RESULT_CANCELED)
                Log.v(TAG, "BT not activated");
            else
                Log.v(TAG, "result code not known");
        }
        else{
            Log.v(TAG, "request code not known");
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    @Override
    public void onPermissionsGranted(int requestCode, List<String> perms) {

        Log.d(LOG_TAG, "onPermissionsGranted:" + requestCode + ":" + perms.size());
    }

    @Override
    public void onPermissionsDenied(int requestCode, List<String> perms) {

        Log.d(LOG_TAG, "onPermissionsDenied:" + requestCode + ":" + perms.size());

        if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
            new AppSettingsDialog.Builder(this)
                    .setTitle(getString(R.string.title_settings_dialog))
                    .setRationale(getString(R.string.rationale_ask_again))
                    .setPositiveButton(getString(R.string.setting))
                    .setNegativeButton(getString(R.string.cancel))
                    .setRequestCode(RC_SETTINGS_SCREEN_PERM)
                    .build()
                    .show();
        }
    }

    @AfterPermissionGranted(RC_VIDEO_APP_PERM)
    private void requestPermissions() {
        String[] perms = { Manifest.permission.INTERNET, Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO };
        if (EasyPermissions.hasPermissions(this, perms)) {
            // initialize view objects from your layout
            mPublisherViewContainer = (FrameLayout) findViewById(R.id.publisher_container);
            mSubscriberViewContainer = (FrameLayout) findViewById(R.id.subscriber_container);

            // initialize and connect to the session
           // fetchSessionConnectionData();

        } else {
            EasyPermissions.requestPermissions(this, "This app needs access to your camera and mic to make video calls", RC_VIDEO_APP_PERM, perms);
        }
    }
    private void startOtSession(){
        fetchSessionConnectionData();
    }

    private void initializeSession(String apiKey, String sessionId, String token) {

        mSession = new Session.Builder(this, apiKey, sessionId).build();
        mSession.setSessionListener(this);
        mSession.setSignalListener(this);
        mSession.connect(token);
    }

    /* Web Service Coordinator delegate methods */

    @Override
    public void onSessionConnectionDataReady(String apiKey, String sessionId, String token) {

        Log.d(LOG_TAG, "ApiKey: "+apiKey + " SessionId: "+ sessionId + " Token: "+token);
        initializeSession(apiKey, sessionId, token);
    }

    @Override
    public void onWebServiceCoordinatorError(Exception error) {

        Log.e(LOG_TAG, "Web Service error: " + error.getMessage());
        Toast.makeText(this, "Web Service error: " + error.getMessage(), Toast.LENGTH_LONG).show();
        finish();

    }

    /* Session Listener methods */

    @Override
    public void onConnected(Session session) {

        Log.d(LOG_TAG, "onConnected: Connected to session: "+session.getSessionId());

        mSession.sendSignal("", "Hello, Signaling!");

        // initialize Publisher and set this object to listen to Publisher events
        mPublisher = new Publisher.Builder(this).build();
        mPublisher.setPublisherListener(this);

        // set publisher video style to fill view
        mPublisher.getRenderer().setStyle(BaseVideoRenderer.STYLE_VIDEO_SCALE,
                BaseVideoRenderer.STYLE_VIDEO_FILL);
        mPublisherViewContainer.addView(mPublisher.getView());
        if (mPublisher.getView() instanceof GLSurfaceView) {
            ((GLSurfaceView) mPublisher.getView()).setZOrderOnTop(true);
        }

        mSession.publish(mPublisher);
    }

    @Override
    public void onDisconnected(Session session) {

        Log.d(LOG_TAG, "onDisconnected: Disconnected from session: "+session.getSessionId());
    }

    @Override
    public void onStreamReceived(Session session, Stream stream) {

        Log.d(LOG_TAG, "onStreamReceived: New Stream Received "+stream.getStreamId() + " in session: "+session.getSessionId());

        if (mSubscriber == null) {
            mSubscriber = new Subscriber.Builder(this, stream).build();
            mSubscriber.getRenderer().setStyle(BaseVideoRenderer.STYLE_VIDEO_SCALE, BaseVideoRenderer.STYLE_VIDEO_FILL);
            mSubscriber.setSubscriberListener(this);
            mSession.subscribe(mSubscriber);
            mSubscriberViewContainer.addView(mSubscriber.getView());
        }
    }

    @Override
    public void onStreamDropped(Session session, Stream stream) {

        Log.d(LOG_TAG, "onStreamDropped: Stream Dropped: "+stream.getStreamId() +" in session: "+session.getSessionId());

        if (mSubscriber != null) {
            mSubscriber = null;
            mSubscriberViewContainer.removeAllViews();
        }
    }

    @Override
    public void onError(Session session, OpentokError opentokError) {
        Log.e(LOG_TAG, "onError: "+ opentokError.getErrorDomain() + " : " +
                opentokError.getErrorCode() + " - "+opentokError.getMessage() + " in session: "+ session.getSessionId());

        showOpenTokError(opentokError);
    }

    /* Publisher Listener methods */

    @Override
    public void onStreamCreated(PublisherKit publisherKit, Stream stream) {

        Log.d(LOG_TAG, "onStreamCreated: Publisher Stream Created. Own stream "+stream.getStreamId());

    }

    @Override
    public void onStreamDestroyed(PublisherKit publisherKit, Stream stream) {

        Log.d(LOG_TAG, "onStreamDestroyed: Publisher Stream Destroyed. Own stream "+stream.getStreamId());
    }

    @Override
    public void onError(PublisherKit publisherKit, OpentokError opentokError) {

        Log.e(LOG_TAG, "onError: "+opentokError.getErrorDomain() + " : " +
                opentokError.getErrorCode() +  " - "+opentokError.getMessage());

        showOpenTokError(opentokError);
    }

    @Override
    public void onConnected(SubscriberKit subscriberKit) {

        Log.d(LOG_TAG, "onConnected: Subscriber connected. Stream: "+subscriberKit.getStream().getStreamId());
        mSession.setSignalListener(this); //needs to be here otherwise not started!
        mSession.sendSignal("", "Hello, Signaling!");
        connected = true;
    }

    @Override
    public void onDisconnected(SubscriberKit subscriberKit) {
        connected = false;
        Log.d(LOG_TAG, "onDisconnected: Subscriber disconnected. Stream: "+subscriberKit.getStream().getStreamId());
    }

    @Override
    public void onError(SubscriberKit subscriberKit, OpentokError opentokError) {

        Log.e(LOG_TAG, "onError: "+opentokError.getErrorDomain() + " : " +
                opentokError.getErrorCode() +  " - "+opentokError.getMessage());

        showOpenTokError(opentokError);
    }

    private void showOpenTokError(OpentokError opentokError) {

        Toast.makeText(this, opentokError.getErrorDomain().name() +": " +opentokError.getMessage() + " Please, see the logcat.", Toast.LENGTH_LONG).show();
        finish();
    }

    private void showConfigError(String alertTitle, final String errorMessage) {
        Log.e(LOG_TAG, "Error " + alertTitle + ": " + errorMessage);
        new AlertDialog.Builder(this)
                .setTitle(alertTitle)
                .setMessage(errorMessage)
                .setPositiveButton("ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        MainActivity.this.finish();
                    }
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }



    private void sendMessage() { //for sending signaling
    if (connected) {
            Log.d(LOG_TAG, "Send Message");
            //   mSession.sendSignal("", "Hello, Signaling!");

            mSession.sendSignal(SIGNAL_TYPE, message);
        }
    /*else Toast.makeText(MainActivity.this,
            "Not Connected",
            Toast.LENGTH_SHORT).show();*/
    }

    private void sendTxtMessage() { //for sending chat message
        if (connected) {
            Log.d(LOG_TAG, "Send Chat Message");
            //   mSession.sendSignal("", "Hello, Signaling!");

            mSession.sendSignal(MSG_TYPE, message);
        }
    /*else Toast.makeText(MainActivity.this,
            "Not Connected",
            Toast.LENGTH_SHORT).show();*/
    }

    @Override
    public void onSignalReceived(Session session, String type, String data, Connection connection) { //opentok signaling reception
       // Log.d(LOG_TAG, "Received Message");
        if (type != null && type.equals(SIGNAL_TYPE)) {
            Log.d(LOG_TAG, "Received Message " + data);
            // display the received text....
/*            Toast.makeText(MainActivity.this,
                    "Received " + data,
                    Toast.LENGTH_LONG).show();*/
            if (data.equals("LU 255")) {
                MainActivity.LU = 255;
                leftProx.setBackgroundColor(Color.RED);

            }
            if (data.equals("LU 0")) {
                MainActivity.LU = 0;
                leftProx.setBackgroundColor(Color.GREEN);

            }
            if (data.equals("CU 255")) {
                MainActivity.CU = 255;
                midProx.setBackgroundColor(Color.RED);

            }
            if (data.equals("CU 0")) {
                MainActivity.CU = 0;
                midProx.setBackgroundColor(Color.GREEN);

            }
            if (data.equals("RU 255")) {
                MainActivity.RU = 255;
                rightProx.setBackgroundColor(Color.RED);

            }
            if (data.equals("RU 0")) {
                MainActivity.RU = 0;
                rightProx.setBackgroundColor(Color.GREEN);

            }
            if (data.equals("BU 255")) {
                MainActivity.BU = 255;
                rearProx.setBackgroundColor(Color.RED);

            }
            if (data.equals("BU 0")) {
                MainActivity.BU = 0;
                rearProx.setBackgroundColor(Color.GREEN);

            }

            if (data.contains("battery")){

                String BattValStr=data.substring(8);
                double BatVal = Double.parseDouble(BattValStr);
                double BatValDoublePercent = (BatVal/13.8)*100;
                BatteryBar.setProgress((int) BatValDoublePercent);
                String BatValDoubleStr=Double.toString(BatVal);
                BatteryText.setText(("Battery Volts ")+(BatValDoubleStr.substring(0,5)));

            }

            if (data.equals("btConnect")) {

                logview.append("Attempting BT Conn" + "\n");
                bt.connect();

              /* Toast.makeText(MainActivity.this,
                        "trying to connect BT via remote " + data,
                        Toast.LENGTH_LONG).show();*/
            }

            if (data.equals("btAttached")){
                logview.append("Bluetooth is connected" + "\n");
                btremoteconnected = true; //flag to ind remote systems bt is connected
                connect.setText("BT CONNECTED");//update the local button text
            }
            // Log.d(LOG_TAG, "send to bthere before check.. " + data);
            // if (btremoteconnected == true) { //only send bt data to bt interface if it is connected
             if ((data.contains("hbd"))) { // only send bt data to bthere if contains hbd - handle blink drive
            //send to bluetooth on Bthere
            //  Log.d(LOG_TAG, "send to bthere.. " + data);
             delay = delay + 1;
                if  (delay == 5) { // send only once every n times so as to not drop bt traffic
                    bt.sendData(data);
                    delay = 0;
                    //data = "";
                }
        }

            if ((data.contains("tlt"))) { // only send bt data to bthere if contains tlt - handle tilt drive
                //send to bluetooth on Bthere
                //  Log.d(LOG_TAG, "send to bthere.. " + data);
               // delay = delay + 1;
               // if  (delay == 5) { // send only once every n times so as to not drop bt traffic
                    bt.sendData(data);
               //     delay = 0;
                    //data = "";
               // }
            }
               if ((data.contains("play"))) {
                    bt.sendData(data);
                    //data = "";
             }



        }

        if (type != null && type.equals(MSG_TYPE)) { //receiving chat message
            textChatText.setText (data); //set text value on text dialog
        }
    }


    @Override
        public void onClick(View v) {
        if(v == otConnect) {
            Log.d(LOG_TAG, "Trying to connect Opentok");
            startOtSession(); //connect to OpenTok
        }

        if(v == otDisConnect) {
             Log.d(LOG_TAG, "Trying to Disconnect Opentok");
            mSession.disconnect();
        }

        if(v == connect) {
            Log.d(LOG_TAG, "Trying to connect BT");
            message="btConnect";
            sendMessage(); //allows remote end user to connect bthere end device to arduino bt
            bt.connect(); // connect the bluetooth

            if(BtInterface.btconnected == 1){
              //  connect.setEnabled(false);
                connect.setText("BT CONNECTED");//update the button text
                message="btAttached";
                sendMessage(); // send connected message over signalling to the far end
            }

        }

        else if(v == forwardArrow) {
            //addToLog("Move Forward");
            message="forward";
            sendMessage();
        }
        else if(v == stop) {
            //addToLog("Move Forward");
             message="stop";
             sendMessage();
        }
        else if(v == play_sound) {
            //addToLog("Play Sound");
            message="play 1";
            sendMessage();
        }

        else if(v == sndTxtMsg) {
            //addToLog("send text message");
            message= String.valueOf(textChatText.getText());
            sendTxtMessage();
        }
       // else if(v == leftProx) {
       //     addToLog("left prox");

       // }
    }

    //private void addToLog(String trying_to_connect) {
    //}
}
