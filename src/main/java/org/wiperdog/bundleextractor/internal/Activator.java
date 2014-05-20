package org.wiperdog.bundleextractor.internal;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.wiperdog.bundleextractor.BundleExtractor;

public class Activator implements BundleActivator{

	public void start(BundleContext context)  {
		try{
		String lstBundlesCfg = System.getProperty("bundle_extractor.list_bundles");
		String repoCfg = System.getProperty("bundle_extractor.repositories");
		File listResources = new File(lstBundlesCfg);
		FileInputStream fis = new FileInputStream(new File(repoCfg));
		List<String> repositories = new ArrayList<String>();
		try{
			BufferedReader bf = new BufferedReader(new InputStreamReader(fis));
			String line = null;
			while(((line = bf.readLine()) != null) && (!line.trim().equals(""))){				
				repositories.add(line);
			}					
		}catch(Exception ex){
			System.out.println("[BundleExtrator] : Failed to read maven repositories configuration file !");			
		}

		BundleExtractor instance = new BundleExtractorImpl();
		instance.setRepositories(repositories);
		context.registerService(BundleExtractor.class.getName(), instance, null);
		if(listResources.exists()){
			instance.setListResources(listResources);
			instance.processListResources();
		}
		}catch(Exception ex){
			ex.printStackTrace();
		}
	}

	public void stop(BundleContext context) throws Exception {
		System.out.println("Bye :(");
	}

}
