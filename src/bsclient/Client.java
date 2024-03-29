package bsclient;

import bsshared.*;
import messages.*;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;

public class Client
{
	private ClientGUI cgui;	// the GUI this client is tied to.
	
	protected User user;
	
	private ObjectInputStream streamIn;	// object stream for incoming communication
	private ObjectOutputStream streamOut;	// object stream for outgoing communication
	private Socket socket;	// the socket of the connection
	
	private final String server = "localhost";	// server name
	private final int port = 8800;	// server port
	
	protected ArrayList<Train> trainList;
	
	/**
	 * Constructor.
	 * @param cgui the ClientGUI this Client instance is tied to.
	 */
	public Client(ClientGUI cgui)
	{
		this.cgui = cgui;
	}
	
	/**
	 * Starts the client. Opens a socket and in/out object streams with the server.
	 * @return true if the client was started successfully.
	 */
	public boolean start()
	{
		// Try to open a socket.
		try
		{
			socket = new Socket(server, port);
		}
		catch (Exception e)
		{
			System.err.println("Failed to open socket.");
			return false;
		}
		
		// Try to open object streams.
		try
		{
			streamIn = new ObjectInputStream(socket.getInputStream());
			streamOut = new ObjectOutputStream(socket.getOutputStream());
		}
		catch (IOException ioe)
		{
			System.err.println("Failed to open object streams.");
			ioe.printStackTrace();
			return false;
		}
		
		return true;
	}
	
	/**
	 * Authenticates the user.
	 * @param username username given through the login prompt.
	 * @param password password given through the login prompt.
	 * @return true if login was successful.
	 */
	protected boolean authenticate(String username, String password)
	{
		// Create a User object using the given credentials.
		this.user = new User(username, password);
		
		// Send the User object to the server to check if they were correct.
		try
		{
			streamOut.writeObject(user);
			streamOut.flush();
			boolean res = streamIn.readBoolean();
			if (res)
				user = (User)streamIn.readObject();
			else
				user = null;
			return res;
		}
		catch (IOException ioe)
		{
			System.err.println("Exception when trying to send User object.");
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;
	}
	
	/**
	 * Retrieves the up-to-date list of trains.
	 */
	protected void fetchTrains()
	{
		try
		{
			streamOut.writeObject("getTrains");
			streamOut.flush();
			trainList = (ArrayList<Train>)streamIn.readObject();
		}
		catch (IOException ioe)
		{
			ioe.printStackTrace();
		}
		catch (ClassNotFoundException cnfe)
		{
			cnfe.printStackTrace();
		}
	}
	
	/**
	 * Creates a new user with the given credentials.
	 * @param username username given through the login prompt.
	 * @param password password given through the login prompt.
	 * @return true if the account creation was successful.
	 */
	protected boolean createNewUser(String username, String password)
	{
		// Create a NewUser object using the given credentials.
		NewUser nuser = new NewUser(username, password);
		
		// Send the NewUser object to the server to check if the name was already taken.
		try
		{
			streamOut.writeObject(nuser);
			streamOut.flush();
			boolean res = streamIn.readBoolean();
			if (res)
				user = (User)streamIn.readObject();
			else
				user = null;
			return res;
		}
		catch (IOException ioe)
		{
			System.err.println("Exception when trying to send NewUser object.");
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return false;
	}
	
	/**
	 * Updates the user profile on the server.
	 */
	private void updateUser()
	{
		
		if (socket != null && !socket.isClosed())
		{
			try
			{
				while (true)
				{
					streamOut.writeObject(user);
					streamOut.flush();
					if (streamIn.readBoolean()) break;
				}
			}
			catch (IOException ioe) {}
		}
	}
	
	/**
	 * Logs out the user.
	 */
	protected void logout()
	{
		boolean logOutFromLoginScreen = true;
		if(user != null){
			logOutFromLoginScreen=false;
		}
		
		if (socket != null && !socket.isClosed())
		{
			try
			{
				if (user != null)
					updateUser();
				streamOut.writeObject(new Logout());
				streamOut.flush();
				
			}
			catch (IOException ioe)	{}

		}
	}
}
