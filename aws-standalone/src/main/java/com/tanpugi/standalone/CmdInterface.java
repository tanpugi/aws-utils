package com.tanpugi.standalone;

public interface CmdInterface {

	public void run() throws Exception;
	public void init() throws Exception;
	public void process() throws Exception;
	public void destroy() throws Exception;
}
