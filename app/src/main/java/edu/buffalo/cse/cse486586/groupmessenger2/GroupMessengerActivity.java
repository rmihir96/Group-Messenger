package edu.buffalo.cse.cse486586.groupmessenger2;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.PriorityQueue;

import static android.content.ContentValues.TAG;

/**
 * GroupMessengerActivity is the main Activity for the assignment.
 * 
 * @author stevko
 *
 */
class compareSequence implements Comparator<Messages>{
    public int compare(Messages m1, Messages m2) {
        if(m1.seqno > m2.seqno)
            return 1;
        else  if(m1.seqno < m2.seqno)
            return  -1;
        return 0;
    }
}
class Messages {
    public String msg;
    public Double seqno;
    public boolean deliverable;
    public String source;

    public Messages(Boolean deliverable, String msg, Double seqno, String source) {
        this.deliverable = deliverable;
        this.msg = msg;
        this.seqno = seqno;
        this.source = source;
    }
    public String getMsg() {
        return msg;
    }
}

public class GroupMessengerActivity extends Activity {

    static final String REMOTE_PORT0 = "11108";
    static final String REMOTE_PORT1 = "11112";
    static final String REMOTE_PORT2 = "11116";
    static final String REMOTE_PORT3 = "11120";
    static final String REMOTE_PORT4 = "11124";
    static final int SERVER_PORT = 10000;
    int sequence_counter = 0;
    String failed = "";
    String myPort;
    HashMap<String, String> map = new HashMap<String, String>();




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_messenger);

        TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);

        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        myPort = String.valueOf((Integer.parseInt(portStr) * 2));
        Log.e("my port", myPort);

        /*Create server socket */
        try {
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);

        }catch(IOException e){
            Log.e(TAG, "Can't create a ServerSocket");
            return;

        }


        map.put(REMOTE_PORT0, "1");
        map.put(REMOTE_PORT1, "2");
        map.put(REMOTE_PORT2, "3");
        map.put(REMOTE_PORT3, "4");
        map.put(REMOTE_PORT4, "5");


        TextView tv = (TextView) findViewById(R.id.textView1);
        tv.setMovementMethod(new ScrollingMovementMethod());
        
        /*
         * Registers OnPTestClickListener for "button1" in the layout, which is the "PTest" button.
         * OnPTestClickListener demonstrates how to access a ContentProvider.
         */
        findViewById(R.id.button1).setOnClickListener(
                new OnPTestClickListener(tv, getContentResolver()));

        final EditText editText = (EditText) findViewById(R.id.editText1);
        Button button = (Button) findViewById(R.id.button4);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Do something in response to button click
                String msg = editText.getText().toString() + "\n";
                editText.setText("");


                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg, myPort);

            }
        });
    }

    private class ServerTask extends AsyncTask<ServerSocket, String, Void> {
        @Override
        protected Void doInBackground(ServerSocket... serverSockets) {
            ServerSocket serversocket = serverSockets[0];
            int count = 0;
            //ArrayList<Messages> final_seq = new ArrayList<Messages>();
            PriorityQueue<Messages> final_messages = new PriorityQueue<Messages>(50, new compareSequence());
            try {
                 while (true) {
                    sequence_counter += 1;
                    Socket s = serversocket.accept();
                    s.setSoTimeout(2000);
                    BufferedReader br1 = new BufferedReader(new InputStreamReader(s.getInputStream()));
                    PrintWriter pp = new PrintWriter(new OutputStreamWriter(s.getOutputStream()));
                    String msg = br1.readLine();
                   // Log.e("msg", msg);
                    /*Check if initial message*/
                    if (msg != null && msg.startsWith("Init")) {
                        //Log.i("Init msg received", msg);
                        String[] MessageStream = msg.split("-");
                        /* Generate Seq no for the message */
                        String seqno = String.valueOf(sequence_counter)+"."+map.get(myPort);
                        /*Create message object and add to Priority Queue */
//                        Log.e("MessageStream", MessageStream[0]+MessageStream[1]+"-"+ MessageStream[2]);
                        Messages message = new Messages(false, MessageStream[1], Double.parseDouble(seqno), MessageStream[2]);
                        final_messages.add(message);
                        /*Send back the Sequence number */
                        pp.println("ACK" + "-" + seqno + "-" + myPort);
                        pp.flush();
                    }
                    /*Check if final message : MaxSeq number */
                    else if (msg != null && msg.startsWith("Final")) {
                        String[] FinalStream = msg.split("-");
                        sequence_counter = Math.max(sequence_counter, (int) Double.parseDouble(FinalStream[2])) + 2;
                        Log.e( "Final Seq no :", FinalStream[2]);
                        pp.println("ack");
                        pp.flush();
                        Iterator iter_msg = final_messages.iterator();
                        Messages mm = null;
                        while (iter_msg.hasNext()){
                            mm =  (Messages) iter_msg.next();
                            if (mm.seqno == Double.parseDouble(FinalStream[1])){
                                break;
                            }
                        }
                        if(mm != null) {
                            final_messages.remove(mm);
                            mm.seqno = Double.parseDouble(FinalStream[2]);
                            mm.deliverable = true;
                            final_messages.add(mm);
                        }
                        while (!final_messages.isEmpty()){
                             if (final_messages.peek() != null && final_messages.peek().deliverable) {
                                Log.e("PQ peek", String.valueOf(final_messages.peek().seqno));
                                publishProgress(final_messages.peek().msg, String.valueOf(count++));
                                final_messages.poll();
                            }
                            else if (final_messages.peek()!= null && final_messages.peek().source.equals(failed)){
                                Log.e("PQ Poll", String.valueOf(final_messages.peek().seqno));
                                final_messages.poll();

                            }
                            else{
                                break;
                            }
                        }
                    }else if (msg!= null && msg.startsWith("Dead")){
                        String[] dead_avds = msg.split("-");
                        failed = dead_avds[1];

                        Log.e("Failed avd set", failed);
                    }
                    Iterator iter = final_messages.iterator();
                    Messages fmsg = null;
                    while(iter.hasNext()){
                        fmsg =  (Messages) iter.next();
                        Log.e("Element",  fmsg.msg + "and seq no:" + fmsg.seqno);
                    }
                 }
            } catch (IOException e) {
                Log.e("Server Dead", "AVD Shut down");
                e.printStackTrace();
            }
        return null;
        }

        @Override
        protected void onProgressUpdate(String... strings) {
            ContentResolver contentResolver = getContentResolver();
            Uri contentUri = Uri.parse("content://edu.buffalo.cse.cse486586.groupmessenger2.provider");


            ContentValues contentValues = new ContentValues();


            String strReceived = strings[0].trim();
            TextView tv = (TextView) findViewById(R.id.textView1);
            tv.append(strReceived + "\t\n");


            contentValues.put("key",strings[1]);
            contentValues.put("value", strReceived);
            contentResolver.insert(contentUri, contentValues);
            Log.i("Key", String.valueOf(strings[1]));
            Log.i("message", strReceived);


        }
    }

    private class ClientTask extends AsyncTask<String, Void, Void> {


        @Override
        protected Void doInBackground(String... msgs) {
            ArrayList<Double> maxSeq = new ArrayList<Double>();
            HashMap<String, String> ports_seq = new HashMap<String, String>();
            String ack;
            String[] remotePorts = {REMOTE_PORT0, REMOTE_PORT1, REMOTE_PORT2, REMOTE_PORT3, REMOTE_PORT4};

            for (int i = 0; i < remotePorts.length; i++) {
                try {
                    if (remotePorts[i].equals(failed)){
                        continue;
                    }
                    Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), Integer.parseInt(remotePorts[i]));
                    socket.setSoTimeout(2500);
                    String msgToSend = msgs[0].trim();
                    //Log.i("message to send", msgToSend);
                    /* Send Initial message to the server */
                    PrintWriter pf0 = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())));
                    BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    // Log.i("", "Initial message Sent");

                    pf0.println("Init-" + msgToSend + "-" + myPort);
                    pf0.flush();
                    /* Receive Sequence numbers back */
                    ack = br.readLine();
                    if (ack != null && ack.startsWith("ACK")) {
                       // Log.e("Init Recieved", "Init seq received at- " + remotePorts[i]);
                        socket.close();
                        String[] Seqnos = ack.split("-"); //seqnos[1] - seq no
                        Log.i("Seq no", Seqnos[1]);
                        maxSeq.add(Double.parseDouble(Seqnos[1]));
                        ports_seq.put(Seqnos[2], Seqnos[1]);
                    }
                    else{
                        throw new IOException();
                    }
                    Log.e("Initial + Seq no", "completed from avd" + remotePorts[i]);

                }
                catch (IOException e) {
                    Log.e("Exception", "AVD Shut down at INIT");
                    failed = remotePorts[i];
                    Log.e("Failed", "AVD" + failed + "failed");
                    for (int k = 0; k < remotePorts.length; k++){
                        try {
                            if (remotePorts[k].equals(failed)){
                                continue;
                            }
                            Socket socket2 = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), Integer.parseInt(remotePorts[k]));
                            PrintWriter pf2 = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket2.getOutputStream())));
                            BufferedReader br2 = new BufferedReader(new InputStreamReader(socket2.getInputStream()));
                            pf2.println("Dead-" + failed);
                            pf2.flush();
                        } catch (IOException e1) {
                            e1.printStackTrace();
                        }
                    }
                    e.printStackTrace();
                }
            }

            String final_seq = String.valueOf(Collections.max(maxSeq));
            //Log.i("Max Seq no", final_seq);

            /* Send the objects back with final sequence number */
            for (int j = 0; j < remotePorts.length; j++) {
                try {
                    if (remotePorts[j].equals(failed)){
                        continue;
                    }
                    Socket socket0 = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), Integer.parseInt(remotePorts[j]));
                    socket0.setSoTimeout(2000);
                    PrintWriter pf1 = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket0.getOutputStream())));
                    BufferedReader br2 = new BufferedReader(new InputStreamReader(socket0.getInputStream()));
                    pf1.println("Final-" + ports_seq.get(remotePorts[j]) + "-" + final_seq);
                    pf1.flush();
                    String aa = br2.readLine();
                    if (aa != null && aa.equals("ack")) {
                        Log.e("Received", "Final seq received at- " + remotePorts[j]);
                        socket0.close();
                    }
                    else{
                        throw new IOException();
                    }

                }
                catch (IOException e) {
                    Log.e("Exception", "AVD Shut down at Final");
                    failed = remotePorts[j];
                    Log.e("Failed", "AVD-" + failed + "failed");
                    e.printStackTrace();
                    for (int k = 0; k < remotePorts.length; k++){
                        try {
                            if (remotePorts[k].equals(failed)){
                                continue;
                            }
                            Socket socket2 = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), Integer.parseInt(remotePorts[k]));
                            PrintWriter pf2 = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket2.getOutputStream())));
                            BufferedReader br2 = new BufferedReader(new InputStreamReader(socket2.getInputStream()));
                            pf2.println("Dead-" + failed);
                            pf2.flush();
                        } catch (IOException e1) {
                            e1.printStackTrace();
                        }
                    }
                }

            }
            maxSeq.clear();
            ports_seq.clear();
            return null;
        }
    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_group_messenger, menu);
        return true;
    }
}
