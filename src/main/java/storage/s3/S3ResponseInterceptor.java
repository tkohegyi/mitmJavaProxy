package storage.s3;

import net.lightbody.bmp.core.har.HarContent;
import net.lightbody.bmp.core.har.HarResponse;
import net.lightbody.bmp.proxy.jetty.util.URI;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import website.magyar.mitm.proxy.ResponseInterceptor;
import website.magyar.mitm.proxy.http.MitmJavaProxyHttpResponse;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class S3ResponseInterceptor implements ResponseInterceptor {

	private final String region;
	private final String bucket;

	private final S3AsyncClient s3AsyncClient;

	public S3ResponseInterceptor(String region,
															 String bucket) {
		this.region = region;
		this.bucket = bucket;
		AwsCredentialsProvider credentialsProvider = StaticCredentialsProvider.create(
			AwsBasicCredentials.create(System.getenv("AWS_ACCESS_KEY_ID"),
				System.getenv("AWS_SECRET_ACCESS_KEY")));

		s3AsyncClient = S3AsyncClient.builder()
			.region(Region.of(this.region))
			.credentialsProvider(credentialsProvider)
			.build();
	}

	@Override
	public void process(MitmJavaProxyHttpResponse response) {
		byte[] bytes = response.getBodyBytes();
		if (bytes == null) return;

		// System.err.println(Arrays.toString(response.getRequestHeaders()));
		// System.err.println(response.getMethod().getMethod());
		// System.err.println(response.getProxyRequestURI().getScheme());
		// System.err.println(response.getProxyRequestURI().getHost());
		// System.err.println(response.getProxyRequestURI().getPath());
		// System.err.println(response.getProxyRequestURI().getParameters());

		HarResponse harResponse = response.getEntry().getResponse();
		// System.err.println("Headers: " + harResponse.getHeaders());
		// System.err.println("Status: " + harResponse.getStatus());
		// System.err.println("Cookies: " + harResponse.getCookies());
		// System.err.println("Body size: " + harResponse.getBodySize());

		HarContent content = harResponse.getContent();
		// System.err.println("Mime: " + content.getMimeType());
		// System.err.println("Content size: " + content.getSize());

		Map<String, String> metadata = new HashMap<>();
		metadata.put("timestamp", response.getEntry().getStartedDateTime().toInstant().toString());
		metadata.put("request-headers", Arrays.toString(response.getRequestHeaders()));
		metadata.put("response-headers", Arrays.toString(response.getHeaders()));

		PutObjectRequest putObjectRequest = PutObjectRequest.builder()
			.bucket(bucket)
			.key(key(response.getProxyRequestURI()))
			.contentType(content.getMimeType())
			.metadata(metadata)
			.contentMD5(Base64.encodeBase64String(DigestUtils.md5(bytes)))
			.build();

		try {
			s3AsyncClient.putObject(putObjectRequest, AsyncRequestBody.fromBytes(bytes))
				.thenAccept(putObjectResponse -> {
					System.err.println("Saved " + bytes.length + " to " + putObjectRequest.key());
				});
		} catch (Exception e) {
			e.printStackTrace(System.err);
		}
	}

	private String key(URI uri) {
		return uri.getScheme() + "/" + uri.getHost() + uri.getPath();
	}
}
