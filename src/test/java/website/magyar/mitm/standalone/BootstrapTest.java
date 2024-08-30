package website.magyar.mitm.standalone;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import website.magyar.mitm.proxy.ProxyServer;
import website.magyar.mitm.standalone.helper.PropertiesNotAvailableException;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.BDDMockito.given;

public class BootstrapTest {

    private static final String[] ARGS = {"proxy.conf.properties"};

    @Mock
    private PropertyLoader propertyLoader;

    @InjectMocks
    private Bootstrap underTest;

    @BeforeEach
    public void setUp() throws Exception {
        underTest = Mockito.spy(new Bootstrap());
        MockitoAnnotations.initMocks(this);
        Properties properties = new Properties();
        given(propertyLoader.loadProperties(ARGS[0])).willReturn(properties);
    }

    @Test
    public void bootstrapWrongPropertyFile() {
        Assertions.assertThrows(PropertiesNotAvailableException.class, () -> {
            //GIVEN
            //WHEN
            underTest.bootstrap(ARGS);
            //THEN
            //exception shall occur
        });
    }

    @Test
    public void bootstrapWithoutPropertyFile() {
        //GIVEN
        //WHEN
        ProxyServer proxyServer = underTest.bootstrap(null);
        //THEN
        assertNotNull(proxyServer);
        assertTrue(proxyServer.getPort() > 0);
    }

    @Test
    public void bootstrapWithHardcodedParameters() {
        //GIVEN
        //WHEN
        ProxyServer proxyServer = underTest.bootstrapFixed();
        //THEN
        assertNotNull(proxyServer);
        assertEquals(9092, proxyServer.getPort());
    }

}