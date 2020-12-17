import java.util.HashMap;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Base64;
import java.net.URLEncoder;
import java.util.List;
import java.util.ArrayList;

class Slave
{
	
	boolean flag;
	
	class WorkerThread extends Thread
	{
		public void run()
		{
			for ( ; ; )
			{
				Push();
				
				do
				{
					flag = false;
					try
					{
						Thread.sleep(LameST.DELAY_PULL);
					}
					catch (InterruptedException e)
					{
						return;
					}
				}
				while(flag);
				
				Pull();
			}
		}
	}
	
    public final String Host;
    public final int Port;
    public final String User;
    public final String Pass;

    public final HashMap<String, Account> Accounts = new HashMap<String, Account>();

    public boolean Removed;

    Thread tWorker;

    public Slave(String host, int port, String user, String pass)
    {
        Host = host;
        Port = port;
        User = user;
        Pass = pass;

        tWorker = new WorkerThread();
        tWorker.start();
    }

    public void Pull()
    {
        Socket socket = null;

        List<String> lines = new ArrayList<String>(); 

        try
        {
            socket = new Socket(Host, Port);

            InputStream stream = socket.getInputStream();
            Writer writer = new OutputStreamWriter(socket.getOutputStream(), "UTF-8");
            
	        //stream.ReadTimeout = LameR.TIME_OUT;
	        //stream.WriteTimeout = LameR.TIME_OUT;

            writer.write("GET /aggregate HTTP/1.1\n");
            writer.write("Authorization: Basic " + Base64.getEncoder().encodeToString((User + ":" + Pass).getBytes("UTF-8")) + "\n");
            writer.write("\n");
            writer.flush();

            //String response =
            Helper.readLine(stream);

            String header = Helper.readLine(stream);
            while (!header.equals("") & header != null)
            {
                String[] header_s = header.split(":", 2);

                if (header_s.length == 2)
                {
                    //String header_name = header_s[0].trim();
                    //String header_value = header_s[1].trim();

                    //switch (header_name)
                    //{
                    //}
                }

                header = Helper.readLine(stream);
            }
            
            String line;
            while ((line = Helper.readLine(stream)) != null)
            	lines.add(line);

        }
        catch (Exception ex)
        {
        }
        finally
        {
            if (socket != null)
            {
                try { socket.close(); } catch (IOException e) { }
            }
        }
        
        synchronized (LameST.class)
        {
            for (Object account : Helper.toArray(Accounts))
            {
                Feed feed = ((Account)account).Feed;
                if ( feed != null)
                {
                	for(Object user : Helper.toArray(feed.Users))
                	{
                        ((User)user).Disconnected = true;
                	}
                }
            }
            
            for(String line : lines)
            {
                String[] comment = { null };
                String[] args = Helper.parseLine(line, comment);

                if (args.length >= 1)
                {
                    switch (args[0])
                    {
                        case "CONNECTION":

                            int port = Integer.parseInt(args[1]);
                            String user = args[2];
                            //String remoteAddress = args[3];
                            //String status = args[4];

                            User User = LameST.Users.get(user);
                            if (User != null)
                            {
                            	Feed feed = User.Feed;
                            	if (feed != null)
                            	{
                                    if (feed.Port == port)
                                    {
                                    	User.Disconnected = false;
                                    }
                            	}
                            }
                            break;

                        default:

                            break;
                    }
                }
            }

            for(Object account : Helper.toArray(Accounts))
            {
                Feed feed = ((Account)account).Feed;
                if (feed != null)
                {
                	for (Object user : Helper.toArray(feed.Users))
                	{
                        if (((User)user).Disconnected == true)
                        {
                        	LameST.User2Feed((User)user, null);
                        }
                	}
                }
            }
        }
    }
    
    public void Push()
    {
    	flag = true;
        
        Socket socket = null;

        try
        {
			socket = new Socket();
            socket.setSoTimeout(LameST.TIME_OUT);
            socket.connect(new InetSocketAddress(Host, Port), LameST.TIME_OUT);
            
            InputStream stream = socket.getInputStream();
            Writer writer = new OutputStreamWriter(socket.getOutputStream(), "UTF-8");
            
            StringBuilder sbConfig = new StringBuilder();
            sbConfig.append("STATUS " + Port + " " + User + " " +  Pass + "\n");
            sbConfig.append("\n");
            
            for(Object account : Helper.toArray(Accounts))
            {
            	Feed feed = ((Account)account).Feed;
                if (feed != null)
                {
                    String url = feed.Url;
                    url = url.replace("$(user)", ((Account)account).User);
                    url = url.replace("$(pass)", ((Account)account).Pass);

                    sbConfig.append("FEED " + ((Account)account).User + " " + feed.Port + " " + url + " # " + feed.Comment + "\n");

                    for (Object user : feed.Users)
                    {
                        sbConfig.append("USER " + ((User)user).User_ + " " + ((User)user).Pass + " " + feed.Port + "\n");
                    	
                    }
                }
            }

            String content = "config=" + URLEncoder.encode(sbConfig.toString(), "UTF-8");

            writer.write("POST /config HTTP/1.1\n");
            writer.write("Content-Length: " + content.length() + "\n");
            writer.write("Content-Type: application/x-www-form-urlencoded\n");
            writer.write("Authorization: Basic " + Base64.getEncoder().encodeToString((User + ":" + Pass).getBytes("UTF-8")) + "\n");
            writer.write("\n");
            writer.write(content);
            writer.flush();

            //Wait for Response
            //String response =
            Helper.readLine(stream); //ignore response
        }
        catch (Exception ex)
        {
        }
        finally
        {
            if (socket != null)
            {
                try { socket.close(); } catch (IOException e) { }
            }
        }
    }

    public void Close()
    {
        tWorker.interrupt();
    }
}