package com.xiehongfeng100.carctrlpanel;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {

    // Information in Setting
    // Save information in Setting
    private final String  PREFERENCES_NAME = "settinginfo";
//    private SharedPreferences preferences = getSharedPreferences(PREFERENCES_NAME, Activity.MODE_PRIVATE);
    // Connectivity
    private static String Client_IP = "127.0.0.1";
    private static int Client_Port = 8888;
    private static InetAddress serAddr = null;
    private Socket socket = null;
    private Boolean isConnected = false;

    // Speed
    private static int SpeedDelta = 0;
    private static int speed = 0;

    // Car control data structure
    private static int index = 0;
    private static CarCtrlDataStruct runForward;
    private static CarCtrlDataStruct runBackward;
    private static CarCtrlDataStruct turnLeft;
    private static CarCtrlDataStruct turnRight;
    private static CarCtrlDataStruct runStop;

    // Buttons to control the car
    private static ImageView imageRunStop = null;
    private static ImageView imageRunForward = null;
    private static ImageView imageRunBackward = null;
    private static ImageView imageTurnLeft = null;
    private static ImageView imageTurnRight = null;

    // Operation type
    private static byte operType = 0;
    private static final int OPER_RUNSTOP = 0;
    private static final int OPER_RUNFORWARD = 1;
    private static final int OPER_RUNBACKWARD = 2;
    private static final int OPER_TURNLEFT = 3;
    private static final int OPER_TURNRIGHT = 4;

    // Echo log
    private static TextView echoLog = null;
//    private static ScrollView scrollView = null;
//    private static int logCount = 0;
    // Log type
    private final int logIDBase = 0;
    private final int LOG_REMIND_CONNECT = logIDBase + 0;
    private final int LOG_CONNECTION_SUCCESS = logIDBase + 1;
    private final int LOG_CONNECTION_FAILED = logIDBase + 2;
    private final int LOG_CONNECTION_CLOSED = logIDBase + 3;
    private final int LOG_CONNECTION_CLOSURE_FAILED = logIDBase + 4;
    private final int LOG_SETTING_INCORRECT = logIDBase + 5;
    private final int LOG_RUNSTOP = logIDBase + 7;
    private final int LOG_RUNFORWARD = logIDBase + 8;
    private final int LOG_RUNBACKWARD = logIDBase + 9;
    private final int LOG_TURNLEFT = logIDBase + 10;
    private final int LOG_TURNRIGHT = logIDBase + 11;
    private final int LOG_ACTION_EDIT_SETTING = logIDBase + 12;
    private final int LOG_ACTION_SETTING_IS_SET = logIDBase + 13;
    private final int LOG_ACTION_SETTING_IS_CANCELED = logIDBase + 14;
    private final int LOG_ACTION_SETTING_INPUTS_IS_INVALID = logIDBase + 15;
    private final int LOG_ACTION_HELP = logIDBase + 16;
    private final int LOG_ACTION_QUIT = logIDBase + 17;

    // Handle double click event
    private static final long DOUBLE_PRESS_INTERVAL = 250; // in millis
    private static long lastPresstime = System.currentTimeMillis();

    // Verify input if it is IPv4 address or not
    private static final Pattern IPV4_PATTERN =
            Pattern.compile(
                    "^(25[0-5]|2[0-4]\\d|[0-1]?\\d?\\d)(\\.(25[0-5]|2[0-4]\\d|[0-1]?\\d?\\d)){3}$");
    private static final Pattern PORT_PATTERN =
            Pattern.compile(
                    "^6[0-5][0-5][0-3][0-5]|[0-5]\\d{0,4}|[0-9]\\d{0,3}");
    private static final Pattern SPEED_DELTA_PATTERN =
            Pattern.compile(
                    "^200|2[0-9]|[0-1]\\d{0,2}|[0-9]\\d");
    public static boolean isIPv4Address(final String input) {
        return IPV4_PATTERN.matcher(input).matches();
    }
    public static boolean isPortValid(final String input){
        return PORT_PATTERN.matcher(input).matches();
    }
    public static boolean isSpeedDeltaValid(final String input){
        return SPEED_DELTA_PATTERN.matcher(input).matches();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        imageRunStop = (ImageView) findViewById(R.id.image_runstop);
        imageRunForward = (ImageView) findViewById(R.id.image_runforward);
        imageRunBackward = (ImageView) findViewById(R.id.image_runbackward);
        imageTurnLeft = (ImageView) findViewById(R.id.image_turnleft);
        imageTurnRight = (ImageView) findViewById(R.id.image_turnright);

        echoLog = (TextView) findViewById(R.id.echo_log);
//        scrollView = (ScrollView) findViewById(R.id.echo_log_scrollview);
        findFocusForEchoLogEditText();
        echoLog.append("Starting to log...\n");
//        echoLog.append("Important Instructions:\n" +
//                "1. Edit Setting to set information of your client.\n" +
//                "2. Double click on the 'S' button to start an connection.\n" +
//                "3. Double click on it when you want the car to stop.\n" +
//                "4. Long press it to close the connection and double click to create a new connection.\n");


        imageRunStop.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                new Thread(new CloseConnectionThread()).start();
                return false;
            }
        });

        imageRunStop.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {

                // Handl double click
                long curPressTime = System.currentTimeMillis();
                if (curPressTime - lastPresstime < DOUBLE_PRESS_INTERVAL) {
                    operType = OPER_RUNSTOP;
                    new Thread(new ClientThread()).start();
                }
//                else if(!isConnected){
//                    asynMsgSend(LOG_SINGLE_CLICK_INVALID);
//                }
                lastPresstime = curPressTime;

            }

        });

        imageRunForward.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                operType = OPER_RUNFORWARD;
                new Thread(new ClientThread()).start();
            }
        });

        imageRunBackward.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                operType = OPER_RUNBACKWARD;
                new Thread(new ClientThread()).start();
            }
        });

        imageTurnLeft.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                operType = OPER_TURNLEFT;
                new Thread(new ClientThread()).start();
            }
        });

        imageTurnRight.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                operType = OPER_TURNRIGHT;
                new Thread(new ClientThread()).start();
            }
        });

    }


    class ClientThread implements Runnable {

        @Override
        public void run() {

            if (!isConnected)
            {
                switch (operType)
                {
                    case OPER_RUNFORWARD:
                    case OPER_RUNBACKWARD:
                    case OPER_TURNLEFT:
                    case OPER_TURNRIGHT:
                        asynMsgSend(LOG_REMIND_CONNECT);
                        break;
                    case OPER_RUNSTOP:
                        try{
                            serAddr = InetAddress.getByName(Client_IP);
                            socket = new Socket(serAddr, Client_Port);
                            if (socket.isConnected()) {
                                asynMsgSend(LOG_CONNECTION_SUCCESS);
                                isConnected = true;

                                DataOutputStream dOut = new DataOutputStream(socket.getOutputStream());
                                dOut.writeBytes("control");
                                dOut.flush();

                            } else {
                                asynMsgSend(LOG_CONNECTION_FAILED);
                                isConnected = false;
                            }
                        } catch (UnknownHostException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        if(!isConnected){
                            asynMsgSend(LOG_SETTING_INCORRECT);
                        }

                        break;
                }

            }

            else {
                try {
                    if (socket.isConnected()) {

                        // Write to Server
                        byte[] byteToWrite = null;
                        switch (operType)
                        {
                            case OPER_RUNSTOP:
                                setRunStop();
                                byteToWrite = runStop.serializedStream;
                                asynMsgSend(LOG_RUNSTOP);
                                break;
                            case OPER_RUNFORWARD:
                                setRunForward();
                                byteToWrite = runForward.serializedStream;
                                asynMsgSend(LOG_RUNFORWARD);
                                break;
                            case OPER_RUNBACKWARD:
                                setRunBackward();
                                byteToWrite = runBackward.serializedStream;
                                asynMsgSend(LOG_RUNBACKWARD);
                                break;
                            case OPER_TURNLEFT:
                                setTurnLeft();
                                byteToWrite = turnLeft.serializedStream;
                                asynMsgSend(LOG_TURNLEFT);
                                break;
                            case OPER_TURNRIGHT:
                                setTurnRight();
                                byteToWrite = turnRight.serializedStream;
                                asynMsgSend(LOG_TURNRIGHT);
                                break;
                        }

                        DataOutputStream dOut = new DataOutputStream(socket.getOutputStream());
                        dOut.write(byteToWrite);
                        dOut.flush();

                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            // Set focus at every thread
//            findFocusForEchoLogEditText();
        }

    }

    class CloseConnectionThread implements Runnable{

        @Override
        public void run() {

            if(isConnected){
                try{
                    if(socket.isConnected()){
                        speed = 0;
                        index = 0;
                        socket.close();
                        asynMsgSend(LOG_CONNECTION_CLOSED);
                    }
                }catch (IOException e){
                    e.printStackTrace();
                    asynMsgSend(LOG_CONNECTION_CLOSURE_FAILED);
                }

                // Reset connect state
                isConnected = false;
            }else {
                // nothing to do
            }

            // Set focus at every thread
//            findFocusForEchoLogEditText();
        }

    }

    public void setRunStop() {
        speed = 0;
        this.runForward.index = index;
        this.runForward.type = 0x0001;
//        this.runForward.key = 0x0070;
        this.runForward.key = 0x0100;
        this.runForward.val = speed;
        this.runForward.serialization();
        index++;
    }

    public void setRunForward() {
        speed += SpeedDelta;
        this.runForward.index = index;
        this.runForward.type = 0x0001;
        this.runForward.key = 0x0100;
        this.runForward.val = speed;
        this.runForward.serialization();
        index++;
    }

    public void setRunBackward() {
        speed -= SpeedDelta;
        this.runForward.index = index;
        this.runForward.type = 0x0001;
        this.runForward.key = 0x0101;
        this.runForward.val = speed;
        this.runForward.serialization();
        index++;
    }

    public void setTurnLeft() {
        this.runForward.index = index;
        this.runForward.type = 0x0004;
        this.runForward.key = 0x0102;
        this.runForward.val = 90;
        this.runForward.serialization();
        index++;
    }

    public void setTurnRight() {
        this.runForward.index = index;
        this.runForward.type = 0x0004;
        this.runForward.key = 0x0103;
        this.runForward.val = -90;
        this.runForward.serialization();
        index++;
    }

    // Handle log
    public void asynMsgSend(int logID)
    {
        Message log = new Message();
        log.what = logID;
        handler.sendMessage(log);
    }

    private Handler handler = new Handler() {
        public void handleMessage(Message log) {
            switch (log.what) {
                case LOG_REMIND_CONNECT:
                    echoLogFunc("Please connect to client first.");
                    Toast.makeText(getApplicationContext(), "Please connect to client first.", Toast.LENGTH_SHORT).show();
                    break;
                case LOG_CONNECTION_SUCCESS:
                    echoLogFunc("New Connection Success.");
                    break;
                case LOG_CONNECTION_FAILED:
                    echoLogFunc("New Connection Failed.");
                    break;
                case LOG_CONNECTION_CLOSED:
                    echoLogFunc("Old Connection Closed. Let index = 0 and speed = 0.");
                    break;
                case LOG_CONNECTION_CLOSURE_FAILED:
                    echoLogFunc("Old Connection Closure Failed.");
                    break;
                case LOG_SETTING_INCORRECT:
                    echoLogFunc("Setting is incorrect.");
                    break;
                case LOG_RUNSTOP:
                    echoLogFunc("Stop.");
                    break;
                case LOG_RUNFORWARD:
                    echoLogFunc("Run Forward.");
                    break;
                case LOG_RUNBACKWARD:
                    echoLogFunc("Run Backward.");
                    break;
                case LOG_TURNLEFT:
                    echoLogFunc("Turn Left.");
                    break;
                case LOG_TURNRIGHT:
                    echoLogFunc("Turn Right.");
                    break;
                case LOG_ACTION_EDIT_SETTING:
                    echoLogFunc("Edit Setting.");
                    break;
                case LOG_ACTION_SETTING_IS_SET:
                    echoLogFunc("Setting is set.");
                    break;
                case LOG_ACTION_SETTING_IS_CANCELED:
                    echoLogFunc("Setting is canceled.");
                    break;
                case LOG_ACTION_SETTING_INPUTS_IS_INVALID:
                    echoLogFunc("Setting inputs is invalid.");
                    Toast.makeText(getApplicationContext(), "Setting inputs is invalid.", Toast.LENGTH_SHORT).show();
                    break;
                case LOG_ACTION_HELP:
                    echoLogFunc("Help.");
                    break;
                case LOG_ACTION_QUIT:
                    echoLogFunc("Exit.");
                    break;
                default:
                    echoLogFunc("Something Wrong.");
                    break;
            }
        }
    };

    public void echoLogFunc(String log)
    {
        // Get system time
        SimpleDateFormat sDateFormat = new SimpleDateFormat("HH:mm:ss");
        Date curDate = new Date(System.currentTimeMillis());
        String timeStr = sDateFormat.format(curDate);

//        Log.i("", log);

        // Display at PC terminal
        System.out.println(timeStr + "#" + index + " " + log);

        // Display at Phone
        echoLog.append("\n");
        echoLog.setHighlightColor(1407849);
        echoLog.append(timeStr + "#" + index + " " + log);

//        logCount++;
    }

    public void findFocusForEchoLogEditText()
    {
        // With this, the echoLog EditText will not lose focus because of its parent ScrollView
        // Referred blog: http://blog.csdn.net/zy1409/article/details/40453751
        echoLog.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                echoLog.getParent().requestDisallowInterceptTouchEvent(true);
                return false;
            }
        });
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
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

            asynMsgSend(LOG_ACTION_EDIT_SETTING);

            // get settingdlg.xml view
            LayoutInflater layoutInflater = LayoutInflater.from(MainActivity.this);
            View viewSettingDlg = layoutInflater.inflate(R.layout.settingdlg, null);
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(MainActivity.this);
            alertDialogBuilder.setTitle("Setting Dialog");
            alertDialogBuilder.setView(viewSettingDlg);

            final EditText editClientIP = (EditText) viewSettingDlg.findViewById(R.id.edit_IP);
            final EditText editClientPort = (EditText) viewSettingDlg.findViewById(R.id.edit_Port);
            final EditText editSpeedDelta = (EditText) viewSettingDlg.findViewById(R.id.edit_Delta);

            final SharedPreferences preferences = getSharedPreferences(PREFERENCES_NAME, Activity.MODE_PRIVATE);
            editClientIP.setText(preferences.getString("ClientIP", null));
            editClientPort.setText(preferences.getString("ClientPort", null));
            editSpeedDelta.setText(preferences.getString("SpeedDelta", null));

            // setup a dialog window
            alertDialogBuilder.setCancelable(false)
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {

                            // Take precaution for invalid inputs
                            String clientIP = editClientIP.getText().toString();
                            String clientPort = editClientPort.getText().toString();
                            String speedDelta = editSpeedDelta.getText().toString();

                            if(clientIP.length() == 0 || clientPort.length() == 0 || speedDelta.length() == 0 ||
                                    !isIPv4Address(clientIP) || !isPortValid(clientPort) || !isSpeedDeltaValid(speedDelta)){
                                asynMsgSend(LOG_ACTION_SETTING_INPUTS_IS_INVALID);
                            }else { // If inputs are all valid

                                Client_IP = editClientIP.getText().toString();
                                Client_Port = Integer.parseInt(editClientPort.getText().toString());
                                SpeedDelta = Integer.parseInt(editSpeedDelta.getText().toString());

                                try {
                                    if (isConnected && socket.isConnected()) {
                                        speed = 0;
                                        index = 0;
                                        socket.close();
                                        asynMsgSend(LOG_CONNECTION_CLOSED);
                                    }
                                } catch (IOException e) {
                                    e.printStackTrace();
                                    asynMsgSend(LOG_CONNECTION_CLOSURE_FAILED);
                                }

                                // Setting is set
                                asynMsgSend(LOG_ACTION_SETTING_IS_SET);

                                // Reset connect state
                                isConnected = false;

                                // Save information in setting
//                              SharedPreferences preferences = getSharedPreferences(PREFERENCES_NAME, Activity.MODE_PRIVATE);
                                SharedPreferences.Editor editor = preferences.edit();
                                editor.putString("ClientIP", editClientIP.getText().toString());
                                editor.putString("ClientPort", editClientPort.getText().toString());
                                editor.putString("SpeedDelta", editSpeedDelta.getText().toString());
                                editor.commit();

                            }

                        }
                    })
                    .setNegativeButton("Cancel",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    // Setting is unset
                                    asynMsgSend(LOG_ACTION_SETTING_IS_CANCELED);
                                    dialog.cancel();
                                }
                            });

            // create an alert dialog
            AlertDialog alert = alertDialogBuilder.create();
            alert.show();

//             return false;

        }else if(id == R.id.action_help) {

            asynMsgSend(LOG_ACTION_HELP);

            LayoutInflater layoutInflater = LayoutInflater.from(MainActivity.this);
            View viewAboutDlg = layoutInflater.inflate(R.layout.aboutdlg, null);
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(MainActivity.this);
            alertDialogBuilder.setView(viewAboutDlg);
            alertDialogBuilder.setTitle("Help");
            AlertDialog alert = alertDialogBuilder.create();
            alert.show();

        }else if(id == R.id.action_quit){

            asynMsgSend(LOG_ACTION_QUIT);

            System.exit(0);
        }

        return super.onOptionsItemSelected(item);
    }
}
