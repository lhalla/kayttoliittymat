package bsshared;

import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

public class UserlistInitialiser
{
	private ArrayList<User> users;
	
	public UserlistInitialiser()
	{
		users = new ArrayList<User>();
		
		createUsers();
		saveList();
	}
	
	public void createUsers()
	{
		users.add(new User("admin","admin"));
		users.add(new User("bob","halibut"));
		
	}
	
	public void saveList()
	{
		try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("src/data/users.dat")))
		{
			oos.writeObject(users);
			System.out.println("Userlist successfully initialised.");
		}
		catch (Exception e)
		{
			System.err.println("Userlist initialisation failed.");
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args)
	{
		new UserlistInitialiser();
	}
}
