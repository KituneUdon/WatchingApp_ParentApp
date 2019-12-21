package com.example.s162132.parentapp2;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.beardedhen.androidbootstrap.BootstrapButton;

import java.util.Set;

import io.skyway.Peer.DataConnection;
import io.skyway.Peer.OnCallback;
import io.skyway.Peer.Peer;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link FindBluetoothFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link FindBluetoothFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class FindBluetoothFragment extends Fragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private OnFragmentInteractionListener mListener;

    private ListView listView;
    private Set<BluetoothDevice> pairedDevices;
    private BluetoothAdapter mBluetoothAdapter;
    final private int REQUEST_ENABLE_BT = 1;
    private DataConnection dataConnection;
    private Peer peer;

    public FindBluetoothFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment FindBluetoothFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static FindBluetoothFragment newInstance(String param1, String param2) {
        FindBluetoothFragment fragment = new FindBluetoothFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
        bluetoothCheck();

        if (dataConnection != null) {
            //既に子機と接続している場合は、子機との接続を終了する。
            dataConnection.close();
            dataConnection = null;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_find_bluetooth, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        //Bluetoothデバイスの一覧の取得
        pairedDevices = mBluetoothAdapter.getBondedDevices();

        listView = (ListView)getView().findViewById(R.id.listView);
        //listViewにBluetoothデバイスの一覧を表示する
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                getActivity(),
                android.R.layout.simple_list_item_1);
        for (BluetoothDevice device : pairedDevices) {
            adapter.add(device.getName());
        }
        listView.setAdapter(adapter);
    }

    @Override
    public void onStart() {
        super.onStart();

        //listViewをタップしたときの処理
        final Object[] device = pairedDevices.toArray();
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                BluetoothDevice mBluetoothDevice = (BluetoothDevice) device[i];
                String address = mBluetoothDevice.getAddress().toString();
                Activity activity = (Activity)getContext();
                AppData appData = (AppData) activity.getApplication();
                if (peer == null) {
                    peer = appData.getPeer();
                }

                //bluetoothの接続をして、SkyWayの交換をする
                String instruction = appData.getEXCHANGE_OF_ID();   //bluetoothの接続をしてIDを交換するためのフラグ
                BluetoothConnectThread bluetoothConnectThread = new BluetoothConnectThread(address, mBluetoothAdapter, peer, appData, instruction);
                bluetoothConnectThread.start();
                try {
                    //同期処理
                    bluetoothConnectThread.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                dataConnection = appData.getDataConnection();

                //setPeerCallback();

                //通信に成功しなくてもdataConnectionに値は入るので,PeerEventEnum.CONNECTIONに成功したのか失敗したのかを記述するべき

                if (dataConnection == null) {
                    Toast.makeText(getActivity(), "接続に失敗しました。", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getActivity(), "接続に成功しました。", Toast.LENGTH_SHORT).show();

                    BootstrapButton btnNext = (BootstrapButton) getActivity().findViewById(R.id.Next);
                    btnNext.setEnabled(true);

                    appData.setDataConnection(dataConnection);
                }
            }
        });
    }

    public void setPeerCallback() {
        peer.on(Peer.PeerEventEnum.CONNECTION, new OnCallback() {
            @Override
            public void onCallback(Object o) {
                Handler handler = new Handler();
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getActivity(), "接続に成功しました。", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }
    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public void bluetoothCheck() {
        //BluetoothがONになっているかどうか
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        //mBluetoothAdapterがnullだった場合は端末がbluetoothをサポートしていない。
        if (mBluetoothAdapter == null) {
            Toast.makeText(getActivity(), "この端末はbluetoothに対応していません。", Toast.LENGTH_SHORT).show();
            return;
        }

        //BluetoothをONにする処理
        if (!mBluetoothAdapter.isEnabled()) {
            //bluetoothがOFFになっているときの処理
            //bluetoothを有効にするダイアログを表示する
            Intent reqEnableBTIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(reqEnableBTIntent, REQUEST_ENABLE_BT);
        } else {
            //OFFになっているときの処理
            //Toast.makeText(getActivity(), "ONになっています。", Toast.LENGTH_SHORT).show();
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_ENABLE_BT:
                if (resultCode == Activity.RESULT_OK) {

                } else {
                    Toast.makeText(getActivity(), "Bluetoothを許可しないとこのアプリは使用できません", Toast.LENGTH_SHORT).show();
                    getFragmentManager().beginTransaction().remove(this).commit();
                }
        }
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (dataConnection != null) {
            dataConnection.close();
        }
        if (peer != null) {
            peer.disconnect();
            peer.destroy();
        }
    }
}
