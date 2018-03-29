package com.turner.helpers;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class ImportXiIncludeFiles {
	public static void main(String[] args) throws Exception {
		ImportXiIncludeFiles horrid = new ImportXiIncludeFiles();
		//horrid.createCombinedWorkflowFile("cfg/workflow.xml");
		Path root=Paths.get("/blah/blah/man");
		System.out.println(root.resolve(Paths.get("fan/dan")));
	}
	private DocLink rootLink;
	private String rootPath;
	public void print() {
		this.rootLink.print();
	}
	public String cleanClassPathWorkFlowFileName(String classPathWorkflow) {
		StringBuilder sb=new StringBuilder(classPathWorkflow.trim());
		if(sb.charAt(0)=='/')
			sb.deleteCharAt(0);
		return rootPath+"/"+sb.toString();
	}
	
	public Document createCombinedWorkflowFile(String rootPath,String unCleanClassPathWorkflow) {
		if(rootPath==null) {
			this.rootPath="";
		}else {
			this.rootPath=rootPath;
		}
		String classPathWorkflow=cleanClassPathWorkFlowFileName(unCleanClassPathWorkflow);
		this.rootLink = new DocLink();
		this.rootLink.setLink(classPathWorkflow);
		try {
			findXiIncludes(this.rootLink);
			String sb = this.rootLink.createEmbbeddedDoc();
			InputStream stream = new ByteArrayInputStream(sb.getBytes(StandardCharsets.UTF_8.name()));
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			return db.parse(stream);
		} catch (Exception e) {
			throw new RuntimeException("Couldn't create a combined workflowFile",e);
		}

	}

	private void findXiIncludes(DocLink docParent) throws Exception {

		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();
		InputStream stream = new FileInputStream(docParent.link);

		Document doc = db.parse(stream);
		NodeList nodelist = doc.getElementsByTagName("xi:include");
		docParent.setDocRef(doc);
		for (int i = 0; i < nodelist.getLength(); i++) {
			Element el = (Element) nodelist.item(i);
			String s = el.getAttribute("href");
			DocLink docLink = new DocLink();

			docLink.setParent(docParent);
			docLink.setLink(cleanClassPathWorkFlowFileName(s));
			docParent.addChildDoc(docLink);
			findXiIncludes(docLink);
		}

	}

	protected static class DocLink {
		private List<DocLink> childDocLinkList;
		private Document docRef;
		private DocLink parent;
		private String link;

		public void print() {
			Pusher pusher = new Pusher();
			this.print(pusher);
		}

		public String createEmbbeddedDoc() throws Exception {
			List<DocLink> refs = new ArrayList<>();
			this.getFlattenReferences(refs);
			Document root = this.docRef;
			deleteXiIncludes(root);
			
			for (DocLink dockLink : refs) {
				readFileAndDeleteXiIncludesAndAppend(root, dockLink);
			}
			return getStringOfDocument(root); 
		}

		private void deleteXiIncludes(Document doc) {
			NodeList nodelistOfIncludeTags = doc.getElementsByTagName("xi:include");
			int size = nodelistOfIncludeTags.getLength();
			List<Element> xiIncludeList=new ArrayList<>();
			for (int i = 0; i < size; i++) {
				Element el = (Element) nodelistOfIncludeTags.item(i);
				xiIncludeList.add(el);
			}
			xiIncludeList.forEach((element)->{element.getParentNode().removeChild(element);});
		}

		private Document readFileAndDeleteXiIncludesAndAppend(Document rootDocument, DocLink docLink) throws Exception {
			Document doc = docLink.getDocRef();
			Element rootElement = (Element) doc.getElementsByTagName("workflow").item(0);
			deleteXiIncludes(doc);
			NodeList nodeList = rootElement.getChildNodes();
			for (int i = 0; i < nodeList.getLength(); i++) {
				Node el = (Node) rootDocument.importNode(nodeList.item(i), true);
				rootDocument.getDocumentElement().appendChild(el);
			}
			return rootDocument;
		}

		private String getStringOfDocument(Node doc) throws TransformerException {
			Transformer transformer = TransformerFactory.newInstance().newTransformer();
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			StreamResult result = new StreamResult(new StringWriter());
			DOMSource source = new DOMSource(doc);
			transformer.transform(source, result);

			String xmlString = result.getWriter().toString();
			return xmlString;
		}

		private void getFlattenReferences(List<DocLink> refs) {
			if (refs == null)
				refs = new ArrayList<>();
			for (DocLink doc : this.childDocLinkList) {
				refs.add(doc);
				doc.getFlattenReferences(refs);
			}

		}

		private void print(Pusher pusher) {

			String output = "";
			if (pusher.getColumns() > 0)
				output += "|";
			for (int i = 0; i < pusher.getColumns(); i++) {

				output += "-";
			}
			Pusher child = pusher.incrementedChild();
			for (DocLink doc : this.childDocLinkList) {
				doc.print(child);
			}
		}

		public DocLink() {
			this.childDocLinkList = new ArrayList<>();
		}

		public void addChildDoc(DocLink childDoc) {
			if (childDoc != null)
				this.childDocLinkList.add(childDoc);
		}

		public DocLink getParent() {
			return parent;
		}

		public void setParent(DocLink parent) {
			this.parent = parent;
		}

		public String getLink() {
			return link;
		}

		public void setLink(String link) {
			this.link = link;
		}

		public Document getDocRef() {
			return docRef;
		}

		public void setDocRef(Document docRef) {
			this.docRef = docRef;
		}

	}

	private static class Pusher {
		private int columns;
		private int rows;

		public Pusher() {
			this.columns = 0;
			this.rows = 0;
		}

		private Pusher(Pusher pusher) {
			this.columns = 1 + pusher.columns;
			this.rows = 1 + pusher.rows;
		}

		public Pusher incrementedChild() {
			Pusher child = new Pusher(this);
			return child;
		}

		public int getColumns() {
			return columns;
		}

		public int getRows() {
			return rows;
		}

	}
}
