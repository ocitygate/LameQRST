import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.format.DateTimeFormatter;
import java.io.FileInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Random;

class LameST
{
	public static final String NAME = "LameST";
	public static final String DESCRIPTION = "Lame maSTer";
	public static final String VERSION = "1.01";
	
    public static final int RECONNECT_WAIT = 3000;
    public static final int REBIND_WAIT = 3000;
    public static final int POLL_WAIT = 200;
    public static final int TIME_OUT = 3000;
    public static final int DELAY_PULL = 10000;

    static Status Status = null;
    static Server Server = null;
    
    static final HashMap<String, Slave> Slaves = new HashMap<String, Slave>();
    static final HashMap<Integer, Feed> Feeds = new HashMap<Integer, Feed>();
    static final List<Feed> Feeds_ASC = new ArrayList<Feed>();
    static final HashMap<String, User> Users = new HashMap<String, User>();

    static final List<Account> qAccounts = new ArrayList<Account>();
    
    static final Random random = new Random();

    static synchronized void config_begin()
    {
        Log("CONFIG BEGIN");

        if (Status != null) Status.Removed = true;
        if (Server != null) Server.Removed = true;

        for (Object slave : Helper.toArray(Slaves))
        {
        	((Slave)slave).Removed = true;
			for(Object account : Helper.toArray(((Slave)slave).Accounts))
    			((Account)account).Removed = true;
        }
    }
        
    static synchronized void touchStatus(int port, String user, String pass)
    {
        if (Status == null)
        {
            Status = new Status(port, user, pass);
            Log("STATUS " + port + " ENABLED");
        }
        else
        {
        	if (Status.Port != port | !Status.User.equals(user) | !Status.Pass.equals(pass))
        	{
            	removeStatus();
            	
                Status = new Status(port, user, pass);
                Log("STATUS " + port + " ENABLED");
        	}
        	else
        	{
                Status.Removed = false;
        	}
        }
    }
    
    static synchronized void touchServer(String host, int port)
    {
        if (Server == null)
        {
            Server = new Server(host, port);
            Log("SERVER " + host + " " + port + " ENABLED");
        }
        else
        {
        	if (!Server.Host.equals(host) | Server.Port != port)
        	{
            	removeServer();
        		
                Server = new Server(host, port);
                Log("SERVER " + host + " " + port + " ENABLED");
        	}
        	else
        	{
                Server.Removed = false;
        	}
        }
    }

    static synchronized Slave touchSlave(String host, int port, String user, String pass)
    {
        Slave slave = Slaves.get(host);
        
        if (slave != null)
        {
            if (!slave.Host.equals(host) | slave.Port != port | !slave.User.equals(user) | !slave.Pass.equals(pass))
            {
            	removeSlave(slave);
            }
            else
            {
            	slave.Removed = false;
            	return slave;
            }
        }
        
        slave = new Slave(host, port, user, pass);
        Slaves.put(host, slave);
    	Log("SLAVE " + host + " ADDED");

    	return slave;
    }
    
    static synchronized void touchAccount(Slave slave, String user, String pass)
    {
        Account account = slave.Accounts.get(user);
        
        if (account != null)
        {
        	if (!account.User.equals(user) | !account.Pass.equals(pass))
        	{
        		removeAccount(account);
        	}
        	else
        	{
        		account.Removed = false;
        		return;
        	}
        }
        
        account = new Account(slave, user, pass);
        
        slave.Accounts.put(user,  account);
        
        LameST.qAccounts.add(account);

    	Log("ACCOUNT " + user + " ADDED");
    }
    
    static synchronized void removeStatus()
    {
        if (Status != null)
        {
            Status.Close();
            Status = null;
        	Log("STATUS " + Status.Port + " REMOVED");
        }
    }
    
    static synchronized void removeServer()
    {
        if (Server != null)
        {
        	Server.Close();
        	Server = null;
        	Log("SERVER " + Server.Host + " " + Server.Port + " REMOVED");
        }        	
    }
    
