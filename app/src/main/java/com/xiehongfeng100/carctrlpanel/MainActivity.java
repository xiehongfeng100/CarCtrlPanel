package com.xiehongfeng100.carctrlpanel;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    // Connectivity
    private static String SERVER_IP = "10.0.126.117";
    private static int SERVER_PORT = 8888;
    private static InetAddress serAddr = null;
    private Socket socket = null;
    private Boolean isConnected = false;

    // Speed
    private static int speedDelta = 0;
    private static int speed = 0;

    // Car control data structure
    private static int index = 0;
    private static CarCtrlDataStruct runForward;
    private static CarCtrlDataStruct runBackward;
    private static CarCtrlDataStruct turnLeft;
    private static CarCtrlDataStruct turnRight;
    private static CarCtrlDataStruct runStop;

    // Buttons to control the car
    private static Button runForwardBtn = null;
    private static Button runBackwardBtn = null;
    private static Button turnLeftBtn = null;
    private static Button turnRightBtn = null;
    private static Button runStopBtn = null;

    // Operation type
    private static byte operType = 0;
    final int OPER_RUNSTOP = 0;
    final int OPER_RUNFORWARD = 1;
    final int OPER_RUNBACKWARD = 2;
    final int OPER_TURNLEFT = 3;
    final int OPER_TURNRIGHT = 4;

    // Echo log
    private static TextView echoLog = null;
    private static int logCount = 0;
    // Log type
    final int logIDBase = 0;
    final int LOG_REMIND_CONNECT = logIDBase + 0;
    final int LOG_CONNECTION_SUCCESS = logIDBase + 1;
    final int LOG_CONNECTION_FAILED = logIDBase + 2;
    final int LOG_CONNECTION_CLOSED = logIDBase + 3;
    final int LOG_CONNECTION_CLOSURE_FAILED = logIDBase + 4;
    final int LOG_RUNSTOP = logIDBase + 5;
    final int LOG_RUNFORWARD = logIDBase + 6;
    final int LOG_RUNBACKWARD = logIDBase + 7;
    final int LOG_TURNLEFT = logIDBase + 8;
    final int LOG_TURNRIGHT = logIDBase + 9;
    final int LOG_ACTION_SETTINGS = logIDBase + 10;
    final int LOG_ACTION_ABOUT = logIDBase + 11;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        runStopBtn = (Button) findViewById(R.id.runstop);
        runForwardBtn = (Button) findViewById(R.id.runforward);
        runBackwardBtn= (Button) findViewById(R.id.runbackward);
        turnLeftBtn = (Button) findViewById(R.id.turnleft);
        turnRightBtn = (Button) findViewById(R.id.turnright);

        echoLog = (TextView) findViewById(R.id.echo_log);
        echoLog.append("--------- LogDog ---------");

        runStopBtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                operType = OPER_RUNSTOP;
                new Thread(new ClientThread()).start();
            }
        });

        runForwardBtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                operType = OPER_RUNFORWARD;
                new Thread(new ClientThread()).start();
            }
        });

        runBackwardBtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                operType = OPER_RUNBACKWARD;
                new Thread(new ClientThread()).start();
            }
        });

        turnLeftBtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                operType = OPER_TURNLEFT;
                new Thread(new ClientThread()).start();
            }
        });

        turnRightBtn.setOnClickListener(new View.OnClickListener() {

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
                            serAddr = InetAddress.getByName(SERVER_IP);
                            socket = new Socket(serAddr, SERVER_PORT);
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
        speed += speedDelta;
        this.runForward.index = index;
        this.runForward.type = 0x0001;
        this.runForward.key = 0x0100;
        this.runForward.val = speed;
        this.runForward.serialization();
        index++;
    }

    public void setRunBackward() {
        speed -= speedDelta;
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
                    echoLogFunc("Old Connection Closed.");
                    break;
                case LOG_CONNECTION_CLOSURE_FAILED:
                    echoLogFunc("Old Connection Closure Failed.");
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
                case LOG_ACTION_SETTINGS:
                    echoLogFunc("Setting.");
                    break;
                case LOG_ACTION_ABOUT:
                    echoLogFunc("About.");
                    break;
                default:
                    echoLogFunc("Something Wrong.");
                    break;
            }
        }
    };

    public void echoLogFunc(String log)
    {
        SimpleDateFormat sDateFormat = new SimpleDateFormat("hh:mm:ss");
        Date curDate = new Date(System.currentTimeMillis());
        String timeStr = sDateFormat.format(curDate);

//        Log.i("", log);

        // Display at PC terminal
        System.out.println(timeStr + "#" + logCount + " " + log);

        // Display at Phone
        echoLog.append("\n");
        echoLog.append(timeStr + "#" + logCount + " " + log);

        logCount++;
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

            asynMsgSend(LOG_ACTION_SETTINGS);

            // get settingdlg.xml view
            LayoutInflater layoutInflater = LayoutInflater.from(MainActivity.this);
            View viewSettingDlg = layoutInflater.inflate(R.layout.settingdlg, null);
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(MainActivity.this);
            alertDialogBuilder.setTitle("Setting Dialog");
            alertDialogBuilder.setView(viewSettingDlg);

            final EditText editIP = (EditText) viewSettingDlg.findViewById(R.id.edit_IP);
            final EditText editPort = (EditText) viewSettingDlg.findViewById(R.id.edit_Port);
            final EditText editDelta = (EditText) viewSettingDlg.findViewById(R.id.edit_Delta);

            // setup a dialog window
            alertDialogBuilder.setCancelable(false)
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            SERVER_IP = editIP.getText().toString();
                            SERVER_PORT = Integer.parseInt(editPort.getText().toString());
                            speedDelta = Integer.parseInt(editDelta.getText().toString());

                            try{
                                if(isConnected && socket.isConnected()){
                                    speed = 0;
                                    socket.close();
                                    asynMsgSend(LOG_CONNECTION_CLOSED);
                                }
                            }catch (IOException e){
                                e.printStackTrace();
                                asynMsgSend(LOG_CONNECTION_CLOSURE_FAILED);
                            }

                            // Create a new socket
                            isConnected = false;
//                            new Thread(new ClientThread()).start();

                        }
                    })
                    .setNegativeButton("Cancel",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    dialog.cancel();
                                }
                            });

            // create an alert dialog
            AlertDialog alert = alertDialogBuilder.create();
            alert.show();

//             return false;

        }else if(id == R.id.action_about){

            asynMsgSend(LOG_ACTION_ABOUT);

            new AlertDialog.Builder(this)
                    .setTitle("------- About -------")
                    .setMessage("Thanks for using! This APP is developed and maintained by Hongfeng Xie (xiehongfeng100@hotmail.com).")
                    .show();
        }

        return super.onOptionsItemSelected(item);
    }
}
