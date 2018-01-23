package com.tanpugi.standalone;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CmdDummy implements CmdInterface {
	private static final Logger logger = LoggerFactory.getLogger(CmdDummy.class);

	public CmdDummy(String[] args) {}
	
	@Override
	public void run() {
		logger.error("Command is not supported.");
	}

	@Override
	public void process() throws Exception {
		// TODO Auto-generated method stub
	}
	
	@Override
	public void init() throws Exception {
		// TODO Auto-generated method stub
	}

	@Override
	public void destroy() throws Exception {
		// TODO Auto-generated method stub
	}
}