    static synchronized void removeSlave(Slave slave)
    {
    	slave.Close();

    	for (Object account : Helper.toArray(((Slave)slave).Accounts))
    		removeAccount((Account)account);

    	Slaves.remove(slave.Host);

    	Log("SLAVE " + slave.Host + " REMOVED");
    }
    
    static synchronized void removeAccount(Account account)
    {
        qAccounts.remove(account);
        
        Feed feed = account.Feed;
        
        if (feed != null)
            for (Object user : Helper.toArray(feed.Users))
            	User2Feed((User)user, null);

        Account2Feed((Account)account, null);

        account.Slave.Accounts.remove(account.User);

        Log("ACCOUNT " + account.User + " REMOVED");
    }
    
    static synchronized void config_end()
    {
    	if (Status != null) if (Status.Removed)
    		removeStatus();            
        
    	if (Server != null) if (Server.Removed)
    		removeServer();            
        
    	for (Object slave : Helper.toArray(Slaves))
        	if (((Slave)slave).Removed)
        		removeSlave((Slave)slave);
    		else
    			for(Object account : Helper.toArray(((Slave)slave).Accounts))
        			if (((Account)account).Removed)
        				removeAccount((Account)account);
   	
    	Log("CONFIG END");
    }
    
    static synchronized void config_load()
    {
    	BufferedReader reader;
		try
		{
			reader = new BufferedReader(new InputStreamReader(new FileInputStream("LameST.txt"), "UTF-8"));
		}
		catch (Exception e)
		{
	        Log("CONFIG LOAD ERROR");
	        return;
		}

		config_begin();
		
        Slave slave = null;

        try
        {
	        int line_no = 1;
	        String line;
	        while ((line = reader.readLine()) != null)
	        {
	            String[] comment = { null };
	            String[] args = Helper.parseLine(line, comment);
	
	            if (args.length >= 1)
	            {
	                switch (args[0])
	                {
	                    case "STATUS":
                        	try
                        	{
                                int port = Integer.parseInt(args[1]);
                                String user = args[2];
                                String pass = args[3];
                                
                                touchStatus(port, user, pass);
                            }
                            catch (Exception ex)
                            {
                                Log("CONFIG ERROR LINE " + line_no + " Expected STATUS <port> <user> <pass>");
                            }
                            break;
	                    case "SERVER":
                        	try
                        	{
                        		String host = args[1];
                        		int port = Integer.parseInt(args[2]);
                                
                                touchServer(host, port);
                            }
                            catch (Exception ex)
                            {
	                            Log("CONFIG ERROR LINE " + line_no + " Expected SERVER <host> <port>");
                            }
                            break;
	                    case "SLAVE":
	                    	try
	                    	{
	                            String host = args[1];
	                            int port = Integer.parseInt(args[2]);
	                            String user = args[3];
	                            String pass = args[4];
	                            
	                            slave = touchSlave(host, port, user, pass);
	                    	}
	                    	catch (Exception ex)
	                    	{
	                            Log("CONFIG ERROR LINE " + line_no + " Expected SLAVE <host> <port> <user> <pass>");
	                    	}
	                        break;
	                    case "ACCOUNT":
                            if (slave != null)
                            {
                            	try
                            	{
	                                String user = args[1];
	                                String pass = args[2];
	                                
	                                touchAccount(slave, user, pass);
                            	}
                            	catch (Exception ex)
                            	{
    	                            Log("CONFIG ERROR LINE " + line_no + ". Expected ACCOUNT <user> <pass>");
                            	}
                            }
                            else
                            {
                                Log("CONFIG ERROR LINE " + line_no + " Expected ACCOUNT after SLAVE");
                            }
	                        break;
	                    default:
	                        Log("CONFIG ERROR LINE " + line_no + " Invalid command.");
	                        break;
	                }
	            }
	            line_no++;
	        }
	        reader.close();

	        config_end();
        }
        catch (IOException e)
        {
	        Log("CONFIG LOAD ERROR");
	        return;
        }
    }
    
    static synchronized void users_begin()
    {
        Log("USERS BEGIN");

    	for (Object user : Helper.toArray(Users)) ((User)user).Removed = true;
    }    	

