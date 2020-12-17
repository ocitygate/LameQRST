class Account
{
    public final Slave Slave;
    public final String User;
    public final String Pass;

    Feed Feed;

    public boolean Removed;

    public Account(Slave slave, String user, String pass)
    {
        Slave = slave;
        User = user;
        Pass = pass;
    }
}