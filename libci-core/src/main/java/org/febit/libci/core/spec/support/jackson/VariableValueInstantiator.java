/*
 * Copyright 2025-present febit.org (support@febit.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.febit.libci.core.spec.support.jackson;

import org.febit.libci.core.spec.variable.IVariable;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.deser.ValueInstantiator;

import java.math.BigDecimal;
import java.math.BigInteger;

public class VariableValueInstantiator extends ValueInstantiator.Base {

    public VariableValueInstantiator(Class<?> type) {
        super(type);
    }

    @Override
    public IVariable.IBuilder<?> createUsingDefault(DeserializationContext context) {
        var type = this._valueType;
        Object builder;
        try {
            builder = type.getConstructor().newInstance();
        } catch (Exception e) {
            return context.reportBadDefinition(_valueType,
                    "Cannot create variable instance using default constructor: " + e.getMessage());
        }
        if (!(builder instanceof IVariable.IBuilder)) {
            return context.reportBadDefinition(_valueType,
                    "Expect a variable builder (IVariable.IBuilder), but got: "
                            + type.getName());
        }
        return (IVariable.IBuilder<?>) builder;
    }

    @Override
    public boolean canCreateFromString() {
        return true;
    }

    @Override
    public boolean canCreateFromInt() {
        return true;
    }

    @Override
    public boolean canCreateFromLong() {
        return true;
    }

    @Override
    public boolean canCreateFromBigInteger() {
        return true;
    }

    @Override
    public boolean canCreateFromBigDecimal() {
        return true;
    }

    @Override
    public boolean canCreateFromDouble() {
        return true;
    }

    @Override
    public boolean canCreateFromBoolean() {
        return true;
    }

    @Override
    public boolean canCreateUsingDefault() {
        return true;
    }

    private Object create0(DeserializationContext context, Object value) throws JacksonException {
        return createUsingDefault(context)
                .value(value);
    }

    @Override
    public Object createFromString(DeserializationContext context, String value) throws JacksonException {
        return create0(context, value);
    }

    @Override
    public Object createFromInt(DeserializationContext context, int value) throws JacksonException {
        return create0(context, value);
    }

    @Override
    public Object createFromLong(DeserializationContext context, long value) throws JacksonException {
        return create0(context, value);
    }

    @Override
    public Object createFromBigInteger(DeserializationContext context, BigInteger value) throws JacksonException {
        return create0(context, value);
    }

    @Override
    public Object createFromDouble(DeserializationContext context, double value) throws JacksonException {
        return create0(context, value);
    }

    @Override
    public Object createFromBigDecimal(DeserializationContext context, BigDecimal value) throws JacksonException {
        return create0(context, value);
    }

    @Override
    public Object createFromBoolean(DeserializationContext context, boolean value) throws JacksonException {
        return create0(context, value);
    }

}