    static synchronized void touchUser(String user_, String pass)
    {
        User user = Users.get(user_);
        
        if (user != null)
        {
            if (!user.Pass.equals(pass))
            {
            	removeUser(user);
            }
            else
            {
            	user.Removed = false;
            	return;
            }
        }
        
        user = new User(user_, pass);
        Users.put(user_, user);
    	Log("USER " + user_ + " ADDED");
    }
    
    static synchronized void removeUser(User user)
    {
        User2Feed(user, null);
        
        Users.remove(user.User_);

        Log("USER " + user.User_ + " REMOVED");
    }

    static synchronized void users_end()
    {
    	for (Object user : Helper.toArray(Users))
        	if (((User)user).Removed)
        		removeUser((User)user);
    	
    	Log("USERS END");
    }
    
    static synchronized void users_load()
    {
    	synchronized(LameST.class)
    	{
        	BufferedReader reader;
    		try
    		{
    			reader = new BufferedReader(new InputStreamReader(new FileInputStream("LameST.users.txt"), "UTF-8"));
    		}
    		catch (Exception e)
    		{
    	        Log("USERS LOAD ERROR");
    	        return;
    		}

    		users_begin();
    		
            try
            {
    	        int line_no = 1;
    	        String line;
    	        while ((line = reader.readLine()) != null)
    	        {
    	            String[] comment = { null };
    	            String[] args = Helper.parseLine(line, comment);
    	
    	            if (args.length >= 1)
    	            {
    	                switch (args[0])
    	                {
    	                    case "USER":
    	                    	try
    	                    	{
    	                            String user = args[1];
    	                            String pass = args[2];
    	                            
    	                            touchUser(user, pass);
    	                    	}
    	                    	catch (Exception ex)
    	                    	{
    	                            Log("CONFIG ERROR LINE " + line_no + " Expected USER <user> <pass>");
    	                    	}
    	                        break;
    	                    default:
    	                        Log("CONFIG ERROR LINE " + line_no + " Invalid command.");
    	                        break;
    	                }
    	            }
    	            line_no++;
    	        }
    	        reader.close();

    	        users_end();
            }
            catch (IOException e)
            {
    	        Log("USERS LOAD ERROR");
    	        return;
            }
    	}
    }
    
	static int feeds_added;
	static int feeds_removed;
	
	static synchronized void feeds_begin()
	{
        Log("FEEDS BEGIN");

        for (Object feed : Helper.toArray(Feeds)) ((Feed)feed).Removed = true;
        
        Feeds_ASC.clear();
        
        feeds_added = 0;
        feeds_removed = 0;
	}
	
	static synchronized void touchFeed(int port, String url, String comment, String bouquet)
	{
        Feed feed = Feeds.get(port);
        
        if (feed != null)
        {
            if (!feed.Url.equals(url))
            {
            	removeFeed(feed);
            }
            else
            {
            	feed.Comment = comment;
            	feed.Bouquet = bouquet;
            	feed.Removed = false;
                
            	Feeds_ASC.add(feed);
            	
            	return;
            }
        }
        
        feed = new Feed(port, url, comment, bouquet);
        Feeds.put(port, feed);
        
        Feeds_ASC.add(feed);
        
        feeds_added++;
	}
	
	static synchronized void removeFeed(Feed feed)
	{
		for (Object user : Helper.toArray(feed.Users))
			User2Feed((User)user, null);

		Feeds.remove(feed.Port);
		
		feeds_removed++;
	}
	
	static synchronized void feeds_end()
	{
        for (Object feed : Helper.toArray(Feeds))
        	if (((Feed)feed).Removed)
        		removeFeed((Feed)feed);
    	Log("FEEDS " + feeds_added + " ADDED, " + feeds_removed + " REMOVED");
    	Log("FEEDS END");
	}
	
