import java.io.InputStreamReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.BufferedReader;
import java.util.concurrent.ConcurrentHashMap;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

class LameR
{
	public static final String NAME = "LameR";
	public static final String DESCRIPTION = "Lame Restreamer";
	public static final String VERSION = "1.01";

    public static final int RECONNECT_WAIT = 3000;
    public static final int REBIND_WAIT = 3000;
    public static final int POLL_WAIT = 200;
    public static final int TIME_OUT = 10000;
    public static final int BUFFER_FRAMES = 320;
    public static final int BUFFER_FRAME_SIZE = 65536;
    public static final int DELAY_FRAMES = 64;

    public static Status Status = null;
    public static ConcurrentHashMap<String, Feed> Feeds = new ConcurrentHashMap<String, Feed>();
    public static ConcurrentHashMap<String, User> Users = new ConcurrentHashMap<String, User>();

    static class config
    {
        static void begin()
        {
	        Log("CONFIG BEGIN");

	        if (Status != null) Status.Removed = true;
        	for (Object feed : Helper.toArray(Feeds)) ((Feed)feed).Removed = true;
        	for (Object user : Helper.toArray(Users)) ((User)user).Removed = true;
        }

        static void touchStatus(int port, String user, String pass)
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
        
        static void touchFeed(String account, int port, String url, String comment)
        {
            Feed feed = Feeds.get(account);
            
            if (feed != null)
            {
                if (feed.Port != port | !feed.Url.equals(url))
                {
                	removeFeed(feed);
                }
                else
                {
                	feed.Removed = false;
                	feed.Comment = comment;
                	return;
                }
            }
            
            feed = new Feed(account, port, url, comment);
            Feeds.put(account, feed);
        	Log("FEED " + account + " " + port + " ADDED");
        }

        static void touchUser(String user_, String pass, int port)
        {
            User user = Users.get(user_);
            
            if (user != null)
            {
                if (!user.Pass.equals(pass) | user.Port != port)
                {
                	removeUser(user);
                }
                else
                {
                	user.Removed = false;
                	return;
                }
            }

            user = new User(user_, pass, port);
            Users.put(user_, user);
        	Log("USER " + user_ + " " + port + " ADDED");
        }
        
        static void removeStatus()
        {
            if (Status != null)
            {
	            Status.Close();
	            Status = null;
            	Log("STATUS " + Status.Port + " REMOVED");
            }
        }
        
        static void removeFeed(Feed feed)
        {
        	feed.Close();
        	Feeds.remove(feed.Account);
        	Log("FEED " + feed.Account + " " + feed.Port + " REMOVED");
        }
        
        static void removeUser(User user)
        {
        	Users.remove(user.User_);
        	Log("USER " + user.User_ + " " + user.Port + " REMOVED");
        }
        
        static void end()
        {
        	if (Status != null) if (Status.Removed)
        		removeStatus();            
            
        	for (Object feed : Helper.toArray(Feeds))
            	if (((Feed)feed).Removed)
            		removeFeed((Feed)feed);

        	for (Object user : Helper.toArray(Users))
            	if (((User)user).Removed)
            		removeUser((User)user);

        	Log("CONFIG END");
        }

        public synchronized static void load()
        {
        	BufferedReader reader;
    		try
    		{
    			reader = new BufferedReader(new InputStreamReader(new FileInputStream("LameR.txt"), "UTF-8"));
    		}
    		catch (Exception e)
    		{
    	        Log("CONFIG LOAD ERROR");
    	        return;
    		}

            try
            {
                begin();

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
                            case "FEED":
                            	try
                            	{
                            		String account = args[1];
                                    int port = Integer.parseInt(args[2]);
                                    String url = args[3];

                                    touchFeed(account, port, url, comment[0]);
                            	}
                            	catch (Exception ex)
                            	{
                            		Log("CONFIG ERROR LINE " + line_no + " Expected FEED <port> <url> #<comment>");
                                }
                                break;
                            case "USER":
    							try
    							{
                                    String user_ = args[1];
                                    String pass = args[2];
                                    int port = Integer.parseInt(args[3]);
                                    
                                    touchUser(user_, pass, port);
                                }
    							catch (Exception ex)
    							{
                            		Log("CONFIG ERROR LINE " + line_no + " Expected USER <user> <pass> <port>");
                                }
                                break;
                            default:
                                Log("CONFIG ERROR LINE " + line_no + " Invalid command");
                                break;
                        }
                    }
                    line_no++;
                }
                reader.close();
                
                end();
        	}
            catch (IOException e)
            {
    	        Log("CONFIG LOAD ERROR");
    	        return;
            }
        }
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

        config.load();

        while (true) {
	        try { Thread.sleep(Long.MAX_VALUE);	}
	        catch (InterruptedException e) { return; }
        }
    }
}