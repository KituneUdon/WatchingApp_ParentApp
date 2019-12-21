package com.example.s162132.parentapp2;

import android.content.Context;
import android.content.SharedPreferences;

import io.skyway.Peer.ConnectOption;
import io.skyway.Peer.DataConnection;
import io.skyway.Peer.OnCallback;
import io.skyway.Peer.Peer;
import io.skyway.Peer.PeerOption;

/**
 * Created by s162132 on 2017/07/02.
 */

public class SkyWayIdSetting {

    public String id;
    public Peer peer;
    public SharedPreferences pref;
    public Context con;
    public AppData appData;
    public successfulGetPeerIdListener successfulListener;
    public SkyWayIdSetting.failureConnectionPeerListener failureConnectionPeerListener;
    public DataConnection dataConnection;

    public Peer getPeerId(Context context, AppData appData) {
        con = context;
        this.appData = appData;
        pref = con.getSharedPreferences("SkyWayId", Context.MODE_PRIVATE);
        String str = pref.getString("myId", null);

        System.out.println("called:SkyWayId");

        PeerOption option = new PeerOption();
        option.key = "26fb276e-33c6-45fb-8ff6-aedd8a50af5f";
        option.domain = "localhost";

        if (str == null) {
            System.out.println("called:str == null");
            peer = new Peer(con, option);
        } else {
            System.out.println("called:str != null");
            peer = new Peer(con, str, option);
        }

        appData.setPeer(peer);
        setPeerCallBack(peer);

        return peer;
    }

    public DataConnection connectStart(String childId, Peer peer) {
        System.out.println("called:connectStart");

        //skyWayのoptionを設定する
        ConnectOption option = new ConnectOption();
        option.metadata = "data connection";
        option.label = "chat";
        option.serialization = DataConnection.SerializationEnum.BINARY;

        System.out.println("connectStart:childId=" + childId);
        //子どもと接続する
        dataConnection = peer.connect(childId, option);
        System.out.println("connectStart:dataConnection=" + dataConnection);

        return dataConnection;
    }

    public void setPeerCallBack(Peer peer) {
        System.out.println("called:setPeerCallBack");
        peer.on(Peer.PeerEventEnum.OPEN, new OnCallback(){
                    public void onCallback(Object object){
                        System.out.println("called:PeerEventEnum.OPEN");
                        System.out.println("object = " + object);
                        if (object instanceof String){
                            id = (String) object;
                            System.out.println("getPeerId:myId=" + id);
                            SharedPreferences.Editor editor = pref.edit();
                            editor.putString("myId", id);
                            editor.commit();
                            successfulListener.successGetPeerID();
                        }
                    }
                }
        );

        peer.on(Peer.PeerEventEnum.ERROR, new OnCallback() {
            @Override
            public void onCallback(Object o) {
                System.out.println("PeerEventEnum.ERROR = peerに重大なエラーが発生しました");
                //failureConnectionPeerListener.failureConnectionPeer();
            }
        });
    }

    public interface successfulGetPeerIdListener {
        void successGetPeerID();
    }

    public interface failureConnectionPeerListener {
        void failureConnectionPeer();
    }

    public void setFailureConnectionPeerListener(SkyWayIdSetting.failureConnectionPeerListener listener) {
        this.failureConnectionPeerListener = listener;
    }

    public void setMyGetPeerIdEventListener(SkyWayIdSetting.successfulGetPeerIdListener listener) {
        this.successfulListener = listener;
    }
}
