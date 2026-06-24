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
package org.febit.libci.core.spec.support;

import lombok.experimental.UtilityClass;
import org.febit.lang.PeriodDuration;
import org.febit.lang.jackson.JacksonCodec;
import org.febit.lang.jackson.JacksonCodecImpl;
import org.febit.lang.jackson.JacksonStandard;
import org.febit.libci.core.document.yaml.YamlUtils;
import org.febit.libci.core.spec.ISpec;
import org.febit.libci.core.spec.support.jackson.HandlerInstantiatorImpl;
import org.febit.libci.core.spec.support.jackson.PeriodDurationDeserializer;
import org.jspecify.annotations.Nullable;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.introspect.ClassIntrospector;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;

import static org.febit.lang.jackson.JacksonTypes.FACTORY;

@UtilityClass
public class SpecMapper {

    public static final JacksonCodec JACKSON;
    public static final JacksonCodec JACKSON_PRETTY;
    public static final ClassIntrospector CLASS_INTROSPECTOR;

    private static final JavaType TYPE_STRING_LIST = FACTORY.constructCollectionLikeType(
            ArrayList.class, String.class);

    static {
        var builder = JacksonStandard.standard(JsonMapper.builder())
                .propertyNamingStrategy(new SnakeCaseStrategy())
                .enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
                .handlerInstantiator(HandlerInstantiatorImpl.INSTANCE)
                .addModule(new SimpleModule()
                        .addDeserializer(PeriodDuration.class, PeriodDurationDeserializer.INSTANCE)
                );

        var mapper = builder.build();
        CLASS_INTROSPECTOR = mapper.deserializationConfig().classIntrospectorInstance();

        JACKSON = JacksonCodecImpl.of(mapper);
        JACKSON_PRETTY = JacksonCodecImpl.of(builder
                .enable(SerializationFeature.INDENT_OUTPUT)
                .build()
        );
    }

    public static String jsonify(@Nullable Object data, boolean pretty) {
        return (pretty ? JACKSON_PRETTY : JACKSON)
                .stringify(data);
    }

    public static String toYaml(@Nullable ISpec spec) {
        if (spec == null) {
            return "null";
        }
        var raw = SpecMapper.toNamedMap(spec);
        return YamlUtils.dump(raw);
    }

    public static <T> List<T> toBeanList(Object raw, Class<T> itemType) {
        var bean = JACKSON.toList(raw, itemType);
        Objects.requireNonNull(bean);
        return bean;
    }

    @SuppressWarnings({
            "java:S1319", // "Map" rather than specific implementation
    })
    public static LinkedHashMap<String, Object> toNamedMap(Object raw) {
        var bean = (LinkedHashMap<String, Object>) JACKSON.toNamedMap(raw);
        Objects.requireNonNull(bean);
        return bean;
    }

    public static <T> T toBean(Object raw, Class<T> type) {
        T bean = JACKSON.to(raw, type);
        Objects.requireNonNull(bean);
        return bean;
    }

    public static <T> T toBean(Object raw, JavaType type) {
        @SuppressWarnings("unchecked")
        T bean = (T) JACKSON.to(raw, type);
        Objects.requireNonNull(bean);
        return bean;
    }

    @SuppressWarnings({
            "java:S1319" // "List" rather than specific implementation
    })
    public static ArrayList<String> toStringList(Object raw) {
        ArrayList<String> list = JACKSON.to(raw, TYPE_STRING_LIST);
        Objects.requireNonNull(list);
        return list;
    }
}
