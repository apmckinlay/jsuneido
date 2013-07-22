package suneido.jdbc;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.util.Properties;

//TODO
// Create SuProperties
// Implement parseURL()
// Add comments
// create minimal set of stub classes
// Implement stub classes to enable JDBC tests

/**
 *
 * @author Nana
 */
public class jdbcDriver implements Driver {
    /**
    * the URL prefix used by the driver (i.e <code>jdbc:jsuneido:</code>).
    */
    public static final String DRIVER_PREFIX = "jdbc:jsuneido:";

    /**
    * Driver major version.
    */
    static final int MAJOR_VERSION = 0;
    /**
    * Driver minor version.
    */
    static final int MINOR_VERSION = 1;

    // Property constants
    public static final String HOST  = "prop.host";
    public static final String USERNAME  = "prop.username";
    public static final String PASSWORD = "prop.password";
    public static final String PORT  = "prop.defaultport";

    static {
        try {
            // Register this with the DriverManager
            DriverManager.registerDriver(new jdbcDriver());
        } catch (Exception e) {}
    }

    public Connection connect(String url, Properties info) throws SQLException {
        if (url == null || !url.toLowerCase().startsWith(DRIVER_PREFIX)) {
            return null;
        }
        
        Properties props = parseURL(url, info);

        return new jdbcConnection(props);
       
    }

    /**
    * Parses the datasource URL which encodes a mandatory part in
    * the beginning, a server address and optionally additional parameters.
    *
    * @param url The datasource URL encoded as String object
    * @param info Other properties already available
    * @return a Properties object populated with found properties and
    * null if parsing of the datasource URL fails.
    */
    protected static Properties parseURL(String url, Properties info) {
        Properties props = new Properties();
        return props;
    }

    public DriverPropertyInfo[] getPropertyInfo(String url, Properties info)
            throws SQLException {

        DriverPropertyInfo[] pinfo   = new DriverPropertyInfo[3];
        DriverPropertyInfo   p;

        p          = new DriverPropertyInfo(HOST, "localhost");
        p.value    = info.getProperty(HOST);
        p.required = true;
        pinfo[0]   = p;

        p          = new DriverPropertyInfo(PORT, "3456");
        p.value    = info.getProperty(PORT);
        p.required = true;
        pinfo[1]   = p;


        p          = new DriverPropertyInfo(USERNAME, null);
        p.value    = info.getProperty(USERNAME);
        p.required = false;
        pinfo[2]   = p;

        p          = new DriverPropertyInfo(PASSWORD, null);
        p.value    = info.getProperty(PASSWORD);
        p.required = false;
        pinfo[3]   = p;

       return pinfo;

    }
    
    public boolean acceptsURL(String url) throws SQLException {
        if (url == null) {
            return false;
        }
        return url.toLowerCase().startsWith(DRIVER_PREFIX);
    }

    /**
    *  Gets the driver's major version number.
    *
    * @return  this driver's major version number
    */
    public int getMajorVersion() {
        return MAJOR_VERSION;
    }

    /**
    *  Gets the driver's minor version number.
    *
    * @return  this driver's minor version number
    */
    public int getMinorVersion() {
        return MINOR_VERSION;
    }

    public boolean jdbcCompliant() {
        return false;
    }


}
