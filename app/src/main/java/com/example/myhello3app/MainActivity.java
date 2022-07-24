package com.example.myhello3app;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    ServerSocket httpServerSocket;

    EditText welcomeMsg;
    TextView infoIp;
    TextView infoMsg;
    String msgLog = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        welcomeMsg = findViewById(R.id.welcomemsg);
        infoMsg = findViewById(R.id.msg);
//        infoIp = (TextView) findViewById(R.id.infoip);
//        infoIp.setText(getIpAddress() + ":" + HttpServerThread.HttpServerPORT + "\n");

        MyHttpServerThread myHttpServerThread = new MyHttpServerThread();
        Button mButtonStart = findViewById(R.id.start_server);
        mButtonStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                myHttpServerThread.start();
            }
        });

//        Button mButtonStop = findViewById(R.id.stop_server);
//        mButtonStop.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                httpServerThread.cancel();
//            }
//        });

    }

    private String getIpAddress() {
        StringBuilder ip = new StringBuilder();
        try {
            Enumeration<NetworkInterface> enumNetworkInterfaces = NetworkInterface
                    .getNetworkInterfaces();
            while (enumNetworkInterfaces.hasMoreElements()) {
                NetworkInterface networkInterface = enumNetworkInterfaces
                        .nextElement();
                Enumeration<InetAddress> enumInetAddress = networkInterface
                        .getInetAddresses();
                while (enumInetAddress.hasMoreElements()) {
                    InetAddress inetAddress = enumInetAddress.nextElement();

                    if (inetAddress.isSiteLocalAddress()) {
                        ip.append("SiteLocalAddress: ").append(inetAddress.getHostAddress()).append("\n");
                    }
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
            ip.append("Something Wrong! ").append(e).append("\n");
        }
        return ip.toString();
    }



    private class MyHttpServerThread extends Thread {

        static final int HttpServerPORT = 8080;
        boolean cancelled = false;
        int requestCount = 0;

        @Override
        public void run() {
            Socket socket;
            boolean processNextRequest = true;
            try {
                httpServerSocket = new ServerSocket(HttpServerPORT);
                System.out.println("@Server starting with port: " + HttpServerPORT);
                System.out.println("*** @Process Next Request: " + processNextRequest);
                while(processNextRequest){
                    requestCount++;
                    if(cancelled){
                        System.out.println("stop server");
                        return;
                    }
                    System.out.println("Start to request....accepting");
                    socket = httpServerSocket.accept();
                    // start response thread
                    MyHttpResponseThread httpResponseThread = new MyHttpResponseThread(socket, welcomeMsg.getText().toString());
                    httpResponseThread.start();
                    System.out.println("@requestCount: " + requestCount);
                    // if(requestCount > 5) processNextRequest = false;
                }
                System.out.println("*** @Process Next Request: " + processNextRequest);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void cancel(){
            cancelled = true;
            interrupt();
        }
        public void startAgain(){
            cancelled = false;
        }
    }


    private class MyHttpResponseThread extends Thread {

        Socket socket;
        String replyText;

        MyHttpResponseThread(Socket socket, String msg){
            this.socket = socket;
            replyText = msg;
        }

        @Override
        public void run() {

            //if(Thread.currentThread().isInterrupted()) {return;}
            BufferedReader brIs;
            PrintWriter os;
            String request;

            try {
                // Get input and output streams
//                BufferedReader in = new BufferedReader( new InputStreamReader( socket.getInputStream() ) );
//                PrintWriter out = new PrintWriter( socket.getOutputStream() );
                // ---------------------------------------------------------------------------------
                brIs = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                //code to read and print headers
                // read request
                String req1stLine = brIs.readLine();
                boolean isPost = req1stLine.startsWith("POST");
                String headerLine;
                while((headerLine = brIs.readLine()).length() != 0){
                    System.out.println("HTTP-HEADER: " + headerLine);
                }
                // process GET / POST params
                Map<String, String> qParamsMap = new HashMap<>();
                String postBody = "";
                if(isPost){
                    postBody = readPostBodyData(brIs);
                    qParamsMap = getQueryParamsFromGetRequest(req1stLine);
                } else {
                    qParamsMap = getQueryParamsFromGetRequest(req1stLine);
                }
                // ---------------------------------------------------------------------------------

                if(qParamsMap.containsKey("q")){
                    replyText = qParamsMap.get("q");
                }
                if(qParamsMap.containsKey("dl")){
                    storeDeviceListData(postBody);
                }
                if(qParamsMap.containsKey("dd")){
                    storeDeviceReadingData(qParamsMap.get("dd"), qParamsMap.get("dn"));
                }
                String deviceReadingData = "";
                // app?d=deviceName
                if(qParamsMap.containsKey("d")){
                    deviceReadingData = displayDeviceReadingData(qParamsMap.get("d"));
                    replyText = deviceReadingData;
                }
                String deviceList = "";
                // app?d=deviceName
                if(qParamsMap.containsKey("l")){
                    deviceList = displayDeviceList();
                    replyText = deviceList;
                }

                os = new PrintWriter(socket.getOutputStream(), true);
                String response =
                                "<html>" +
                                    "<head>" +
                                    "</head>" +
                                    "<body>" +
                                        "<h1>Response from server</h1>" +
                                        "<br>" +
                                        "<br>" +
                                        "<h1>Device List:<h1>" +
                                        "<br>" +
                                        "<div>" + replyText + "</div>" +
                                    "</body>" +
                                "</html>";

                os.print("HTTP/1.0 200" + "\r\n");
                os.print("Content type: text/html" + "\r\n");
                os.print("Content length: " + response.length() + "\r\n");
                os.print("\r\n");
                os.print(response + "\r\n");
                os.flush();
                socket.close();

                msgLog += "Request of " + req1stLine
                        + " from " + socket.getInetAddress().toString() + "\n";
                MainActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        infoMsg.setText(msgLog);
                    }
                });

            } catch (IOException e) {
                e.printStackTrace();
            }
        }


        private Map<String, String> getQueryParamsFromGetRequest(String request){
            Map<String, String> qParamsMap = new HashMap<>();
            request = request.replace("HTTP/1.1", "");
            String[] qArr = request.split("\\?");
            if(qArr.length > 1){
                String queryString = qArr[1];
                if(queryString.length() != 0){
                    String[] qsArr = queryString.split("&");
                    for (String userQueryParam : qsArr) {
                        String userQueryParamKey = userQueryParam.split("=")[0];
                        String userQueryParamValue = userQueryParam.split("=")[1];
                        qParamsMap.put(userQueryParamKey, userQueryParamValue);
                    }
                }
            }
            System.out.println("@qParamsMap====>");
            System.out.println(qParamsMap);
            return qParamsMap;
        }

        public String readPostBodyData(BufferedReader brIs) throws IOException {
            //code to read the post payload data
            StringBuilder payload = new StringBuilder();
            while(brIs.ready()){
                payload.append((char) brIs.read());
            }
            System.out.println("Payload data is: "+ payload);
            return payload.toString();
        }

    }



    // Others Method *******************************************************************************
    public void storeDeviceListData(String deviceListData) {
        FileOutputStream fos = null;
        try {
//            File tempBlueDir= getDir("TempBlue", Context.MODE_APPEND);
            File tempBlueDir= getDir("TempBlue", Context.MODE_PRIVATE);
            if (!tempBlueDir.exists()){
                tempBlueDir.mkdirs();
            }
            File fileWithinMyDir = new File(tempBlueDir, "dl.txt");
            String fileFullPath = fileWithinMyDir.getAbsolutePath();
            fos = new FileOutputStream(fileWithinMyDir);    //Use the stream as usual to write into the file.
            fos.write(deviceListData.getBytes());
            System.out.println("Saved to @fileFullPath " + fileFullPath);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void storeDeviceReadingData(String deviceReadingData, String deviceName) {
        String saveFileName = deviceName + "_dd.txt";
        FileOutputStream fos = null;
        try {
//            fos = mContext.openFileOutput(FILE_NAME, MODE_PRIVATE);
//            fos = mContext.openFileOutput(FILE_NAME, MODE_APPEND);
            File tempBlueDir= getDir("TempBlue", Context.MODE_APPEND);
            if (!tempBlueDir.exists()){
                tempBlueDir.mkdirs();
            }
            File fileWithinMyDir = new File(tempBlueDir, saveFileName);
            String fileFullPath = fileWithinMyDir.getAbsolutePath();
            fos = new FileOutputStream(fileWithinMyDir);    //Use the stream as usual to write into the file.
            fos.write(deviceReadingData.getBytes());
            System.out.println("Saved to @fileFullPath " + fileFullPath);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public String displayDeviceReadingData(String deviceName){
        String loadFileName = deviceName + "_dd.txt";
        File tempBlueDir= getDir("TempBlue", Context.MODE_APPEND);
        FileInputStream fis = null;
        StringBuilder sb = null;
        try {
//            File fileBlue = new File(tempBlueDir, "dd.txt");
            File fileBlue = new File(tempBlueDir, loadFileName);
            System.out.println(fileBlue.getAbsolutePath());

            fis = new FileInputStream(fileBlue);
            InputStreamReader isr = new InputStreamReader(fis);
            BufferedReader br = new BufferedReader(isr);
            sb = new StringBuilder();
            String text;
            while ((text = br.readLine()) != null) {
                sb.append(text).append("\n");
            }
            System.out.println("Load Data: " + sb);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        if(sb == null) return "";
        return sb.toString();
    }

    public String displayDeviceList(){
        File tempBlueDir= getDir("TempBlue", Context.MODE_APPEND);
        FileInputStream fis = null;
        StringBuilder sb = null;
        try {
            File fileBlue = new File(tempBlueDir, "dl.txt");
            System.out.println(fileBlue.getAbsolutePath());

            fis = new FileInputStream(fileBlue);
            InputStreamReader isr = new InputStreamReader(fis);
            BufferedReader br = new BufferedReader(isr);
            sb = new StringBuilder();
            String text;
            while ((text = br.readLine()) != null) {
                sb.append(text).append("\n");
            }
            System.out.println("Load Data: " + sb);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        if(sb == null) return "";
        return sb.toString();
    }



    private String loadTempBlueDeviceReadingData(String deviceName){
        File tempBlueDir= getDir("TempBlue", Context.MODE_APPEND);
        FileInputStream fis = null;
        StringBuilder sb = null;
        try {
            File fileBlue = new File(tempBlueDir, "dd.txt");
            System.out.println(fileBlue.getAbsolutePath());

            fis = new FileInputStream(fileBlue);
            InputStreamReader isr = new InputStreamReader(fis);
            BufferedReader br = new BufferedReader(isr);
            sb = new StringBuilder();
            String text;
            while ((text = br.readLine()) != null) {
                sb.append(text).append("\n");
            }
            System.out.println("Load Data: " + sb);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        if(sb == null) return "";
        return sb.toString();
    }



}