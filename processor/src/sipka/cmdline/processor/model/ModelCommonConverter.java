/*
 * Copyright (C) 2020 Bence Sipka
 *
 * This program is free software: you can redistribute it and/or modify 
 * it under the terms of the GNU General Public License as published by 
 * the Free Software Foundation, version 3.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package sipka.cmdline.processor.model;

import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;

public class ModelCommonConverter {
	private TypeMirror targetType;
	private TypeElement methodDeclaringType;
	private String methodName;

	public ModelCommonConverter(TypeMirror targetType, TypeElement methodDeclaringType, String methodName) {
		this.targetType = targetType;
		this.methodDeclaringType = methodDeclaringType;
		this.methodName = methodName;
	}

	public TypeMirror getTargetType() {
		return targetType;
	}

	public TypeElement getMethodDeclaringType() {
		return methodDeclaringType;
	}

	public String getMethodName() {
		return methodName;
	}
}
