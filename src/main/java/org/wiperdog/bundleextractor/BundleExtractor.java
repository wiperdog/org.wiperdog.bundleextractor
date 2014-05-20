package org.wiperdog.bundleextractor;

import java.io.File;
import java.util.List;
import java.util.Map;

public interface BundleExtractor {
	
	public File getListResources();
	
	public void setListResources(File listResources);
	
	public String executeCommand(String cmd);
	
	public List<String> getRepositories();
	public void setRepositories(List<String> repositories);
	public boolean processListResources();
	public String processResource(Map bundle);
	public void extractPackage(File packageBundle);
}
