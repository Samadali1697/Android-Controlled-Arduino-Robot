package com.example.philipgo.arduinobluetoothrccar;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.philipgo.arduinobluetoothrccar.adapter.PairedListAdapter;
import com.example.philipgo.arduinobluetoothrccar.dialog.PairedDevicesDialog;
import com.example.philipgo.arduinobluetoothrccar.utility.MyLog;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;



public class BTMainActivity extends AppCompatActivity implements PairedDevicesDialog.PairedDeviceDialogListener {
    Handler handler = new Handler(); Runnable runnable; int refreshTime = 3000;

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final String TAG_DIALOG = "dialog";
    private static final String NO_BLUETOOTH = "Oops, your device doesn't support bluetooth";
    private static final String[] PIDS = {
            "01", "02", "03", "04", "05", "06", "07", "08",
            "09", "0A", "0B", "0C", "0D", "0E", "0F", "10",
            "11", "12", "13", "14", "15", "16", "17", "18",
            "19", "1A", "1B", "1C", "1D", "1E", "1F", "20"
    };
    
    // Commands
    //private static final String[] INIT_COMMANDS = {"AT Z", "AT SP 0", "0105", "010C", "010D", "0131"};
    //private static final String[] INIT_COMMANDS = {"ATIB 96", "ATIIA 13", "ATSH8213F0", "ATAL", "ATSP6", "0100"};
    private static final String[] INIT_COMMANDS = {"atsp6", "atsh7e0", "2105"};

    private int mCMDPointer = -1;

    // Intent request codes
    private static final int REQUEST_ENABLE_BT = 101;
    private static final int REQUEST_CONNECT_DEVICE_SECURE = 102;
    private static final int REQUEST_CONNECT_DEVICE_INSECURE = 103;

    // Message types accessed from the BluetoothIOGateway Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;

    // Key names accesses from the BluetoothIOGateway Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast_message";

    // Bluetooth
    private static BluetoothIOGateway mIOGateway;
    private static BluetoothAdapter mBluetoothAdapter;
    private DeviceBroadcastReceiver mReceiver;
    private PairedDevicesDialog dialog;
    private List<BluetoothDevice> mDeviceList;

    // Widgets
    private TextView mConnectionStatus;
    private TextView mMonitor;
    private EditText mCommandPrompt;
    private Button mBtnSend, mBtnScanBT;
    
    // Variable def
    private boolean inSimulatorMode = false;
    private static StringBuilder mSbCmdResp;
    private static StringBuilder mPartialResponse;
    private String mConnectedDeviceName;

