package propertyusage.handlers;

import java.awt.PageAttributes.PrintQualityType;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;

import oscar.OscarProperties;
import oscar.Startup;

/**
 * Our PropertyUsage handler extends AbstractHandler, an IHandler base class.
 * @see org.eclipse.core.commands.IHandler
 * @see org.eclipse.core.commands.AbstractHandler
 */
public class PropertyUsage extends AbstractHandler {

	private static final String JDT_NATURE = "org.eclipse.jdt.core.javanature";

	// Load all key/value pairs from Oscar property file.  Expects a copy of
	// oscar_mcmaster.properties in user's home directory.  All commented out
	// properties in original should be uncommented in this copy.
	private OscarProperties op = loadOscarProperties();

	// Load OscarProperty methods so they are immediately available
	final private Set<String> oscarPropertyMethods = loadOscarPropertiesMethods();
	// Used to store OscarProperty methods eventually found by visiting AST nodes.
	// Will compare with those in loadOscarPropertiesMethods().
	final private Set<String> oscarPropertyMethodsCheck = new HashSet<String>();

	// Store nodes with Oscar properties
	final private Set<ASTNode> astNodes = new HashSet<ASTNode>();
	final private Set<ASTNode> astOscarPropertyNodes = new HashSet<ASTNode>();

	// Used to store {<key, [number of usages, number of boolean usages]>}.
	//inal private Map<String, Integer[]> propMap = initializePropertyMap();
	final private Map<String, Integer[]> allPropMap = initializePropertyMap(); //new HashMap<String, Integer[]>();

	// an array of strings that could represent Properties that are instances
	// of OscarProperties.getInstance()
	final String[] possibleOPVarsStr = {"oscarVariables.getProperty","ap.getProperty",
			"op.getProperty","prop.getProperty","oscarVars.getProperty",
			"p.getProperty","proppies.getProperty","opr.getProperty",
			"pvar.getProperty","p2.getProperty","props.getProperty","pr.getProperty"};

	final private Pattern pattern = Pattern.compile("\\bProperties\\b");

	private String varName = null;
	private Boolean isOscarPropertiesVariable = null;

	private Boolean possibleOPExpression(String string) {
		Boolean result = false;
		for (int i=0; i < possibleOPVarsStr.length; i++) {
			if (string.contains(possibleOPVarsStr[i])) {
				return true;
			}
		}
		return result;
	}

	//	final private PropertySet ps = PropertySet.getInstance();

	private void printMethods() {
		for (String methodName: oscarPropertyMethods) {
			System.out.println(methodName);
		}
	}

	public OscarProperties loadOscarProperties() {
		Startup start = new Startup();
		start.contextInitialized();
		OscarProperties op = OscarProperties.getInstance();
		//
		//		for(String key : op.stringPropertyNames()) {
		//			String value = op.getProperty(key);
		//			System.out.println(key + " => " + value);
		//		}			
		//		System.out.println("Oscar Property count:" +op.size());
		return op;
	}

	// Initialization adds all properties found in the Oscar properties
	// file.  Needed to detect properties in the Oscar properties file
	// that are not actually used in the code base.
	public Map<String, Integer[]> initializePropertyMap() {
		Map<String, Integer[]> propMap = new HashMap<String, Integer[]>();
		Integer[] init_val = new Integer[] {0,0};
		for(String key : op.stringPropertyNames()) {
			propMap.put(key, init_val);
		}
		System.out.println("Oscar Property map count:" +propMap.size());
		return propMap;
	}

	// Records count of properties found with provision for adding
	// properties not present in the Oscar properties file.
	public void addProperty(String key, Boolean b) {
		if (!allPropMap.containsKey(key)) {
			// not present in Oscar properties file
			// and first time encountered in the code base
			allPropMap.put(key, new Integer[]{0,0});
		}
		Integer[] intArray = allPropMap.get(key);
		int totalCount = intArray[0] + 1;
		int boolCount;
		if (b) {
			boolCount = intArray[1] + 1;
		} else {
			boolCount = intArray[1];
		}
		allPropMap.put(key, new Integer[] {totalCount, boolCount});	
	}

