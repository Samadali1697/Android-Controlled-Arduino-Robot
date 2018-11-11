package com.example.philipgo.arduinobluetoothrccar;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    boolean connected; //variable to check bluetooth connection

    Button forward_btn, forward_left_btn, forward_right_btn, reverse_btn, reverse_left_btn, reverse_right_btn,
            gearPlus, gearMinus;
    TextView gearTextView;
    int gear=0;
    String command; //string variable that will store value to be transmitted to the bluetooth module

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        connected = false;
        //declaration of button variables
        forward_btn = findViewById(R.id.forward_btn);
        forward_left_btn = findViewById(R.id.forward_left_btn);
        forward_right_btn = findViewById(R.id.forward_right_btn);
        reverse_btn =  findViewById(R.id.reverse_btn);
        reverse_right_btn = findViewById(R.id.reverse_right_btn);
        reverse_left_btn = findViewById(R.id.reverse_left_btn);
        gearPlus = findViewById(R.id.btnPlus);
        gearMinus = findViewById(R.id.btnMinus);
        gearTextView = findViewById(R.id.tvGear);




        //OnTouchListener code for the gear + (button long press)
        gearPlus.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) //MotionEvent.ACTION_DOWN is when you hold a button down
                {
                    if(gear<5) {
                        command = "+";
                        command = "+";
                        BTMainActivity.sendOBD2CMD(command);
                        gear++;
                        gearTextView.setText(gear+"");
                    }
                }
                else if(event.getAction() == MotionEvent.ACTION_UP) //MotionEvent.ACTION_UP is when you release a button
                {
                    command = ".";
                    BTMainActivity.sendOBD2CMD(command);
                }
                return false;
            }
        });

        //OnTouchListener code for the gear - (button long press)
        gearMinus.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) //MotionEvent.ACTION_DOWN is when you hold a button down
                {
                    if(gear>0) {
                        command = "-";
                        BTMainActivity.sendOBD2CMD(command);
                        gear--;
                        gearTextView.setText(gear+"");
                    }
                }
                else if(event.getAction() == MotionEvent.ACTION_UP) //MotionEvent.ACTION_UP is when you release a button
                {
                    command = ".";
                    BTMainActivity.sendOBD2CMD(command);
                }
                return false;
            }
        });


        //OnTouchListener code for the Forward (button long press)
        forward_btn.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                if (event.getAction() == MotionEvent.ACTION_DOWN) //MotionEvent.ACTION_DOWN is when you hold a button down
                {
                    command = "F";
                    BTMainActivity.sendOBD2CMD(command);
                }
                else if(event.getAction() == MotionEvent.ACTION_UP) //MotionEvent.ACTION_UP is when you release a button
                {
                    command = ".";
                    BTMainActivity.sendOBD2CMD(command);
                    /*try
                    {
                        outputStream.write(command.getBytes());
                    }
                    catch(IOException e)
                    {
                        e.printStackTrace();
                    }*/

                }

                return false;
            }

        });

        //OnTouchListener code for the Reverse (button long press)
        reverse_btn.setOnTouchListener(new View.OnTouchListener(){
            @Override
            public boolean onTouch(View v, MotionEvent event)
            {
                if(event.getAction() == MotionEvent.ACTION_DOWN)
                {
                    command = "R";

                    BTMainActivity.sendOBD2CMD(command);
                }
                else if(event.getAction() == MotionEvent.ACTION_UP)
                {
                    command = ".";
                    BTMainActivity.sendOBD2CMD(command);

                }
                return false;
            }
        });

        //OnTouchListener code for the forward_left button (button long press)
        forward_left_btn.setOnTouchListener(new View.OnTouchListener(){
            @Override
            public boolean onTouch(View v, MotionEvent event)
            {
                if(event.getAction() == MotionEvent.ACTION_DOWN)
                {
                    command = "Q";
                    BTMainActivity.sendOBD2CMD(command);
                }
                else if(event.getAction() == MotionEvent.ACTION_UP)
                {
                    command = ".";
                    BTMainActivity.sendOBD2CMD(command);

                }
                return false;
            }
        });

        //OnTouchListener code for the forward_right button (button long press)
        forward_right_btn.setOnTouchListener(new View.OnTouchListener(){
           @Override
            public boolean onTouch(View v, MotionEvent event)
           {
                if(event.getAction() == MotionEvent.ACTION_DOWN)
                {
                    command = "W";

                    BTMainActivity.sendOBD2CMD(command);
                }
                else if(event.getAction() == MotionEvent.ACTION_UP)
                {
                    command = ".";
                    BTMainActivity.sendOBD2CMD(command);

                }
                return false;
           }
        });

        //OnTouchListener code for the reverse_left button (button long press)
        reverse_left_btn.setOnTouchListener(new View.OnTouchListener(){
            @Override
            public boolean onTouch(View v, MotionEvent event)
            {
                if(event.getAction() == MotionEvent.ACTION_DOWN)
                {
                    command = "E";
                    BTMainActivity.sendOBD2CMD(command);
                }
                else if(event.getAction() == MotionEvent.ACTION_UP)
                {
                    command = ".";
                    BTMainActivity.sendOBD2CMD(command);

                }
                return false;
            }
        });

        //OnTouchListener code for the reverse_right button (button long press)
        reverse_right_btn.setOnTouchListener(new View.OnTouchListener(){
            @Override
            public boolean onTouch(View v, MotionEvent event)
            {
                if(event.getAction() == MotionEvent.ACTION_DOWN)
                {
                    command = "S";
                    BTMainActivity.sendOBD2CMD(command);
                }
                else if(event.getAction() == MotionEvent.ACTION_UP)
                {
                    command = ".";
                    BTMainActivity.sendOBD2CMD(command);

                }
                return false;
            }
        });


    }


    @Override
    protected void onStart() {
        super.onStart();
    }

}
