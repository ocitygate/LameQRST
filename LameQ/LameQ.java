import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;

public class LameQ
{
	public static final String NAME = "LameQ";
	public static final String DESCRIPTION = "Lame Queue";
	public static final String VERSION = "1.01";

	public static final int BACKLOG = 1024;
    public static final int RECONNECT_WAIT = 3000;
    public static final int REBIND_WAIT = 3000;
    public static final int POLL_WAIT = 200;
    public static final int TIME_OUT = 10000;
    public static final int BUFFER_FRAMES = 320;
    public static final int BUFFER_FRAME_SIZE = 65536;
    public static final int DELAY_FRAMES = 64;
    
    public static void Log(String message)
    {
    	System.out.println(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH:mm:ss.SSS")) + " " + message);
    }

    public static void main(String[] args)
	{
		@SuppressWarnings("unused")
		Feed feed = new Feed(Integer.parseInt(args[0]), args[1], "");
		
		@SuppressWarnings("resource")
		Scanner scanner = new Scanner(System.in);
		
		for ( ; ; )
		{
			String line = scanner.nextLine();
			String[] comment = { null };
			String[] args2 = Helper.parseLine(line, comment);
			if (args2.length > 0)
			{
				switch (args2[0])
				{
//					case "LIST":
//						for (Object connection : Helper.toArray(feed.Connections))
//							System.out.println(((Connection)connection).RemoteAddress);
//						System.out.println();
//						
//						break;
				}
			}
		}
	}
}
