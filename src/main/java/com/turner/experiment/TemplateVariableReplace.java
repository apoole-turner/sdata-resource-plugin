package com.turner.experiment;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.MojoExecutionException;

public class TemplateVariableReplace {
	public static String myPat = "<%= *@([a-zA-Z0-9_]*) *%>";
	private Map<String, String> envMap;
	private Map<String, String> templateVarMap;
	private List<String> templateLines;
	private List<String> erbFiles;
	private File targetFile;
	private Log log;
	private String[] reserveNames;
	private String javaEnvReplaceCommand="$javaVarReplace";
	public void main(String[] args) throws MojoExecutionException {

//		BufferedWriter writer = null;
//
//		try {
//			
//		} catch (IOException e) {
//			throw new MojoExecutionException("Failed to to open targetFile:" + targetFile, e);
//		}

		// Get a map of the enviornment variables.
		Pattern pattern = Pattern.compile(myPat); 
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(targetFile, false));){
			for (int i = 0; i < templateLines.size(); i++) {
				String line = templateLines.get(0);
				Matcher matcher = pattern.matcher(line);

				if (matcher.find()) {
					int start;

					while ((start = line.indexOf("<%=")) != -1) {
						int end = line.indexOf("%>") + 2;
						String patStr = line.substring(start, end);
						String patName = patStr.substring(3, patStr.length() - 2).trim().substring(1);
						if (isReserveName(patName)) {
							line = handleReserveWord(line, patName,patStr);
						} else {
							boolean foundMatch = false;
							String value = templateVarMap.get(patName);
							if (value != null) {
								line = line.replace(patStr, value);
								foundMatch = true;
							}
							if (!foundMatch) {
								log.info("ERROR:  Failed to find an environment variable match for " + patName);
								throw new MojoExecutionException("ERROR:  Failed to find an environment variable match for " + patName);
							}
						}
					}
				}

				log.info("****** " + line);
				writer.write(line);
				writer.newLine();
			}
		} catch (Exception t) {
			throw new MojoExecutionException("Check futher in stack trace for error",t);
		}

		System.out.println("Return for variable replacement.");
	}

	private String handleReserveWord(String line, String patName, String patStr) {
		StringBuilder sb=new StringBuilder();
		if(patName.equals("JAVA_VAR_REPLACE")) {
			
			for(String path:erbFiles) {
				line=line.replaceAll(patStr, sb.append(javaEnvReplaceCommand).append(" ").append("$APP_DIR/").append(path).toString());
			}
		}else if(patName.equals("ENVIRONMENT_VARIABLES")) {
			
		}
		return null;
	}

	private boolean isReserveName(String name) {
		for (String str : reserveNames)
			if (name.equals(str))
				return true;
		return false;
	}

	public Map<String, String> getEnvMap() {
		return envMap;
	}

	public void setEnvMap(Map<String, String> envMap) {
		this.envMap = envMap;
	}

	public Map<String, String> getTemplateVarMap() {
		return templateVarMap;
	}

	public void setTemplateVarMap(Map<String, String> templateVarMap) {
		this.templateVarMap = templateVarMap;
	}

	public List<String> getErbFiles() {
		return erbFiles;
	}

	public void setErbFiles(List<String> erbFiles) {
		this.erbFiles = erbFiles;
	}

}
