package com.turner.helpers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.json.JSONArray;
import org.json.JSONObject;

public class ResourceUtil {
	public static void main(String[] args) throws MojoExecutionException {
		System.out.println(Paths.get("/df/dfdf/dfd").toString());
		setAdditionalResourceFolder("[{'cfg':'cfg/'},{null:null}]", null);
	}

	public static List<String> getErbFiles(Path folder) throws MojoExecutionException {
		if (!Files.isDirectory(folder)) {
			if (Files.isRegularFile(folder) && (folder.toString().toLowerCase().endsWith(".erb")
					|| folder.getFileName().toString().toLowerCase().startsWith("erb."))) {
				List<String> singleErbFileResource = new ArrayList<>();
				singleErbFileResource.add(folder.toString());
				return singleErbFileResource;
			}
			return new ArrayList<>();
		}
		Path resourcePath = folder.toAbsolutePath();
		Function<Path, String> toString = (path) -> {
			return path.toString();
		};
		try {
			Stream<Path> stream = Files.walk(resourcePath).sorted(Collections.reverseOrder())
					.filter((pathz) -> pathz.toString().toLowerCase().endsWith(".erb")
							|| pathz.getFileName().toString().toLowerCase().startsWith("erb."));
			return stream.map(toString).collect(Collectors.toList());
		} catch (IOException e) {
			throw new MojoExecutionException("Couldn't use Files.walk on " + resourcePath.toAbsolutePath(), e);
		}

	}
	public static void setAdditionalResourceFolder(String json, MavenProject project) throws MojoExecutionException {
		JSONArray array = null;
		try {
			array = new JSONArray(json);
			for (int i = 0; i < array.length(); i++) {
				JSONObject object = array.getJSONObject(i);
				for (String key : object.keySet()) {
					String source = makeRelative(key);
					String target = makeRelative(object.get(key).toString());
					target = makeValidTarget(target);
					Resource resource = new Resource();
					resource.setDirectory(project.getBasedir().getAbsolutePath() + "/" + source);// Add cfg to resource
																									// folder
					if (target != null)
						resource.setTargetPath(target);
					project.addResource(resource);

				}
			}
		} catch (Exception e) {
			throw new MojoExecutionException("Failed to parse Resource map", e);
		}

	}

	private static String makeValidTarget(String target) {
		if (target == null || target.trim().isEmpty() || target.trim().toLowerCase().equals("null")
				|| target.trim().toLowerCase().equals("undefined"))
			return null;
		else
			return target;
	}

	private static String makeRelative(String object) {
		object = object.trim();
		if (object.startsWith("/")) {
			return object.substring(1);
		}
		return object;
	}

}
