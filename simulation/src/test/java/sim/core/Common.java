package sim.core;

/**
 * Created by khaled on 3/7/17.
 */
public class Common {
    public static String getLastOctet(String key) {
        try {
            return key.substring(key.lastIndexOf('.') + 1, key.length());
        }
        catch (Exception ex){
            return "";
        }
    }
}