	public Set<String> loadOscarPropertiesMethods() {
		String[] methods = 
			{"getProperty", "getInstance", "readFromFile", "hasProperty",
					"getBooleanProperty", "isPropertyActive", "getStartTime",
					"isTorontoRFQ", "isProviderNoAuto", "isPINEncripted", "isSiteSecured",
					"isAdminOptionOn", "isLogAccessClient", "isLogAccessProgram",
					"isAccountLockingEnabled", "isOntarioBillingRegion",
					"isBritishColumbiaBillingRegion", "isAlbertaBillingRegion",
					"isCaisiLoaded", "getDbType", "getDbUserName", "getDbPassword",
					"getDbUri", "getDbDriver", "getBuildDate", "getBuildTag",
					"isOscarLearning", "faxEnabled", "isRxFaxEnabled",
					"isConsultationFaxEnabled", "isEFormSignatureEnabled",
					"isEFormFaxEnabled", "isFaxEnabled", "isRxSignatureEnabled",
					"isConsultationSignatureEnabled", "isSpireClientEnabled",
					"getSpireClientRunFrequency", "getSpireServerUser",
					"getSpireServerPassword", "getSpireServerHostname",
					"getSpireDownloadDir", "getHL7A04BuildDirectory",
					"getHL7A04SentDirectory", "getHL7A04FailDirectory",
					"getHL7SendingApplication", "getHL7SendingFacility",
					"getHL7ReceivingApplication", "getHL7ReceivingFacility",
					"isHL7A04GenerationEnabled", "isEmeraldHL7A04TransportTaskEnabled",
					"getEmeraldHL7A04TransportAddr", "getEmeraldHL7A04TransportPort",
					"getIntakeProgramAccessServiceId", "getIntakeProgramCashServiceId",
					"getIntakeProgramAccessFId", "getConfidentialityStatement",
					"getIntakeProgramCashFId", "isLdapAuthenticationEnabled"};
		Set<String> theSet = new HashSet<String>();
		Collections.addAll(theSet, methods);
		return theSet;
	}

	private Boolean compareOscarPropertiesMethods(Set<String> hardCodedSet, Set<String> discoveredSet) {
		Boolean result = true;
		for (String s: hardCodedSet) {
			// the 'getProperty' method is inherited from Properties so not specified in OscarProperties
			if (!s.equals("getProperty") && !discoveredSet.contains(s)) {
				System.out.println("Property not found in discovered set: " + s);
				return false;
			}
		}
		for (String s: discoveredSet) {
			if (!s.equals("OscarProperties") && !hardCodedSet.contains(s)) {
				System.out.println("Property not found in hardcoded set: " + s);
				return false;
			}
		}
		return result;
	}

