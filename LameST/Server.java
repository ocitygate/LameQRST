import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLEncoder;

class Server
{
	class WorkerThread extends Thread
	{
		public void run()
		{
            for (; ; )
            {
                try
                {
    				server = new ServerSocket(Port, 1024);
                    break;
                }
                catch (IOException e)
                {
                	LameST.Log("SERVER BIND FAILED");
                }
                
                try { Thread.sleep(LameST.REBIND_WAIT); }
                catch (InterruptedException e) { return; }
            }

	        for (; ; )
	        {
		        Socket socket;
				try	{ socket = server.accept();	}
				catch (IOException e) {	return; }
				
		        new ProcessRequestThread(socket).start();
	        }
		}		
	}
	
	class ProcessRequestThread extends Thread
	{
		Socket Socket;
		
		ProcessRequestThread(Socket socket)
		{
			Socket = socket;
		}
		
		public void run()
		{
	        try
	        {
	            Socket.setSoTimeout(LameST.TIME_OUT);

	            InputStream stream = Socket.getInputStream();
				Writer writer = new OutputStreamWriter(Socket.getOutputStream(), "UTF-8");

				String request = Helper.readLine(stream);
				if (request == null) return;
				
	            String[] request_s = request.split(" ");

	            if (request_s.length != 3)
	            {
	                return;
	            }

	            String header = Helper.readLine(stream);
	            while (!header.equals("") & header != null)
	            {
	                String[] header_s = header.split(":", 2);

	                if (header_s.length == 2)
	                {
	                    //String header_name = header_s[0].trim();
	                    //String header_value = header_s[1].trim();
	                }

	                header = Helper.readLine(stream);
	            }


	            String[][] matches = { null };
	            //LameST.Log(Socket.getRemoteSocketAddress() + " " + request_s[1]);
	            if (Helper.preg_match("^\\/(.+)\\/(.+)\\/m3u$", request_s[1], matches))
	            {
	                String user_ = matches[0][1];
	                String pass = matches[0][2];

	                User user = LameST.Authenticate(user_, pass);
	                if (user == null) return;

	                writer.write("HTTP/1.1 200 OK\n");
	                writer.write("Content-Type: audio/x-mpegurl\n");
	                writer.write("Content-Disposition: attachment; filename=\"iptv_" + user.User_ + ".m3u\"\n");
	                writer.write("\n");
	                writer.write("#EXTM3U\n");

	                for (Object feed : LameST.Feeds_ASC)
	                {
	                    writer.write("#EXTINF:0 group-title=\"" + ((Feed)feed).Bouquet + "\"," + ((Feed)feed).Comment + "\n");
	                    writer.write("http://" + Host + ":" + Port + "/" + user.User_ + "/" + user.Pass + "/" + ((Feed)feed).Port + "\n");
	                }

	                writer.flush();
	            }
	            else if (Helper.preg_match("^\\/(.+)\\/(.+)\\/enigma216_script$", request_s[1], matches))
	            {
	                String user_ = matches[0][1];
	                String pass = matches[0][2];

	                User user = LameST.Authenticate(user_, pass);
	                if (user == null) return;

	                writer.write("HTTP/1.1 200 OK\n");
	                //writer.write("Content-Type: audio/x-mpegurl\n");
	                //writer.write("Content-Disposition: attachment; filename=\"iptv_" + user.User_ + ".m3u\"\n");
	                writer.write("\n");
	                writer.write("USERNAME=\"" + user_ + "\";PASSWORD=\"" + pass + "\";bouquet=\"IPTV_OTT_IPTV\";directory=\"/etc/enigma2/iptv.sh\";url=\"http://" + Host +  ":" + Port + "/" + user.User_ + "/" + user.Pass + "/enigma16\";rm /etc/enigma2/userbouquet.\"$bouquet\"__tv_.tv;wget -O /etc/enigma2/userbouquet.\"$bouquet\"__tv_.tv $url;if ! cat /etc/enigma2/bouquets.tv | grep -v grep | grep -c $bouquet > /dev/null;then echo \"[+]Creating Folder for iptv and rehashing...\";cat /etc/enigma2/bouquets.tv | sed -n 1p > /etc/enigma2/new_bouquets.tv;echo '#SERVICE 1:7:1:0:0:0:0:0:0:0:FROM BOUQUET \"userbouquet.'$bouquet'__tv_.tv\" ORDER BY bouquet' >> /etc/enigma2/new_bouquets.tv; cat /etc/enigma2/bouquets.tv | sed -n '2,$p' >> /etc/enigma2/new_bouquets.tv;rm /etc/enigma2/bouquets.tv;mv /etc/enigma2/new_bouquets.tv /etc/enigma2/bouquets.tv;fi;rm /usr/bin/enigma2_pre_start.sh;echo \"writing to the file.. NO NEED FOR REBOOT\";echo \"/bin/sh \"$directory\" > /dev/null 2>&1 &\" > /usr/bin/enigma2_pre_start.sh;chmod 777 /usr/bin/enigma2_pre_start.sh;wget -qO - \"http://127.0.0.1/web/servicelistreload?mode=2\";wget -qO - \"http://127.0.0.1/web/servicelistreload?mode=2\";");
	                writer.flush();
	            }
	            else if (Helper.preg_match("^\\/(.+)\\/(.+)\\/enigma22_script$", request_s[1], matches))
	            {
	                String user_ = matches[0][1];
	                String pass = matches[0][2];

	                User user = LameST.Authenticate(user_, pass);
	                if (user == null) return;

	                writer.write("HTTP/1.1 200 OK\n");
	                //writer.write("Content-Type: audio/x-mpegurl\n");
	                //writer.write("Content-Disposition: attachment; filename=\"iptv_" + user.User_ + ".m3u\"\n");
	                writer.write("\n");
	                writer.write("USERNAME=\"" + user_ + "\";PASSWORD=\"" + pass + "\";bouquet=\"IPTV_OTT_IPTV\";directory=\"/etc/enigma2/iptv.sh\";url=\"http://" + Host +  ":" + Port + "/" + user.User_ + "/" + user.Pass + "/dreambox" + "\";rm /etc/enigma2/userbouquet.\"$bouquet\"__tv_.tv;wget -O /etc/enigma2/userbouquet.\"$bouquet\"__tv_.tv $url;if ! cat /etc/enigma2/bouquets.tv | grep -v grep | grep -c $bouquet > /dev/null;then echo \"[+]Creating Folder for iptv and rehashing...\";cat /etc/enigma2/bouquets.tv | sed -n 1p > /etc/enigma2/new_bouquets.tv;echo '#SERVICE 1:7:1:0:0:0:0:0:0:0:FROM BOUQUET \"userbouquet.'$bouquet'__tv_.tv\" ORDER BY bouquet' >> /etc/enigma2/new_bouquets.tv; cat /etc/enigma2/bouquets.tv | sed -n '2,$p' >> /etc/enigma2/new_bouquets.tv;rm /etc/enigma2/bouquets.tv;mv /etc/enigma2/new_bouquets.tv /etc/enigma2/bouquets.tv;fi;rm /usr/bin/enigma2_pre_start.sh;echo \"writing to the file.. NO NEED FOR REBOOT\";echo \"/bin/sh \"$directory\" > /dev/null 2>&1 &\" > /usr/bin/enigma2_pre_start.sh;chmod 777 /usr/bin/enigma2_pre_start.sh;wget -qO - \"http://127.0.0.1/web/servicelistreload?mode=2\";wget -qO - \"http://127.0.0.1/web/servicelistreload?mode=2\";");
	                writer.flush();
	            }
	            else if (Helper.preg_match("^\\/(.+)\\/(.+)\\/enigma16$", request_s[1], matches))
	            {
	                String user_ = matches[0][1];
	                String pass = matches[0][2];

	                User user = LameST.Authenticate(user_, pass);
	                if (user == null) return;

	                writer.write("HTTP/1.1 200 OK\n");
	                //writer.write("Content-Type: audio/x-mpegurl\n");
	                writer.write("Content-Disposition: attachment; filename=\"userbouquet.favourites.tv\"\n");
	                writer.write("\n");
	                writer.write("#NAME IPTV_OTT_IPTV\n");

	                for (Object feed : LameST.Feeds_ASC)
	                {
		                writer.write("#NAME IPTV_OTT_IPTV\n");
		                writer.write("#SERVICE 4097:0:1:0:0:0:0:0:0:0:" + URLEncoder.encode("http://" + Host + ":" + Port + "/" + user.User_ + "/" + user.Pass + "/" + ((Feed)feed).Port, "UTF-8") + "\n");
		                writer.write("#DESCRIPTION " + ((Feed)feed).Comment + "\n");
	                }

	                writer.flush();
	            }
	            else if (Helper.preg_match("^\\/(.+)\\/(.+)\\/dreambox$", request_s[1], matches))
	            {
	                String user_ = matches[0][1];
	                String pass = matches[0][2];

	                User user = LameST.Authenticate(user_, pass);
	                if (user == null) return;

	                writer.write("HTTP/1.1 200 OK\n");
	                //writer.write("Content-Type: audio/x-mpegurl\n");
	                writer.write("Content-Disposition: attachment; filename=\"userbouquet.favourites.tv\"\n");
	                writer.write("\n");
	                writer.write("#NAME IPTV_OTT_IPTV\n");

	                for (Object feed : LameST.Feeds_ASC)
	                {
		                writer.write("#NAME IPTV_OTT_IPTV\n");
		                writer.write("#SERVICE 1:0:1:0:0:0:0:0:0:0:" + URLEncoder.encode("http://" + Host + ":" + Port + "/" + user.User_ + "/" + user.Pass + "/" + ((Feed)feed).Port, "UTF-8") + "\n");
		                writer.write("#DESCRIPTION " + ((Feed)feed).Comment + "\n");
	                }

	                writer.flush();
	            }
	            else if (Helper.preg_match("^\\/(.+)\\/(.+)\\/(\\d+)$", request_s[1], matches))
	            {
	                String user_ = matches[0][1];
	                String pass = matches[0][2];
	                int port = Integer.parseInt(matches[0][3]);

	                User user = LameST.Authenticate(user_, pass);
	                if (user == null) return;
	                Feed feed = LameST.Feeds.get(port);

	                String Location;
	                if ((Location = LameST.Alloc(feed, user)) != null)
	                {
	                    writer.write("HTTP/1.1 302 Found\n");
	                    writer.write("Location: " + Location + "\n");
	                    writer.write("\n");
	                    writer.flush();
	                }
	            }
	        }
	        catch (Exception e)
	        {
	        	//e.printStackTrace();
	        }
	        finally
	        {
	            try { Socket.close(); } catch (IOException e) { }
	        }
		}
	}
	
	public final String Host;
    public final int Port;

    public boolean Removed;
    
    ServerSocket server;

    Thread tWorker;

    public Server(String host, int port)
    {
        Host = host;
        Port = port;

        tWorker = new WorkerThread();
        tWorker.start();
    }

    public void Close()
    {
        tWorker.interrupt();

        try { server.close(); } catch (IOException e) { }
    }
}