	static synchronized void feeds_load()
	{
    	BufferedReader reader;
		try
		{
			reader = new BufferedReader(new InputStreamReader(new FileInputStream("LameST.feeds.txt"), "UTF-8"));
		}
		catch (Exception e)
		{
	        Log("FEEDS LOAD ERROR");
	        return;
		}


		feeds_begin();
		
        String bouquet = "";

        try
        {
	        int line_no = 1;
	        String line;
	        while ((line = reader.readLine()) != null)
	        {
	            String[] comment = { null };
	            String[] args = Helper.parseLine(line, comment);
	
	            if (args.length >= 1)
	            {
	                switch (args[0])
	                {
                        case "BOUQUET":
                            bouquet = comment[0];
                            break;
                        case "FEED":
                        	try
                        	{
                                int port = Integer.parseInt(args[1]);
                                String url = args[2];
                                
                                touchFeed(port, url, comment[0], bouquet);
                        	}
                        	catch (Exception ex)
                        	{
                                Log("FEEDS ERROR LINE " + line_no + " Expected FEED <port> <url> #<comment>");
                        	}
                            break;
	                    default:
	                        Log("FEEDS ERROR LINE " + line_no + " Invalid command");
	                        break;
	                }
	            }
	            line_no++;
	        }
	        reader.close();

	        feeds_end();
        }
        catch (IOException e)
        {
	        Log("FEEDS LOAD ERROR");
	        return;
        }
	}
    
    public static synchronized User Authenticate(String user, String pass)
    {
    	User User = Users.get(user);
        if (User == null) return null;
        if (!User.Pass.equals(pass)) return null;
        return User;
    }

    public static synchronized void Account2Feed(Account account, Feed value)
    {
        if (value != account.Feed)
        {
        	Log((value == null ? "0" : value.Port + " " + value.Comment) + " => " + account.User);

        	if (account.Feed != null)
            {
            	account.Feed.Account = null;
            }
            account.Feed = value;
            if (account.Feed == null)
                LameST.qAccounts.add(account);
            else
            	account.Feed.Account = account;
        }    	
    }
    
    public static synchronized void User2Feed(User user, Feed value)
    {
        if (value != user.Feed)
        {
        	Log((value == null ? "0" : value.Port + " " + value.Comment) + " <= " + user.User_);
        	
            if (user.Feed != null)
            {
            	user.Feed.Users.remove(user);
            	if (user.Feed.Users.size() == 0)
            	{
                	if (user.Feed.Account != null)
                    {
                		Account2Feed(user.Feed.Account, null);
                    }
            	}
            }
            user.Feed = value;
            if (user.Feed != null)
            	user.Feed.Users.add(user);
        }
    }

    public static String Alloc(Feed feed, User user)
    {
    	Account account;

        synchronized(LameST.class)
    	{
        	User2Feed(user, null);

            account = feed.Account;
            
            if (account == null)
            {
            	account = user.Account;
            	if (account != null)
            	{
                	if (qAccounts.contains(account))
                        qAccounts.remove(account);
                	else
                		account = null;
            	}
            }
            
            if (account == null)
            {
            	if (qAccounts.size() > 0)
            	{
                    account = qAccounts.get(random.nextInt(qAccounts.size()));
                    
                    qAccounts.remove(account);

                    user.Account = account;
            	}
            }
            
            if (account == null)
            {
            	//TODO overload
            	return null;
            }
            else 
            {
            	User2Feed(user, feed);
            	Account2Feed(account, feed);
            }
    	}

        account.Slave.Push();
        return "http://" + account.Slave.Host + ":" + feed.Port + "/" + user.User_ + "/" + user.Pass;
    }

    public static void Log(String message)
    {
    	System.out.println(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH:mm:ss.SSS")) + " " + message);
    }

    public static void main(String[] args)
    {
    	System.out.println();
    	String title = NAME + " v" + VERSION + " " + DESCRIPTION;
    	System.out.println(title);
    	System.out.println(new String(new char[title.length()]).replace("\0", "="));
    	System.out.println();

    	config_load();
    	users_load();
    	feeds_load();

        while (true) {
	        try { Thread.sleep(Long.MAX_VALUE);	}
	        catch (InterruptedException e) { return; }
        }
    }
   
}
