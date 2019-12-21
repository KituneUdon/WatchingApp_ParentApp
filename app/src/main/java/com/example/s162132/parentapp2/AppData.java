package com.example.s162132.parentapp2;

import android.app.Application;
import android.bluetooth.BluetoothSocket;
import android.content.SharedPreferences;

import com.beardedhen.androidbootstrap.TypefaceProvider;

import io.skyway.Peer.DataConnection;
import io.skyway.Peer.Peer;

/**
 * Created by s162132 on 2017/06/30.
 */

public class AppData extends Application {
    //グローバル変数
    private DataConnection dataConnection;
    private String childAddress;
    private Peer peer;
    private BluetoothSocket bluetoothSocket;
    private final String EXCHANGE_OF_ID = "Exchange of ID";
    private final String RECEIVED_BLUETOOTH_CONNECT = "Connect with Bluetooth";
    private final String RECEIVED_BLUETOOTH_DISCONNECT = "Disconnect with Bluetooth";
    private final String CONNECT_WITH_WiFi = "connect with wifi";
    private final String DISCONNECT_FROM_WiFi = "disconnect from wifi";

    @Override public void onCreate() {
        super.onCreate();
        TypefaceProvider.registerDefaultIconSets();
    }

    //getter & setter
    public DataConnection getDataConnection() {
        return dataConnection;
    }

    public void setDataConnection(DataConnection dataConnection) {
        this.dataConnection = dataConnection;
    }

    public String getRECEIVED_BLUETOOTH_CONNECT() {
        return RECEIVED_BLUETOOTH_CONNECT;
    }

    public String getRECEIVED_BLUETOOTH_DISCONNECT() {
        return RECEIVED_BLUETOOTH_DISCONNECT;
    }

    public String getChildAddress() {
        return childAddress;
    }

    public void setChildAddress(String childAddress) {
        this.childAddress = childAddress;
    }

    public String getEXCHANGE_OF_ID() {
        return EXCHANGE_OF_ID;
    }


    public Peer getPeer() {
        return peer;
    }

    public void setPeer(Peer peer) {
        this.peer = peer;
    }

    public BluetoothSocket getBluetoothSocket() {
        return bluetoothSocket;
    }

    public void setBluetoothSocket(BluetoothSocket bluetoothSocket) {
        this.bluetoothSocket = bluetoothSocket;
    }

    public String getCONNECT_WITH_WiFi() {
        return CONNECT_WITH_WiFi;
    }

    public String getDISCONNECT_FROM_WiFi() {
        return DISCONNECT_FROM_WiFi;
    }

    //ここから下は共有プリファレンスに値を保存したいが
    //共有プリファレンスが使えないクラスのためのメソッド
    public void setChildIdPref(String childId) {
        SharedPreferences pref = getSharedPreferences("SkyWayId", MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        System.out.println("setChildIdPref:childId = " + childId);
        editor.putString("childId", childId);
        editor.commit();
        System.out.println("called:setChildIdPref");
    }

    public void setChildAddressPref(String str) {
        SharedPreferences pref = getSharedPreferences("SkyWayId", MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        editor.putString("ChildAddress", str);
        editor.commit();
        System.out.println("called:setChildAddressPref");
    }
}
