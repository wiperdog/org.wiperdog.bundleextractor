package org.wiperdog.bundleextractor.internal;

import java.security.acl.LastOwnerException;
import java.util.List;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.wiperdog.bundleextractor.BundleExtractor;

public class BundleExtractorImpl implements BundleExtractor {
	private File listResources;
	public String MANIFEST_ATTRIBUTE = "Destination";
	private List<String> repositories;
	public File getListResources() {
		return listResources;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public boolean processListResources() {
		// Read listResource file for list bundle to get and extract
		List<Map> listBundle = readResourceFile(listResources);
		// Process get and extract bundles
		for (Map bundle : listBundle) {
			String location = processResource(bundle);
			if(location != null && !"".equals(location)){
				File gotBundle = new File(location);
				//System.out.println(gotBundle.getName() + ":" + gotBundle.exists());
				extractPackage(gotBundle);
			}
			bundle.put("getit", false);
		}
		
		// Rewrite listResource file for updating get status
		rewriteFile(listBundle);
		return true;
	}

	public String processResource(Map bundle) {
		boolean getit = (Boolean) bundle.get("getit");
		if(getit){			
			String groupId = (String) bundle.get("groupId");
			String artifactId = (String) bundle.get("artifactId");
			String version = (String) bundle.get("version");
			String location =(String) bundle.get("location");
			if(!"".equals(groupId) && !"".equals(artifactId) && !"".equals(version) && !"".equals(location)){
				//Write and execute maven command to get bundle from maven repository
				StringBuffer sb = new StringBuffer();
				sb.append("mvn org.apache.maven.plugins:maven-dependency-plugin:2.8:get ");
				if(version != null && !version.equals("")){
					sb.append("-Dartifact=" + groupId+ ":" + artifactId +":" + version + " ");
				} else {
					sb.append("-Dartifact=" + groupId+ ":" + artifactId +":LATEST" + " ");
				}
				sb.append("-Ddest=" + location + " ");
				String repoStr = "-DremoteRepositories=";
				for(String repo : repositories){
					repoStr += repo + ",";
				}				
				if(repoStr.trim().endsWith(",")){
					repoStr = repoStr.substring(0,repoStr.lastIndexOf(","));
				}
								
				sb.append(repoStr + " ");
				String result = executeCommand(sb.toString());
				if(!"".equals(result)){
					System.out.println(result);
					return location;
				}
			}
		}
		return null;
	}
	
	public void extractPackage(File packageBundle){
		try {
			if(!packageBundle.exists()){
				System.out.println("Package bundle not found: " + packageBundle.getAbsolutePath());
				return;
			}
			JarFile jar = new JarFile(packageBundle);
			String destination = jar.getManifest().getMainAttributes().getValue(MANIFEST_ATTRIBUTE);
			if(destination == null || "".equals(destination)){
				System.out.println("Destination in MANIFEST is empty.");
				return;
			}
			// Make sure the folder is made
			File dest = new File(System.getProperty("felix.home") + File.separator + destination);
			dest.mkdirs();
			Enumeration entries = jar.entries();
			while(entries.hasMoreElements()){
				JarEntry entry = (JarEntry) entries.nextElement();
				File f = new File(System.getProperty("felix.home") + File.separator + destination + File.separator + entry.getName());
				
				if(entry.isDirectory() && !entry.getName().equalsIgnoreCase("META-INF/") ){
					f.mkdir();
					continue;
				}
				
				if(f.getName().equalsIgnoreCase("manifest.mf") || f.getName().equalsIgnoreCase("META-INF")){
					continue;
				}
				System.out.println("-Extract " + entry.getName());
				InputStream is = jar.getInputStream(entry);
				FileOutputStream fos = new FileOutputStream(f);
				while(is.available() > 0){
					fos.write(is.read());
				}
				fos.close();
				is.close();
			}
		} catch (IOException e) {
			System.out.println(e.getMessage());
		}
	}

	public String executeCommand(String cmd) {
		StringBuffer output = new StringBuffer();
		Process p;
		try {
			p = Runtime.getRuntime().exec(cmd);
			p.waitFor();
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					p.getInputStream()));
			String line = "";
			while ((line = reader.readLine()) != null) {
				output.append(line + "\n");
			}
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
		return output.toString();
	}
	
	/**
	 * Rewrite to update get status 
	 * @param listBundle
	 */
	public void rewriteFile(List<Map> listBundle){
		try {
			FileWriter fw = new FileWriter(listResources.getAbsoluteFile());
			BufferedWriter bw = new BufferedWriter(fw);
			StringBuffer sb = new StringBuffer();
			for (Map bundle : listBundle) {
				for (Object key : bundle.keySet()) {
					sb.append(bundle.get(key) + ",");
				}
			}
			bw.write(sb.toString());
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Read listResource file for list bundles to get and extract
	 * listResource file has format : groupID, artifactID, version, destination, repoUrl, get status(true/false)
	 * @param listResource
	 * @return list bundles
	 */
	@SuppressWarnings("unchecked")
	public List<Map> readResourceFile(File listResource){
		List<Map> listBundle = new ArrayList<Map>();
		try {
			BufferedReader br = new BufferedReader(new FileReader(listResource));
			String line;
			while((line = br.readLine()) != null){
				String[] values = line.split(",");
				Map bundle = new LinkedHashMap();
				bundle.put("groupId", values[0]);
				bundle.put("artifactId", values[1]);
				bundle.put("version", values[2]);
				bundle.put("location", values[3]);
				bundle.put("getit", values[4] != null ? Boolean.valueOf(values[5]) : true);
				listBundle.add(bundle);
			}
			br.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return listBundle;
	}

	public void setListResources(File listResources) {
		this.listResources = listResources;
	}

	public List<String> getRepositories() {		
		return this.repositories;
	}

	public void setRepositories(List<String> repositories) {
		this.repositories = repositories;		
	}

}
