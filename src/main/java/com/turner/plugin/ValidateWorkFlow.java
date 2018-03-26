package com.turner.plugin;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.turner.helpers.BaseFolder;
import com.turner.helpers.ErbReplace;
import com.turner.helpers.ImportXiIncludeFiles;
import com.turner.helpers.ResourceUtil;

@Mojo(name = "validateWorkflow", defaultPhase = LifecyclePhase.COMPILE)
public class ValidateWorkFlow extends AbstractMojo {
	private List<String> missingActions = new ArrayList<>();
	private List<String> errorMsg = new ArrayList<>();

	@Parameter(property = "workflowFile", required = true, defaultValue = "cfg/workflow.xml")
	private String workflowFile;

	@Parameter(property = "failOnError", required = false, defaultValue = "true")
	private boolean failOnError;
	

	@Parameter(property = "resourceJson", required = false)
	private String resourceJson;
	
	private MavenProject project;
	private BaseFolder baseFolder;
	@Override
	public void execute() throws MojoExecutionException {
		
		project = (MavenProject) getPluginContext().get("project");
		if(resourceJson!=null) {
			ResourceUtil.setAdditionalResourceFolder(resourceJson, project);
		}
		baseFolder=new BaseFolder(project);
		
		Path tempDir=null;
		try {
			tempDir=baseFolder.createTempResources();
		} catch (IOException e1) {
			throw new MojoExecutionException("couldn't create temp directory",e1);
		}
		try {
			ErbReplace.replaceERB(tempDir);
			ImportXiIncludeFiles include = new ImportXiIncludeFiles();
			Document doc = include.createCombinedWorkflowFile(tempDir.toAbsolutePath().toString(),
					this.workflowFile);

			NodeList actionNodes = doc.getElementsByTagName("action");
			List<ActionRoot> actions = new ArrayList<>();
			for (int i = 0; i < actionNodes.getLength(); i++) {
				Element action = (Element) actionNodes.item(i);
				String name = action.getAttribute("name");
				String clazz = action.getAttribute("class");
				ActionRoot ac = new ActionRoot();
				ac.setName(name);
				ac.setClazz(clazz);
				ac.setElement(action);
				actions.add(ac);
			}
			checkRepeatedActionName(actions);
			checkLoadableClasses(actions);
			checkSuccessActions(actions);
			if(errorMsg.size()>0) {
				getLog().error("--------------------------------------");
				getLog().error(this.workflowFile+" Errors");
				getLog().error("--------------------------------------");
				for(String error:errorMsg)
					getLog().info(error);
				getLog().error("--------------------------------------");
				getLog().error("Errors End");
				getLog().error("--------------------------------------");

			}
		} catch (Exception e) {
			if(failOnError)
				throw new MojoExecutionException("ValidateWorkFlow plugin failed to execute", e);
			else
				e.printStackTrace();
		}finally {
			getLog().info(baseFolder.getTempPath().toString());
			//baseFolder.cleanUp();
		}
		if(failOnError && errorMsg.size()>0)
			throw new MojoExecutionException("Errors exist in the workflow and failOnError enabled");

	}

	private void checkSuccessActions(List<ActionRoot> actions) {

		for (ActionRoot action : actions) {
			List<Element> nodes = getShallowSuccessFail(action.getElement());
			for (int i = 0; i < nodes.size(); i++) {
				Element el = nodes.get(i);
				ActionChain ac = commonChain(el, action, actions);
				checkSuccessActionsHelper(ac, actions);
			}

		}

	}

	private ActionChain commonChain(Element el, Action parentChain, List<ActionRoot> actionRootList) {
		ActionChain childAc = new ActionChain(parentChain);
		String name = el.getAttribute("action");

		childAc.setName(name);
		childAc.setElement(el);
		childAc.setFailNode(el.getTagName().equalsIgnoreCase("failure"));
		if (!hasAction(actionRootList, name)) {
			String prepend="";
			if(childAc.isFailNode) {
				prepend="Failue";
			}else
				prepend="Success";
			String msg = prepend+" Action is missing " + childAc.getAuditTrail();
			errorMsg.add(msg);
		}
		return childAc;
	}

	private void checkSuccessActionsHelper(ActionChain ac, List<ActionRoot> actionRootList) {
		List<Element> list = getShallowSuccessFail(ac.getElement());
		for (Element el : list) {
			ActionChain childAc = commonChain(el, ac, actionRootList);
			checkSuccessActionsHelper(childAc, actionRootList);
		}
	}

	private boolean hasAction(List<ActionRoot> actionRootList, String actionName) {
		for (int i = 0; i < actionRootList.size(); i++) {
			if (actionRootList.get(i).getName().equals(actionName)) {
				return true;
			}
		}
		return false;
	}

	private List<Element> getShallowSuccessFail(Element el) {
		NodeList list = el.getChildNodes();
		List<Element> newList = new ArrayList<>();
		for (int i = 0; i < list.getLength(); i++) {
			if (list.item(i).getNodeType() == Node.ELEMENT_NODE) {
				Element child = (Element) list.item(i);
				if (child.getTagName().equalsIgnoreCase("success") || child.getTagName().equalsIgnoreCase("failure"))
					newList.add(child);
			}
		}
		return newList;
	}

