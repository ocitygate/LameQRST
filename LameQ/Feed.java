import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import java.util.Base64;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.net.InetSocketAddress;

class Feed
{
	class ClientWorkerThread extends Thread
	{
		public void run()
		{
	        for (; ; )
	        {
	            Socket socket = null;

	            try
	            {
	                String url_ = Url;

	                String Location = "";
	                String TransferEncoding = "";

	                InputStream stream;
	                
					do
					{
						URL url = new URL(url_);

						socket = new Socket();
			            socket.setSoTimeout(LameQ.TIME_OUT);
			            socket.connect(new InetSocketAddress(url.getHost(), url.getPort()), LameQ.TIME_OUT);
			            
		                Writer writer = new OutputStreamWriter(socket.getOutputStream(), "UTF-8");
		                stream = socket.getInputStream();
		                
		                writer.write("GET " + url.getFile() + " HTTP/1.1\n");
		                writer.write("Host: " + url.getHost() + ":" + url.getPort() + "\n");
		                writer.write("Accept: */*\n");
		                if (url.getUserInfo() != null)
		                	writer.write("Authorization: Basic " + Base64.getEncoder().encodeToString(url.getUserInfo().getBytes("UTF-8")) + "\n");
		                writer.write("\n");
		                writer.flush();
		                
		                //String response = 
		                Helper.readLine(stream); //ignore response
		                //LameQ.Log("F:" + response);
		                
		                Location = "";
		                TransferEncoding = "";
		                ContentType = "";

		                String header = Helper.readLine(stream);
		                while (!header.equals("") & header != null)
		                {
			                //LameQ.Log("F:" + header);

			                String[] header_s = header.split(":", 2);

		                    if (header_s.length == 2)
		                    {
		                        String header_name = header_s[0].trim();
		                        String header_value = header_s[1].trim();

		                        switch (header_name)
		                        {
		                            case "Location":
		                                Location = header_value;
		                                break;
		                            case "Transfer-Encoding":
		                                TransferEncoding = header_value;
		                                break;
		                            case "Content-Type":
		                                ContentType = header_value;
		                                break;
		                        }
		                    }

			                header = Helper.readLine(stream);
		                }

		                if (!Location.equals(""))
		                {
		                    socket.close();
		                    url_ = Location;
		                }
					}
					while(!Location.equals(""));

	                if (TransferEncoding.equals("chunked"))
	                {
	                    byte[] frame = buffer.GetNextFrame();
	                    int i = 0;

	                    int chunkSize = Integer.parseInt(Helper.readLine(stream), 16);
	                    int j = 0;

	                    for (; ; )
	                    {
	                    	if (Thread.interrupted()) return;
	                        int bytes = stream.read(frame, i, Math.min(LameQ.BUFFER_FRAME_SIZE - i, chunkSize - j));

	                        if (bytes == 0) break;

	                        i += bytes;
	                        j += bytes;

	                        if (i == LameQ.BUFFER_FRAME_SIZE) //end of frame
	                        {
	                            buffer.FrameWritten();
	                            setStatus_Uplink("ONLINE");
	                            frame = buffer.GetNextFrame();
	                            i = 0;
	                        }
	                        if (j == chunkSize) //end of chunk
	                        {
	                            Helper.readLine(stream);
	    	                    chunkSize = Integer.parseInt(Helper.readLine(stream), 16);
	                            j = 0;
	                        }
	                    }
	                }
	                else
	                {
	                    byte[] frame = buffer.GetNextFrame();
	                    int i = 0;

	                    for (; ; )
	                    {
	                    	if (Thread.interrupted()) return;
	                        int bytes = stream.read(frame, i, LameQ.BUFFER_FRAME_SIZE - i);

	                        if (bytes == 0) break;

	                        i += bytes;

	                        if (i == LameQ.BUFFER_FRAME_SIZE) //end of frame
	                        {
	                            buffer.FrameWritten();
	                            setStatus_Uplink("ONLINE");
	                            frame = buffer.GetNextFrame();
	                            i = 0;
	                        }
	                    }
	                }
	            }
	            catch (Exception e)
	            {
	            	//e.printStackTrace();
	            }
	            finally
	            {
	                if (socket != null)
	                {
		            	setStatus_Uplink("OFFLINE");

		            	try { socket.close(); } catch (IOException e) { }
		            	
		            	socket = null;
	                }
	            }

	            try { Thread.sleep(LameQ.RECONNECT_WAIT); }
	            catch (InterruptedException e) { return; }
	        }
		}
	}
	
	class ServerWorkerThread extends Thread
	{
		public void run()
		{
	        for (; ; )
	        {
	            try
	            {
	    	        server = new ServerSocket(Port, LameQ.BACKLOG);
	                break;
	            }
	            catch (IOException e)
	            {
	            	setStatus_Downlink("OFFLINE");
	                return;
	            }
	        }

        	setStatus_Downlink("ONLINE");

	        for (; ; )
	        {
		        Socket socket;
				try	{ socket = server.accept();	}
				catch (IOException e)
				{
	            	setStatus_Downlink("OFFLINE");
		        	return;
				}

				Connections.add(new Connection(Feed.this, socket));
	        }
		}
	}
	
    public final int Port;
    public String Url;
    public String Comment;

    public Buffer buffer = new Buffer(LameQ.BUFFER_FRAMES, LameQ.BUFFER_FRAME_SIZE, LameQ.DELAY_FRAMES);

    Thread tClientWorker = new ClientWorkerThread();
    Thread tServerWorker = new ServerWorkerThread();

    public List<Connection> Connections = Collections.synchronizedList(new ArrayList<Connection>());

    public String ContentType = "";

    ServerSocket server;

    public boolean Removed = false;

    public String Status_Uplink = "NEW";
    public String Status_Downlink = "NEW";

    public Feed(int port, String url, String comment)
    {
        Port = port;
        Url = url;
        Comment = comment;

        tClientWorker.start();
        tServerWorker.start();
    }
    
    public void setStatus_Uplink(String value)
    {
    	if (Status_Uplink != value)
    	{
    		Status_Uplink = value;
    		
        	LameQ.Log("FEED UPLINK " + Status_Uplink);
    	}
    }

    public void setStatus_Downlink(String value)
    {
    	if (Status_Downlink != value)
    	{
    		Status_Downlink = value;
    		
        	LameQ.Log("FEED DOWNLINK " + Status_Downlink);
    	}
    }
}
