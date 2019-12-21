package com.example.s162132.parentapp2;

import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;

import io.skyway.Peer.DataConnection;
import io.skyway.Peer.OnCallback;

public class MapsActivity extends AppCompatActivity implements MapsFragment.OnFragmentInteractionListener{

    private MapsFragment mapsFragment = new MapsFragment();
    private DataConnection dataConnection;
    private final String RECEIVED_FLG = "Tell me the location";
    private Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        //マップのフラグメントを呼び出す
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.add(R.id.container, mapsFragment);
        transaction.commit();

        AppData appData = (AppData) getApplication();
        this.dataConnection = appData.getDataConnection();
        System.out.println("MapsActivity:dataConnection = " + dataConnection);

        if (dataConnection != null) {
            setDataConnectCallback();
        } else {
            Toast.makeText(MapsActivity.this, "接続に失敗しました。", Toast.LENGTH_SHORT).show();
            finish();
        }

        findViewById(R.id.Back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        findViewById(R.id.Reload).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sentFlg();
            }
        });
    }

    public void sentFlg() {
        System.out.println("sentFlg:dataConnection=" + dataConnection);
        Boolean result = dataConnection.send(RECEIVED_FLG);
        if (result) {
            //Toast.makeText(MapsActivity.this, "送信成功", Toast.LENGTH_SHORT).show();
            System.out.println("現在地確認フラグの送信成功");
        } else {
            //Toast.makeText(MapsActivity.this, "送信失敗", Toast.LENGTH_SHORT).show();
            System.out.println("現在地確認フラグの送信失敗");
        }
    }

    public void setDataConnectCallback() {
        dataConnection.on(DataConnection.DataEventEnum.DATA, new OnCallback() {
            @Override
            public void onCallback(Object o) {
                final Object object = o;
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (object instanceof String) {
                            String str = object.toString();
                            String location[] = str.split(",");
                            double latitude = Double.valueOf(location[0]);
                            double longitude = Double.valueOf(location[1]);
                            mapsFragment.setLocation(latitude, longitude);
                        }
                    }
                });
            }
        });


    }

    @Override
    public void indicatedMap() {
        sentFlg();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onPause() {
        super.onPause();
        System.out.println("called:MapsActivity:onStop");
        dataConnection.on(DataConnection.DataEventEnum.DATA, null);
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}