	private void checkLoadableClasses(List<ActionRoot> actions) throws ClassNotFoundException, IOException {
		List<Artifact> artifactList = new ArrayList<>(project.getDependencyArtifacts());

		List<String> classes = GetDependenciesClasses(artifactList);
		List<String> sourceClasses = getJavaSourceClasses();
		classes.addAll(sourceClasses);
		for (ActionRoot ar : actions) {
			if (!classes.contains(ar.getClazz())) {
				String msg = "Class:" + ar.getClazz() + " is not in the classPath";
				errorMsg.add(msg);
			}
		}

	}

	@SuppressWarnings("rawtypes")
	public List<String> GetDependenciesClasses(List<Artifact> arts) throws IOException, ClassNotFoundException {
		List<String> classes = new ArrayList<>();
		for (Artifact art : arts) {
			if (!art.getScope().equals(Artifact.SCOPE_COMPILE))
				continue;
			String pathToJar = null;
				pathToJar = art.getFile().getAbsolutePath();	
			try (JarFile jarFile = new JarFile(art.getFile())) {
				Enumeration<JarEntry> e = jarFile.entries();
				URL[] urls = { new URL("jar:file:" + pathToJar + "!/") };
				URLClassLoader cl = URLClassLoader.newInstance(urls);
				while (e.hasMoreElements()) {
					JarEntry je = e.nextElement();
					if (je.isDirectory() || !je.getName().endsWith(".class")) {
						continue;
					}
					String className = je.getName().substring(0, je.getName().length() - 6);
					className = className.replace('/', '.');
					classes.add(className);
				}
			}
		}
		return classes;
	}

	public List<String> getJavaSourceClasses() throws IOException {
		List<String> classList = new ArrayList<>();
		List<String> roots = project.getCompileSourceRoots();
		Function<String, Function<Path, String>> replaceWithJavaQualifiedName = (rootSource) -> (path) -> {
			return path.toString().replace(rootSource, "").replace(".java", "").replaceAll("/", ".");
		};

		for (String rootSource : roots) {
			Function<Path, String> func = replaceWithJavaQualifiedName.apply(rootSource + "/");
			List<String> classes = Files.walk(Paths.get(rootSource))
					.filter((pathz) -> pathz.toString().toLowerCase().endsWith(".java")).map(func)
					.collect(Collectors.toList());
			classList.addAll(classes);
		}
		return classList;
	}

	private void checkRepeatedActionName(List<ActionRoot> actions) {
		Map<String, Integer> actionNamesCount = new HashMap<>();
		for (Action action : actions) {

			if (actionNamesCount.get(action.getName()) == null) {
				actionNamesCount.put(action.getName(), 0);
			} else {
				Integer num = actionNamesCount.get(action.getName());
				actionNamesCount.put(action.getName(), ++num);

			}
		}
		for (Entry<String, Integer> entry : actionNamesCount.entrySet()) {
			if (entry.getValue() > 0) {
				String s="";
				if(entry.getValue()==1)
					s="s";
				String msg = "Action: " + entry.getKey() + " is repeats " + entry.getValue() + " time"+s;
				errorMsg.add(msg);
			}
		}

	}

	private String getSuccessActions(List<ActionChain> actionsList, Node node) {
		Element el = (Element) node;
		NodeList successes = el.getElementsByTagName("success");
		for (int i = 0; i < successes.getLength(); i++) {
			Element success = (Element) successes.item(i);
			String name = success.getAttribute("action");
			if (!actionsList.contains(name)) {
				missingActions.add(name);
			}
		}
		return null;
	}

	public String getWorkflowFile() {
		return workflowFile;
	}

	public void setWorkflowFile(String workflowFile) {
		this.workflowFile = workflowFile;
	}

	private static abstract class Action {
		private List<ActionChain> childrenActions;
		private String name;
		private Element element;

		public Action() {
			childrenActions = new ArrayList<>();
		}

		public void addChild(ActionChain ac) {
			childrenActions.add(ac);
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public abstract String getAuditTrail();

		public Element getElement() {
			return element;
		}

		public void setElement(Element element) {
			this.element = element;
		}

	}

	private static class ActionRoot extends Action {
		private String clazz;

		public String getClazz() {
			return clazz;
		}

		public void setClazz(String clazz) {
			this.clazz = clazz;
		}

		@Override
		public String getAuditTrail() {
			return this.getName();
		}

	}

	private static class ActionChain extends Action {
		private Action parent;
		private boolean isFailNode;

		public ActionChain() {
			super();

		}

		public ActionChain(Action parent) {
			super();
			if (parent == null)
				throw new RuntimeException("You'd better pass in a parent");
			this.parent = parent;
		}

		public Action getParent() {
			return parent;
		}

		public void setParent(Action parent) {
			this.parent = parent;
		}

		public boolean isFailNode() {
			return isFailNode;
		}

		public void setFailNode(boolean isFailNode) {
			this.isFailNode = isFailNode;
		}

		public String getAuditTrail() {
			if (parent != null)
				return this.parent.getAuditTrail() + ":" + this.getName();
			else
				return this.getName();

		}

	}

}
