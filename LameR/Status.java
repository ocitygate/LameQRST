import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.management.ManagementFactory;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

class Status
{
	class WorkerThread extends Thread
	{
		public void run()
		{
            for (; ; )
            {
                try
                {
    				server = new ServerSocket(Port);
                    break;
                }
                catch (IOException e)
                {
                	LameR.Log("[STATUS] Bind failed");
                }
                
                try { Thread.sleep(LameR.REBIND_WAIT); }
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
	            Socket.setSoTimeout(LameR.TIME_OUT);

	            InputStream stream = Socket.getInputStream();
                Writer writer = new OutputStreamWriter(Socket.getOutputStream(), "UTF-8");
                
    	        //stream.ReadTimeout = LameR.TIME_OUT;
    	        //stream.WriteTimeout = LameR.TIME_OUT;

                String user = "";
	            String pass = "";
	            String contentType = "";
	            int contentLength = 0;

	            String request = Helper.readLine(stream);
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
	                    String header_name = header_s[0].trim();
	                    String header_value = header_s[1].trim();

	                    switch (header_name)
	                    {
	                        case "Content-Type":
	                            contentType = header_value;
	                            break;
	                        case "Content-Length":
                                contentLength = Integer.parseInt(header_value);
	                            break;
	                        case "Authorization":
	                            String[] authorization = header_value.split(" ");
	                            if (authorization.length == 2 && authorization[0].equals("Basic"));
	                            {
                                    String[] userInfo = new String(Base64.getDecoder().decode(authorization[1]), StandardCharsets.UTF_8).split(":", 2);
                                    user = userInfo.length > 0 ? userInfo[0] : "";
                                    pass = userInfo.length > 1 ? userInfo[1] : "";
	                            }
	                            break;
	                    }
	                }

	                header = Helper.readLine(stream);
	            }

	            if (!user.equals(User) | !pass.equals(Pass))
	            {
	                LameR.Log("[STATUS] Access Denied (" + user + "@" + Socket.getRemoteSocketAddress() + ")");

	                writer.write("HTTP/1.1 401 Unauthorized\n");
	                writer.write("WWW-Authenticate: Basic realm=\"LameR\"\n");
	                writer.write("\n");
	                writer.flush();

	                return;
	            }
	            
