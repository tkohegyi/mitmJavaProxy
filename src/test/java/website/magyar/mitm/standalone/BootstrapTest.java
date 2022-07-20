package website.magyar.mitm.standalone;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import website.magyar.mitm.proxy.ProxyServer;
import website.magyar.mitm.standalone.helper.PropertiesNotAvailableException;

import java.util.Properties;

import static org.mockito.BDDMockito.given;

public class BootstrapTest {

    private static final String[] ARGS = {"proxy.conf.properties"};

    @Mock
    private PropertyLoader propertyLoader;

    @InjectMocks
    private Bootstrap underTest;

    @Before
    public void setUp() throws Exception {
        underTest = Mockito.spy(new Bootstrap());
        MockitoAnnotations.initMocks(this);
        Properties properties = new Properties();
        given(propertyLoader.loadProperties(ARGS[0])).willReturn(properties);
    }

    @Test(expected = PropertiesNotAvailableException.class)
    public void bootstrapWrongPropertyFile() {
        //GIVEN
        //WHEN
        underTest.bootstrap(ARGS);
        //THEN
        //exception shall occur
    }

    @Test
    public void bootstrapWithoutPropertyFile() {
        //GIVEN
        //WHEN
        ProxyServer proxyServer = underTest.bootstrap(null);
        //THEN
        Assert.assertNotNull(proxyServer);
        Assert.assertTrue(proxyServer.getPort() > 0);
    }

}