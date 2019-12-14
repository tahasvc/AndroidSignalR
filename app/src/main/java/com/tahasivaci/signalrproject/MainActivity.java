package com.tahasivaci.signalrproject;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import microsoft.aspnet.signalr.client.Credentials;
import microsoft.aspnet.signalr.client.Platform;
import microsoft.aspnet.signalr.client.SignalRFuture;
import microsoft.aspnet.signalr.client.http.Request;
import microsoft.aspnet.signalr.client.http.android.AndroidPlatformComponent;
import microsoft.aspnet.signalr.client.hubs.HubConnection;
import microsoft.aspnet.signalr.client.hubs.HubProxy;
import microsoft.aspnet.signalr.client.hubs.SubscriptionHandler1;
import microsoft.aspnet.signalr.client.hubs.SubscriptionHandler2;
import microsoft.aspnet.signalr.client.transport.ClientTransport;
import microsoft.aspnet.signalr.client.transport.ServerSentEventsTransport;

public class MainActivity extends AppCompatActivity {
    EditText username, message, send_message;
    Button connection, disconnection, send;
    Spinner users;
    HubConnection hubConnection; //Do the signalR definitions
    HubProxy hubProxy;
    Handler mHandler = new Handler(); //listener
    List<User> userList;
    List<String> user_names;
    User sender;
    Context cx;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        username = (EditText) findViewById(R.id.putText);
        message = (EditText) findViewById(R.id.edit_message);
        send_message = (EditText) findViewById(R.id.messageScreen);
        connection = (Button) findViewById(R.id.btnconnect);
        disconnection = (Button) findViewById(R.id.btndisconnect);
        send = (Button) findViewById(R.id.send_message);
        users = (Spinner) findViewById(R.id.userList);
        cx = this;

        users.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position > 0) {
                    sender = userList.get(position - 1);
                    send.setEnabled(true);
                } else {
                    send.setEnabled(false);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!message.getText().toString().trim().equals("") && sender != null) { // WebApi Methods
                    hubProxy.invoke("sendMessage", new Object[]{message.getText().toString().trim(), sender.connectionID
                    });//we have parameterized what we want in the web API method
                }
            }
        });
        connection.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                connect(); // connect chat server
            }
        });
        disconnection.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                disconnect(); //disconnect chat server
            }
        });

    }

    void connect() {
        Platform.loadPlatformComponent(new AndroidPlatformComponent());
        Credentials credentials = new Credentials() {
            @Override
            public void prepareRequest(Request request) {
                request.addHeader("username", username.getText().toString().trim()); //get username
            }
        };
        String serverUrl = "https://server-mr5.conveyor.cloud/signalr"; // connect to signalr server
        hubConnection = new HubConnection(serverUrl);
        hubConnection.setCredentials(credentials);
        hubConnection.connected(new Runnable() {
            @Override
            public void run() {
            }
        });
        String CLIENT_METHOD_BROADAST_MESSAGE = "getUserList"; // get webapi serv methods
        hubProxy = hubConnection.createHubProxy("chatHub"); // web api  necessary method name
        ClientTransport clientTransport = new ServerSentEventsTransport((hubConnection.getLogger()));
        SignalRFuture<Void> signalRFuture = hubConnection.start(clientTransport);
        try {
            signalRFuture.get();
        } catch (InterruptedException | ExecutionException e) {
            Log.e("SimpleSignalR", e.toString());
            return;
        }
        hubProxy.on(CLIENT_METHOD_BROADAST_MESSAGE, new SubscriptionHandler1<String>() {
            @Override
            public void run(String s) {
                try { // we added the list of connected users
                    JSONArray jsonArray = new JSONArray(s);
                    userList = new ArrayList<User>();
                    user_names = new ArrayList<String>();
                    user_names.add("Select User");
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject jsonObject = jsonArray.getJSONObject(i);
                        String username = jsonObject.getString("username");
                        String connection_id = jsonObject.getString("connectionID");
                        User user = new User(username, connection_id);
                        userList.add(user);
                        user_names.add(username);
                    }
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            ArrayAdapter<String> adapter = new ArrayAdapter<String>(cx, android.R.layout.simple_list_item_1, user_names);
                            adapter.setDropDownViewResource(android.R.layout.simple_list_item_1);
                            users.setAdapter(adapter);
                        }
                    });
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, String.class);
        hubProxy.on("sendMessage", new SubscriptionHandler2<String, String>() {

            @Override
            public void run(final String s, final String s2) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        send_message.setText(send_message.getText() + "\n" + s2 + " : " + s);
                    }
                });
            }
        }, String.class, String.class);
    }

    void disconnect() { //disconnection server
        hubConnection.stop();
        userList.clear();
        users.setAdapter(null);
        send.setEnabled(false);
        sender = null;
    }
}
