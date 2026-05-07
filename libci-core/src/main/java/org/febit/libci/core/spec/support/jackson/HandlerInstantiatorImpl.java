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

import org.jspecify.annotations.Nullable;
import tools.jackson.databind.DeserializationConfig;
import tools.jackson.databind.KeyDeserializer;
import tools.jackson.databind.SerializationConfig;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.cfg.HandlerInstantiator;
import tools.jackson.databind.cfg.MapperConfig;
import tools.jackson.databind.deser.ValueInstantiator;
import tools.jackson.databind.introspect.Annotated;
import tools.jackson.databind.jsontype.TypeIdResolver;
import tools.jackson.databind.jsontype.TypeResolverBuilder;

public class HandlerInstantiatorImpl extends HandlerInstantiator {

    public static final HandlerInstantiatorImpl INSTANCE = new HandlerInstantiatorImpl();

    @Nullable
    @Override
    public ValueInstantiator valueInstantiatorInstance(
            MapperConfig<?> config, Annotated annotated, Class<?> resolverClass) {
        if (resolverClass == VariableValueInstantiator.class) {
            return new VariableValueInstantiator(annotated.getRawType());
        }
        return null;
    }

    @Nullable
    @Override
    public ValueDeserializer<?> deserializerInstance(
            DeserializationConfig config, Annotated annotated, Class<?> deserClass) {
        return null;
    }

    @Nullable
    @Override
    public KeyDeserializer keyDeserializerInstance(
            DeserializationConfig config, Annotated annotated, Class<?> keyDeserClass) {
        return null;
    }

    @Nullable
    @Override
    public ValueSerializer<?> serializerInstance(
            SerializationConfig config, Annotated annotated, Class<?> serClass) {
        return null;
    }

    @Nullable
    @Override
    public TypeResolverBuilder<?> typeResolverBuilderInstance(
            MapperConfig<?> config, Annotated annotated, Class<?> builderClass) {
        return null;
    }

    @Nullable
    @Override
    public TypeIdResolver typeIdResolverInstance(
            MapperConfig<?> config, Annotated annotated, Class<?> resolverClass) {
        return null;
    }
}
