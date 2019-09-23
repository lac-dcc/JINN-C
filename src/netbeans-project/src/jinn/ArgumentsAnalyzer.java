/*
 * Copyright (C) 2019 juniocezar.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */
package jinn;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import jinn.util.Logger;
import soot.ArrayType;
import soot.Body;
import soot.jimple.Jimple;
import soot.BodyTransformer;
import soot.DoubleType;
import soot.IntType;
import soot.Local;
import soot.LongType;
import soot.BooleanType;
import soot.PrimType;
import soot.RefType;
import soot.Scene;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.Type;
import soot.Unit;
import soot.Value;
import soot.jimple.AssignStmt;
import soot.jimple.IntConstant;
import soot.jimple.InvokeExpr;
import soot.jimple.JimpleBody;
import soot.jimple.LengthExpr;
import soot.jimple.Stmt;
import soot.tagkit.AnnotationAnnotationElem;
import soot.tagkit.AnnotationArrayElem;
import soot.tagkit.AnnotationElem;
import soot.tagkit.AnnotationStringElem;
import soot.tagkit.AnnotationTag;
import soot.tagkit.Tag;
import soot.tagkit.VisibilityAnnotationTag;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.graph.UnitGraph;
import soot.util.Chain;

/**
 * This class is responsible for adding profiling information to target input
 * methods, which contains the @AdaptiveMethod tag.
 * @author juniocezar
 */
public class ArgumentsAnalyzer extends BodyTransformer {
    // classes we want to avoid analyzing
    private static Set<SootClass> excludedClasses;
    //
    // methods we already visited. -- Soot sometimes visit the same method
    // more than once, so we won't add profiling instrummentation every visit.
    private Set<SootMethod> visitedMethods;
    //
    // Counter for the profiling information inserted
    private static int profilingCounter = 0;
    //
    // Signatures Map.
    private Map<Integer, String> signatures;
    //
    // variables to be inserted in target methods.
    Local var_init, var_end, var_diff, var_text;
    Local var_builder, var_ps;
    //
    // space logger
    private String numSpaces = "  ";

    /**
     * Default constructor. Create all necessary shared objects;
     */
    public ArgumentsAnalyzer () {
        visitedMethods  = new HashSet<SootMethod>();
        excludedClasses = new HashSet<SootClass>();
        signatures = new HashMap <Integer, String> ();
    }

    @Override
    protected void internalTransform(Body body, String string, Map map) {
        SootClass sClass = body.getMethod().getDeclaringClass();
        SootMethod sMethod = body.getMethod();
        String methodName = sMethod.getName();
        //
        // discarting already visited methods
        if (visitedMethods.contains(sMethod)) {
            return;
        }
        //
        // discarting classes created by javac
        else if (methodName.contains("<init>") || methodName.contains("<clinit>")) {
            return;
        //
        // discarting classes from libraries
        } else if (! sClass.isApplicationClass()) {
            return;
        //
        // discarting classes from a pre-selected exclude list
        } else if (excludedClasses.contains(sClass)) {
            return;
        //
        // checking if remaining classes have AnnotationTag
        } else if (isAdaptiveMethod(sMethod)) {
            Logger.log("Found Adaptive Method: " + sMethod.getSignature());
            visitedMethods.add(sMethod);
            Map<String, List<String>> tags = parseJinnTags(sMethod);

            // logging
            if (tags.size() == 0) {
                Logger.shortlog("    -> method does not contain any special tag (@Input "
                        + "@HiddenInput)."
                        + "\n        JINN-C will try to use all parameters");
            } else {                
                Logger.shortlog("    -> method uses the tags = " + tags.toString());
                numSpaces = "  ";
            }
            synchronized (body) {
                profileMethod(tags, (JimpleBody)body);
                addDumper(body);
            }
        } else {
            //
            // the profiling data is sabed to memory. This method inserts a dump
            // to a file in the main method of the target application.
            addDumper(body);
        }
    }

