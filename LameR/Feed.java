import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import java.util.concurrent.ConcurrentHashMap;

public class Feed
{
	class WorkerThread extends Thread
	{
		public void run()
		{
			BufferedReader output = new BufferedReader(new InputStreamReader(Process.getInputStream()));
			String line;
			String[] comment = { null };
			try
			{
				while ((line = output.readLine()) != null)
				{
					String[] args = Helper.parseLine(line, comment);
					
					switch(args[1])
					{
						case "FEED":
							switch (args[2])
							{
								case "UPLINK":
									Status_Uplink = args[3];
									LameR.Log("FEED " + Account + " " + Port + " UPLINK " + Status_Uplink);
									break;
								case "DOWNLINK":
									Status_Downlink = args[3];
									LameR.Log("FEED " + Account + " " + Port + " DOWNLINK " + Status_Downlink);
									break;
							}
							break;
						case "CONNECTION":
							String user = args[2];
							String remoteAddress = args[3];
							String status = args[4];
							
							switch (status)
							{
								case "OFFLINE":
									Connections.remove(user + "@" + remoteAddress);
									LameR.Log("FEED " + Account + " " + Port + " CONNECTION " + user + " " + remoteAddress + " " + status);
									break;
								case "ONLINE":
									Connection connection = Connections.get(user + "@" + remoteAddress);
									if (connection == null)
									{
										connection = new Connection(user, remoteAddress);
										Connections.put(user + "@" + remoteAddress, connection);
									}
									connection.Status = status;
									LameR.Log("FEED " + Account + " " + Port + " CONNECTION " + user + " " + remoteAddress + " " + status);
									break;
								default:
									break;
							}
							
							
					}
				}
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
	}
	
	class Connection
	{
		String User;
		String RemoteAddress;

		public Connection(String user, String remoteAddress)
		{
			User = user;
			RemoteAddress = remoteAddress; 
		}

		String Status = "NEW";
	}
	
	public final String Account;
	public final int Port;
	public final String Url;
    public String Comment;
	
	public ConcurrentHashMap<String, Connection> Connections = new ConcurrentHashMap<String, Connection>();
	
    Process Process = null;
    
	//public String Process_Status = "NEW";
    public String Status_Uplink = "NEW";
    public String Status_Downlink = "NEW";
    
    public boolean Removed;

    public Feed(String account, int port, String url, String comment)
	{
		Account = account;
		Port = port;
		Url = url;
		Comment = comment;

		try
		{
			ProcessBuilder processBuilder = new ProcessBuilder("java", "-jar", "LameQ.jar", Integer.toString(Port), Url);
			//processBuilder.directory(new File("C:/Users/Administrator/Desktop/deploy/LameR"));
			//TODO commend above
			
			Process = processBuilder.start();
		} 
		catch (IOException e)
		{
			e.printStackTrace();
			return;
		}

		new WorkerThread().start();
	}
	
    public void Close()
    {
    	Process.destroyForcibly();
    }

}
