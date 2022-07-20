package website.magyar.mitm.standalone;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import website.magyar.mitm.standalone.helper.PropertiesNotAvailableException;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class PropertyLoader {
    private final Logger logger = LoggerFactory.getLogger(PropertyLoader.class);

    /**
     * Loads properties from the specified property file. Also validates
     * the property file.
     * @param configFile the configuration file to be read
     * @return the loaded {@link Properties}
     */
    public Properties loadProperties(final String configFile) {
        Properties properties = new Properties();
        if (configFile != null) {
            try {
                checkPropertyFileArgument(configFile);
                InputStream inputStream = new FileInputStream(configFile);
                properties.load(inputStream);
                logger.debug("Properties loaded from external configuration.");
            } catch (IOException e) {
                throw new PropertiesNotAvailableException("Configuration file " + configFile + " cannot be loaded.");
            }
        }
        return properties;
    }

    private void checkPropertyFileArgument(final String args) {
        if (args == null || "".equals(args)) {
            throw new PropertiesNotAvailableException("Configuration file was not specified as input argument!");
        } else if (!args.endsWith(".properties")) {
            throw new PropertiesNotAvailableException("Configuration file must be a properties file!");
        }
    }

}
