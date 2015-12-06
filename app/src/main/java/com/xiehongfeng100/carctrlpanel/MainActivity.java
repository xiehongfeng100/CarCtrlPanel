package com.xiehongfeng100.carctrlpanel;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

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
    // 00: stop
    // 01: forward
    // 02: backward
    // 03: turn left
    // 04: turn right
    private static byte operType = 0x00;

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

        runStopBtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                operType = 0x00;
                new Thread(new ClientThread()).start(); //Create a new Thread
            }
        });

        runForwardBtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                operType = 0x01;
                new Thread(new ClientThread()).start();
            }
        });

        runBackwardBtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                operType = 0x02;
                new Thread(new ClientThread()).start();
            }
        });

        turnLeftBtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                operType = 0x03;
                new Thread(new ClientThread()).start();
            }
        });

        turnRightBtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                operType = 0x04;
                new Thread(new ClientThread()).start();
            }
        });

    }

    class ClientThread implements Runnable {

        @Override
        public void run() {

            if (!isConnected)
            {
//                try{
//                    if(socket.isConnected()){
//                        Log.i("", "Close Connection");
//                        System.out.println("Close Connection");
//                        socket.close();
//                    }
//                }catch (IOException e){
//                    e.printStackTrace();
//                }

                try{
                    serAddr = InetAddress.getByName(SERVER_IP);
                    socket = new Socket(serAddr, SERVER_PORT);
                    if (socket.isConnected()) {
                        Log.i("", "Connection Established");
                        System.out.println("Connection Established");
                        isConnected = true;

                        DataOutputStream dOut = new DataOutputStream(socket.getOutputStream());
                        dOut.writeBytes("control");
                        dOut.flush();

                    } else {
                        Log.i("", "Connection Failed!");
                        System.out.println("Connection Failed");
                        isConnected = false;
                    }
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            else {
                try {
                    if (socket.isConnected()) {

                        // Write to Server
                        byte[] byteToWrite = null;
                        switch (operType)
                        {
                            case 0x00:
                                setRunStop();
                                byteToWrite = runStop.serializedStream;
                                break;
                            case 0x01:
                                setRunForward();
                                byteToWrite = runForward.serializedStream;
                                break;
                            case 0x02:
                                setRunBackward();
                                byteToWrite = runBackward.serializedStream;
                                break;
                            case 0x03:
                                setTurnLeft();
                                byteToWrite = turnLeft.serializedStream;
                                break;
                            case 0x04:
                                setTurnRight();
                                byteToWrite = turnRight.serializedStream;
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
        this.runForward.key = 0x0070;
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
                                    Log.i("", "Close Connection");
                                    System.out.println("Close Connection");
                                    speed = 0;
                                    socket.close();
                                }
                            }catch (IOException e){
                                e.printStackTrace();
                            }

                            isConnected = false;

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

        }

        return super.onOptionsItemSelected(item);
    }
}
