import java.util.Collection;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.net.URLDecoder;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

public class Helper 
{
    static String[] parseLine(String line, String[] comment)
    {
        List<String> tokens = new ArrayList<String>();
        String token = "";
        comment[0] = "";
        boolean inComment = false;
        for (char c : line.toCharArray())
        {
            if (inComment)
            {
                comment[0] += c;
            }
            else
            {
                if (c == ' ')
                {
                    if (!token.equals("")) tokens.add(token);
                    token = "";
                }
                else if (token.equals("") & c == '#')
                {
                    inComment = true;
                }
                else
                {
                    token += c;
                }
            }
        }
        if (!token.equals("")) tokens.add(token);

        comment[0] = comment[0].trim();

        String[] temp = new String[tokens.size()];
        tokens.toArray(temp);
        return temp;
    }

	public static Object[] toArray(Collection<?> collection)
	{
        return collection.toArray(); 
	}
	
	public static Object[] toArray(AbstractMap<?,?> map)
	{
        return  map.values().toArray(); 
	}
	
	public static String readLine(InputStream stream)
	{
        StringBuilder sb = new StringBuilder();

        for (; ; )
        {
            int i;
			try { i = stream.read(); }
			catch (IOException e) {	return null; }
			
            if (i == -1)
            {
                if (sb.length() == 0) return null;
                return sb.toString();
            }

            char c = (char)i;
            switch (c)
            {
                case '\r':
                    break;
                case '\n':
                    return sb.toString();
                default:
                    sb.append(c);
                    break;
            }
        }
	}

    public static boolean preg_match(String pattern, String input, String[][] matches)
    {
    	Pattern r = Pattern.compile(pattern);
    	Matcher m = r.matcher(input);
    	
		if (m.matches())
		{
			int groupCount = m.groupCount() + 1;
			matches[0] = new String[groupCount];
			for(int i = 0; i < groupCount; i++)
			{
				matches[0][i] = m.group(i);
			}
			return true;
		}
		else
		{
			return false;
		}
    }	

    public static HashMap<String, String> parseQueryString(String queryString)
    {
    	HashMap<String, String> temp = new HashMap<String, String>();
    	String[] params = queryString.split("&");
    	for(String param : params)
    	{
    		String[] keyval = param.split("=", 2);
    		try { temp.put(keyval[0], keyval.length > 1 ? URLDecoder.decode(keyval[1], "UTF-8") : ""); }
    		catch (UnsupportedEncodingException e) { }
    	}
    	return temp;
    }

    public static String HttpDate()
    {
    	return LocalDateTime.now().format(DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss z").withZone(ZoneId.of("UTC")));
    }

    public static String formatMS(long ms)
    {
    	long days = ms / 86400000L;
    	ms = ms % 86400000;
    	
    	long hours = ms / 3600000L;
    	ms = ms % 3600000L;
    	
    	long minutes = ms / 60000L;
    	ms = ms % 60000L;
    			
    	long seconds = ms / 1000L;
    	ms = ms % 1000L;
    	
    	return days + "d " + String.format("%02d", hours) + ":" + String.format("%02d", minutes) + ":" + String.format("%02d", seconds);
    }
}
