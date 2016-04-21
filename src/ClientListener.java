/**
 * Nakov Chat Server - (c) Svetlin Nakov, 2002
 *
 * ClientListener class is purposed to listen for client messages and
 * to forward them to ServerDispatcher.
 */

import java.io.*;
import java.net.*;
import java.util.Scanner;

public class ClientListener extends Thread
{
	private ServerDispatcher mServerDispatcher;
	private ClientInfo mClientInfo;
	private BufferedReader mIn;
	private boolean trigger;
	private File profList;
	private File tabooList;

	public ClientListener(ClientInfo aClientInfo, ServerDispatcher aServerDispatcher)
	throws IOException
	{
		mClientInfo = aClientInfo;
		mServerDispatcher = aServerDispatcher;
		Socket socket = aClientInfo.mSocket;
		mIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		profList = new File("ProfanityList.txt");
		tabooList = new File("TabooList.txt");
	}

	/**
	 * Until interrupted, reads messages from the client socket, forwards them
	 * to the server dispatcher's queue and notifies the server dispatcher.
	 */
	public void run()
	{
		try {
			while (!isInterrupted()) 
			{
				String message = mIn.readLine();
				trigger = false;
				Scanner sc = new Scanner(tabooList);
				while(trigger==false & sc.hasNextLine()) 
				{
					String aWord = sc.nextLine();
					if (message.toLowerCase().contains(aWord)) 
					{
						message = "USER HAS USED TABOO STATEMENTS. SERVER SHUTTING DOWN.";
						mClientInfo.mClientSender.sendMessage(message);
						mServerDispatcher.dispatchMessage(mClientInfo, message);
						mServerDispatcher.shutdown();
						trigger = true;
					}
				}
				sc.close();
				sc = new Scanner(profList);
				while(trigger==false & sc.hasNextLine()) 
				{
					String aWord = sc.nextLine();
					if (message.toLowerCase().contains(aWord)) 
					{
						message = "USER HAS BEEN DISCONNECTED FOR STRONG LANGUAGE";
						mClientInfo.mClientSender.sendMessage(message);
						mServerDispatcher.dispatchMessage(mClientInfo, message);
						trigger = true;
					}
				}
				sc.close();
				if (message == null | trigger == true)
					break;
				mServerDispatcher.dispatchMessage(mClientInfo, message);
			}
		} catch (IOException ioex) {
			// Problem reading from socket (communication is broken)
		}

		// Communication is broken. Interrupt both listener and sender threads
		mClientInfo.mClientSender.interrupt();
		mServerDispatcher.deleteClient(mClientInfo);
	}

}