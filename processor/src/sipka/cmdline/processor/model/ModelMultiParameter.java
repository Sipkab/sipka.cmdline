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

import javax.lang.model.type.TypeMirror;

public class ModelMultiParameter {

	private TypeMirror elementType;
	private String methodName;

	public ModelMultiParameter(TypeMirror elementType, String methodName) {
		this.elementType = elementType;
		this.methodName = methodName;
	}

	public TypeMirror getElementType() {
		return elementType;
	}

	public String getMethodName() {
		return methodName;
	}
}
