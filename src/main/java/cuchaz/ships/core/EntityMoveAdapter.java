/*******************************************************************************
 * Copyright (c) 2013 jeff.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 * jeff - initial API and implementation
 ******************************************************************************/
package cuchaz.ships.core;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class EntityMoveAdapter extends ObfuscationAwareAdapter {

    private final String EntityClassName;

    private String m_className;

    public EntityMoveAdapter(int api, ClassVisitor cv, boolean isObfuscatedEnvironment) {
        super(api, cv, isObfuscatedEnvironment);

        m_className = null;

        // cache the runtime class names
        EntityClassName = "net/minecraft/entity/Entity";
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        super.visit(version, access, name, signature, superName, interfaces);

        m_className = name;
    }

    @Override
    public MethodVisitor visitMethod(int access, final String methodName, String methodDesc, String signature,
        String[] exceptions) {
        if (m_className.equals(EntityClassName)) {
            // void moveEntity( double, double, double )
            // func_70091_d
            if (methodDesc.equals("(DDD)V")
                && methodName.equals(getRuntimeMethodName(m_className, "moveEntity", "func_70091_d"))) {
                return new MethodVisitor(api, cv.visitMethod(access, methodName, methodDesc, signature, exceptions)) {

                    @Override
                    public void visitCode() {
                        // insert a call to our intermediate
                        // nothing on the stack, push this to stack, push the arguments to the stack, then invoke
                        // intermediary
                        mv.visitVarInsn(Opcodes.ALOAD, 0);
                        mv.visitVarInsn(Opcodes.DLOAD, 1);
                        mv.visitVarInsn(Opcodes.DLOAD, 3);
                        mv.visitVarInsn(Opcodes.DLOAD, 5);
                        mv.visitMethodInsn(
                            Opcodes.INVOKESTATIC,
                            ShipIntermediary.Path,
                            "preEntityMove",
                            String.format("(L%s;DDD)V", EntityClassName));

                        super.visitCode();
                    }

                    @Override
                    public void visitInsn(int opcode) {
                        if (opcode == Opcodes.RETURN) {
                            // just before the final return statement, insert our call
                            mv.visitVarInsn(Opcodes.ALOAD, 0);
                            mv.visitVarInsn(Opcodes.DLOAD, 1);
                            mv.visitVarInsn(Opcodes.DLOAD, 3);
                            mv.visitVarInsn(Opcodes.DLOAD, 5);
                            mv.visitMethodInsn(
                                Opcodes.INVOKESTATIC,
                                ShipIntermediary.Path,
                                "postEntityMove",
                                String.format("(L%s;DDD)V", EntityClassName));
                        }

                        // and add the return as normal
                        super.visitInsn(opcode);
                    }
                };
            }
        }

        return super.visitMethod(access, methodName, methodDesc, signature, exceptions);
    }
}
