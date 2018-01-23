package com.tanpugi.standalone.aws;

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.Flushable;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StreamUtils;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.Protocol;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.tanpugi.standalone.CmdInterface;
import com.tanpugi.standalone.CmdUtil;
import com.tanpugi.standalone.ParamValidator;


public class S3MigrationCmd implements CmdInterface {
	private static final Logger logger = LoggerFactory.getLogger(S3MigrationCmd.class);
	
	private String MODE = "ls";
	private String SUBMODE = "delta";
	private int OFFSET_COUNT = 0;
	private int PROCESS_LIMIT = 2;
	private boolean USE_PROXY = true;
	private String PROXY_HOST = "";
	private int PROXY_PORT = 8080;
	
	private String[] S3_FILTERS = {""};
	private String S3_SOURCE_BUCKET = null;
	private String S3_SOURCE_ACCESSKEY = null;
	private String S3_SOURCE_SECRETKEY = null;
	private String S3_SOURCE_REGION = "ap-southeast-1";
	
	private String S3_DESTINATION_BUCKET = null;
	private String S3_DESTINATION_ACCESSKEY = null;
	private String S3_DESTINATION_SECRETKEY = null;
	private String S3_DESTINATION_REGION = "ap-southeast-1";
	
	private String TEMP_DIR = "temp_s3_dir";
	
	private File tempDir;
	private File successFile;
	private File errorFile;
	
	private AmazonS3 sourceConn;
	private AmazonS3 targetConn;
	
	public S3MigrationCmd(String[] args) {
		String currentParam = "";
		try {
			for (String arg : args) {
				currentParam = arg;
				MODE = CmdUtil.convertArg(arg, "mode", MODE, ParamValidator.IS_REQUIRED);
				SUBMODE = CmdUtil.convertArg(arg, "submode", SUBMODE, ParamValidator.IS_REQUIRED);
				OFFSET_COUNT  = CmdUtil.convertArg(arg, "offset", OFFSET_COUNT, null);
				PROCESS_LIMIT = CmdUtil.convertArg(arg, "procLimit", PROCESS_LIMIT, null);
				USE_PROXY 	  = CmdUtil.convertArg(arg, "useProxy", USE_PROXY, null);
				PROXY_HOST 	  = CmdUtil.convertArg(arg, "proxyHost", PROXY_HOST, null);
				PROXY_PORT 	  = CmdUtil.convertArg(arg, "proxyPort", PROXY_PORT, null);
				TEMP_DIR 	  = CmdUtil.convertArg(arg, "tempDir", TEMP_DIR, null);

				S3_FILTERS 	  = CmdUtil.convertArg(arg, "filters", S3_FILTERS, null);
				
				S3_SOURCE_BUCKET = CmdUtil.convertArg(arg, "s3SourceBucket", S3_SOURCE_BUCKET, ParamValidator.IS_REQUIRED);
				S3_SOURCE_ACCESSKEY = CmdUtil.convertArg(arg, "s3SourceAccessKey", S3_SOURCE_ACCESSKEY, ParamValidator.IS_REQUIRED);
				S3_SOURCE_SECRETKEY = CmdUtil.convertArg(arg, "s3SourceSecretKey", S3_SOURCE_SECRETKEY, ParamValidator.IS_REQUIRED);
				S3_SOURCE_REGION = CmdUtil.convertArg(arg, "s3SourceRegion", S3_SOURCE_REGION, null);
				
				S3_DESTINATION_BUCKET = CmdUtil.convertArg(arg, "s3TargetBucket", S3_DESTINATION_BUCKET, ParamValidator.IS_REQUIRED);
				S3_DESTINATION_ACCESSKEY = CmdUtil.convertArg(arg, "s3TargetAccessKey", S3_DESTINATION_ACCESSKEY, ParamValidator.IS_REQUIRED);
				S3_DESTINATION_SECRETKEY = CmdUtil.convertArg(arg, "s3TargetSecretKey", S3_DESTINATION_SECRETKEY, ParamValidator.IS_REQUIRED);
				S3_DESTINATION_REGION = CmdUtil.convertArg(arg, "s3TargetRegion", S3_DESTINATION_REGION, null);
			}
		} catch (Exception e) {
			logger.error("For "+currentParam);
			throw e;
		}
	}

	
	@Override
	public void run() throws Exception {
		init();
		process();
		destroy();
	}

	@Override
	public void init() throws Exception {
		List<String> lines = new ArrayList<>();
		lines.add("");
		lines.add("################################################################");
		lines.add(LocalDate.now() + " Starting run of S3MigrationUtil");
		lines.add(LocalDate.now() + " sourceBucketName: " + S3_SOURCE_BUCKET);
		lines.add(LocalDate.now() + " targetBucketName: " + S3_DESTINATION_BUCKET);
		lines.add("################################################################");
		
		tempDir = new File(TEMP_DIR);
		tempDir.mkdirs();
		successFile = new File(tempDir,"success.txt");
		errorFile = new File(tempDir,"error.txt");
		FileUtils.writeLines(successFile, lines, true);
		FileUtils.writeLines(errorFile, lines, true);
		
		sourceConn = getS3Connection(S3_SOURCE_ACCESSKEY, S3_SOURCE_SECRETKEY, S3_SOURCE_REGION);
		targetConn = getS3Connection(S3_DESTINATION_ACCESSKEY, S3_DESTINATION_SECRETKEY, S3_DESTINATION_REGION);
	}

