package com.turner.plugin;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import com.turner.helpers.PropertyCheck;
import com.turner.helpers.ResourceUtil;

@Mojo(name = "generateEnvironmentStub", defaultPhase = LifecyclePhase.COMPILE)
public class GenerateEnvironmentVariables extends AbstractMojo {

	@Parameter(property = "stubFileType", required = true, defaultValue = "properties") // properties, bash
	private String stubFileType;
	@Parameter(property = "stubFileName", required = true, defaultValue = "stub")
	private String stubFileName;

	@Parameter(property = "showMetaData", required = true, defaultValue = "false")
	private boolean showMetaData;

	private String stubFileNameAb;
	private Map<String, Set<PropertyCheck>> knownEnvMapSet;
	private Map<String, List<PropertyCheck>> knownEnvMapList;
	private MavenProject project;
	@Parameter(property = "resourceJson", required = false)
	private String resourceJson;
	public void execute() throws MojoExecutionException {
		project = (MavenProject) getPluginContext().get("project");
		if (resourceJson != null) {
			ResourceUtil.setAdditionalResourceFolder(resourceJson, project);
		}
		if (stubFileType.equalsIgnoreCase("bash")) {
			stubFileNameAb = project.getBasedir().getAbsolutePath() + "/" + stubFileName + ".sh";
		} else {
			stubFileNameAb = project.getBasedir().getAbsolutePath() + "/" + stubFileName + ".properties";
		}

		knownEnvMapSet = new HashMap<>();
		knownEnvMapList = new HashMap<>();
		List<Resource> projectResources = project.getResources();
		List<String> erbList = new ArrayList<>();
		for (Resource resource : projectResources) {
			if (resource.getDirectory() != null)
				erbList.addAll(ResourceUtil.getErbFiles(Paths.get(resource.getDirectory())));
		}
		for (String str : erbList) {
			readFiles(str);
		}
		getLog().info("--------------------------------------");
		getLog().info("Creating stub file:" + stubFileNameAb);
		getLog().info("--------------------------------------");
		try {
			createStubFile();
		} catch (Exception e) {
			throw new MojoExecutionException("Couldn't create the stub file " + stubFileNameAb);
		}
	}

	private void createStubFile() throws FileNotFoundException {
		Map<String, PropertyCheck> map = new HashMap<>();
		List<String> usedProperties = new ArrayList<>();
		
		try (PrintWriter output = new PrintWriter(new File(stubFileNameAb))) {
			StringBuilder metaData = new StringBuilder("");
			for (Entry<String, Set<PropertyCheck>> entry : knownEnvMapSet.entrySet()) {

				for (PropertyCheck property : entry.getValue()) {
					if (usedProperties.contains(property.getProperty())) {
					} else if (stubFileType.equalsIgnoreCase("bash")) {
						usedProperties.add(property.getProperty());
						output.println(createBashString(property.getProperty()));
					} else {
						usedProperties.add(property.getProperty());
						output.println(createPropertyString(property.getProperty()));
					}
				}
				if (showMetaData) {
					
					metaData.append("#File: " + entry.getKey() + ":").append(System.lineSeparator());
					for (PropertyCheck property : knownEnvMapList.get(entry.getKey())) {
						metaData.append("\t#line "+property.getLineNumber()+":"+property.getProperty()).append(System.lineSeparator());
					}
					metaData.append(System.lineSeparator());
				}
			}
			output.println(metaData);

		}
	}

	private String createBashString(String property) {
		return "export " + property + "=";
	}

	private String createPropertyString(String property) {
		return property + "=";
	}

	private List<String> getEnvProperties() throws FileNotFoundException, IOException {
		Map<String, String> map = System.getenv();
		List<String> env = new ArrayList<>();
		for (Entry<String, String> entry : map.entrySet()) {
			env.add(entry.getKey());
		}
		return env;
	}

	


	private void readFiles(String fileName) throws MojoExecutionException {
		try (BufferedReader in = new BufferedReader(new FileReader(fileName));) {
			List<String> lines = Files.readAllLines(Paths.get(fileName));
			Pattern pattern = Pattern.compile("<%= *@([a-zA-Z0-9_]*) *%>");
			Set<PropertyCheck> checkSet = new HashSet<>();
			List<PropertyCheck> checkList = new ArrayList<>();
			int lineNumber = 1;
			for (String line : lines) {

				Matcher matcher = pattern.matcher(line);

				if (matcher.find()) {
					int start;

					while ((start = line.indexOf("<%=")) != -1) {
						int end = line.indexOf("%>") + 2;
						String patStr = line.substring(start, end);
						String patName = patStr.substring(3, patStr.length() - 2).trim().substring(1);
						PropertyCheck prop = new PropertyCheck(fileName, patName, lineNumber);
						checkSet.add(prop);
						checkList.add(prop);
						line = line.replace(patStr, "");
					}
				}
				lineNumber++;
			}
			knownEnvMapList.put(fileName, checkList);
			knownEnvMapSet.put(fileName, checkSet);
		} catch (Exception e) {
			throw new MojoExecutionException("Couldn't open erb file: " + fileName, e);
		}
	}

	public String getStubFileType() {
		return stubFileType;
	}

	public void setStubFileType(String stubFileType) {
		this.stubFileType = stubFileType;
	}

	public boolean isShowMetaData() {
		return showMetaData;
	}

	public void setShowMetaData(boolean showMetaData) {
		this.showMetaData = showMetaData;
	}

}
