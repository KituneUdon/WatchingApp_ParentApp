package com.example.s162132.parentapp2;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;

import io.skyway.Peer.Browser.Canvas;
import io.skyway.Peer.Browser.MediaStream;
import io.skyway.Peer.CallOption;
import io.skyway.Peer.DataConnection;
import io.skyway.Peer.MediaConnection;
import io.skyway.Peer.OnCallback;
import io.skyway.Peer.Peer;

public class VideoActivity extends AppCompatActivity {

    private AppData appData;
    private Peer peer;
    private DataConnection dataConnection;
    private String childId;
    private MediaStream mediaStream = null;
    private MediaStream mediaStreamRemote;
    private MediaConnection mediaConnection;
    private Canvas canvas;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video);

        SharedPreferences pref = getSharedPreferences("SkyWayId", MODE_PRIVATE);
        canvas = (Canvas) findViewById(R.id.svPrimary);

        //必要なものをグローバル変数を定義したクラスから持ってくる
        appData = (AppData) getApplication();
        peer = appData.getPeer();
        dataConnection = appData.getDataConnection();
        childId = pref.getString("childId", null);

        if (childId == null) {
            Toast.makeText(VideoActivity.this, "子機の情報が登録されてません。", Toast.LENGTH_SHORT).show();
            finish();
        }

        findViewById(R.id.button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mediaConnection.close();
                unsetMediaCallback();
                mediaConnection = null;
                //mediaStream = null;
                finish();
            }
        });
    }

    public void calling() {
        CallOption option = new CallOption();
        mediaConnection = peer.call(childId, mediaStream, option);
        System.out.println("calling:mediaConnection = " + mediaConnection);
        if (mediaConnection != null) {
            setMediaCallback();
        }
    }

    public void setMediaCallback() {
        mediaConnection.on(MediaConnection.MediaEventEnum.STREAM, new OnCallback() {
            @Override
            public void onCallback(Object o) {
                //相手から映像が送られてきた
                if (o instanceof MediaStream) {
                    System.out.println("called:MediaEventEnum.STREAM");
                    mediaStreamRemote = (MediaStream) o;
                    System.out.println("mediaStreamRemote = " + mediaStreamRemote);
                    canvas.addSrc(mediaStreamRemote, 0);
                }
            }
        });

        mediaConnection.on(MediaConnection.MediaEventEnum.CLOSE, new OnCallback() {
            @Override
            public void onCallback(Object o) {
                System.out.println("called:MediaEventEnum.CLOSE");
                //通話が切れた
                canvas.removeSrc(mediaStreamRemote ,0);
            }
        });

        mediaConnection.on(MediaConnection.MediaEventEnum.ERROR, new OnCallback() {
            @Override
            public void onCallback(Object o) {
                //エラー処理
                Toast.makeText(VideoActivity.this, "エラーが発生しました。", Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void unsetMediaCallback() {
        mediaConnection.on(MediaConnection.MediaEventEnum.STREAM, null);
        mediaConnection.on(MediaConnection.MediaEventEnum.CLOSE, null);
        mediaConnection.on(MediaConnection.MediaEventEnum.ERROR, null);
    }

    @Override
    protected void onStart() {
        super.onStart();

        //相手との通話を開始する
        calling();
    }

    @Override
    protected void onStop() {
        super.onStop();
        System.out.println("called:onStop");
        canvas.removeSrc(mediaStreamRemote ,0);

        System.out.println("onStop:mediaConnection = " + mediaConnection);
        if (mediaConnection != null) {
            unsetMediaCallback();
            mediaConnection.close();
        }
        mediaConnection = null;
        mediaStreamRemote = null;
        mediaStream = null;
    }

    @Override
    protected void onPause() {
        super.onPause();

    }
}