	@Override
	public void process() throws Exception {
		final List<String> sourceKeys = getKeys(sourceConn, S3_SOURCE_BUCKET, S3_FILTERS);
		final List<String> targetKeys = getKeys(targetConn, S3_DESTINATION_BUCKET, S3_FILTERS);
		final Map<String, String> targetKeysMap = new HashMap<>();
		for (String key : targetKeys) {
			targetKeysMap.put(key, key);
		}
		
		final List<String> deltaKeys = new ArrayList<>();
		for (String key : sourceKeys) {
			if (targetKeysMap.containsKey(key)) {
				String submode = SUBMODE.toLowerCase();
				switch (submode) {
					case "all": { deltaKeys.add(key); break; }
					default: continue;
				}
			} else {
				deltaKeys.add(key);
			}
		}
		
		logger.info("***** Starting processing items:{} from sourcebucket:{} to targetbucket:{}",
			deltaKeys.size(), S3_SOURCE_BUCKET, S3_DESTINATION_BUCKET);

		int cnt = 0;
		for (String key : deltaKeys) {
			cnt++;
			if (cnt > OFFSET_COUNT) {
				logger.info("Processing cnt:{} key:{}", cnt, key);
				String mode = MODE.toLowerCase();
				switch (mode) {
					case "cp-put": { migratePut(key); break; } //download first and upload
					case "cp-get": { migrateGet(key); break; } //s3 to s3 copy
					default: break;
				}
			}

			if (cnt == PROCESS_LIMIT) {
				logger.info("***** Stopped because it exceeded the COUNT_LIMIT");
				return;
			}
		}
	}

	@Override
	public void destroy() throws Exception {
	}
	
	private void migrateGet(String key) throws Exception {}
	private void migratePut(String key) throws Exception {
		final S3Object object = sourceConn.getObject(new GetObjectRequest(S3_SOURCE_BUCKET, key));
		final ObjectMetadata origMetadata = object.getObjectMetadata();
		final ObjectMetadata updatedMetadata = new ObjectMetadata();
   		final List<String> lines = new ArrayList<>();

   		File tempFile = null;
   		InputStream is = null;
   		OutputStream os = null;

   		try {
   	   		tempFile = new File(tempDir, UUID.randomUUID().toString());
   	   		is = object.getObjectContent();
   			os = new FileOutputStream(tempFile);
   			StreamUtils.copy(is, os);
   		
   			if (origMetadata.getContentDisposition() != null) { updatedMetadata.setContentDisposition(origMetadata.getContentDisposition()); }
   			if (origMetadata.getContentLength() >= 0) { updatedMetadata.setContentLength(origMetadata.getContentLength()); }
   			if (origMetadata.getContentType() != null) { updatedMetadata.setContentType(origMetadata.getContentType()); }

			PutObjectRequest putObjectRequest = new PutObjectRequest(S3_DESTINATION_BUCKET, key, tempFile);
			putObjectRequest.setCannedAcl(CannedAccessControlList.PublicRead);
			putObjectRequest.setMetadata(updatedMetadata);
			
			targetConn.putObject(putObjectRequest);
			
			lines.add(LocalDate.now() + " " + key + " size:" + origMetadata.getContentLength());
	   		FileUtils.writeLines(successFile, lines, true);
   		} catch (Exception e) {
   			logger.error(e.getMessage());
   			lines.add(LocalDate.now() + " " + key + " size:" + origMetadata.getContentLength() + " - " + e.getMessage());
   			FileUtils.writeLines(errorFile, lines, true);
		} finally{
   			close(is);
   			close(os);
			if (tempFile != null) { tempFile.delete(); }
		}
	}

	private AmazonS3 getS3Connection(String accessKey, String secretKey,
		String region) {
   		AWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);
   		ClientConfiguration clientConfiguration = new ClientConfiguration();;
   		clientConfiguration.setSocketTimeout(2000000);
   		clientConfiguration.setConnectionTimeout(2000000);
   		clientConfiguration.setClientExecutionTimeout(2000000);
   		clientConfiguration.setRequestTimeout(2000000);
   		clientConfiguration.setProtocol(Protocol.HTTP);
       
   		if (USE_PROXY) {
   			clientConfiguration.setProxyHost(PROXY_HOST);
   			clientConfiguration.setProxyPort(PROXY_PORT);
   		}

   		AmazonS3ClientBuilder s3ClientBuilder = AmazonS3Client.builder();
   		s3ClientBuilder.setCredentials(new AWSStaticCredentialsProvider(credentials));
   		s3ClientBuilder.setClientConfiguration(clientConfiguration);
   		s3ClientBuilder.setRegion(region);
   		
   		return s3ClientBuilder.build();
	}

	private List<String> getKeys(AmazonS3 s3Conn, String bucketName, String[] keyFilters) {
		List<String> existingKeys = new ArrayList<>();
		try {
			for (String keyFilter : keyFilters) {
				ListObjectsRequest listRequest = new ListObjectsRequest();
				listRequest.withBucketName(bucketName);
				listRequest.withPrefix(keyFilter);
			
				ObjectListing listing;
				do {
					listing = s3Conn.listObjects(listRequest);
					for (S3ObjectSummary objectSummary : listing.getObjectSummaries()) {
						existingKeys.add(objectSummary.getKey());
					}
					listRequest.setMarker(listing.getNextMarker());
				} while (listing.isTruncated());
			}
		} catch (Exception e) {
			throw e;
		}
		
		return existingKeys;
	}
	
	private void close(Object o) throws Exception {
		if (o != null && o instanceof Flushable) {
			((Flushable) o).flush();
		}
		if (o != null && o instanceof Closeable) {
			((Closeable) o).close();
		}
	}
}
