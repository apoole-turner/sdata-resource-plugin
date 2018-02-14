package com.turner.plugin;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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

import com.turner.helpers.BaseFolder;
import com.turner.helpers.PropertyCheck;

@Mojo(name = "envValidate", defaultPhase = LifecyclePhase.COMPILE)
public class ValidateEnvironmentVariables extends AbstractMojo {
	@Parameter(name = "failOnError", required = false, defaultValue = "true")
	private boolean failOnError;
	private Set<PropertyCheck> unknownEnvList;
	private MavenProject project;
	private BaseFolder baseFolder;

	public void execute() throws MojoExecutionException {
		project = (MavenProject) getPluginContext().get("project");
		baseFolder = new BaseFolder(project);
		try {
			baseFolder.createTempResources();
		} catch (IOException e) {
			throw new MojoExecutionException("Couldn't create temp directory", e);
		}
		unknownEnvList = new HashSet<>();
		List<String> list = erbFiles();
		getLog().info(list.size() + "");
		for (String str : list) {
			readFiles(str);
		}

		getLog().info("Checking for Missing Variables ");
		getLog().info("--------------------------------------");
		Set<String> printedAlready=new HashSet<>();
		for (PropertyCheck unknown : unknownEnvList) {
			if(!printedAlready.contains(unknown.getProperty()))
				getLog().warn("Missing Property: " + unknown.getProperty());
			printedAlready.add(unknown.getProperty());
		}
		if(failOnError && unknownEnvList.size()>0)
			throw new MojoExecutionException("Missing environment variables");
		getLog().info("--------------------------------------");
	}

	private List<String> erbFiles() throws MojoExecutionException {
		Path resourcePath = baseFolder.getTempPath();
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

	private void readFiles(String fileName) throws MojoExecutionException {
		try (BufferedReader in = new BufferedReader(new FileReader(fileName));) {
			List<String> lines = Files.readAllLines(Paths.get(fileName));
			Pattern pattern = Pattern.compile("<%= *@([a-zA-Z0-9_]*) *%>");
			int lineNumber = 0;
			for (String line : lines) {

				Matcher matcher = pattern.matcher(line);

				if (matcher.find()) {
					int start;

					while ((start = line.indexOf("<%=")) != -1) {
						int end = line.indexOf("%>") + 2;
						String patStr = line.substring(start, end);
						String patName = patStr.substring(3, patStr.length() - 2).trim().substring(1);
						Boolean isMissing = System.getenv(patName) == null;
						if (isMissing )
							this.unknownEnvList.add(new PropertyCheck(fileName, patName, lineNumber));

						line = line.replace(patStr, "");
					}
				}
				lineNumber++;
			}
		} catch (Exception e) {
			throw new MojoExecutionException("Couldn't open erb file: " + fileName, e);
		}
	}
}