    /**
     * Parse method Annotation Tags, looking for Jinn-C specific tags, like @Input.
     * @param method SootMethod to be analyzed.
     */
    private Map<String, List<String>> parseJinnTags (SootMethod method) {
        Map<String, List<String>> usedTags = new HashMap<String, List<String>> ();
        List<Tag> tags = method.getTags();
        Body body = method.retrieveActiveBody();
        List<Local> parameters  = body.getParameterLocals();

        for (Tag tag : tags) {
            if (tag instanceof VisibilityAnnotationTag) {
                ArrayList<AnnotationTag> aTags =
                        ((VisibilityAnnotationTag)tag).getAnnotations();
                for (AnnotationTag annotation : aTags) {
                    String name = annotation.getType();
                    ////System.err.println("\t " + name);
                    //
                    // Case when we have a single @HiddenInput or @Input annotation
                    if (name.equals("LHiddenInput;") || name.equals("LInput;")) {
                        String typeName = name.replace("L", "").replace(";", "");
                        AnnotationElem ae = annotation.getElems().iterator().next();
                        String expression = ((AnnotationStringElem)ae).getValue();
                        ////System.err.println("\t" + typeName +" (expr, param): " +
                       ////                            ((AnnotationStringElem)ae).getValue());
                        //
                        // Adding variable in tag to our annotation map
                        usedTags.put(typeName, Collections.singletonList(
                                                ((AnnotationStringElem)ae).getValue()));


                        // GENERAL NOTE: The rest of this if branch should be moved to
                        // another method
                        // ===========================================================================
                        boolean found = false;

                        SootClass sclass = method.getDeclaringClass();
                        String[] elements = expression.split("\\.");
                        String base = elements[0];
                        // first look for base of expression in globals of class
                        try {
                            SootField global = sclass.getFieldByName(base);
                            ////System.err.println("\tHiddenInput: " + base + " is a global var");
                            found = true;
                        } catch (RuntimeException e) {
                                //System.err.println("Error: " + base + " is not a global");
                        }
                        // second lookup base of expression in classpath
                        // NOTE: TODO: Currently only handling java.lang.Runtime classes
                        // This happens because we need to resolve the names before calling
                        // soot.Main(). To solve that we could have a previous stage which
                        // would find all required classes from path, add it to a file
                        // then the JinnDriver would parse this file and resolve all classes
                        // at the level of signatures.
//                        if (!found) {
//                            SootClass exprClass =
//                                       Scene.v().getSootClass("java.lang." + base + "");
//                            System.err.println("SootClass found: " + exprClass);
//                            // ToDo: Check if elements[1] is a field or a method
//                            // if method, do the same for elements[2] ...
//                            // After that, we may reconstruct the expression
//
//
//                        }
                    }
                    //
                    // Case when we have a single @Input annotation (included in above case)
//                    } else if (name.equals("LInput;")) {
//                        AnnotationElem ae = annotation.getElems().iterator().next();
//                        String inputName = ((AnnotationStringElem)ae).getValue();
//                        //
//                        // Adding variable in tag to our annotation map
//                        usedTags.put("Input", Collections.singletonList(
//                                                ((AnnotationStringElem)ae).getValue()));
//
//                        // GENERAL NOTE: The rest of this if branch should be moved to
//                        // another method
//                        // ===========================================================================
//                        boolean found = false;
//                        // first lookup name in formal params list
//                        for (Local parameter : parameters) {
//                            if (parameter.getName().equals(inputName)) {
//                                System.err.println("\tInput: " + inputName + " is a formal parameter");
//                                found = true;
//                                break;
//                            }
//                        }
//                        // second lookup name in global variables list
//                        if (!found) {
//                            SootClass sclass = method.getDeclaringClass();
//                            try {
//                                SootField global = sclass.getFieldByName(inputName);
//                                System.err.println("\tInput: " + inputName + " is a global var");
//                                found = true;
//                            } catch (RuntimeException e) {
//                                System.err.println("Error: " + inputName + " is neither a"
//                                        + " formal parameter, nor a global ");
//                            }
//                        }
//                        System.err.println("\tInput param: " + ((AnnotationStringElem)ae).getValue());
//                    }
                    //
                    // Case when we have multiple @Input annotations
                    if (name.equals("LInputs;") || name.equals("LHiddenInputs;")) {
                        String typeName = name.replace("L", "").replace(";", "");
                        List<String> innerTags = new ArrayList<String>();
                        Collection<AnnotationElem> annElemnts = annotation.getElems();
                        for (AnnotationElem ae : annElemnts) {
                            AnnotationArrayElem annArray = (soot.tagkit.AnnotationArrayElem) ae;
                            for (AnnotationElem annElemInner : annArray.getValues()) {
                                AnnotationAnnotationElem aae = (AnnotationAnnotationElem) annElemInner;
                                AnnotationStringElem annStr = (AnnotationStringElem)
                                        aae.getValue().getElems().iterator().next();
                                ////System.out.println("<>" + annStr.getValue());
                                //
                                // Adding variable in tag to our annotation map
                                innerTags.add(annStr.getValue());
//                                if (annElemInner instanceof AnnotationStringElem) {
//                                    String expression = ((AnnotationStringElem)annElemInner).getValue();
//                                    System.err.println(">>>" + expression);
//                                    String[] elements = expression.split("\\.");
//                                    System.err.println("Elements size: " + elements.length);
//                                    SootField field = method.getDeclaringClass().getFieldByName(elements[0]);
//                                    System.out.println(">> field: " + field);
//                                }
                            }
                        }
                        usedTags.put(typeName, innerTags);
                    }
                }
            }
        }
        return usedTags;
    }

