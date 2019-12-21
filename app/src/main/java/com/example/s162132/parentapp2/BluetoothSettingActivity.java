package com.example.s162132.parentapp2;

import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

public class BluetoothSettingActivity extends AppCompatActivity {

    FindBluetoothFragment findBluetoothFragment = new FindBluetoothFragment();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_setting);

        //レイアウトのcontainerにFindBluetoothFragmentを設置する
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.add(R.id.container, findBluetoothFragment);
        transaction.commit();

        findViewById(R.id.Next).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
    }
}
