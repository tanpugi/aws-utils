package com.tanpugi.standalone;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.helpers.Loader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tanpugi.standalone.aws.S3MigrationCmd;


public class Cmdline {
	private static final Logger logger = LoggerFactory.getLogger(Cmdline.class);
	
	public static String[] init(String[] argz) {
        String log4jConfigFile = Loader.getResource("log4j.properties").getPath();
        PropertyConfigurator.configure(log4jConfigFile);		
		return argz;
	}
	
	public static void main(String[] args) {
		args = init(args);
		try {
			if (args != null && args.length > 0 && args[0] != null) {
				getCmd(args).run();
			} else {
				logger.error("Need minimum of 1 parameter.");
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}
	
	private static CmdInterface getCmd(String[] args) {
		String cmd = args[0].toLowerCase();
		switch(cmd) {
			case "aws-s3": return new S3MigrationCmd(args);
			default: return new CmdDummy(args);
		}
	}
}
