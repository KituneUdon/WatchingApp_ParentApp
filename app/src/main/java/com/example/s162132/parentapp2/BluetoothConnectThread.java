package com.example.s162132.parentapp2;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.UUID;

import io.skyway.Peer.DataConnection;
import io.skyway.Peer.Peer;

/**
 * Created by s162132 on 2017/06/29.
 */

public class BluetoothConnectThread extends Thread {
    private DataConnection dataConnection;
    private final UUID MY_UUID =
            UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private BluetoothSocket mBluetoothSocket;
    private BluetoothAdapter mBluetoothAdapter;
    private String childId;
    private Peer peer;
    private AppData appData;
    private String instruction;

    protected BluetoothConnectThread(String address, BluetoothAdapter bluetoothAdapter,
                                     Peer peer, AppData appData, String instruction) {
        mBluetoothAdapter = bluetoothAdapter;
        this.peer = peer;
        this.appData = appData;
        this.instruction = instruction;
        BluetoothDevice mBluetoothDevice = mBluetoothAdapter.getRemoteDevice(address);
        //共有プリファレンスに子どもの端末のMACアドレスを保存する
        appData.setChildAddressPref(address);
        //グローバル変数を定義しているクラスに子どもの端末のMACアドレスを保存する
        appData.setChildAddress(address);
        BluetoothSocket tmp = null;
        try {
            tmp = mBluetoothDevice.createInsecureRfcommSocketToServiceRecord(MY_UUID);
        } catch (IOException e) {
            e.printStackTrace();
        }
        mBluetoothSocket = tmp;
        System.out.println("BluetoothConnectThread:mBluetoothSocket = " + mBluetoothSocket);
    }

    @Override
    public void run() {
        System.out.println("mBluetoothSocket=" + mBluetoothSocket);
        mBluetoothAdapter.cancelDiscovery();
        try{
            mBluetoothSocket.connect();
            if (instruction.equals(appData.getEXCHANGE_OF_ID())) {
                //skyWayのIDを交換する処理
                getChildId();
            } else if (instruction.equals(appData.getRECEIVED_BLUETOOTH_CONNECT())) {
                //bluetoothの切断を検出する準備
                //mBluetoothSocket.connectで通信はできているので特に何も実行しない。
                appData.setBluetoothSocket(mBluetoothSocket);
                System.out.println("appData;bluetoothSocket = " + appData.getBluetoothSocket());
            }
        } catch (IOException e) {
            try {
                e.printStackTrace();
                mBluetoothSocket.close();
            } catch (IOException e1) {
                e.printStackTrace();
                return;
            }
        }
    }
    public void getChildId() {
        InputStream tmpIn;
        try {
            tmpIn = mBluetoothSocket.getInputStream();
            ObjectOutputStream out = new ObjectOutputStream(mBluetoothSocket.getOutputStream());
            out.flush();
            ObjectInputStream in = new ObjectInputStream(mBluetoothSocket.getInputStream());

            try {
                childId = (String) in.readObject();

                appData.setChildIdPref(childId);
                System.out.println("getChildId:childId=" + childId);

                //skyWayの接続を開始する
                SkyWayIdSetting skyWayIdSetting = new SkyWayIdSetting();
                peer = appData.getPeer();
                System.out.println("BluetoothConnectThread:childId=" + childId + "\npeer=" + peer);
                dataConnection = skyWayIdSetting.connectStart(childId, peer);
                System.out.println("BluetoothConnectThread:dataConnection = " + dataConnection);
                appData.setDataConnection(dataConnection);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }

            tmpIn.close();
            mBluetoothSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