	            String[][] matches = { null };
	            if (Helper.preg_match("^\\/$", request_s[1], matches))
	            {
	                StringBuilder sbOutput = new StringBuilder();
	                sbOutput.append("<b>");
	                sbOutput.append(LameR.NAME);
	                sbOutput.append(" <i>v");
	                sbOutput.append(LameR.VERSION);
	                sbOutput.append("</i></b><br><br><b>Uptime: </b>");
	                sbOutput.append(Helper.formatMS(ManagementFactory.getRuntimeMXBean().getUptime()));

	                serve(writer, sbOutput.toString());
	            }
	            else if (Helper.preg_match("^\\/aggregate$", request_s[1], matches))
	            {
	                writer.write("HTTP/1.1 200 OK\n");
	                writer.write("Content-Type: text/plain\n");
	                writer.write("\n");

	                for (Object feed : Helper.toArray(LameR.Feeds))
	                {
	                	for (Object connection : Helper.toArray(((Feed)feed).Connections))
            			{
	                        writer.write("CONNECTION " + ((Feed)feed).Port + " " + ((Feed.Connection)connection).User + " " + ((Feed.Connection)connection).RemoteAddress + " " + ((Feed.Connection)connection).Status + "\n");
            			}
	                }
	                writer.flush();
	            }
	            else if (Helper.preg_match("^\\/config$", request_s[1], matches))
	            {
	                if (request_s[0].equals("POST"))
	                {
	                    if (contentType.equals("application/x-www-form-urlencoded"))
	                    {
	                        byte[] data = new byte[contentLength];
	                        int bytes;
	                        for (int i = 0; i < contentLength; i += bytes)
	                        {
	                            bytes = stream.read(data, i, contentLength - i);
	                            if (bytes == 0) 
                            	{
	                            	writer.close();
	                            	return;
                            	}
	                        }

	                        String _POST_DATA = new String(data, "UTF-8");
	                        HashMap<String,String> _POST = Helper.parseQueryString(_POST_DATA);
	                        String config = _POST.get("config");
	                        FileOutputStream file = new FileOutputStream("LameR.txt");
	                        file.write(config.getBytes("UTF-8"));
	                        file.close();
	                        LameR.config.load();
	                    }
	                }

	                StringBuilder sbOutput = new StringBuilder();
	                sbOutput.append("<table><tbody>\n");
	                sbOutput.append("<tr><td><b>LameR.txt</b></td></tr>\n");
	                sbOutput.append("<tr><td><form method=\"POST\">\n");
	                sbOutput.append("<textarea name=\"config\" wrap=\"off\">\n");
	                sbOutput.append(new String(Files.readAllBytes(Paths.get("LameR.txt")), "UTF8"));
	                sbOutput.append("</textarea></td></tr>\n");
	                sbOutput.append("</tbody></table>\n");
	                sbOutput.append("<input type=\"submit\" value=\"Save\" >\n");
	                sbOutput.append("</form>\n");

	                serve(writer, sbOutput.toString());
	            }
	            else if (Helper.preg_match("^\\/feeds$", request_s[1], matches))
	            {
	                StringBuilder sbOutput = new StringBuilder();

	                sbOutput.append("<table><tbody>\n");
	                sbOutput.append("<tr><td colspan=\"5\"><b>Feeds</b></td></tr>\n");
	                sbOutput.append("<tr><td class=\"right \"><b>Port</b></td><td><b>Url / Comment</b></td><td class=\"center \"><b>Uplink<br/>Status</b></td><td class=\"center \"><b>Downlink<br/>Status</b></td><td class=\"center \"><b>Downlink<br/>Connections</b></td></tr>\n");

	                for(Object feed_ : Helper.toArray(LameR.Feeds))
	                {
	                	Feed feed = (Feed)feed_;
	                	sbOutput.append("<tr>");
	                	sbOutput.append("<td class=\"right \">" + feed.Port + "</td>");
	                	sbOutput.append("<td>" + (feed.Comment.equals("") ? feed.Url : feed.Comment) + "</td>");
	                	sbOutput.append("<td class=\"center \">" + feed.Status_Uplink + "</td>");
	                	sbOutput.append("<td class=\"center \">" + feed.Status_Downlink + "</td>");
	                	sbOutput.append("<td class=\"center \"><a href=\"/feed/" + feed.Port + "/connections\">" + feed.Connections.size() + "</a></td>");
	                	sbOutput.append("</tr>\n");
	                }
	                
	                sbOutput.append("</tbody></table>\n");

	                serve(writer, sbOutput.toString());
	            }
	            else
	            {
	                writer.write("HTTP/1.1 404 Not Found\n");
	                writer.write("\n");
	                writer.flush();
	            }
	        }
	        catch (Exception e)
	        {
	        	
	        }
	        finally
	        {
	            try { Socket.close(); }
	            catch (IOException e) { }
	        }
		}
	}
	
    public final int Port;
    public final String User;
    public final String Pass;

    public boolean Removed;

    ServerSocket server;

    Thread tWorker = new WorkerThread();

    public Status(int port, String user, String pass)
    {
        Port = port;
        User = user;
        Pass = pass;

        tWorker.start();
    }

    public void Close()
    {
    	tWorker.interrupt();
        try { server.close(); } catch (IOException e) {	}
    }

    void serve(Writer writer, String output)
    {
        try
        {
			writer.write("HTTP/1.1 200 OK\n");
	        writer.write("Content-Type: text/html\n");
	        writer.write("\n");
	        writer.write("<!DOCTYPE html>\n");
	        writer.write("<html><head>\n");
	        writer.write("<meta charset=\"UTF-8\">\n");
	        writer.write("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
	        writer.write("<style>\n");
	        writer.write("* { font-family: Verdana; font-size: 12px; }\n");
	        writer.write("a { color: #00f; } a:visited { color: #00f; }\n");
	        writer.write("textarea { width: 600px; height: 450px; border: none; overflow: scroll; font-family: Courier New; }\n");
	        writer.write("table { border-collapse: collapse; }\n");
	        writer.write("td { border: solid 1px; padding: 4px; vertical-align: top; }\n");
	        writer.write(".center { text-align: center; }\n");
	        writer.write(".right { text-align: right; }\n");
	        writer.write("</style></head><body>\n");
	        writer.write("<b><a href=\"/\">Status</a> | <a href=\"/feeds\">Feeds</a> | <a href=\"/users\">Users</a> | <a href=\"/config\">Config</a> </b><br/><br/>\n");
	        writer.write(output + "\n");
	        writer.write("</body></html>\n");
	        writer.flush();
 		}
        catch (IOException e)
        {
		}
   }
}