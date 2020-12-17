class User
{
    public final String User_;
    public final String Pass;

    public Feed Feed;
    public Account Account;

    public boolean Removed;
    public boolean Disconnected;
    
    public User(String user_, String pass)
    {
        User_ = user_;
        Pass = pass;
    }
}