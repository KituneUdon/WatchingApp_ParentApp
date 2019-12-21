package com.example.s162132.parentapp2;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.io.IOException;

import io.skyway.Peer.DataConnection;
import io.skyway.Peer.OnCallback;
import io.skyway.Peer.Peer;

public class ParentMainActivity extends AppCompatActivity
        implements CompoundButton.OnCheckedChangeListener, SkyWayIdSetting.successfulGetPeerIdListener,
                    SkyWayIdSetting.failureConnectionPeerListener{

    private String childId;
    private Peer peer;
    private boolean sentBluetoothCheckFlg =false;
    private SharedPreferences pref;
    private SkyWayIdSetting skyWayIdSetting = new SkyWayIdSetting();
    private AppData appData;
    private BluetoothSocket bluetoothSocket;
    private ToggleButton s;
    private Vibrator vibrator;
    private DataConnection dataConnection = null;
    private TextView textView;
    private final String HOME_CONFIRMATION = "home confirmation";
    private boolean homeConfirmationFlg = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parent_main);

        //https://firespeed.org/diary.php?diary=kenz-1821
        //パーミッションの許可を求める
        if (ContextCompat.checkSelfPermission(
                this, android.Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
            // 許可されている時の処理
        } else {
            //許可されていない時の処理
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.CALL_PHONE)) {
                //拒否された時 Permissionが必要な理由を表示して再度許可を求めたり、機能を無効にしたりします。
                findViewById(R.id.btnCall).setEnabled(false);
                findViewById(R.id.btnCallSetting).setEnabled(false);
                Toast.makeText(ParentMainActivity.this, "電話の許可を出さない場合、電話の機能は使えません。", Toast.LENGTH_SHORT).show();
            } else {
                //まだ許可を求める前の時、許可を求めるダイアログを表示します。
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.CALL_PHONE}, 0);
            }
        }

        skyWayIdSetting.setMyGetPeerIdEventListener(this);
        skyWayIdSetting.setFailureConnectionPeerListener(this);

        appData = (AppData) getApplication();
        pref = getSharedPreferences("SkyWayId", Context.MODE_PRIVATE);

        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        textView = (TextView) findViewById(R.id.ChildNow);

        System.out.println("ParentMainActivity:dataConnection=" + appData.getDataConnection());
        childId = pref.getString("childId", null);

        if (appData.getDataConnection() == null) {
            //相手と通信していない場合
            //PeerIdの取得
            Context context = getApplicationContext();
            peer = skyWayIdSetting.getPeerId(context, appData);
        } else {
            peer = appData.getPeer();
            dataConnection = appData.getDataConnection();
        }
        setPeerCallback();

        s = (ToggleButton) findViewById(R.id.LostPrev);
        s.setOnCheckedChangeListener(this);

        findViewById(R.id.Setting).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(ParentMainActivity.this, BluetoothSettingActivity.class);
                startActivity(intent);
            }
        });

        findViewById(R.id.Gps_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(ParentMainActivity.this, MapsActivity.class);
                startActivity(intent);
            }
        });

        findViewById(R.id.Cam_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(ParentMainActivity.this, VideoActivity.class);
                startActivity(intent);
            }
        });

        findViewById(R.id.btnCall).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                callStart();
            }
        });

        findViewById(R.id.btnCallSetting).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setContactInformation();
            }
        });

        //ブロードキャストレシーバーを実行するための準備
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        this.registerReceiver(mReceiver, filter);
    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
        if (compoundButton == findViewById(R.id.LostPrev)) {
            if (b == true) {
                sentBluetoothCheckFlg = true;
                BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                String address = pref.getString("ChildAddress", null);
                System.out.println("address = " + address);

                if (address == null) {
                    //共有プリファレンスに子どもの端末のMACアドレスが保存されていない
                    sentBluetoothCheckFlg = false;
                    return;
                }

                String str = appData.getRECEIVED_BLUETOOTH_CONNECT();
                boolean bo = dataConnection.send(str);
                if (!bo) {
                    s.setChecked(false);
                    return;
                }
                //skyWayIdSetting.connectStart(appData.getDETECTION_OF_DISCONNECT(), peer);　たぶんいらない
                //bluetoothでの接続を開始する。
                BluetoothConnectThread bluetoothConnectThread = new BluetoothConnectThread(address, mBluetoothAdapter, peer, appData, str);
                bluetoothConnectThread.start();
                try {
                    bluetoothConnectThread.join();
                } catch (InterruptedException e) {
                    //下二つは必要なのかわからないが念のために記述
                    sentBluetoothCheckFlg =false;
                    s.setChecked(false);
                    e.printStackTrace();
                    finish();
                }
                bluetoothSocket = appData.getBluetoothSocket();
                System.out.println("ParentMainActivity:bluetoothSocket = " + bluetoothSocket);
                if (bluetoothSocket == null) {
                    Toast.makeText(ParentMainActivity.this, "接続に失敗しました。", Toast.LENGTH_SHORT).show();
                    s.setChecked(false);
                    sentBluetoothCheckFlg = false;
                }
            } else {
                sentBluetoothCheckFlg = false;
            }
        }
    }

    //The BroadcastReceiver that listens for bluetooth broadcasts
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

            if (sentBluetoothCheckFlg == true) {
                if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                    //Device found
                }
                else if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
                    //Device is now connected
                }
                else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                    //Done searching
                }
                else if (BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED.equals(action)) {
                    //Device is about to disconnect
                }
                else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
                    //Device has disconnected
                    Toast.makeText(ParentMainActivity.this, "切断されました", Toast.LENGTH_SHORT).show();
                    s.setChecked(false);
                    vibrator.vibrate(1000);
                    sentBluetoothCheckFlg = false;
                    System.out.println("BroadcastReceiverによって切断が検出されました。");
                    try {
                        bluetoothSocket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

        }
    };

    @SuppressWarnings("MissingPermission")
    public void callStart() {
        String s = pref.getString("PhoneNumber" , null);

        if (s != null) {
            Uri uri = Uri.parse("tel:" + s);
            Intent i = new Intent(Intent.ACTION_CALL, uri);
            startActivity(i);
        } else {
            Toast.makeText(ParentMainActivity.this, "電話番号が保存されてません。", Toast.LENGTH_SHORT).show();
        }
    }

    public void setContactInformation() {
        LayoutInflater inflater = (LayoutInflater) this.getSystemService(LAYOUT_INFLATER_SERVICE);
        final View layout = inflater.inflate(R.layout.dialog_content,
                (ViewGroup) findViewById(R.id.layout_root));

        //アロートダイアログを生成
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("連絡先");
        builder.setView(layout);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int which) {
                //OKボタンのクリック処理
                EditText text = (EditText) layout.findViewById(R.id.edPhoneNumber);

                //共有プリファレンス処理
                String s = text.getText().toString();
                SharedPreferences.Editor e = pref.edit();
                e.putString("PhoneNumber", s);
                e.commit();
            }

        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int which) {
                //Cancelのボタン処理
            }
        });
        //表示
        builder.create().show();
    }

    @Override
    public void successGetPeerID() {
        System.out.println("called:successGetPeerID");
        childId = pref.getString("childId", null);

        System.out.println("successGetPeerId:childId = " + childId);
        if (childId != null) {
            System.out.println("childId=" + childId + "\npeer=" + peer);
            dataConnection = skyWayIdSetting.connectStart(childId, peer);
            appData.setDataConnection(dataConnection);
            setDataConnectionCallback();
            System.out.println("successGetPeerID:dataConnection = " + dataConnection);
        } else {
            Toast.makeText(ParentMainActivity.this, "ChildIdが保存されていません。\n" +
                    "bluetooth設定画面で子機と接続してください。", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void failureConnectionPeer() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(ParentMainActivity.this, "子機との接続に失敗した可能性があります。\n" +
                        "子機と親機のアプリを再起動してください。", Toast.LENGTH_LONG).show();
            }
        });
    }

    public void setPeerCallback() {
        peer.on(Peer.PeerEventEnum.CONNECTION, new OnCallback() {
            @Override
            public void onCallback(Object o) {
                System.out.println("called:PeerEventEnum.CONNECTION");
                dataConnection = (DataConnection) o;
                appData.setDataConnection(dataConnection);
            }
        });

        peer.on(Peer.PeerEventEnum.CLOSE, new OnCallback() {
            @Override
            public void onCallback(Object o) {
                System.out.println("called:ParentMainActivity:EventEnum.CLOSE");
            }
        });

        peer.on(Peer.PeerEventEnum.DISCONNECTED, new OnCallback() {
            @Override
            public void onCallback(Object o) {
                System.out.println("called:PeerEventEnum.DISCONNECTED");
            }
        });
    }

    public void setDataConnectionCallback() {
        System.out.println("called:setDataConnectionCallback()");
        dataConnection.on(DataConnection.DataEventEnum.OPEN, new OnCallback() {
            @Override
            public void onCallback(Object o) {
                homeConfirmation();
            }
        });

        dataConnection.on(DataConnection.DataEventEnum.DATA, new OnCallback() {
            @Override
            public void onCallback(Object o) {
                if (o instanceof String) {
                    System.out.println("DataEventEnum.DATA:Object = " + o);
                    if (o.equals(appData.getCONNECT_WITH_WiFi())) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                textView.setText("在宅中");
                            }
                        });
                    } else if (o.equals(appData.getDISCONNECT_FROM_WiFi())) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                textView.setText("外出中");
                            }
                        });
                    }
                }
            }
        });

        dataConnection.on(DataConnection.DataEventEnum.ERROR, new OnCallback() {
            @Override
            public void onCallback(Object o) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(ParentMainActivity.this, "エラーが発生しました。\nアプリを再起動してください。", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                });
            }
        });

        dataConnection.on(DataConnection.DataEventEnum.CLOSE, new OnCallback() {
            @Override
            public void onCallback(Object o) {
                System.out.println("called:ParentMainActivity:DataEventEnum.CLOSE");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        textView.setText(getString(R.string.childOff));
                    }
                });
            }
        });
    }

    public void unsetDataConnectionCallback() {
        dataConnection.on(DataConnection.DataEventEnum.ERROR, null);
        dataConnection.on(DataConnection.DataEventEnum.DATA, null);
        dataConnection.on(DataConnection.DataEventEnum.CLOSE, null);
    }

    public void homeConfirmation() {
        if (dataConnection != null) {
            if (!homeConfirmationFlg) {
                System.out.println(dataConnection);
                boolean b = dataConnection.send(HOME_CONFIRMATION);
                if (b) {
                    System.out.println("在宅確認のフラグの送信完了");
                } else {
                    System.out.println("在宅確認のフラグの送信失敗");
                }
                homeConfirmationFlg = true;
            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        homeConfirmation();
    }

    @Override
    protected void onResume() {
        super.onResume();
        System.out.println("called:ParentMainActivity:onResume");
        System.out.println("ParentMainActivity:onResume:dataConnection = " + dataConnection);
        if (dataConnection != null) {
            setDataConnectionCallback();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        System.out.println("called:ParentMainActivity:onStop");
        System.out.println("onStop:dataConnection = " + dataConnection);
        if (dataConnection != null) {
            unsetDataConnectionCallback();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
        if (dataConnection != null) {
            dataConnection.close();
        }
        if (peer != null) {
            peer.disconnect();
            peer.destroy();
        }
    }
}