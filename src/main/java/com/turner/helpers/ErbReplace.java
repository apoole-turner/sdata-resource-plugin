package com.turner.helpers;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.maven.plugin.MojoExecutionException;

public class ErbReplace {
	
	public static void main(String[] args) throws IOException, MojoExecutionException {
		//ErbReplace.replaceERB(Paths.get("/Users/apoole/git/sdata-pga-rydercup-points/target/classes/cfg"));
		System.out.println(System.getenv("house"));
	}
	
	
	private static void replaceFiles(String fileName,String targetFile) throws MojoExecutionException {
		try (BufferedReader in = new BufferedReader(new FileReader(fileName));	BufferedWriter out=new BufferedWriter(new FileWriter(targetFile));) {
			List<String> lines = Files.readAllLines(Paths.get(fileName));
			Pattern pattern = Pattern.compile("<%= *@([a-zA-Z0-9_]*) *%>");
			for (String line : lines) {
				Matcher matcher = pattern.matcher(line);
				if (matcher.find()) {
					int start;
					while ((start = line.indexOf("<%=")) != -1) {
						int end = line.indexOf("%>") + 2;
						String patStr = line.substring(start, end);
						line = line.replace(patStr, "");
					}
				}
				out.write(line+System.lineSeparator());
			}
		} catch (Exception e) {
			throw new MojoExecutionException("Couldn't open erb file: " + fileName, e);
		}
	}
	
	
	public static  void replaceERB(Path pathToResources) throws MojoExecutionException {
		Function<String, Path> toPath = (path) -> {
			return Paths.get(path);
		};
		List<Path> list = ResourceUtil.getErbFiles(pathToResources).stream().map(toPath).collect(Collectors.toList());
		for (Path path : list) {
			String erbFile = path.toAbsolutePath().toString();
			String newFile ="";
			if(erbFile.endsWith(".erb"))
				newFile=erbFile.substring(0, erbFile.indexOf(".erb"));
			else {
				Path parentFolder=path.getParent();
				newFile=parentFolder.toAbsolutePath()+"/"+path.getFileName().toString().substring(4);
			}
			replaceFiles(erbFile,newFile);
			;
		}
	}


}
