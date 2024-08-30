import storage.s3.S3ResponseInterceptor;
import website.magyar.mitm.proxy.ProxyServer;

public class Main {

	public static void main(String[] args) throws Exception {
		ProxyServer proxyServer = new ProxyServer(8080);

		proxyServer.start(100000);

		proxyServer.setCaptureContent(true);
		proxyServer.setCaptureBinaryContent(true);
		proxyServer.addResponseInterceptor(new S3ResponseInterceptor("eu-west-1", "forever-proxy"));
	}
}