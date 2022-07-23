package com.example.myhello3app;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import java.io.BufferedReader;
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
                brIs = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                request = brIs.readLine();
                Map<String, String> qParamsMap = getQueryParamsFromGetRequest(request);
                if(qParamsMap.containsKey("q")){
                    replyText = qParamsMap.get("q");
                }
                os = new PrintWriter(socket.getOutputStream(), true);
                String response =
                                "<html>" +
                                    "<head>" +
                                    "</head>" +
                                    "<body>" +
                                        "<h1>Response from server</h1>" +
                                        "<br>" +
                                        "<h1>" + replyText + "</h1>" +
                                    "</body>" +
                                "</html>";

                os.print("HTTP/1.0 200" + "\r\n");
                os.print("Content type: text/html" + "\r\n");
                os.print("Content length: " + response.length() + "\r\n");
                os.print("\r\n");
                os.print(response + "\r\n");
                os.flush();
                socket.close();

                msgLog += "Request of " + request
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
            System.out.println("@qParamsMap: ");
            System.out.println(qParamsMap);
            return qParamsMap;
        }

    }



}