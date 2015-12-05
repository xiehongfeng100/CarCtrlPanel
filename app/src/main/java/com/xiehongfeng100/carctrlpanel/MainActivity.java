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

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

public class MainActivity extends AppCompatActivity {

    private String SERVER_IP = "127.0.0.1";
    private int SERVER_PORT = 8888;
    InetAddress serAddr = null;
    private Socket socket = null;
    Boolean isConnected = false;

    private  int speedDelta = 0;

    private EditText editText = null;
    private Button sendButton = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        editText = (EditText) findViewById(R.id.edit_message);
        sendButton = (Button) findViewById(R.id.send_message);
        sendButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
//                OutputText.setText("");
//                long SLEEP_TIME = 100; //Sleep Ti
                new Thread(new ClientThread()).start(); //Create a new Thread
//                OutputText.append("\nConnecting to " + SERVER_IP + "\n");
//                if (isConnected) {
//                    OutputText.append(socket.getRemoteSocketAddress().toString());
//                    OutputText.append("\n[+] Connection Established");
//                } else if (!isConnected) {
//                    OutputText.append("\n[-] Connection Failed");
//                }

            }
        });

//        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
//        fab.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                        .setAction("Action", null).show();
//            }
//        });
    }

    class ClientThread implements Runnable {

        @Override
        public void run() {
            try {
                serAddr = InetAddress.getByName(SERVER_IP);
                socket = new Socket(serAddr, SERVER_PORT);
                if(socket.isConnected()){
                    Log.i("", "Connection Established");
                    System.out.println("Connection Established");
                    isConnected = true;

                    // Write to Server
                    PrintWriter out = new PrintWriter(new BufferedWriter(
                            new OutputStreamWriter(socket.getOutputStream())), true);

                    out.println(editText.getText());
                    System.out.println("msg=" + editText.getText());

                }else{
                    Log.i("", "Connection Failed!");
                    System.out.println("Connection Failed");
                    isConnected = false;
                }
            } catch (UnknownHostException e1) {
                e1.printStackTrace();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }

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

            // get prompts.xml view
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
        }

        return super.onOptionsItemSelected(item);
    }
}
