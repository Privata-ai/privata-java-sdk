package ventures.blockbird.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import org.apache.log4j.Logger;

/**
 * Util
 */
public class PropsUtil {
	
	final static Logger logger = Logger.getLogger(PropsUtil.class);

	private Properties props = null;
	
	/**
	 * PropsUtil can read a properties file
	 * @param propsName is the name of the file. It is located in the src/main/resources folder
	 * 
	 **/
    public PropsUtil(String propsName) {
		try {
			InputStream propertyStream = getClass().getResourceAsStream(propsName);

			if (propertyStream == null) {
				throw new IOException("Could not find a blockbird properties file named " + propsName);
			}
			
			this.props = new Properties();
			loadProperties(props, propertyStream);
			propertyStream.close();
			logger.info("Using runtime properties file:" + propsName);
			props.getProperty("firebaseApiKey");
		}
		catch (Exception ex) {
			logger.info("Got an error while attempting to load the runtime properties", ex);			
		}
    }

    public String getProps(String prop) {
       return props.getProperty(prop);        
	}
	
	/**
	 * This method is a replacement for Properties.load(InputStream) so that we can load in utf-8
	 * characters. Currently the load method expects the inputStream to point to a latin1 encoded
	 * file. <br>
	 * NOTE: In Java 6, you will be able to pass the load() and store() methods a UTF-8
	 * Reader/Writer object as an argument, making this method unnecessary.
	 * 
	 * @param props the properties object to write into
	 * @param input the input stream to read from
	 */
	public static void loadProperties(Properties props, InputStream inputStream) {
		InputStreamReader reader = null;
		try {
			reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
			props.load(reader);
		}
		catch (FileNotFoundException fnfe) {
			logger.error("Unable to find properties file" + fnfe);
		}
		catch (UnsupportedEncodingException uee) {
			logger.error("Unsupported encoding used in properties file" + uee);
		}
		catch (IOException ioe) {
			logger.error("Unable to read properties from properties file" + ioe);
		}
		finally {
			try {
				if (reader != null) {
					reader.close();
				}
			}
			catch (IOException ioe) {
				logger.error("Unable to close properties file " + ioe);
			}
		}
	}
	
}