	public void reportResults() {
		System.out.println("Method count: " +oscarPropertyMethods.size());
		System.out.println("Method nodes found: " + astOscarPropertyNodes.size());
		//printMethods();
		if (!compareOscarPropertiesMethods(oscarPropertyMethods, oscarPropertyMethodsCheck)) {
			System.out.println("Hard-coded OscarProperties methods no longer match methods discovered in OscarProperies.java");
		} else {
			System.out.println("No new OscarProperties methods found");
		}
		System.out.println("Property, Num Usages, Num Boolean Usages, %Boolean, Known Property, Is Set");
		for (String s : allPropMap.keySet()) {
			Boolean knownProperty = false;
			Boolean isSet = false;
			if (op.hasProperty(s)) {
				knownProperty = true;
				isSet = op.isPropertyActive(s);
			}
			System.out.print("" + s + "," + allPropMap.get(s)[0] + "," + allPropMap.get(s)[1]);
			if (allPropMap.get(s)[0]>0) {
				System.out.print(","+100*allPropMap.get(s)[1]/(allPropMap.get(s)[0]));
			} else {
				System.out.print(",");
			}
			if (knownProperty) {
				System.out.println(","+knownProperty+","+isSet);
			} else {
				System.out.println(","+knownProperty+",");
			}

		}

		System.out.println("Nodes found: "+astNodes.size());
		//		System.out.println("PropertyInstances found: "+ps.size());
		//		for (ASTNode node: astNodes) {
		//			System.out.println("StringLiteral Full Expression: " + getFullExpression((StringLiteral) node));
		//		}
		//		ArrayList<String> notFound = new ArrayList<String>();
		//		int count=0;
		//		for (String p: op.stringPropertyNames()) {
		//			count++;
		//			Boolean found = false;
		//			for (ASTNode node: astNodes) {
		//				if (node instanceof StringLiteral && p.equals(((StringLiteral) node).getLiteralValue())) {
		//					found = true;
		//					break;
		//				}
		//			}
		//			if (!found) {
		//				notFound.add(p);
		//			}
		//
		//		}
		System.out.println("Number properties: "+op.size());
		//		if (!notFound.isEmpty()) {
		//			System.out.println("[" + notFound.size() + "] properties not found:");
		//			Collections.sort(notFound);
		//			for (String s: notFound) {
		////				System.out.println(s);
		//			}
		//		}
	}

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		//		IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);
		//		MessageDialog.openInformation(
		//				window.getShell(),
		//				"PropertyUsage",
		//				"Determine Property Usage");

		findProject();