        /**
     * Dumps the signature map <HashCode, SigName> to a file.
     */
    public void dumpMap () {
        try {
            FileWriter fw = new FileWriter("methods-signaturesMap.txt");
            BufferedWriter bw = new BufferedWriter(fw);
            for (Map.Entry<Integer, String> entry : signatures.entrySet()) {
                String value = entry.getValue();
                Integer key  = entry.getKey();
                //bw.write(key + ", " + value + "\n");
                bw.write(key + "\n");
            }
            bw.close();
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Return the signatures map created for the analyzed methods.
     * @return Map of <Integer, String> containing the map of Hash codes to signatures.
     */
    public Map<Integer, String> getSigMap () {
        return signatures;
    }


    /**
     * Iterates over all Tags of input method, looking for the @AdaptiveMethod
     * tag. Returns true if such tag is found, and false otherwise.
     * @param m Input method.
     * @return True if tag is present, false otherwise.
     */
    private boolean isAdaptiveMethod (SootMethod method) {
        List<Tag> tags = method.getTags();
        for (Tag tag : tags) {
            if (tag instanceof VisibilityAnnotationTag) {
                ArrayList<AnnotationTag> aTags =
                        ((VisibilityAnnotationTag)tag).getAnnotations();
                for (AnnotationTag annotation : aTags) {
                    String name = annotation.getType();
                    if (name.equals("LAdaptiveMethod;")) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
    // TODO: Temporary, remove this method below and use the top one
//        private boolean isAdaptiveMethod (SootMethod method) {
//        List<Tag> tags = method.getTags();
//        boolean found = false;
//        for (Tag tag : tags) {
//            if (tag instanceof VisibilityAnnotationTag) {
//                ArrayList<AnnotationTag> aTags =
//                        ((VisibilityAnnotationTag)tag).getAnnotations();
//                for (AnnotationTag annotation : aTags) {
//                    String name = annotation.getType();
//                    System.err.println("\t " + name);
//                    if (name.equals("LAdaptiveMethod;")) {
//                        found = true;
//                        //return true;
//                    }
//                    if (name.equals("LHiddenInputs;")) {
//                        Collection<AnnotationElem> ae = annotation.getElems();
//                        for (AnnotationElem k : ae) {
//                            AnnotationArrayElem novos = (soot.tagkit.AnnotationArrayElem) k;
//                            for (AnnotationElem veja : novos.getValues()) {
//                                AnnotationAnnotationElem aae = (AnnotationAnnotationElem) veja;
//                                AnnotationStringElem aas = (AnnotationStringElem)
//                                        aae.getValue().getElems().iterator().next();
//                                System.out.println("<>" + aas.getValue());
//                                if (veja instanceof AnnotationStringElem) {
//                                    String expression = ((AnnotationStringElem)veja).getValue();
//                                    System.err.println(">>>" + expression);
//                                    String[] elements = expression.split("\\.");
//                                    System.err.println("Elements size: " + elements.length);
//                                    SootField field = method.getDeclaringClass().getFieldByName(elements[0]);
//                                    System.out.println(">> field: " + field);
//                                }
//                            }
//                        }
//                    }
//                }
//            }
//        }
//        return found;
//    }


    /**
     * Get all exit points in the input body.
     * @param body Input body.
     * @return List of all exit points.
     */
    private List<Unit> findExitUnits (Body body) {
        // does not include calls to System.exit
        UnitGraph cfg = new  ExceptionalUnitGraph(body);
        List<Unit> exitNodes = new ArrayList<Unit>(cfg.getTails());
        // adding calls to System.exit()
        Chain<Unit> units = body.getUnits();
        for (Unit u : units) {
            if (u instanceof Stmt && ((Stmt)u).containsInvokeExpr()) {
                InvokeExpr invoke = ((Stmt)u).getInvokeExpr();
                SootMethod callee = invoke.getMethod();
                if (callee.getSignature().equals(
                    "<java.lang.System: void exit(int)>")) {
                    exitNodes.add(u);
                }
            }
        }
        return exitNodes;
    }

    /**
     * Initializes all local variables used for logging the input arguments
     * and total execution time of method.
     * @param body
     */
    private void initializeVariables(Body body) {
        //
        // Inserting the declaration of variable which we will use to collect
        // profiling information.
        var_init = insertDeclaration("$r_initTime" + profilingCounter,
                                                                "long", body);
        var_end  = insertDeclaration("$r_endTime"  + profilingCounter,
                                                                "long", body);
        var_diff = insertDeclaration("$r_diffTime" + profilingCounter,
                                                                "long", body);
        // var_text    = insertDeclaration("$r_pid_str", "java.lang.String", body);
        // var_builder = insertDeclaration("$r_str_builder", "java.lang.StringBuilder", body);
        // var_ps      = insertDeclaration("$r_ps", "java.io.PrintStream", body);
        //
        // incrementing our profiling counter
        profilingCounter++;
    }

    /**
     * Method for inserting local declaration in sootMethod.
     * @param name Local ASCII name.
     * @param type Local type.
     * @param body Method's body.
     * @return Reference to the local, already inserted into the method's body.
     */
    private Local insertDeclaration(String name, String type, Body body) {
        Local tmp;
        switch(type) {
            case "long":
                tmp = Jimple.v().newLocal(name, LongType.v());
                break;
            case "int":
                tmp = Jimple.v().newLocal(name, IntType.v());
                break;
            case "double":
                tmp = Jimple.v().newLocal(name, DoubleType.v());
                break;
            default:
                tmp = Jimple.v().newLocal(name, RefType.v(type));
                break;
        }

        // check if we already have the dec in method
        Chain<Local> locals = body.getLocals();
        for (Local l : locals) {
            if(l.equals(tmp))
                return l;
        }

        locals.add(tmp);
        return tmp;
    }

    /**
     * Adds a call to a dump method in the external logging library. The library then logs
     * the data to a file (ideally). This method looks for the main method of the target
     * application.
     * @param body Body of the main method.
     */
    private void addDumper (Body body) {
        boolean isMainMethod = body.getMethod().getSubSignature()
                                               .equals("void main(java.lang.String[])");
        // ToDo: This is the second call to findExitUnits. THey could be merged, to avoid
        // unnecessary computation.
        List<Unit> exitUnits = findExitUnits(body);
        Chain<Unit> units = body.getUnits();
        SootClass loggerClass  = Scene.v().getSootClass("jinn.exlib.DataLogger");
        for (Unit exit : exitUnits) {
            Unit dump = Jimple.v().newInvokeStmt(
                        Jimple.v().newStaticInvokeExpr(loggerClass.getMethod(
                        "void dump()").makeRef()));
            if (isMainMethod) {
                units.insertBefore(dump, exit);
            } else
              if (exit instanceof Stmt && ((Stmt)exit).containsInvokeExpr()) {
                units.insertBefore(dump, exit);
            }
        }
    }


    /**
     * Builds the format (ready for dumping) of the method signature + input
     * arguments + total execution time.
     * @param hashCode HashCode of the signature.
     * @param args Number of compatible arguments in method's signature.
     * @return Formatted string.
     */
    private String buildLoggerSignature (int hashCode, int args) {
        String loggerSignature = "void logStr(int,long,";
        for (int i = 0; i < args; i++) {
            loggerSignature += "java.lang.String";
            if (i + 1 != args) loggerSignature += ",";
        }
        loggerSignature += ")";
        return loggerSignature;
    }


    /**
     * Checks if the input local variable is primitive/Collection or exports intValue.
     * @param local Local parameter to be analyzed.
     * @return True in case the input local is compatible with JINN-C.
     */
    private boolean isCompatibleVar (Local local) {
        Type type = local.getType();
        if (type instanceof PrimType) {
            if (type instanceof BooleanType) {
                System.err.println("           * ignored boolean parameter" +
                    " (" + local.getName() + ")");
                return false;
            }
            return true;
        } else if (type instanceof ArrayType)  {
            return true;
        } else {
            RefType ref = (RefType) type;
            SootClass sClass = ref.getSootClass();
            if (sClass.declaresMethod("int size()")) {
                return true;
            } else if (sClass.declaresMethod("int length()")){
                return true;
            } else if (sClass.declaresMethod("int intValue()")) {
                // should gets Integer, Float, Double....
                return true;
            } else {
                System.err.println("           * ignored " +
                    "incompatible class parameter " +
                    "(" + local.getType() + ")");
            }
        }
        return false;
    }

    /**
     * Iterates over the parameters of the input method's body, compare them with input
     * tags and get all
     * parameters which are compatible with our instrumentation. So far, we
     * work with Objects which declares the method size(), Primitive values,
     * Integers, Double, Float...
     * @param body Input method's body.
     * @return List of compatible parameters - List<Object>, where Object is either Local
     * , SootField or a String[]  (for expr reconstruction).
     * ToDo: Create an Structure that may be either Local, SootField, or String
     */
    private List<Object> getCompatibleParameters (Body body,
                                                        Map<String, List<String>> tags) {
        List<Local> parameters  = body.getParameterLocals();
        List<Object> compatibles = new ArrayList<Object>();
            for (Map.Entry<String, List<String>> tagEntry : tags.entrySet()) {
                String tagType = tagEntry.getKey();
                List<String> vars = tagEntry.getValue();
                if (tagType.equals("Input") || tagType.equals("Inputs")) {
                    Outer:
                    for (String var : vars) {
                        // checks @Input tag for formal parameters
                        for (Local local : parameters) {
                            if (var.equals(local.getName()) && isCompatibleVar(local)) {
                                compatibles.add(local);
                                continue Outer;
                            }
                        }
                        // checks @Input tag for globals
                        SootClass sclass = body.getMethod().getDeclaringClass();
                        try {
                            SootField global = sclass.getFieldByName(var);
                            compatibles.add(global);
                        } catch (RuntimeException e) {
                            System.err.println("Error: " + var + " is neither a"
                                + " formal parameter, nor a global ");
                        }
                    }
                } else if (tagType.equals("HiddenInput") ||
                                                    tagType.equals("HiddenInputs")) {
                    for (String expression : vars) {
                        // List for storing all elements of an expr . . .

                        List<Unit> exprUnits = new ArrayList<Unit>();
                        SootClass sclass = body.getMethod().getDeclaringClass();
                        // NOTE: handling very simple cases with single derreference
                        String[] elements = expression.split("\\.");
                        compatibles.add(elements);
                    }
                }

                for (Object o : compatibles) {
                    if (o instanceof Local) {
                        System.out.println("\t\t Local : " + ((Local)o).getName());
                    } else if (o instanceof SootField) {
                        System.out.println("\t\t Global: " + ((SootField)o).getName());
                    }
                }
        }
        return compatibles;
    }

     /**
     * Iterates over the parameters of the input method's body and get all
     * parameters which are compatible with our instrumentation. So far, we
     * work with Objects which declares the method size(), Primitive values,
     * Integers, Double, Float...
     * @param body Input method's body.
     * @return List of compatible parameters - List<Local>.
     */
    private List<Local> getCompatibleParameters (Body body) {
        List<Local> parameters  = body.getParameterLocals();
        List<Local> compatibles = new ArrayList<Local>();
        for (Local local : parameters) {
            if (isCompatibleVar(local))
                compatibles.add(local);
        }
        return compatibles;
    }

    /**
     * Instruments input method's body, inserting instructions to collect
     * arguments information and execution time.
     * @param body Input method's body.
     * @param exitPoints List of units where to insert dump information.
     */
    private void profileMethod (Map<String, List<String>> tags, JimpleBody body) {
        ////List<Local> parameters
        ////List<Object> parameters = getCompatibleParameters(body, tags);

        //
        // Case when we have only a @AdaptiveMethod, without any @Input/@HiddenInput
        if (tags.size() == 0) {
            List<Local> compParameters = getCompatibleParameters(body);
            if (compParameters.size() == 0) {
                System.err.println("Warning: Method @" + body.getMethod() + " cannot be"
                        + " optimized by JINN-c, because it does not contain compatible"
                        + " parameters.");
                return;
            } else {
                instrCompMethod(body, compParameters);
            }
        }
        //
        // Case when we need to take into account input tags
        else {
            List<Object> compParameters = getCompatibleParameters(body, tags);
            instrTaggedMethod(body, compParameters);
        }
    }
    
    private Unit recursiveCheck (Local instanceBase, SootClass base, int index,
            String[] elements, JimpleBody body) {

        if (index >= elements.length) {            
            return null;
        } else {
            numSpaces += numSpaces;
            String element = elements[index];
            if (element.contains("(")) {
                // handler for function call.
                // Note: What if we have more than a method with the same name?
                SootMethod sm = base.getMethodByName(element.replace("()", ""));
                System.out.println(numSpaces + "   Method found: " + base.getName() + "." + sm.getName() + "()");
                System.out.println(numSpaces + "   Method return type: " + sm.getReturnType());
                //
                // create a local to receive the return value of the method
                Local l = insertDeclaration(sm.getName() + sm.hashCode(),
                                                    sm.getReturnType().toString(), body);

                InvokeExpr call = (instanceBase == null?
                        Jimple.v().newStaticInvokeExpr(sm.makeRef()) :
                            ( base.isInterface() == true ?                        
                                Jimple.v().newInterfaceInvokeExpr(instanceBase, sm.makeRef()) :
                                Jimple.v().newVirtualInvokeExpr(instanceBase, sm.makeRef())));
                Unit assignment = Jimple.v().newAssignStmt(l, call);
                body.getUnits().insertBefore(assignment, body.getFirstNonIdentityStmt());

                //// System.out.println(elements.length + " <<>> " + (index + 1));

                if (sm.getReturnType() instanceof RefType)
                    return recursiveCheck(l, ((RefType)sm.getReturnType()).getSootClass(),
                                index+1, elements, body);
                else
                    return assignment;


            } else {
                // handler for field access
                SootField sf = base.getFieldByName(element);
                System.out.println(" Field found: " + base.getName() + "." + sf.getName() + "()");

                if (sf.getType() instanceof RefType) {
                    recursiveCheck(null, ((RefType)sf.getType()).getSootClass(),
                            index+1, elements, body);
                }
                return null; // fix
            }
        }
    }

    private Unit reconstructExpr (JimpleBody body, String[] elements, Local local) {
        Unit assign = null;
        String base = elements[0];
        // First check if base is a global in the current class
        boolean isClassField = false;
        SootField exprField = null;
        SootClass currentClass = body.getMethod().getDeclaringClass();
        Chain<SootField> classFields = currentClass.getFields();
        SootClass phantom = Scene.v().getSootClass(base);

        for (SootField field : classFields) {
            if (field.getName().equals(base)) {
                isClassField = true;
                exprField = field;
                break;
            }
        }

        if (isClassField) {
            SootClass exprClass =  Scene.v().getSootClass(exprField.getType().toString());                        
            local = insertDeclaration("recLocalField" + exprField.hashCode(),
                                                        exprField.getType().toString(), body);
            return recursiveCheck(local, exprClass, 1, elements, body);

        } else if (! phantom.isPhantom()) {
            // The class is actually not a phantom class, instead is a class
            // available in the default package of java
            return recursiveCheck(local, phantom, 1, elements, body);
        } else {
            // For this case, the expr base must be a class in classpath

            // second lookup base of expression in classpath
            // NOTE: TODO: Currently only handling java.lang.Runtime classes
            // This happens because we need to resolve the names before calling
            // soot.Main(). To solve that we could have a previous stage which
            // would find all required classes from path, add it to a file
            // then the JinnDriver would parse this file and resolve all classes
            // at the level of signatures.

            SootClass exprClass =  Scene.v().getSootClass("java.lang." + base + "");
            return recursiveCheck(local, exprClass, 1, elements, body);
        }

        //return assign;
    }

    private void instrTaggedMethod (JimpleBody body, List<Object> parameters) {
        Integer hashCode = body.getMethod().getSignature().hashCode();
        signatures.put(hashCode, body.getMethod().getSignature());
        // Todo: get last identity or null to addFirst
        Unit  insertionPoint     = body.getFirstNonIdentityStmt();
        Chain<Unit> units        = body.getUnits();
        List<Unit> unitsToInsert = new ArrayList<Unit>();
        List<Unit> exitUnits     = findExitUnits(body);
        List<Value> argumentsToLogger = new ArrayList<Value>();
        String loggerSignature = buildLoggerSignature(hashCode,
                                                         parameters.size());
        String timeClass       = "<java.lang.System: long nanoTime()>";
        SootClass loggerClass  = Scene.v().getSootClass(
                                                    "jinn.exlib.DataLogger");
        SootClass strClass  = Scene.v().getSootClass("java.lang.String");
        //
        // Initializing local variables used to store profiled execution
        // time in instrummented method.
        initializeVariables(body);
        //
        // Adding reference of local method signature and local variables
        // to instrumment method.
        argumentsToLogger.add((Value) IntConstant.v(hashCode));
        argumentsToLogger.add((Value) this.var_diff);
        //
        // adding getters to length of arrays and the size() of objects
        //
        {
            int getterId = 0;
            List<Local> specialHandlers = new ArrayList<Local>();
            for (Object parameter : parameters) {
                Local l = null;
                if (parameter instanceof Local) {
                    // we can use this local object directly
                    l = (Local) parameter;
                } else if (parameter instanceof SootField) {
                    // we need to create a new local object and store the value of
                    // parameter into it;
                    SootField sf = (SootField) parameter;
                    l = insertDeclaration("sLocalField" + getterId,
                                                        sf.getType().toString(), body);
                    Value fieldRef = (sf.isStatic()? Jimple.v().newStaticFieldRef(
                            sf.makeRef())
                            :
                            Jimple.v().newInstanceFieldRef(Jimple.v().newThisRef(
                                    body.getMethod().getDeclaringClass().getType())
                                    , sf.makeRef()));

                    Unit assignment = Jimple.v().newAssignStmt(l, fieldRef);
                    units.insertBefore(assignment, insertionPoint);
                } else if (parameter instanceof String[]) {
                    // we need to reconstruct every part of the expr and store the
                    // resulting value into a new local object.
                    String[] elements = (String[]) parameter;
                    Unit assignment = reconstructExpr(body, elements, l);
                    l = (Local)((AssignStmt)assignment).getLeftOp();

                    if (l == null) {
                        System.out.println("L is NULL");
                        System.exit(0);
                    }

                    System.out.println("End of recursive reconstruct. Return type of"
                            + " expr("+ l.getName() + ") is: " + l.getType());
                    numSpaces = "    ";
                    //units.insertBefore(assignment, insertionPoint);
                }
                //
                // Original instrummenter with swap of l <> parameter
                Type type = l.getType();
                if (type instanceof ArrayType) {
                    // This branch handles all array structures
                    //
                    // get length value at the beginning of the method body
                    Local getter = insertDeclaration("$getter_" +
                                                   getterId++, "int", body);
                    LengthExpr lengthOf = Jimple.v().newLengthExpr(l);
                    Unit lengthGetter = Jimple.v().newAssignStmt(getter, lengthOf);
                    units.insertBefore(lengthGetter, insertionPoint);
                    //
                    // converting value to string for logging purpose.
                    Local getterStr = insertDeclaration("$getterStr_" + getterId++,
                                                            "java.lang.String", body);
                    InvokeExpr valueOf =  Jimple.v().newStaticInvokeExpr(
                        strClass.getMethod("java.lang.String valueOf(int)").makeRef(),
                        getter);
                    Unit strGetter = Jimple.v().newAssignStmt(getterStr, valueOf);
                    // ToDo: Move this insertion of before exit points, right after
                    // the end meansurement of time.
                    units.insertAfter(strGetter, lengthGetter);
                    specialHandlers.add(getterStr);
                } else if (!(type instanceof PrimType)) {
                    // This branch handles all supported Objects
                    RefType ref = (RefType) type;
                    Local getter = insertDeclaration("$getter_" + getterId++,
                                                                         "int", body);
                    SootClass sclass = ref.getSootClass();
                    if (sclass.declaresMethod("int size()")) {
                        InvokeExpr invokeExpr = null;
                        if (sclass.isInterface()) {
                            invokeExpr = Jimple.v().newInterfaceInvokeExpr(l,
                                      Scene.v().getMethod("<" + type.toString() + ": "
                                      + "int size()>").makeRef());
                        } else {
                            invokeExpr = Jimple.v().newVirtualInvokeExpr(l,
                                      Scene.v().getMethod("<" + type.toString() + ": "
                                      + "int size()>").makeRef());
                        }
                        //
                        // creating final value assignment and inserting it into the
                        // method body.
                        Unit sizeGetter = Jimple.v().newAssignStmt(getter, invokeExpr);
                        units.insertBefore(sizeGetter, insertionPoint);
                        //
                        // converting value to string for logging purpose.
                        Local getterStr = insertDeclaration("$getterStr_" + getterId++,
                                                            "java.lang.String", body);
                        InvokeExpr valueOf = Jimple.v().newStaticInvokeExpr(
                                    strClass.getMethod("java.lang.String valueOf(int)"
                                    ).makeRef(), getter);
                        Unit strGetter = Jimple.v().newAssignStmt(getterStr, valueOf);
                        // ToDo: Move this insertion of before exit points, right after
                        // the end meansurement of time.
                        units.insertAfter(strGetter, sizeGetter);
                        specialHandlers.add(getterStr);
                    } else {
                        // this branch handles objects like Integer, Double ....
                        Local getterStr = insertDeclaration("$getterStr_" + getterId++,
                                "java.lang.String", body);
                        InvokeExpr valueOf = Jimple.v().newStaticInvokeExpr(strClass.getMethod(
                                        "java.lang.String valueOf(java.lang.Object)").makeRef(),
                                        l);

                        Unit strGetter = Jimple.v().newAssignStmt(getterStr, valueOf);
                        units.insertBefore(strGetter, insertionPoint);
                        specialHandlers.add(getterStr);
                    }
                } else {
                    // this branch handles primitive values
                    Local getterStr = insertDeclaration("$getterStr_" + getterId++,
                                                            "java.lang.String", body);
                    InvokeExpr valueOf = Jimple.v().newStaticInvokeExpr(strClass.getMethod(
                       "java.lang.String valueOf(" + type.toString() + ")").makeRef(),
                        l);

                    Unit strGetter = Jimple.v().newAssignStmt(getterStr, valueOf);
                    units.insertBefore(strGetter, insertionPoint);
                    specialHandlers.add(getterStr);
                }
            }
            argumentsToLogger.addAll(specialHandlers);
        }
        //
        // Avoid weird cases where method has a single return statement within its
        // body.
        if (insertionPoint.toString().contains("return")) return;
        //
        // Creating TimeStamps. Creating instructions to collect system.nanoTime()
        // at the beggining method.
        Unit initTimestamp = Jimple.v().newAssignStmt(var_init,
            Jimple.v().newStaticInvokeExpr(
            Scene.v().getMethod(timeClass).makeRef()));
        // ToDo: // (use: insertAfter(x, LastIdentity))
        units.insertBefore(initTimestamp, insertionPoint);
        //
        // Creating instructions to collect system.nanoTime()
        // at alll exit points of method
        for (Unit exit : exitUnits) {
            unitsToInsert.clear();
            // end timestamp
            unitsToInsert.add(Jimple.v().newAssignStmt(var_end,
                    Jimple.v().newStaticInvokeExpr(
                    Scene.v().getMethod(timeClass).makeRef())));
            // getting diff timestamp (end - init)
            unitsToInsert.add(Jimple.v().newAssignStmt(var_diff,
                    Jimple.v().newSubExpr(var_end, var_init)));
            // adding method call to our external logging library
            unitsToInsert.add(Jimple.v().newInvokeStmt(
                    Jimple.v().newStaticInvokeExpr(loggerClass.getMethod(
                    loggerSignature).makeRef(),
                    argumentsToLogger)));
            // actually inserting new instructions before every exit point.
            for (Unit unit : unitsToInsert) {
                units.insertBefore(unit, exit);
            }
        }
        //
        // Check, against Soot Standards, if the modifications in this method's body
        // are valid.
        try {
            body.validate();
        } catch (Exception e) {
            System.err.println("ERROR: FAILED TO VALIDATE MODIFICATIONS IN A METHOD.");
            System.err.println(e);
            System.err.println(body.getMethod().getSignature());
        }


        for (Object par : parameters) {
            if (par instanceof Local) {

            } else if (par instanceof SootField) {

            } else if (par instanceof String[]) {

            } else {
                System.err.println("Warning: Input tagged object @" + par + " does not "
                        + " have a valid JINN-c type.");
            }
        }
    }


    private void instrCompMethod(JimpleBody body, List<Local> parameters) {
        //
        // We only intrumment methods, which have at least 1 compatible argument
        if (parameters.size() > 0) {
            Integer hashCode = body.getMethod().getSignature().hashCode();
            signatures.put(hashCode, body.getMethod().getSignature());
            // Todo: get last identity or null to addFirst
            Unit  insertionPoint     = body.getFirstNonIdentityStmt();
            Chain<Unit> units        = body.getUnits();
            List<Unit> unitsToInsert = new ArrayList<Unit>();
            List<Unit> exitUnits     = findExitUnits(body);
            List<Value> argumentsToLogger = new ArrayList<Value>();
            String loggerSignature = buildLoggerSignature(hashCode,
                                                             parameters.size());
            String timeClass       = "<java.lang.System: long nanoTime()>";
            SootClass loggerClass  = Scene.v().getSootClass(
                                                        "jinn.exlib.DataLogger");
            SootClass strClass  = Scene.v().getSootClass("java.lang.String");
            //
            // Initializing local variables used to store profiled execution
            // time in instrummented method.
            initializeVariables(body);
            //
            // Adding reference of local method signature and local variables
            // to instrumment method.
            argumentsToLogger.add((Value) IntConstant.v(hashCode));
            argumentsToLogger.add((Value) var_diff);
            //
            // adding getters to length of arrays and the size() of objects
            {
                int getterId = 0;
                List<Local> specialHandlers = new ArrayList<Local>();
                for (Local parameter : parameters) {
                    Type type = parameter.getType();
                    if (type instanceof ArrayType) {
                        // This branch handles all array structures
                        //
                        // get length value at the beginning of the method body
                        Local getter = insertDeclaration("$getter_" +
                                                       getterId++, "int", body);
                        LengthExpr lengthOf = Jimple.v().newLengthExpr(parameter);
                        Unit lengthGetter = Jimple.v().newAssignStmt(getter, lengthOf);
                        units.insertBefore(lengthGetter, insertionPoint);
                        //
                        // converting value to string for logging purpose.
                        Local getterStr = insertDeclaration("$getterStr_" + getterId++,
                                                                "java.lang.String", body);
                        InvokeExpr valueOf =  Jimple.v().newStaticInvokeExpr(
                            strClass.getMethod("java.lang.String valueOf(int)").makeRef(),
                            getter);
                        Unit strGetter = Jimple.v().newAssignStmt(getterStr, valueOf);
                        // ToDo: Move this insertion of before exit points, right after
                        // the end meansurement of time.
                        units.insertAfter(strGetter, lengthGetter);
                        specialHandlers.add(getterStr);
                    } else if (!(type instanceof PrimType)) {
                        // This branch handles all supported Objects
                        RefType ref = (RefType) type;
                        Local getter = insertDeclaration("$getter_" + getterId++,
                                                                             "int", body);
                        SootClass sclass = ref.getSootClass();
                        if (sclass.declaresMethod("int size()")) {
                            InvokeExpr invokeExpr = null;
                            if (sclass.isInterface()) {
                                invokeExpr = Jimple.v().newInterfaceInvokeExpr(parameter,
                                          Scene.v().getMethod("<" + type.toString() + ": "
                                          + "int size()>").makeRef());
                            } else {
                                invokeExpr = Jimple.v().newVirtualInvokeExpr(parameter,
                                          Scene.v().getMethod("<" + type.toString() + ": "
                                          + "int size()>").makeRef());
                            }
                            //
                            // creating final value assignment and inserting it into the
                            // method body.
                            Unit sizeGetter = Jimple.v().newAssignStmt(getter, invokeExpr);
                            units.insertBefore(sizeGetter, insertionPoint);
                            //
                            // converting value to string for logging purpose.
                            Local getterStr = insertDeclaration("$getterStr_" + getterId++,
                                                                "java.lang.String", body);
                            InvokeExpr valueOf = Jimple.v().newStaticInvokeExpr(
                                        strClass.getMethod("java.lang.String valueOf(int)"
                                        ).makeRef(), getter);
                            Unit strGetter = Jimple.v().newAssignStmt(getterStr, valueOf);
                            // ToDo: Move this insertion of before exit points, right after
                            // the end meansurement of time.
                            units.insertAfter(strGetter, sizeGetter);
                            specialHandlers.add(getterStr);
                        } else {
                            // this branch handles objects like Integer, Double ....
                            Local getterStr = insertDeclaration("$getterStr_" + getterId++,
                                    "java.lang.String", body);
                            InvokeExpr valueOf = Jimple.v().newStaticInvokeExpr(strClass.getMethod(
                                            "java.lang.String valueOf(java.lang.Object)").makeRef(),
                                            parameter);

                            Unit strGetter = Jimple.v().newAssignStmt(getterStr, valueOf);
                            units.insertBefore(strGetter, insertionPoint);
                            specialHandlers.add(getterStr);
                        }
                    } else {
                        // this branch handles primitive values
                        Local getterStr = insertDeclaration("$getterStr_" + getterId++,
                                                                "java.lang.String", body);
                        InvokeExpr valueOf = Jimple.v().newStaticInvokeExpr(strClass.getMethod(
                           "java.lang.String valueOf(" + type.toString() + ")").makeRef(),
                            parameter);

                        Unit strGetter = Jimple.v().newAssignStmt(getterStr, valueOf);
                        units.insertBefore(strGetter, insertionPoint);
                        specialHandlers.add(getterStr);
                    }
                }
                argumentsToLogger.addAll(specialHandlers);
            }
            //
            // Avoid weird cases where method has a single return statement within its
            // body.
            if (insertionPoint.toString().contains("return")) return;
            //
            // Creating TimeStamps. Creating instructions to collect system.nanoTime()
            // at the beggining method.
            Unit initTimestamp = Jimple.v().newAssignStmt(var_init,
                Jimple.v().newStaticInvokeExpr(
                Scene.v().getMethod(timeClass).makeRef()));
            // ToDo: // (use: insertAfter(x, LastIdentity))
            units.insertBefore(initTimestamp, insertionPoint);
            //
            // Creating instructions to collect system.nanoTime()
            // at alll exit points of method
            for (Unit exit : exitUnits) {
                unitsToInsert.clear();
                // end timestamp
                unitsToInsert.add(Jimple.v().newAssignStmt(var_end,
                        Jimple.v().newStaticInvokeExpr(
                        Scene.v().getMethod(timeClass).makeRef())));
                // getting diff timestamp (end - init)
                unitsToInsert.add(Jimple.v().newAssignStmt(var_diff,
                        Jimple.v().newSubExpr(var_end, var_init)));
                // adding method call to our external logging library
                unitsToInsert.add(Jimple.v().newInvokeStmt(
                        Jimple.v().newStaticInvokeExpr(loggerClass.getMethod(
                        loggerSignature).makeRef(),
                        argumentsToLogger)));
                // actually inserting new instructions before every exit point.
                for (Unit unit : unitsToInsert) {
                    units.insertBefore(unit, exit);
                }
            }
            //
            // Check, against Soot Standards, if the modifications in this method's body
            // are valid.
            try {
                body.validate();
            } catch (Exception e) {
                System.err.println("ERROR: FAILED TO VALIDATE MODIFICATIONS IN A METHOD.");
                System.err.println(e);
                System.err.println(body.getMethod().getSignature());
            }
        }
    }
}