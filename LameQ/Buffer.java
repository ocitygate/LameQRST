
class Buffer
{
    @SuppressWarnings("serial")
	public class OverrunException extends Exception
    {
    }

    public final int FrameSize;
    public final int Frames;
    public final int DelayFrames;

    public byte[][] data;

    volatile long position = 0;
    int first = -1;
    int last = -1;

    public Buffer(int frames, int frameSize, int delayFrames)
    {
        Frames = frames;
        FrameSize = frameSize;
        DelayFrames = delayFrames;

        data = new byte[Frames][];
        for (int i = 0; i < Frames; i++)
        {
            data[i] = new byte[FrameSize];
        }
    }

    public synchronized byte[] GetNextFrame()
    {
        int next = (last + 1) % Frames;
        if (first == next)
        {
            first = (first + 1) % Frames;
            position++;
        }
        return data[next];
    }

    public synchronized void FrameWritten()
    {
        last = (last + 1) % Frames;
        if (first == -1) first = last;
    }

    public synchronized long GetMidFrameNo()
    {
        if (first == -1 | last == -1)
        {
            return 0;
        }
        else
        {
            return Math.max(position, position + last + (last < first ? Frames : 0) - first - DelayFrames);
        }
    }

    public synchronized byte[] ReadFrame(long frameNo) throws OverrunException
    {
        if (frameNo < position)
        {
            //over run
            throw new OverrunException();
        }

        if ((first == -1 | last == -1) | frameNo > position + last + (last < first ? Frames : 0) - first)
        {
            //under run
            return null;
        }

        int ix = (int)((first + frameNo - position) % Frames);

        return data[ix];
    }
    
    public synchronized void dispose()
    {
    	for(int i = 0; i < data.length; i++)
    	{
    		data[i] = null;
    	}
    	data = null; 
    }
}
