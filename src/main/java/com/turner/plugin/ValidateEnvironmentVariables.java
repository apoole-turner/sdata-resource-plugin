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

import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import com.turner.helpers.PropertyCheck;
import com.turner.helpers.ResourceUtil;

@Mojo(name = "envValidate", defaultPhase = LifecyclePhase.COMPILE)
public class ValidateEnvironmentVariables extends AbstractMojo {
	@Parameter(name = "failOnError", required = false, defaultValue = "true")
	private boolean failOnError;

	@Parameter(property = "resourceJson", required = false)
	private String resourceJson;

	private Set<PropertyCheck> unknownEnvList;
	private MavenProject project;

	public void execute() throws MojoExecutionException {
		try {
			project = (MavenProject) getPluginContext().get("project");
			if (resourceJson != null) {
				ResourceUtil.setAdditionalResourceFolder(resourceJson, project);
			}
			unknownEnvList = new HashSet<>();
			List<Resource> projectResources = project.getResources();

			List<String> erbList = new ArrayList<>();
			for (Resource resource : projectResources) {
				if (resource.getDirectory() != null)
					erbList.addAll(ResourceUtil.getErbFiles(Paths.get(resource.getDirectory())));
			}
			getLog().info("Number of erb files: " + erbList.size() + "");
			for (String str : erbList) {
				readFiles(str);
			}

			getLog().info("Checking for Missing Variables ");
			getLog().info("--------------------------------------");
			Set<String> printedAlready = new HashSet<>();
			for (PropertyCheck unknown : unknownEnvList) {
				if (!printedAlready.contains(unknown.getProperty()))
					getLog().warn("Missing Property: " + unknown.getProperty());
				printedAlready.add(unknown.getProperty());
			}
			if (failOnError && unknownEnvList.size() > 0)
				throw new MojoExecutionException("Missing environment variables");
			getLog().info("--------------------------------------");
		} catch (Exception e) {
			if (failOnError)
				throw new MojoExecutionException("error occured", e);
			else
				e.printStackTrace();
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
						if (isMissing)
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