		return null;
	}

	public void findProject() {

		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IWorkspaceRoot root = workspace.getRoot();
		System.out.println("root " + root.getLocation().toOSString());
		// Get all projects in the workspace
		IProject[] projects = root.getProjects();
		// Loop over all projects
		for (IProject project : projects) {
			try {
				if (project.isOpen() && project.isNatureEnabled(JDT_NATURE)) {
					System.out.println("Project ["+project.getName()+"] has Java nature");
					analyseMethods(project);
				}
			} catch (CoreException e) {
				e.printStackTrace();
			}
		}
	}

	private void analyseMethods(IProject project) throws JavaModelException {
		IPackageFragment[] packages = JavaCore.create(project)
				.getPackageFragments();
		// parse(JavaCore.create(project));
		DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		Date date = new Date();
		System.out.println(dateFormat.format(date)); //2016/11/16 12:08:43
		for (IPackageFragment mypackage : packages) {
			//System.out.println("mypackage.getElementName: " + mypackage.getElementName());
			if (mypackage.getKind() == IPackageFragmentRoot.K_SOURCE) {
				//System.out.println("is java source");
				createAST(mypackage);
			}
		}
		reportResults();
		System.out.println(dateFormat.format(new Date()));

	}

	private void createAST(IPackageFragment mypackage)
			throws JavaModelException {

		for (ICompilationUnit unit : mypackage.getCompilationUnits()) {

			CompilationUnit cu = parse(unit);

			cu.accept(new ASTVisitor() {

				// Search oscar.OscarProperties for methods
				@Override
				public boolean visit(MethodDeclaration node) {
					// store names of all the oscar.OscarProperty methods to detect additions/deletions
					if (cu.getPackage().getName().toString().equals("oscar") && cu.getJavaElement().getElementName().equals("OscarProperties.java")) {
						Collections.addAll(oscarPropertyMethodsCheck, node.getName().toString());
						Boolean r = astOscarPropertyNodes.add(node);
						if (!r) {
							System.out.println("Unxpected presence of node " + node.getName().toString());
						}
					}
					return true;
				}

				public boolean visit(MethodInvocation node) {
					IMethodBinding binding = node.resolveMethodBinding();
					if (binding != null) {
						ITypeBinding type = binding.getDeclaringClass();
						if (type != null && type.getName().toString().equals("OscarProperties")) {
							if (!node.arguments().isEmpty() && node.arguments().get(0) instanceof StringLiteral) {
								String key = ((StringLiteral) node.arguments().get(0)).getLiteralValue();
								Boolean r = astNodes.add(node);
								Boolean b = isBoolean((StringLiteral) node.arguments().get(0));
								addProperty(key, b);
//							} else if (!node.arguments().isEmpty()) {
//								System.out.println("argument: " + node.arguments().get(0).toString());
//								System.out.println("node: " + node.toString());
//								if (node.arguments().get(0) instanceof SimpleName) {
//									varName = node.arguments().get(0).toString();
//									System.out.println("simplename: " + node.arguments().get(0).toString());
//									cu.accept(new ASTVisitor() {
//										public boolean visit(VariableDeclarationStatement node) {
//											for (Iterator iter = node.fragments().iterator(); iter.hasNext();) {
//												VariableDeclarationFragment fragment = (VariableDeclarationFragment) iter
//														.next();
//												if (fragment.toString().startsWith(varName+"=")) {
//													System.out.println("  var decl: "+ node.toString());
//													System.out.println("  in " +getPackageAndFilename(cu));
//													break;
//												}
//											}
//											return true;
//										}
//										public boolean visit(Assignment node) {
//											if (node.getLeftHandSide() instanceof SimpleName &&
//													node.getLeftHandSide().toString().equals(varName)) {
//												IBinding binding = ((SimpleName) node.getLeftHandSide()).resolveBinding();
//												ITypeBinding type = node.getRightHandSide().resolveTypeBinding();
//												System.out.println(getPackageAndFilename(cu));
//												System.out.println("Assignment " + node.toString());
//												System.out.println("lhs binding " + binding.toString());
//											}
//											return true;
//										}
//									});
//								}
							}
						} else if (node.getName().toString().equals("get") &&
								(node.toString().startsWith("OscarProperties.getInstance().get(") ||
										node.toString().startsWith("oscar.OscarProperties.getInstance().get("))) {
							// finds OscarProperties.getInstance().get("myoscar_server_base_url")
							if (!node.arguments().isEmpty() && node.arguments().get(0) instanceof StringLiteral) {
								String key = ((StringLiteral) node.arguments().get(0)).getLiteralValue();
								Boolean r = astNodes.add(node);
								Boolean b = isBoolean((StringLiteral) node.arguments().get(0));
								addProperty(key, b);
							}
						} else if (type != null && type.getName().toString().equals("OscarPropertiesCheck")) {
							if (!node.arguments().isEmpty() && node.arguments().get(0) instanceof StringLiteral) {
								String key = ((StringLiteral) node.arguments().get(0)).getLiteralValue();
								if (!(node.toString().contains(".setValue(\""+key+"\")") ||
										node.toString().contains(".setDefaultVal(\""+key+"\")") ||
										node.toString().contains(".setReverse(\""+key+"\")") )) {
									Boolean r = astNodes.add(node);
									Boolean b = true;
									addProperty(key, b);
								}
							}	
						} else if (type != null && type.getName().toString().equals("IsPropertiesOn")) {
							if (!node.arguments().isEmpty() && node.arguments().get(0) instanceof StringLiteral) {
								String key = ((StringLiteral) node.arguments().get(0)).getLiteralValue();
								Boolean r = astNodes.add(node);
								Boolean b = true;
								addProperty(key, b);
							}
						} else if (type != null && type.getName().toString().equals("IsModuleLoadTag")) {
							if (!node.arguments().isEmpty() && node.arguments().get(0) instanceof StringLiteral) {
								String key = ((StringLiteral) node.arguments().get(0)).getLiteralValue();
								if (node.toString().contains(".setModuleName(\""+key+"\")")) {
									Boolean r = astNodes.add(node);
									Boolean b = true;
									addProperty(key, b);
								}
							}		
						} else if (type != null && type.getName().toString().equals("Properties")) {
							if (!node.arguments().isEmpty() && node.arguments().get(0) instanceof StringLiteral) {
								String key = ((StringLiteral) node.arguments().get(0)).getLiteralValue();

								String[] tmp = node.toString().split("\\.getProperty\\(");
								if (tmp.length > 1) {
									varName=tmp[0];
									isOscarPropertiesVariable = false;
									//System.out.println("varName: " + varName);
									cu.accept(new ASTVisitor() {
										public boolean visit(VariableDeclarationStatement node) {
											for (Iterator iter = node.fragments().iterator(); iter.hasNext();) {
												VariableDeclarationFragment fragment = (VariableDeclarationFragment) iter
														.next();
												if (fragment.toString().startsWith(varName+"=OscarProperties.getInstance()") ||
														fragment.toString().startsWith(varName+"=oscar.OscarProperties.getInstance()")) {
													isOscarPropertiesVariable = true;
													break;
												}
											}
											return true;
										}
									});
									if (isOscarPropertiesVariable) {
										Boolean b = isBoolean((StringLiteral) node.arguments().get(0));
										addProperty(key, b);
									}
								}
							}
						}
					}
					return true;
				}


				//				public boolean visit(VariableDeclarationStatement node) {
				//					for (Iterator iter = node.fragments().iterator(); iter.hasNext();) {
				//						VariableDeclarationFragment fragment = (VariableDeclarationFragment) iter
				//								.next();
				//
				//						// VariableDeclarationFragment: is the plain variable declaration
				//						// part. Example:
				//						// "int x=0, y=0;" contains two VariableDeclarationFragments, "x=0"
				//						// and "y=0"
				//						if (fragment.toString().contains("OscarProperties")) {
				//							Matcher m = pattern.matcher(node.toString());
				//							if (m.find()) {
				//								System.out.println(getPackageAndFilename(cu));
				//								System.out.println("Var Declar " + fragment.toString());
				//								IVariableBinding binding = fragment.resolveBinding();
				//								if (binding!=null) {
				//									System.out.print("Binding: " + binding.getName());
				//									if (!cu.toString().contains(binding.getName()+".getProperty")) {
				//										System.out.println(" but no call to getProperty");
				//									} else {
				//										System.out.println("");
				//										getProperties(binding.getName(), cu.toString());
				//										ASTNode parent = node.getParent();
				//										if (!parent.toString().contains(binding.getName()+"=OscarProperties.getInstance") &&
				//												!parent.toString().contains(binding.getName()+"=oscar.OscarProperties.getInstance")
				//												) {
				//										System.out.println("node.getParent: "+node.getParent().toString());
				//										}
				//									}
				//								}
				//								System.out.println("Node: " + node.toString());
				//							}
				//						}
				//					}
				//					return true;
				//				}

			});
		}
	}

	/**
	 * Reads a ICompilationUnit and creates the AST DOM for manipulating the
	 * Java source file
	 * 
	 * @param unit
	 * @return
	 */

	private static CompilationUnit parse(ICompilationUnit unit) {
		ASTParser parser = ASTParser.newParser(AST.JLS8);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		parser.setSource(unit);
		parser.setResolveBindings(true);
		return (CompilationUnit) parser.createAST(null); // parse
	}

	private String getPackageAndFilename(CompilationUnit cu) {
		return "Package: " + cu.getPackage().getName().getFullyQualifiedName() +
				", File: " + cu.getJavaElement().getElementName();
	}

	private Boolean usesOscarProperties(CompilationUnit cu) {
		Boolean result = false;
		// Check whether oscar.OscarProperties is imported
		List<ImportDeclaration> ilist = cu.imports();
		for (ImportDeclaration inode: ilist) {
			if (inode.toString().contains("import oscar.OscarProperties;") ||
					(inode.toString().contains("import oscar.*;") &&
							cu.toString().contains("OscarProperties") &&
							!cu.toString().contains("util.OscarProperties"))) {
				result = true;
				break;
			}
		}
		// Check whether oscar.OscarProperties is called explicitly or class is in same package
		if (cu.toString().contains("oscar.OscarProperties") ||
				(cu.toString().contains("package oscar;") &&
						cu.toString().contains("OscarProperties") &&
						!cu.toString().contains("util.OscarProperties"))) {
			result = true;
		}
		// There is a second oscar.util.OscarProperties class!
		//		if (result == false && cu.toString().contains("OscarProperties") &&
		//				!cu.toString().contains("util.OscarProperties")) {
		//			System.out.print("Apparent Paradox: ");
		//			printPackageAndFilename(cu);
		//		}
		return result;
	}

	/**
	 * Gets the surrounding {@link Statement} of this a {@link SimpleName} ast
	 * node.
	 *
	 * @param reference
	 *            any {@link SimpleName}
	 * @return the surrounding {@link Statement} as found in the AST
	 *         parent-child hierarchy
	 */
	private Statement getParentStatement(SimpleName reference) {
		ASTNode node = reference;
		while (!(node instanceof Statement)) {
			node = node.getParent();
			if (node == null) break;
		}
		return (Statement) node;
	}

	private Statement getParentStatement(StringLiteral reference) {
		ASTNode node = reference;
		while (!(node instanceof Statement)) {
			node = node.getParent();
			if (node == null) break;
		}
		return (Statement) node;
	}

	private Expression getFullExpression(StringLiteral reference) {
		ASTNode node = reference;
		while (node.getParent() instanceof Expression) {
			node = node.getParent();
		}
		return (Expression) node;
	}

	private Boolean isBoolean(StringLiteral reference) {
		Boolean result = false;
		ASTNode node = reference;
		ITypeBinding expressionType;
		while (node.getParent() instanceof Expression) {
			node = node.getParent();
			expressionType = (ITypeBinding)((Expression)node).resolveTypeBinding();
			if (expressionType != null && expressionType.getQualifiedName() != null &&
					(expressionType.getQualifiedName().contains("Boolean") ||
							expressionType.getQualifiedName().contains("boolean"))) {
				result = true;
				break;
			}
		}
		return result;
	}

	private Boolean hasOscarProperty(CompilationUnit cu, Expression e) {
		if (!usesOscarProperties(cu)) {
			return false;
		}
		String testStr = e.toString();
		if (testStr.contains("OscarProperties")) {
			return true;
		}
		// need something better here
		if (testStr.contains("getProperty") || testStr.contains("getInstance")) {
			return true; // probably true if StringLiteral in oscar.properties
		} else {
			for (String method: oscarPropertyMethods) {
				if (testStr.contains(method)) {
					return true; // probably true
				}
			}
		}
		return false;
	}

	private String[] getProperties(String varName, String str) {
		String[] result = null;
		int start = str.indexOf( varName+".getProperty(" );
		int end = str.indexOf(")", start);
		String substr = str.substring(start, end);
		System.out.println("Property: " +substr);
		return result;
	}

	private Boolean hasOscarProperties(MethodInvocation node) {
		ASTNode n = node;
		Boolean result = false;
		String[] tmp = node.toString().split("\\.getProperty\\(");
		String varName = null;
		if (tmp.length > 1) {
			varName=tmp[0];
			while (n != null) {
				if (n.toString().contains(varName+"=OscarProperties.getInstance()") ||
						n.toString().contains(varName+"=oscar.OscarProperties.getInstance()") ||
						n.toString().contains(varName+" = OscarProperties.getInstance()")) {
					//System.out.println("has " + varName+"=OscarProperties.getInstance()");
					//System.out.println("Found OscarProperties here: " + node.toString());
					result = true;
					break;
				}
				n = n.getParent();
			}
		}
		return result;
	}

	/**
	 * Finds the parent {@link Block} of a {@link Statement}.
	 *
	 * @param s
	 *            the {@link Statement} to find the its parent {@link Block} for
	 * @return the parent block of {@code s}
	 */
	public static Block getParentBlock(Statement s) {
		ASTNode node = s;
		while (!(node instanceof Block)) {
			node = node.getParent();
		}
		return (Block) node;
	}

	public static Block getParentBlock(MethodInvocation s) {
		ASTNode node = s;
		while (!(node instanceof Block)) {
			node = node.getParent();
		}
		return (Block) node;
	}


}
