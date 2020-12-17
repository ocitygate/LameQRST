import java.util.Base64;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

class Connection
{
	class WorkerThread extends Thread
	{
		public void run()
		{
	        try
	        {
	            Socket.setSoTimeout(LameQ.TIME_OUT);
	            
	        	BufferedReader reader = new BufferedReader(new InputStreamReader(Socket.getInputStream(), "UTF-8"));
                OutputStream stream = Socket.getOutputStream();
                //Writer writer = new OutputStreamWriter(stream, "UTF-8");

	            String request = reader.readLine();
	            
	            String[] request_s = request.split(" ");

	            if (request_s.length != 3) return;

	            String[][] matches = { null };
	            if (Helper.preg_match("^\\/(.+)\\/(.+)$", request_s[1], matches))
	            {
	                user_ = matches[0][1];
	                pass = matches[0][2];
	            }

	            String header = reader.readLine();
	            while (!header.equals("") & header != null)
	            {
	                String[] header_s = header.split(":", 2);

	                if (header_s.length == 2)
	                {
	                    String header_name = header_s[0].trim();
	                    String header_value = header_s[1].trim();

	                    switch (header_name)
	                    {
	                        case "Authorization":
	                            String[] authorization = header_value.split(" ");
	                            if (authorization.length == 2 && authorization[0].equals("Basic"))
	                            {
                                    String[] userInfo = new String(Base64.getDecoder().decode(authorization[1]), StandardCharsets.UTF_8).split(":", 2);
                                    user_ = userInfo.length > 0 ? userInfo[0] : "";
                                    pass = userInfo.length > 1 ? userInfo[1] : "";
	                            }
	                            break;
	                    }
	                }

	                header = reader.readLine();
	            }
	            
	            //Authenticate
	            if (user_.equals("")) return;

	            for (int i = 0; ;)
	            {
	                if (Feed.Status_Uplink.equals("ONLINE")) break;

	                if (i == LameQ.TIME_OUT / LameQ.POLL_WAIT) return;
	                Thread.sleep(LameQ.POLL_WAIT);
	                i++;
	            }
	            
	            stream.write(("HTTP/1.1 200 OK\r\n").getBytes("UTF-8"));
	            stream.write(("Content-Type: " + Feed.ContentType + "\r\n").getBytes("UTF-8"));
	            stream.write(("Transfer-Encoding: chunked\r\n").getBytes("UTF-8"));
	            stream.write(("\r\n").getBytes("UTF-8"));

	            long frameNo = Feed.buffer.GetMidFrameNo();
	            if (frameNo == -1)
            	{
            		return;
            	}

	            byte[] data;

	            for (; ; )
	            {
	                try
	                {
	                    while ((data = Feed.buffer.ReadFrame(frameNo)) != null)
	                    {
	        	            stream.write((Integer.toHexString(Feed.buffer.FrameSize) + "\r\n").getBytes("UTF-8"));
	                    	stream.write(data, 0, Feed.buffer.FrameSize);
	        	            stream.write(("\r\n").getBytes("UTF-8"));
	        	            
	        	            setStatus("ONLINE");

	                        frameNo++;
	                    }
	                }
	                catch (Buffer.OverrunException e)
	                {
	                	setStatus("OFFLINE");
	                	return;
	                }

	                Thread.sleep(LameQ.POLL_WAIT);
	            }
	        }
	        catch (Exception e)
	        {
	        }
	        finally
	        {
	            try { Socket.close(); } catch (IOException e) { }
	            
	            Feed.Connections.remove(Connection.this);

	            setStatus("OFFLINE");
	        }
		}
	}
	
    final Feed Feed;
    final Socket Socket;
    String user_ = "";
    String pass = "";

    final Thread tWorker = new WorkerThread();

    public Connection(Feed feed, Socket socket)
    {
        Feed = feed;
        Socket = socket;
        
        tWorker.start();
    }
    
    public String Status = "NEW";
    
    void setStatus(String value)
    {
    	if (!Status.equals(value))
    	{
    		Status = value;
        	LameQ.Log("CONNECTION " + user_ + " " + Socket.getRemoteSocketAddress() + " " + value);
    	}
    }
}