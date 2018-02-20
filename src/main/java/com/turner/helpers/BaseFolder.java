package com.turner.helpers;

import java.io.File;
import java.io.IOException;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.List;

import org.apache.maven.model.Resource;
import org.apache.maven.project.MavenProject;

public class BaseFolder {
	private Path tempPath;
	
	private MavenProject project;
	public BaseFolder(MavenProject project) {
		
		this.project=project;
	}
	public final Path createTempResources() throws IOException {
		//System.out.println(System.getProperty("java.io.tmpdir"));
		tempPath=Files.createTempDirectory("build");
		List<Resource> resources=project.getResources();
		for(Resource resource: resources) {
			
			Path veryTemp=tempPath.toAbsolutePath();
			if(resource.getTargetPath()!=null)
				veryTemp=veryTemp.resolve(resource.getTargetPath());
			if(Files.exists(Paths.get(resource.getDirectory())))
				copyDir(resource.getDirectory(),veryTemp.toAbsolutePath().toString(),true);
		}
		return tempPath;
	}
	public  void cleanUp()  {
		 try{Files.walk(tempPath)
		    .sorted(Comparator.reverseOrder())
		    .map(Path::toFile)
		    .forEach(File::delete);
		 }
		 catch(Exception e) {
			 e.printStackTrace();
		 }
	}
	private static void copyDir(String src, String dest, boolean overwrite) {
	    try {
	        Files.walk(Paths.get(src)).forEach(a -> {
	            Path b = Paths.get(dest, a.toString().substring(src.length()));
	            try {
	           
	                if (!a.toString().equals(src)) {
	                		if(!Files.exists(b.getParent()))
	                			Files.createDirectories(b.getParent());
	                		
	                		Files.copy(a, b, overwrite ? new CopyOption[]{StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.COPY_ATTRIBUTES} : new CopyOption[]{});
	                }
                } catch (IOException e) {
                e.printStackTrace();
	            }
	        });
	    } catch (IOException e) {
	        //permission issue
	        e.printStackTrace();
	    }
	}
	public Path getTempPath() {
		return tempPath;
	}
	
}
