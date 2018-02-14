package com.turner.experiment;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

//@Mojo(name = "startFile", defaultPhase = LifecyclePhase.COMPILE)
public class StartFileGenerator extends AbstractMojo {
	private String[] reservedVariableNames = { "ENVIRONMENT_VARIABLES", "JAVA_VAR_REPLACE" };
 
	@Parameter(property = "greeting", required = true, defaultValue = "${project.version}")
	private String greeting;

	@Parameter(property = "erbSuffix", required = false, defaultValue = "true")
	private boolean erbSuffix;

	@Parameter(property = "envVariablesMap", required = false)
	private Map<String, String> envVariablesMap;

	@Parameter(property = "envPropertiesFilePath", required = true)
	private String envPropertiesFilePath;

	@Parameter(property = "templateFilePath", required = false)
	private String templateFilePath;

	@Parameter(property = "templateVariablesMap", required = false)
	private Map<String, String> templateVariablesMap;

	@Parameter(property = "templatePropertiesFilePath", required = true)
	private String templatePropertiesFilePath;

	@Override
	public void execute() throws MojoExecutionException {
		Map<String, String> envMap = new HashMap<>();
		Map<String, String> templateVarMap = new HashMap<>();
		File templateFile = null;
		List<String> erbFiles = erbFiles();
		if (envPropertiesFilePath != null) {
			Properties props = new Properties();
			try {
				props.load(new FileInputStream(new File(envPropertiesFilePath)));
				addPropsToMap(envMap, props);
			} catch (Exception e) {
				throw new MojoExecutionException("Failed to load envPropertiesFilePath", e);
			}
		}
		if (templateVariablesMap != null && templateVariablesMap.size() > 0) {
			templateVarMap.putAll(templateVariablesMap);
		}
		if (templatePropertiesFilePath != null) {
			Properties props = new Properties();
			try {
				props.load(new FileInputStream(new File(templatePropertiesFilePath)));
				addPropsToMap(templateVarMap, props);
			} catch (Exception e) {
				throw new MojoExecutionException("Failed to load templatePropertiesFilePath", e);
			}
		} else {
			Properties props = new Properties();
			try {
				props.load(new FileInputStream(new File(StartFileGenerator.class.getClassLoader()
						.getResource("start-file-generator/DefaultStartVariables.properties").getFile())));
				addPropsToMap(templateVarMap, props);
			} catch (Exception e) {
				throw new MojoExecutionException("Failed to load templatePropertiesFilePath", e);
			}
		}
		if (templateVariablesMap != null && templateVariablesMap.size() > 0) {
			templateVarMap.putAll(templateVariablesMap);
		}
		String possibleUsedReservedWord = hasReservedName(templateVarMap);
		if (possibleUsedReservedWord != null) {
			throw new MojoExecutionException(
					"Template Variables can not use one of the servered words: " + possibleUsedReservedWord);
		}

		if (templateFilePath != null) {
			templateFile = new File(getClassOrAbsPath(templateFilePath));

		} else {
			templateFile = new File(StartFileGenerator.class.getClassLoader()
					.getResource("start-file-generator/start.sh.temp").getFile());
		}
		List<String> templateLines = null;
		try {
			templateLines = Files.readAllLines(Paths.get(templateFile.toURI()));
		} catch (IOException e) {
			throw new MojoExecutionException("Failed to read templateFile", e);
		}
	}

	private String hasReservedName(Map<String, String> map) {

		for (String str : this.reservedVariableNames)
			if (map.get(str) != null)
				return str;
		return null;
	}

	private List<String> erbFiles() {
		Path resourcePath = Paths.get(this.getClass().getClassLoader().getResource("").getFile());
		String rootPath = resourcePath.getFileName().toString();
		int rootPathLength = rootPath.length();
		Function<Path, String> shortenPath = (path) -> {
			String pathStr = path.toString();
			int indexOfEndOfRoot = pathStr.indexOf(rootPath);
			return pathStr.substring(indexOfEndOfRoot+rootPathLength+1);
		};
		
		try {
			
			Stream<Path> stream = Files.walk(resourcePath).sorted(Collections.reverseOrder())
					.filter((pathz) -> pathz.toString().toLowerCase().endsWith(".erb"));
			return stream.map(shortenPath).collect(Collectors.toList());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

	}

	private String getClassOrAbsPath(String strPath) {
		Path path = Paths.get(strPath);
		if (path.isAbsolute())
			return path.toString();
		else
			return StartFileGenerator.class.getClassLoader().getResource(strPath).getFile();

	}

	private void addPropsToMap(Map<String, String> map, Properties props) {
		for (final String name : props.stringPropertyNames())
			map.put(name, props.getProperty(name));
	}
}
