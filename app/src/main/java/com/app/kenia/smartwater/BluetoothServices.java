package com.app.kenia.smartwater;

import android.app.Activity;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.UUID;

/**
 * Created by luisbarrera on 10/3/17.
 */

public class BluetoothServices extends Service {

    private static final UUID BTMODULEUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); // "random" unique identifier
    // #defines for identifying shared types between calling functions
    private final static int MESSAGE_READ = 2; // used in bluetooth handler to identify message update
    private final static int CONNECTING_STATUS = 3; // used in bluetooth handler to identify message status
    public static Activity mainActivity;
    private static ArrayList<Paquete> paquetes;
    private final String TAG = MainActivity.class.getSimpleName();
    private final String USER_AGENT = "Mozilla/5.0";
    private BluetoothAdapter BA;
    private Handler mHandler; // Our main handler that will receive callback notifications
    private BluetoothServices.ConnectedThread mConnectedThread; // bluetooth background worker thread to send and receive data
    private BluetoothSocket mBTSocket = null; // bi-directional client-to-client data path
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // Add the name and address to an array adapter to show in a ListView
                //mArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                Toast.makeText(getBaseContext(), device.getName(), Toast.LENGTH_SHORT).show();
                if (device.getName().equals("HC-05")) {
                    ConnectBluetooth(device);
                }
            }
        }
    };
    private long tiempoAnterior;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        return START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        paquetes = new ArrayList<Paquete>();
        BA = BluetoothAdapter.getDefaultAdapter();
        if (!BA.isEnabled()) {
            Intent turnOn = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            mainActivity.startActivityForResult(turnOn, 0);
            Toast.makeText(getApplicationContext(), "Turned on", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(getApplicationContext(), "Already on", Toast.LENGTH_LONG).show();
        }
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mReceiver, filter);
        BA.startDiscovery();
        tiempoAnterior = Calendar.getInstance().getTimeInMillis() / 1000;
        mHandler = new Handler() {
            public void handleMessage(android.os.Message msg) {
                if (msg.what == MESSAGE_READ) {
                    String readMessage = "";
                    try {
                        readMessage = new String((byte[]) msg.obj, "UTF-8");
                        String[] cadena = readMessage.split("\r\n");
                        for (int i = 0; i < cadena.length - 1; i++) {
                            int largoCadena = cadena[i].length();
                            int inicio = 0;
                            int Final = 0;
                            for (int j = 0; j < largoCadena; j++) {
                                char actual = cadena[i].charAt(j);
                                if (String.valueOf(actual).equals("$")) {
                                    inicio = j;
                                    break;
                                }
                            }
                            for (int h = inicio + 1; h < largoCadena; h++) {
                                char actual = cadena[i].charAt(h);
                                if (String.valueOf(actual).equals("$")) {
                                    Final = h;
                                    break;
                                }
                            }
                            if ((largoCadena >= Final) && (largoCadena >= inicio + 1)) {
                                try {
                                    String subCadena = cadena[i].substring(inicio + 1, Final);
                                    String[] cadena1 = subCadena.split(",");
                                    if (cadena1.length > 0) {
                                        String temperatura = cadena1[1];
                                        String distancia = cadena1[0];
                                        String turbidez = cadena1[2];
                                        double latitud = LocationServices.mLatitude;
                                        double longitud = LocationServices.mLongitude;
                                        Paquete temp = new Paquete(temperatura, String.valueOf(latitud), String.valueOf(longitud), distancia, turbidez);
                                        paquetes.add(temp);
                                    }
                                } catch (Exception ex) {
                                    System.out.println(ex);
                                }
                            }
                        }
                        Toast.makeText(getBaseContext(), readMessage, Toast.LENGTH_SHORT).show();
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                    long tiempoActual = Calendar.getInstance().getTimeInMillis() / 1000;
                    if ((tiempoActual - tiempoAnterior) >= 15) {
                        new sendPost().execute("", "");
                        tiempoAnterior = tiempoActual;
                    }
                }

                if (msg.what == CONNECTING_STATUS) {
                    if (msg.arg1 == 1)
                        Toast.makeText(getBaseContext(), (String) (msg.obj), Toast.LENGTH_SHORT).show();
                    else
                        Toast.makeText(getBaseContext(), "Connect failed.", Toast.LENGTH_SHORT).show();
                }
            }
        };
    }

    private void ConnectBluetooth(final BluetoothDevice device) {
        if (!BA.isEnabled()) {
            Toast.makeText(getBaseContext(), "Bluetooth not on", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(getBaseContext(), "Connecting...", Toast.LENGTH_SHORT).show();
        // Get the device MAC address, which is the last 17 chars in the View
        //String info = ((TextView) v).getText().toString();
        final String address = device.getAddress();
        final String name = device.getName();

        // Spawn a new thread to avoid blocking the GUI one
        new Thread() {
            public void run() {
                boolean fail = false;

                try {
                    mBTSocket = createBluetoothSocket(device);
                } catch (IOException e) {
                    fail = true;
                    Toast.makeText(getBaseContext(), "Socket creation failed", Toast.LENGTH_SHORT).show();
                }
                // Establish the Bluetooth socket connection.
                try {
                    mBTSocket.connect();
                } catch (IOException e) {
                    try {
                        fail = true;
                        mBTSocket.close();
                        mHandler.obtainMessage(CONNECTING_STATUS, -1, -1)
                                .sendToTarget();
                    } catch (IOException e2) {
                        //insert code to deal with this
                        Toast.makeText(getBaseContext(), "Socket creation failed", Toast.LENGTH_SHORT).show();
                    }
                }
                if (fail == false) {
                    mConnectedThread = new ConnectedThread(mBTSocket);
                    mConnectedThread.start();

                    mHandler.obtainMessage(CONNECTING_STATUS, 1, -1, name)
                            .sendToTarget();
                }
            }
        }.start();
    }

    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {
        try {
            final Method m = device.getClass().getMethod("createInsecureRfcommSocketToServiceRecord", UUID.class);
            return (BluetoothSocket) m.invoke(device, BTMODULEUUID);
        } catch (Exception e) {
            Log.e(TAG, "Could not create Insecure RFComm Connection", e);
        }
        return device.createRfcommSocketToServiceRecord(BTMODULEUUID);
    }

    private class sendPost extends AsyncTask<String, Integer, String> {
        protected void onPostExecute(String result) {
            //	pb.setVisibility(View.GONE);
            super.onPostExecute(result);
        }

        protected void onProgressUpdate(Integer... progress) {
            //		pb.setProgress(progress[0]);
        }

        @Override
        protected String doInBackground(String... strings) {
            String urlParameters = "";
            try {
                JSONObject jsonObject = new JSONObject();
                JSONArray JSONpaquetes = new JSONArray();
                for (int i = 0; i < paquetes.size(); i++) {
                    JSONObject jsonObject1 = new JSONObject();
                    jsonObject1.put("temp", paquetes.get(i).Temperatura);
                    jsonObject1.put("lat", paquetes.get(i).Latitud);
                    jsonObject1.put("lon", paquetes.get(i).Longitud);
                    jsonObject1.put("dist", paquetes.get(i).Distancia);
                    jsonObject1.put("turb", paquetes.get(i).turbidez);
                    JSONpaquetes.put(jsonObject1);
                    jsonObject.put("data", jsonObject1);
                    urlParameters = "data=" + jsonObject.getString("data");


                    //URL url;
                    HttpURLConnection connection = null;
                    try {
                        //Create connection
                        String url = "http://35.202.207.233/arqui2/getdatos_pachon.php?" + urlParameters;
                        URL obj = new URL(url);
                        HttpURLConnection con = (HttpURLConnection) obj.openConnection();

                        //add reuqest header
                        con.setRequestMethod("GET");
                        con.setRequestProperty("User-Agent", USER_AGENT);
                        con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
                        //String urlParameters = "sn=C02G8416DRJM&cn=&locale=&caller=&num=12345";
                        // Send post request
                        con.setDoOutput(true);
                        DataOutputStream wr = new DataOutputStream(con.getOutputStream());
                        wr.writeBytes(urlParameters);
                        wr.flush();
                        wr.close();

                        int responseCode = con.getResponseCode();
                        System.out.println("\nSending 'POST' request to URL : " + url);
                        System.out.println("Post parameters : " + urlParameters);
                        System.out.println("Response Code : " + responseCode);

                        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
                        String inputLine;
                        StringBuffer response = new StringBuffer();

                        while ((inputLine = in.readLine()) != null) {
                            response.append(inputLine);
                        }
                        in.close();

                        //print result
                        System.out.println(response.toString());
                    } catch (Exception e) {
                        e.printStackTrace();

                    } finally {

                        if (connection != null) {
                            connection.disconnect();
                        }
                    }
                }
                paquetes.clear();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            return null;
        }
    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[2048];  // buffer store for the stream
            int bytes; // bytes returned from read()
            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream.available();
                    if (bytes != 0) {
                        buffer = new byte[2048];
                        SystemClock.sleep(100); //pause and wait for rest of data. Adjust this depending on your sending speed.
                        bytes = mmInStream.available(); // how many bytes are ready to be read?
                        bytes = mmInStream.read(buffer, 0, bytes); // record how many bytes we actually read
                        mHandler.obtainMessage(MESSAGE_READ, bytes, -1, buffer)
                                .sendToTarget(); // Send the obtained bytes to the UI activity
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        /* Call this from the main activity to send data to the remote device */
        public void write(String input) {
            byte[] bytes = input.getBytes();           //converts entered String into bytes
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) {
            }
        }

        /* Call this from the main activity to shutdown the connection */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
            }
        }
    }
}
