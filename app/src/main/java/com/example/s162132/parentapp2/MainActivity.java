package com.example.s162132.parentapp2;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.AppLaunchChecker;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.beardedhen.androidbootstrap.BootstrapButton;

import io.skyway.Peer.Peer;

public class MainActivity extends AppCompatActivity {

    FindBluetoothFragment findBluetoothFragment = new FindBluetoothFragment();
    BootstrapButton btnNext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnNext = (BootstrapButton) findViewById(R.id.Next);

        if(!AppLaunchChecker.hasStartedFromLauncher(this)){
            Intent intent = new Intent(MainActivity.this, ParentMainActivity.class);
            startActivity(intent);
        } else {
            Context context = getApplicationContext();
            AppData appData = (AppData) getApplication();
            //SkyWayの準備
            SkyWayIdSetting getSkyWayId = new SkyWayIdSetting();
            Peer peer = getSkyWayId.getPeerId(context, appData);
            appData.setPeer(peer);

            //レイアウトのcontainerにFindBluetoothFragmentを設置する
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.add(R.id.container, findBluetoothFragment);
            transaction.commit();
        }

        btnNext.setEnabled(false);

        findViewById(R.id.Next).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, ParentMainActivity.class);
                startActivity(intent);
            }
        });
    }
}
