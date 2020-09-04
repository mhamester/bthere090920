/*
 * Android application for remote control of Arduino Robot
 * By Matthieu Varagnat, 2013
 * 
 * This application connects over bluetooth to an Arduino, and sends commands
 * It also receives confirmation messages and displays them in a log
 * 
 * Shared under Creative Common Attribution licence * 
 * 
 * This BtInterface class comes from the tutorial (in French) here
 * http://nononux.free.fr/index.php?page=elec-brico-bluetooth-android-microcontroleur
 */

package com.tokbox.android.tutorials.basic_video_chat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import static com.tokbox.android.tutorials.basic_video_chat.MainActivity.message;

@SuppressLint("NewApi")
public class BtInterface {
	
	//Required bluetooth objects
	private BluetoothDevice device = null;
	private BluetoothSocket socket = null;
	private BluetoothAdapter mBluetoothAdapter = null;
	private InputStream receiveStream = null;
	private BufferedReader receiveReader = null;
	private OutputStream sendStream = null;	//no need to buffer it as we are going to send 1 char at a time

	//this thread will listen to incoming messages. It will be killed when connection is closed
	private ReceiverThread receiverThread;

	//these handlers corresponds to those in the main activity
	Handler handlerStatus, handlerMessage;
	public static int btconnected = 0;
	public static int CONNECTED = 1;
	public static int DISCONNECTED = 2;
	static final String TAG = "Chihuahua";	
    public static String btSignalled;
    public String dataToSend;
	public BtInterface(Handler hstatus, Handler h) {
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		handlerStatus = hstatus;
		handlerMessage = h;		
	}
	
	//when called from the main activity, it sets the connection with the remote device
	public void connect() {
		
		if (btconnected == 1) return;
		
		Set<BluetoothDevice> setpairedDevices = mBluetoothAdapter.getBondedDevices();
    	BluetoothDevice[] pairedDevices = (BluetoothDevice[]) setpairedDevices.toArray(new BluetoothDevice[setpairedDevices.size()]);
	
		boolean foundChihuahua = false;
		for(int i=0;i<pairedDevices.length;i++) {
			if(pairedDevices[i].getName().contains("HC-05")) { //note some modules advertise HC-06
				
				device = pairedDevices[i];
				try {
					//the String "00001101-0000-1000-8000-00805F9B34FB" is standard for Serial connections
					socket = device.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
					receiveStream = socket.getInputStream();
					receiveReader = new BufferedReader(new InputStreamReader(receiveStream));
					sendStream = socket.getOutputStream();
					
				} 
				catch(NullPointerException e ){
				e.printStackTrace();
				return;}
				catch (IOException e) {
					e.printStackTrace();
					
				}
				foundChihuahua = true;
				break;
			}
		}
		if(foundChihuahua == false){
			Log.v(TAG, "You have not turned on your Bluetooth");
		}
		
		receiverThread = new ReceiverThread(handlerMessage);
		//This thread will try to connect (it is always recommended to do so in a separate Thread) and return a confirmation message through the Handler handlerstatus
		new Thread() {
			@SuppressLint("NewApi")
			@Override public void run() {

				if (socket != null) { // catch null pointer crash
				
				try {
					socket.connect();
					
					Message msg = handlerStatus.obtainMessage();
					msg.arg1 = CONNECTED;
	                handlerStatus.sendMessage(msg);
	                
					receiverThread.start();
					
				}
				catch (IOException e) {
					Log.v("N", "Connection Failed : "+e.getMessage());
					e.printStackTrace();
				}}
				
			}
		}.start();
		
	
	 btconnected = 1;
	}
	
	//properly closing the socket and updating the status
	public void close() {
		try {
			
			socket.close();
			receiverThread.interrupt();
			
			Message msg = handlerStatus.obtainMessage();
			msg.arg1 = DISCONNECTED;
			handlerStatus.sendMessage(msg);
            
		} 
		catch (IOException e) {
			e.printStackTrace();
		} catch(NullPointerException e ){
			e.printStackTrace();
			return;}
	}
	
	//the main function of the app : sending character over the Serial connection when the user presses a key on the screen
		
	
	
	public void sendData(String data) {


			//first method here drops charactors..

		/*	try {
				sendStream.write(data.getBytes());
				sendStream.flush();
			} catch (IOException e) {
				e.printStackTrace();
			} catch(NullPointerException e ){
				e.printStackTrace();}
		}*/

		//iterate through received string received from far end opentok and then
		//send char by char to bluetooth
		data = data + '\n'; //add new line at end of tx string as delimiter
		for (int i = 0; i < data.length(); i++){
			char SendChar = data.charAt(i);

		try {
				sendStream.write(SendChar); //this is the send stream method for string interation above

			 // sendStream.flush();
			} catch (IOException e) {
				e.printStackTrace();
			} catch(NullPointerException e ){
				e.printStackTrace();}
		}}
	
	
	//this thread listens to replies from Arduino as it performs actions, then update the log through the Handler
	private class ReceiverThread extends Thread {
		Handler handler;
		
		ReceiverThread(Handler h) {
			handler = h;
		}
		
		@Override public void run() {
			while(socket != null) {
				if (isInterrupted()){
						try {
							join();
						}
						catch (InterruptedException e) {
							e.printStackTrace();
						}
				}
				try {
					if(receiveStream.available() > 0) {
						 dataToSend = ""; //when we hit a line break, we send the data
						
						dataToSend = receiveReader.readLine();
						if (dataToSend != null){
							Log.v(TAG, dataToSend);

							//MainActivity.message.equals(dataToSend);

							//MainActivity.sendMessage();
							//message = BtInterface.dataToSend; // change the message to be the received bt data including sonars
							// sendMessage(); // send the received bt data over signalling

							btSignalled = dataToSend;
							Message msg = handler.obtainMessage();
							Bundle b = new Bundle();
							b.putString("receivedData", dataToSend);
			                msg.setData(b);
			                handler.sendMessage(msg);
			                dataToSend = "";
						}
						
					}
				} 
				catch (IOException e) {
					e.printStackTrace();

				}
				catch(NullPointerException e ){
					e.printStackTrace();
					return;}
			}
		}
	}
	
}
