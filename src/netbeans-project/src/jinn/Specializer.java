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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import soot.ArrayType;
import soot.Body;
import soot.BodyTransformer;
import soot.DoubleType;
import soot.ArrayType;
import soot.IntType;
import soot.Local;
import soot.LongType;
import soot.PrimType;
import soot.RefType;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Type;
import soot.Unit;
import soot.Value;
import soot.jimple.ArrayRef;
import soot.jimple.IntConstant;
import soot.jimple.InvokeExpr;
import soot.jimple.Jimple;
import soot.jimple.JimpleBody;
import soot.jimple.LengthExpr;
import soot.tagkit.AnnotationTag;
import soot.tagkit.Tag;
import soot.tagkit.VisibilityAnnotationTag;
import soot.util.Chain;



/**
 *
 * @author juniocezar
 */
public class Specializer extends BodyTransformer {
    private List<Integer> specMethods;

    public Specializer (List<Integer> l) {
        this.specMethods = l;
    }

    @Override
    protected void internalTransform(Body body, String string, Map<String, String> map) {        
        SootMethod sMethod = body.getMethod();
        String methodName = sMethod.getName();
        Integer hashCode = sMethod.getSignature().hashCode();

        if (isAdaptiveMethod(sMethod) && specMethods.contains(hashCode)) {
            List<Local> parameters = getCompatibleParameters(body);
            // iterate over parameters and get their values as double
            specialize(parameters, (JimpleBody) body);
            // create array with inputs
            // add call to predictor
        }
    }

    private void specialize (List<Local> parameters, JimpleBody body) {
        Unit insertionPoint     = body.getFirstNonIdentityStmt();
        Unit lastInsertion = null;
        Chain<Unit> units = body.getUnits();
        List<Local> arrayElements = new ArrayList<Local>();
        SootClass predictorClass = Scene.v().getSootClass("jinn.exlib.Predictor");
        int getterId = 0;
        //
        // Create array with the compatible parameters, dimensions
        Local array = Jimple.v().newLocal("arrayOfDoubles", ArrayType.v(DoubleType.v(),
                1));
        body.getLocals().add(array);
        // initialize array
        Unit newArray = Jimple.v().newAssignStmt(array, Jimple.v().newNewArrayExpr(
                           DoubleType.v(), IntConstant.v(parameters.size())));
        units.insertBefore(newArray, insertionPoint);
        //
        // Iterate over parameters, adding them to body and array
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
                // converting value to double for feeding predictor
                Local localDouble = insertDeclaration("$getterDouble_" + getterId++,
                                                  "double", body);
                Unit getterDouble = Jimple.v().newAssignStmt(localDouble,
                        Jimple.v().newCastExpr(getter, DoubleType.v()));
                // ToDo: Move this insertion of before exit points, right after
                // the end meansurement of time.
                units.insertAfter(getterDouble, lengthGetter);
                lastInsertion = getterDouble;
                arrayElements.add(localDouble);
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
                    // converting value to double for feeding predictor
                    Local getterDouble = insertDeclaration("$getterDouble_" + getterId++,
                                                     "double", body);
                    Unit doubleGetter = Jimple.v().newAssignStmt(getterDouble,
                            Jimple.v().newCastExpr(getter, DoubleType.v()));
                    // ToDo: Move this insertion of before exit points, right after
                    // the end meansurement of time.
                    units.insertAfter(doubleGetter, sizeGetter);
                    lastInsertion = doubleGetter;
                    arrayElements.add(getterDouble);
                } else {
                    // this branch handles objects like Integer, Double ....
                    Local getterDouble = insertDeclaration("$getterDouble_" + getterId++,
                            "double", body);
                    Unit doubleGetter = Jimple.v().newAssignStmt(getterDouble,
                            Jimple.v().newVirtualInvokeExpr(parameter, Scene.v().getMethod(
                            "<" + type.toString() + ": double doubleValue()>").makeRef()));
                    units.insertBefore(doubleGetter, insertionPoint);
                    lastInsertion = doubleGetter;
                    arrayElements.add(getterDouble);
                }
            } else {
                // this branch handles primitive values
                Local getterDouble = insertDeclaration("$getterDouble_" + getterId++,
                                                   "double", body);
                Unit doubleGetter = Jimple.v().newAssignStmt(getterDouble,
                        Jimple.v().newCastExpr(parameter, DoubleType.v()));
                units.insertBefore(doubleGetter, insertionPoint);
                lastInsertion = doubleGetter;
                arrayElements.add(getterDouble);
            }
        }
        //
        // Adding elements to array
        for (int i = 0; i < arrayElements.size(); i++) {
            Value index = IntConstant.v(i);
            ArrayRef leftSide = Jimple.v().newArrayRef(array, index);
            Unit assignment = Jimple.v().newAssignStmt(leftSide, arrayElements.get(i));
            units.insertAfter(assignment, lastInsertion);
            lastInsertion = assignment;
        }
        //
        // Add static call to jinn.exlib.Predictor
        InvokeExpr valueOf =  Jimple.v().newStaticInvokeExpr(
                            predictorClass.getMethod("double predict(double[])").makeRef(),
                            array);
        units.insertAfter(Jimple.v().newInvokeStmt(valueOf), lastInsertion);
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
     * Iterates over all Tags of input method, looking for the @AdaptiveMethod
     * tag. Returns true if such tag is found, and false otherwise.
     * @param m Input method.
     * @return True if tag is present, false otherwise.
     */
    private boolean isAdaptiveMethod (SootMethod method) {
        List<Tag> tags = method.getTags();
        Tag toRemove = null;
        boolean status = false;
        for (Tag tag : tags) {
            if (tag instanceof VisibilityAnnotationTag) {
                ArrayList<AnnotationTag> aTags =
                        ((VisibilityAnnotationTag)tag).getAnnotations();
                for (AnnotationTag annotation : aTags) {
                    String name = annotation.getType();
                    if (name.equals("LAdaptiveMethod;")) {
                        toRemove = tag;
                        status = true;
                        break;
                    }
                }
            }
            if (status) break;
        }
//        if (toRemove != null) {
//            System.out.println("======================== " + toRemove.getName());
//            method.removeTag(toRemove.getName());
//        }
        return status;
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
            Type type = local.getType();
            if (type instanceof PrimType) {
                //if (type instanceof IntType)
                    compatibles.add(local);
            } else if (type instanceof ArrayType)  {
                compatibles.add(local);
            } else {
                RefType ref = (RefType) type;
                SootClass sClass = ref.getSootClass();
                if (sClass.declaresMethod("int size()")) {
                    compatibles.add(local);
                } else if (sClass.declaresMethod("int intValue()")) {
                    // should gets Integer, Float, Double....
                    compatibles.add(local);
                }
            }
        }
        return compatibles;
    }

}
