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

public class ObfuscationAwareAdapter extends ClassVisitor {

    private boolean m_isObfuscatedEnvironment;

    public ObfuscationAwareAdapter(int api, ClassVisitor cv, boolean isObfuscatedEnvironment) {
        super(api, cv);

        m_isObfuscatedEnvironment = isObfuscatedEnvironment;
    }

    public ClassVisitor getPreviousClassVisitor() {
        return cv;
    }

    public void setPreviousClassVisitor(ClassVisitor val) {
        cv = val;
    }

    protected String getRuntimeMethodName(String runtimeClassName, String clearMethodName, String idMethodName) {
        if (m_isObfuscatedEnvironment) {
            /*
             * Ideally we should just return idMethodName, but the original code behaved like this, and I don't want
             * to deal with fixing what changing it might break.
             */
            return runtimeClassName.startsWith("net/minecraft/") ? idMethodName : "";
        } else {
            return clearMethodName;
        }
    }
}
