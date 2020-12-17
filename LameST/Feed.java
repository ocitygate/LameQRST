import java.util.ArrayList;
import java.util.List;

class Feed
{
    public final int Port;
    public final String Url;
    public String Comment;
    public String Bouquet;

    public final List<User> Users = new ArrayList<User>();
    public Account Account;
    
    public boolean Removed;

    public Feed(int port, String url, String comment, String bouquet)
    {
        Port = port;
        Url = url;
        Comment = comment;
        Bouquet = bouquet;
    }
}
