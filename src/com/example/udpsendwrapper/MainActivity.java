package com.example.udpsendwrapper;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

import android.app.Activity;
import android.content.Context;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.MulticastLock;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

public class MainActivity extends Activity {

    Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context = getApplicationContext();

        Thread t = new Thread(new Runnable() {

            @Override
            public void run() {
                // sendBroadcastMsg();
                copyFile("nfd.conf", "/data/data/com.example.udpsendwrapper/"
                        + "nfd.conf", context);
                // String[] nfdExecString = new String[] {
                // "/data/data/com.example.udpsendwrapper/nfd",
                // "--config",
                // "/data/data/com.example.udpsendwrapper/nfd.conf" };
                String[] nfdExecString = new String[] {
                        "/data/data/com.example.udpsendwrapper/nfd", "--version", "2>&1" };
                runNativeBinaryFile("nfd", true, nfdExecString);
            }
        });
        t.start();
        // runNativeBinaryFile();
    }

    private void sendBroadcastMsg() throws IOException {
        Log.i("DEBUG", "Thread starts!");
        String msg = "Message from Android.\n";
        WifiManager wifi = (WifiManager) this
                .getSystemService(Context.WIFI_SERVICE);
        // MulticastLock lock = wifi.createMulticastLock("udp-broadcast");
        // lock.acquire();
        // Log.i("DEBUG", "Wifi lock aquired!");
        DatagramSocket socket = new DatagramSocket(32000);
        socket.setBroadcast(true);
        // socket.setBroadcast(false);
        // InetAddress host = InetAddress.getByName("255.255.255.255");
        // InetAddress host = InetAddress.getByName("255.255.255.255");
        InetAddress host = getBroadcastAddress(wifi);
        Log.i("DEBUG", "broadcast addr: " + host.getHostAddress());
        DatagramPacket packet = new DatagramPacket(msg.getBytes(),
                msg.length(), host, 32000);
        socket.send(packet);

        byte[] buffer = new byte[1024];
        DatagramPacket receivedPacket = new DatagramPacket(buffer,
                buffer.length);
        socket.receive(receivedPacket);
        // lock.release();
        socket.close();

        String s = new String(receivedPacket.getData());
        Log.i("DEBUG", "MSG Received: " + s);
    }

    private InetAddress getBroadcastAddress(WifiManager wifi)
            throws IOException {
        DhcpInfo dhcp = wifi.getDhcpInfo();
        if (dhcp == null) {
            Log.i("DEBUG", "dhcp is null!");
            return InetAddress.getByName("255.255.255.255");
        }
        int broadcast = (dhcp.ipAddress & dhcp.netmask) | ~dhcp.netmask;
        byte[] quads = new byte[4];
        for (int k = 0; k < 4; ++k) {
            quads[k] = (byte) ((broadcast >> k * 8) & 0xFF);
        }
        return InetAddress.getByAddress(quads);
    }

    private void runNativeBinaryFile(String fileName, boolean background,
            String[] execString) {
        copyFile(fileName, "/data/data/com.example.udpsendwrapper/" + fileName,
                context);
        Log.i("DEBUG", "Binary File " + fileName + " copied to destination");
        Process nativeApp = null;
        char[] buffer = new char[4096];
        try {
            ProcessBuilder builder = new ProcessBuilder(execString);
            builder.redirectErrorStream(true);
            nativeApp = builder.start();
            // nativeApp = Runtime.getRuntime().exec(execString);

            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    nativeApp.getInputStream()));
            int read;
            Log.d("NATIVE", "Read buffer prepared");
            while ((read = reader.read(buffer)) > 0) {
                Log.d("Output", String.valueOf(buffer));
            }
            Log.d("NATIVE", "Passed read while loop. Read value: " + read);
            reader.close();

            // Waits for the command to finish.
            nativeApp.waitFor();
            Log.d("NATIVE", "Thread run to the end.");

        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            if (nativeApp != null) {
                nativeApp.destroy();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private static void copyFile(String assetPath, String localPath,
            Context context) {
        try {
            InputStream in = context.getAssets().open(assetPath);
            File outfile = new File(localPath);
            FileOutputStream out = new FileOutputStream(outfile);
            int read;
            byte[] buffer = new byte[4096];
            while ((read = in.read(buffer)) > 0) {
                out.write(buffer, 0, read);
            }
            out.close();
            in.close();

            outfile.setExecutable(true);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