    public static int temp = 0, speed = 0, rpm  = 0, fuelLevel = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        supportRequestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_obd_main);
        BTMainActivity.context = getApplicationContext();

        // log
        displayLog("=>\n***************\n     ELM 327 started\n***************");

        // connect widgets
        mMonitor = (TextView) findViewById(R.id.tvMonitor);
        mMonitor.setMovementMethod(new ScrollingMovementMethod());
        mConnectionStatus = (TextView) findViewById(R.id.tvConnectionStatus);
        //mCommandPrompt = (EditText) findViewById(R.id.etCommandPrompt);
        mBtnSend = (Button) findViewById(R.id.ibSendMessage);
        mBtnScanBT = (Button) findViewById(R.id.ibScanBT);


        mBtnSend.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                /*String command = mCommandPrompt.getText().toString();

                // Clear command
                mCommandPrompt.setText("");

                // Send command
                sendOBD2CMD(command);*/

                Intent i = new Intent(BTMainActivity.this, MainActivity.class);
                startActivity(i);
                // Close keyboard
                InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(mMonitor.getWindowToken(), 0);
            }
        });
        mBtnScanBT.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                queryPairedDevices();
                setupMonitor();
            }
        });

        // make sure user has Bluetooth hardware
        displayLog("Try to check hardware...");
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null)
        {
            // Device does not support Bluetooth
            displayMessage(NO_BLUETOOTH);
            displayLog(NO_BLUETOOTH);

            BTMainActivity.this.finish();
        }
        // log
        displayLog("Bluetooth found.");

        // Init variables
        mSbCmdResp = new StringBuilder();
        mPartialResponse = new StringBuilder();
        mIOGateway = new BluetoothIOGateway(this, mMsgHandler);
    }

    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this)
                .setIcon(R.mipmap.ic_launcher)
                .setTitle(R.string.app_name)
                .setMessage("Are you sure you want to close the App?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if(handler!=null){
                            handler.removeCallbacks(runnable);
                        }
                        finish();
                    }

                })
                .setNegativeButton("No", null)
                .show();
    }

    @SuppressLint("HandlerLeak")
    private final Handler mMsgHandler = new Handler()
    {
        @Override
        public void handleMessage(Message msg)
        {
            switch (msg.what)
            {
                case MESSAGE_STATE_CHANGE:
                    switch (msg.arg1)
                    {
                        case BluetoothIOGateway.STATE_CONNECTING:
                            mConnectionStatus.setText(getString(R.string.BT_connecting));
                            mConnectionStatus.setBackgroundColor(Color.YELLOW);
                            break;

                        case BluetoothIOGateway.STATE_CONNECTED:
                            mBtnSend.setEnabled(true);
                            mConnectionStatus.setText(getString(R.string.BT_status_connected_to) + " " + mConnectedDeviceName);
                            mConnectionStatus.setBackgroundColor(Color.GREEN);
                            mBtnScanBT.setEnabled(false);
                            sendDefaultCommands();
                            /*runnable = new Runnable() {
                                public void run() {
                                    //
                                    // Do the stuff
                                    //
                                    try{
                                        // Send command
                                        sendOBD2CMD("21 05 0C 0D");
                                        //sendOBD2CMD("212F");
                                        //Toast.makeText(getApplicationContext(),"Running", Toast.LENGTH_SHORT).show();
                                    }
                                    catch (Exception e){

                                    }
                                    handler.postDelayed(this, refreshTime);
                                }
                            };
                            runnable.run();*/
                            break;

                        case BluetoothIOGateway.STATE_LISTEN:
                        case BluetoothIOGateway.STATE_NONE:
                            mBtnSend.setEnabled(true);
                            mConnectionStatus.setText(getString(R.string.BT_status_not_connected));
                            mConnectionStatus.setBackgroundColor(Color.RED);
                            mBtnScanBT.setEnabled(true);
                            break;

                        default:
                            break;
                    }
                    break;

                case MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    
                    // construct a string from the valid bytes in the buffer
                    String readMessage = new String(readBuf, 0, msg.arg1);
                    readMessage = readMessage.trim();
                    readMessage = readMessage.toUpperCase();
                    displayLog(mConnectedDeviceName + ": " + readMessage);
                    if (!inSimulatorMode)
                    {
                        char lastChar='>';
                        if(readMessage.length()!=0){
                           lastChar  = readMessage.charAt(readMessage.length() - 1);
                        }


                        if (lastChar == '>')
                        {
                            parseResponse(mPartialResponse.toString() + readMessage);
                            mPartialResponse.setLength(0);
                        }
                        else 
                        {
                            mPartialResponse.append(readMessage);
                        }
                    }
                    else
                    {
                        mSbCmdResp.append("R>>");
                        mSbCmdResp.append(readMessage);
                        mSbCmdResp.append("\n");
                        mMonitor.setText(mSbCmdResp.toString());
                    }
                    break;

                case MESSAGE_WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;

                    // construct a string from the buffer
                    //String writeMessage = new String(writeBuf);
                    //displayLog("Me: " + writeMessage);
                    //mSbCmdResp.append("W>>");
                    //mSbCmdResp.append(writeMessage);
                    //mSbCmdResp.append("\n");
                    //mMonitor.setText(mSbCmdResp.toString());
                    break;

                case MESSAGE_TOAST:
                    displayMessage(msg.getData().getString(TOAST));
                    break;

                case MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
                    break;
            }
        }

    };
    


    @Override
    protected void onStart()
    {
        super.onStart();

        if (mBluetoothAdapter == null)
        {
            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        }

        // make sure Bluetooth is enabled
        displayLog("Try to check availability...");
        if (!mBluetoothAdapter.isEnabled())
        {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
        else
        {
            displayLog("Bluetooth is available");

            queryPairedDevices();
            setupMonitor();
        }
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        // Register EventBus
//        EventBus.getDefault().register(this);
    }

    @Override
    protected void onPause()
    {
        super.onPause();

        // Unregister EventBus
        //EventBus.getDefault().unregister(this);
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();

        // Un register receiver
        if (mReceiver != null)
        {
            unregisterReceiver(mReceiver);
        }

        // Stop scanning if is in progress
        cancelScanning();

        // Stop mIOGateway
        if (mIOGateway != null)
        {
            mIOGateway.stop();
        }
        
        // Clear StringBuilder
        if (mSbCmdResp.length() > 0)
        {
            mSbCmdResp.setLength(0);
        }

        if(handler!=null & runnable !=null){
            handler.removeCallbacks(runnable);
        }
    }



    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);


        switch (requestCode)
        {
            case REQUEST_ENABLE_BT:
                if (resultCode == RESULT_CANCELED)
                {
                    displayMessage("Bluetooth not enabled :(");
                    displayLog("Bluetooth not enabled :(");
                    return;
                }

                if (resultCode == RESULT_OK)
                {
                    displayLog("Bluetooth enabled");

                    queryPairedDevices();
                    setupMonitor();
                }

                break;

            default:
                // nothing at the moment
        }
    }
    
    private void setupMonitor()
    {
        // Start mIOGateway
        if (mIOGateway == null)
        {
            mIOGateway = new BluetoothIOGateway(this, mMsgHandler);
        }

        // Only if the state is STATE_NONE, do we know that we haven't started already
        if (mIOGateway.getState() == BluetoothIOGateway.STATE_NONE)
        {
            // Start the Bluetooth chat services
            mIOGateway.start();
        }

        // clear string builder if contains data
        if (mSbCmdResp.length() > 0)
        {
            mSbCmdResp.setLength(0);
        }
        
    }

    private void queryPairedDevices()
    {
        displayLog("Try to query paired devices...");
        
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        // If there are paired devices
        if (pairedDevices.size() > 0)
        {
            PairedDevicesDialog dialog = new PairedDevicesDialog();
            dialog.setAdapter(new PairedListAdapter(BTMainActivity.this, pairedDevices), false);
            showChooserDialog(dialog);
        }
        else
        {
            displayLog("No paired device found");

            scanAroundDevices();
        }
    }

    private void showChooserDialog(DialogFragment dialogFragment)
    {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        Fragment prev = getSupportFragmentManager().findFragmentByTag(TAG_DIALOG);
        if (prev != null)
        {
            ft.remove(prev);
        }
        ft.addToBackStack(null);

        dialogFragment.show(ft, "dialog");
    }

    private void scanAroundDevices()
    {
        displayLog("Try to scan around devices...");

        if (mReceiver == null)
        {
            // Register the BroadcastReceiver
            mReceiver = new DeviceBroadcastReceiver();
            IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            registerReceiver(mReceiver, filter);
        }

        // Start scanning
        mBluetoothAdapter.startDiscovery();
    }

    private void cancelScanning()
    {        
        if (mBluetoothAdapter.isDiscovering())
        {
            mBluetoothAdapter.cancelDiscovery();

            displayLog("Scanning canceled.");
        }
    }

    /**
     * Callback method for once a new device detected.
     *
     * @param device BluetoothDevice
     */
    public void onEvent(BluetoothDevice device)
    {
        if (mDeviceList == null)
        {
            mDeviceList = new ArrayList<>(10);
        }

        mDeviceList.add(device);

        // create dialog
        final Fragment fragment = this.getSupportFragmentManager().findFragmentByTag(TAG_DIALOG);
        if (fragment != null && fragment instanceof PairedDevicesDialog)
        {
            PairedListAdapter adapter = dialog.getAdapter();
            adapter.notifyDataSetChanged();
        }
        else
        {
            dialog = new PairedDevicesDialog();
            dialog.setAdapter(new PairedListAdapter(BTMainActivity.this, new HashSet<>(mDeviceList)), true);
            //dialog.setAdapter();
            showChooserDialog(dialog);
        }
    }
    private static Context context;

    private static void displayMessage(String msg)
    {

        Toast.makeText(BTMainActivity.context, msg, Toast.LENGTH_SHORT).show();
    }

    private void displayLog(String msg)
    {
        Log.d(TAG, msg);
    }

    @Override
    public void onDeviceSelected(BluetoothDevice device)
    {
        cancelScanning();

        displayLog("Selected device: " + device.getName() + " (" + device.getAddress() + ")");
        
        // Attempt to connect to the device
        mIOGateway.connect(device, true);
    }

    @Override
    public void onSearchAroundDevicesRequested()
    {
        scanAroundDevices();
    }

    @Override
    public void onCancelScanningRequested()
    {
        cancelScanning();
    }

    

    public static void sendOBD2CMD(String sendMsg)
    {
        /*if (mIOGateway.getState() != BluetoothIOGateway.STATE_CONNECTED)
        {
            displayMessage("Bluetooth device is unavailable");
            return;
        }
        
        String strCMD = sendMsg;
        strCMD += '\r';
        */
        byte[] byteCMD = sendMsg.getBytes();
        mIOGateway.write(byteCMD);
    }

    private void sendDefaultCommands()
    {
        if(inSimulatorMode)
        {
            displayMessage("You are in simulator mode!");
            return;
        }
        
        if (mCMDPointer >= INIT_COMMANDS.length)
        {
            mCMDPointer = -1;
            return;
        }
        
        // reset pointer
        if (mCMDPointer < 0)
        {
            mCMDPointer = 0;
        }
        
        sendOBD2CMD(INIT_COMMANDS[mCMDPointer]);
    }
    
    private void parseResponse(String buffer)
    {        
        switch (mCMDPointer)
        {
            case 0: // CMD: AT Z, no parse needed
            case 1: // CMD: AT SP 0, no parse needed
                //mSbCmdResp.append("R>>");
                //mSbCmdResp.append(buffer);
                //mSbCmdResp.append("\n");

                break;
            
            /*case 2: // CMD: 0105, Engine coolant temperature
                //int ect = showEngineCoolantTemperature(buffer);
                String substring = buffer.substring(Math.max(buffer.length() - 2, 0));
                String rr = cleanResponse(substring);
                int ect = showEngineCoolantTemperature(rr);
                mSbCmdResp.append( "Eng. Coolant Temp is "+ect+"\n");
                /*mSbCmdResp.append("R>>");
                mSbCmdResp.append(buffer);
                mSbCmdResp.append( " (Eng. Coolant Temp is ");
                mSbCmdResp.append(ect);
                mSbCmdResp.append((char) 0x00B0);
                mSbCmdResp.append("C)");
                mSbCmdResp.append("\n");
                break;

            case 3: // CMD: 010C, EngineRPM
                int eRPM = showEngineRPM(buffer);
                mSbCmdResp.append("R>>");
                mSbCmdResp.append(buffer);
                mSbCmdResp.append( " (Eng. RPM: ");
                mSbCmdResp.append(eRPM);
                mSbCmdResp.append(")");
                mSbCmdResp.append("\n");
                break;

            case 4: // CMD: 010D, Vehicle Speed
                int vs = showVehicleSpeed(buffer);
                mSbCmdResp.append("R>>");
                mSbCmdResp.append(buffer);
                mSbCmdResp.append( " (Vehicle Speed: ");
                mSbCmdResp.append(vs);
                mSbCmdResp.append("Km/h)");
                mSbCmdResp.append("\n");
                break;
            
            case 5: // CMD: 0131
                int dt = showDistanceTraveled(buffer);
                mSbCmdResp.append("R>>");
                mSbCmdResp.append(buffer);
                mSbCmdResp.append( " (Distance traveled since codes cleared: ");
                mSbCmdResp.append(dt);
                mSbCmdResp.append("Km)");
                mSbCmdResp.append("\n");
                break;*/
            
            default:
                /*mSbCmdResp.append("R>>");
                mSbCmdResp.append(buffer);
                mSbCmdResp.append("\n");*/
                //int type = 0;
                String rr = cleanResponse(buffer);
                String[] tokens = rr.split(":");
                if(tokens.length==3){
                    //displayMessage(rr);
                    try{
                        //displayMessage(tokens[1]);
                        //int tempT = temp;
                        if(tokens[1].contains("05") && tokens[1].contains("0C")){
                            temp = showEngineCoolantTemperature(tokens[1].substring(4,6));
                            rpm = showEngineRPM(tokens[1].substring(8, 10),tokens[1].substring(10,12));

                            mSbCmdResp.append( "Coolant Temp: "+ temp
                                    +"\n"
                                    +"RPM: "+ rpm
                                    +"\n");
                        }
                    }
                    catch (Exception e) {}
                    try{
                        //displayMessage(tokens[2]);
                        //int tempT = temp;
                        if(tokens[1].contains("05") && tokens[1].contains("0C")){
                            speed = showVehicleSpeed(tokens[2].substring(2,4));
                            mSbCmdResp.append( "Vehicle Speed: "+ speed
                                    +"\n\n");
                        }
                    }
                    catch (Exception e) {}
                }


        }

        
        mMonitor.setText(mSbCmdResp.toString());
        
        if (mCMDPointer >= 0)
        {
            mCMDPointer++;
            sendDefaultCommands();
        }
    }
    
    private String cleanResponse(String text)
    {
        text = text.trim();
        text = text.replace("\t", "");
        text = text.replace(" ", "");
        text = text.replace(">", "");
        text = text.replace("OK", "");
        text = text.replace("K", "");
        text = text.trim();

        return text;
    }
    
    private int showEngineCoolantTemperature(String buffer)
    {
        String buf = buffer;

        int A = Integer.valueOf(buf, 16);
        A-= 40;
        return A;

        /*buf = cleanResponse(buf);
        
        if (buf.contains("2105"))
        {
            try
            {
                buf = buf.substring(buf.indexOf("2105"));

                String temp = buf.substring(4, 6);
                int A = Integer.valueOf(temp, 16);
                A -= 40;

                return A;
            }
            catch (IndexOutOfBoundsException | NumberFormatException e)
            {
                MyLog.e(TAG, e.getMessage());
            }
        }*/
    }
    
    private int showEngineRPM(String A, String B)
    {
        try
        {

            int aA = Integer.valueOf(A, 16);
            int bB = Integer.valueOf(B, 16);

            return  ((aA * 256) + bB) / 4;
        }
        catch (IndexOutOfBoundsException | NumberFormatException e)
        {
            MyLog.e(TAG, e.getMessage());
        }
        /*String buf = buffer;
        buf = cleanResponse(buf);
        
        if (buf.contains("210C"))
        {
            try
            {
                buf = buf.substring(buf.indexOf("210C"));
                
                String MSB = buf.substring(4, 6);
                String LSB = buf.substring(6, 8);
                int A = Integer.valueOf(MSB, 16);
                int B = Integer.valueOf(LSB, 16);
                
                return  ((A * 256) + B) / 4;
            }
            catch (IndexOutOfBoundsException | NumberFormatException e)
            {
                MyLog.e(TAG, e.getMessage());
            }
        } */
        
        return -1;
    }
    
    private int showVehicleSpeed(String buffer)
    {

        String buf = buffer;
        return Integer.valueOf(buf, 16);


        /*String buf = buffer;
        buf = cleanResponse(buf);
        
        if (buf.contains("210D"))
        {
            try
            {
                buf = buf.substring(buf.indexOf("210D"));

                String temp = buf.substring(4, 6);

                return Integer.valueOf(temp, 16);
            }
            catch (IndexOutOfBoundsException | NumberFormatException e)
            {
                MyLog.e(TAG, e.getMessage());
            }
        }*/

    }
    
    private int showDistanceTraveled(String buffer)
    {
        String buf = buffer;
        buf = cleanResponse(buf);
        
        if (buf.contains("2131"))
        {
            try
            {
                buf = buf.substring(buf.indexOf("2131"));

                String MSB = buf.substring(4, 6);
                String LSB = buf.substring(6, 8);
                int A = Integer.valueOf(MSB, 16);
                int B = Integer.valueOf(LSB, 16);

                return (A * 256) + B;
            }
            catch (IndexOutOfBoundsException | NumberFormatException e)
            {
                MyLog.e(TAG, e.getMessage());
            }
        }

        return -1;
    }
}
