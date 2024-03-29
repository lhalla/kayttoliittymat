package bsserver;

import bsshared.*;
import messages.*;

import java.io.*;
import java.net.*;

// A thread for each client.
public class ClientThread extends Thread
{
	private Socket socket;	// client's socket
	private ObjectInputStream streamIn;	// object stream for incoming communication
	private ObjectOutputStream streamOut;	// object stream for outgoing communication
	
	private final Server master;	// the master (owner) of this thread
	protected final int id;	// connection id
	
	private User user;	// user logged in
	
	/**
	 * Constructor.
	 * @param master the Server owning of this ClientThread.
	 * @param socket the socket used to communicate with the client.
	 * @param id the unique id of this ClientThread.
	 */
	public ClientThread(Server master, Socket socket, int id)
	{
		master.displayEvent("New ClientThread (" + id + ") started for connection (" + socket.getInetAddress().toString() + ":" + socket.getPort() + ").");
		this.master = master;
		this.id = id;
		this.socket = socket;
	}
	
	/**
	 * Runs the server.
	 */
	public void run()
	{
		// Establish a connection with the client
		try
		{
			// Start the in/out streams.
			streamOut = new ObjectOutputStream(socket.getOutputStream());
			streamIn = new ObjectInputStream(socket.getInputStream());
			
			// LOOP0: Loop until the client has been verified or the log out.
			while (true)
			{
				// Read an object from the incoming stream.
				Object incobj = streamIn.readObject();
				
				// If the client tries to create a new user account, check if the username is taken.
				if (incobj instanceof NewUser)
				{
					user = (User) incobj;	// convert the incoming object into a User.
					
					if (!master.usernameTaken(user))
					{
						// Try to add a new user.
						boolean res = master.addNewUser(user);
						
						// Send the result. If the attempt was successful, break from LOOP0.
						streamOut.writeBoolean(res);
						streamOut.flush();
						if (res)
						{
							streamOut.writeObject(user);
							streamOut.flush();
							break;
						}
					}
					else
					{
						// Send a negative response.
						streamOut.writeBoolean(false);
						streamOut.flush();
						
						master.displayEvent("CT" + id + ": Attempt to create a new user '" + user.getUsername()
							+ "' failed: Username already taken (" + ClientThread.getClientAddress(socket) + ").");
					}
				}
				// If the client logs out or closes the login window, end the thread.
				else if (incobj instanceof Logout)
				{
					master.displayEvent("CT" + id + ": Connection closed: LOGOUT.");
					master.remove(id);
					close();
				}
				// Otherwise check if the credentials are correct.
				else
				{
					user = (User) incobj;	// convert the incoming object into a User.
					
					// Display a login attempt in the event log.
					master.displayEvent("CT" + id + ": Incoming login attempt from user '" 
							+ user.getUsername() + "' (" + ClientThread.getClientAddress(socket) + ").");
					
					// If the user credentials are correct, set the thread's user.
					if (master.users.contains(user))
					{
						// LOOP2: Go through all the users to try to find a match.
						for (User u : master.users)
						{
							// If a match is found, set the user and break out of LOOP2.
							if (u.equals(user))
							{
								user = u;
								break;
							}
						}
						
						// Send a positive response (valid credentials) and break out of LOOP0.
						streamOut.writeBoolean(true);
						streamOut.flush();
						streamOut.writeObject(user);
						streamOut.flush();
						break;
					}
					
					// Send a negative response (invalid credentials).
					streamOut.writeBoolean(false);
					streamOut.flush();
					master.displayEvent("CT" + id + ": Failed login attempt from user '" + user.getUsername() + "' (" + ClientThread.getClientAddress(socket) + ").");
				}
			}
			
			// Display a successful login in the event log.
			master.displayEvent("CT" + id + ": Successful login from user '" + user.getUsername() + "' (" + ClientThread.getClientAddress(socket) + ").");
			
			while(true){
				// Read an object from the incoming stream.
				Object objIn = streamIn.readObject();
				
				// If the user is logging out, remove this thread.
				if (objIn instanceof Logout)
				{
					master.displayEvent("CT" + id + ": Connection closed: LOGOUT.");
					break;
				}
				// If the user profile is to be updated, forward the action to the server.
				else if (objIn instanceof User)
				{
					boolean res = master.updateUser(this.user, (User)objIn);
					master.displayEvent("CT" + id + ": User update " + (res ? "successful." : "failed."));
					streamOut.writeBoolean(res);
					streamOut.flush();
				}
				// String commands.
				else if (objIn instanceof String)
				{
					String cmd = (String)objIn;
					
					if (cmd.equals("getTrains"))
					{
						streamOut.writeObject(master.trains);
						streamOut.flush();
					}
				}
			}
		}
		catch (IOException ioe)
		{
			return;
		}
		catch (ClassNotFoundException cnfe)	{}
		
		// Finally remove this thread from the master's ClientThreads and close it.
		master.remove(id);
		close();
	}
	
	/**
	 * Closes the streams and the socket.
	 */
	protected void close()
	{
		try
		{
			if (streamOut != null)
				streamOut.close();
		}
		catch (Exception e) {}
		
		try
		{
			if (streamIn != null)
				streamIn.close();
		}
		catch (Exception e) {}
		
		try
		{
			if (socket != null)
				socket.close();
		}
		catch (Exception e) {}
	}
	
	private static String getClientAddress(Socket socket)
	{
		return socket.getInetAddress().toString() + ":" + socket.getPort();
	}
}
