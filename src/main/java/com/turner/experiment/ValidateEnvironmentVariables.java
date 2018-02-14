package com.turner.experiment;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import com.turner.helpers.PropertyCheck;

//@Mojo(name = "envValidate", defaultPhase = LifecyclePhase.COMPILE)
public class ValidateEnvironmentVariables extends AbstractMojo {

	
	@Parameter(property = "envPropertiesFileName", required = false)
	private String envPropertiesFileName;
	private List<PropertyCheck> unknownEnvList;
	private MavenProject project;

	public void execute() throws MojoExecutionException {
		project = (MavenProject) getPluginContext().get("project");
		unknownEnvList = new ArrayList<>();
		Map<String, Boolean> envProps = null;
		try {
			envProps = getEnvProperties();
		} catch (Exception e) {
			throw new MojoExecutionException("Couldn't load env file:" + envPropertiesFileName, e);
		}

		List<String> list = erbFiles();

		getLog().info(list.size() + "");
		for (String str : list) {
			readFiles(envProps,str);
		}
		getLog().info("--------------------------------------");
		getLog().info("Checking for Unused Variables in envFile:"+envPropertiesFileName);
		getLog().info("--------------------------------------");
		for (Entry<String,Boolean> entry : envProps.entrySet()) {
			if(entry.getValue()==false)
				getLog().warn("Unused variables:"+entry.getKey());
		}
		getLog().info("--------------------------------------");
		getLog().info("Checking for Unused Variables in envFile:"+envPropertiesFileName);
		getLog().info("--------------------------------------");
		for(PropertyCheck unknown:unknownEnvList) {
			getLog().warn("Unknown Property: "+unknown);
		}
	}

	private Map<String, Boolean> getEnvProperties() throws FileNotFoundException, IOException {
		Map<String, Boolean> map = new HashMap<>();
		Properties props = new Properties();
		
		props.load(new FileInputStream(new File(project.getBuild().getOutputDirectory()+"/"+envPropertiesFileName)));
		for (Object key : props.keySet()) {

			map.put(key.toString(), false);
		}
		return map;
	}

	private List<String> erbFiles() throws MojoExecutionException {
		Path resourcePath = Paths.get(this.getClass().getClassLoader().getResource("").getFile());
		Function<Path, String> shortenPath = (path) -> {
			return path.toString();
		};

		try {

			Stream<Path> stream = Files.walk(resourcePath).sorted(Collections.reverseOrder())
					.filter((pathz) -> pathz.toString().toLowerCase().endsWith(".erb"));
			return stream.map(shortenPath).collect(Collectors.toList());
		} catch (IOException e) {
			throw new MojoExecutionException("Couldn't use Files.walk on base classpath", e);
		}

	}

	private void readFiles(Map<String, Boolean> validationMap, String fileName) throws MojoExecutionException {
		try (BufferedReader in = new BufferedReader(new FileReader(fileName));) {
			List<String> lines = Files.readAllLines(Paths.get(fileName));
			Pattern pattern = Pattern.compile("<%= *@([a-zA-Z0-9_]*) *%>");

			for (String line : lines) {

				Matcher matcher = pattern.matcher(line);

				if (matcher.find()) {
					int start;

					while ((start = line.indexOf("<%=")) != -1) {
						int end = line.indexOf("%>") + 2;
						String patStr = line.substring(start, end);
						String patName = patStr.substring(3, patStr.length() - 2).trim().substring(1);
						Boolean value = validationMap.get(patName);
						if (value != null)
							validationMap.put(patName, true);
						else {
						
							this.unknownEnvList.add(	new PropertyCheck(fileName,patName));
						}

						line = line.replace(patStr, "");
					}
				}
			}
		} catch (Exception e) {
			throw new MojoExecutionException("Couldn't open erb file: " + fileName, e);
		}
	}
}
