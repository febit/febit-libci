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
package org.febit.libci.core.variable;

import lombok.RequiredArgsConstructor;
import org.febit.lang.UncheckedException;
import org.febit.lang.jackson.JacksonTypes;
import org.febit.lang.util.Lists;
import org.febit.libci.core.VarSupplier;
import org.febit.libci.core.spec.ExpandPhase;
import org.febit.libci.core.spec.Expandable;
import org.febit.libci.core.spec.support.SpecMapper;
import org.jspecify.annotations.Nullable;
import tools.jackson.databind.BeanDescription;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.introspect.AnnotatedMethod;
import tools.jackson.databind.introspect.BeanPropertyDefinition;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@RequiredArgsConstructor(staticName = "of")
public class VarExpander {
    private static final Map<Class<?>, List<PropertyDescription>> TYPE_PROPERTIES = new ConcurrentHashMap<>();

    private final Map<Class<?>, Optional<Description>> descriptions = new ConcurrentHashMap<>();

    private final VarSupplier vars;
    private final ExpandPhase phase;

    public static BeanDescription introspect(Class<?> type) {
        var javaType = JacksonTypes.FACTORY.constructType(type);
        return SpecMapper.CLASS_INTROSPECTOR.introspectForSerialization(
                javaType,
                SpecMapper.CLASS_INTROSPECTOR.introspectClassAnnotations(javaType)
        );
    }

    @Nullable
    public <T> T expandNullable(@Nullable T model) {
        if (model == null) {
            return null;
        }
        return expand(model);
    }

    @SuppressWarnings("unchecked")
    public <T> T expand(T model) {
        return switch (model) {
            case Number n -> model;
            case Boolean b -> model;
            case String text -> (T) expandText(text);
            case List<?> list -> (T) expandList((List<@Nullable Object>) list);
            case Map<?, ?> map -> (T) expandMapValue((Map<Object, @Nullable Object>) map);
            default -> expandBean(model);
        };
    }

    private String expandText(String text) {
        var expanded = vars.expand(text);
        return expanded.equals(text) ? text : expanded;
    }

    private <T extends @Nullable Object> List<T> expandList(List<T> list) {
        if (list.isEmpty()) {
            return list;
        }

        int i = 0;
        int size = list.size();

        T expanded = null;
        for (; i < size; i++) {
            var raw = list.get(i);
            if (raw == null) {
                continue;
            }
            expanded = expand(raw);
            if (raw != expanded) {
                break;
            }
        }
        if (i >= size) {
            return list;
        }

        var handled = new ArrayList<T>(list.size());
        // Copy unchanged items before the first changed item.
        handled.addAll(list.subList(0, i));
        // Add the first changed item.
        handled.add(expanded);
        i++;

        // Handle the rest items.
        for (; i < size; i++) {
            var raw = list.get(i);
            if (raw == null) {
                handled.add(null);
                continue;
            }
            handled.add(expand(raw));
        }
        return handled;
    }

    private <K, V extends @Nullable Object> Map<K, V> expandMapValue(Map<K, V> map) {
        if (map.isEmpty()) {
            return map;
        }
        var unchangedKeys = new ArrayList<K>();
        K key = null;
        V expanded = null;

        var iter = map.entrySet().iterator();
        while (iter.hasNext()) {
            var entry = iter.next();
            key = entry.getKey();
            var raw = entry.getValue();
            if (raw == null) {
                unchangedKeys.add(key);
                continue;
            }
            expanded = expand(raw);
            if (raw != expanded) {
                break;
            }
            unchangedKeys.add(key);
        }

        if (unchangedKeys.size() == map.size()) {
            return map;
        }

        Map<K, V> handled = LinkedHashMap.newLinkedHashMap(map.size());
        // Copy unchanged items before the first changed item.
        for (K unchangedKey : unchangedKeys) {
            handled.put(unchangedKey, map.get(unchangedKey));
        }
        // Add the first changed item.
        handled.put(key, expanded);
        // Handle the rest items.
        while (iter.hasNext()) {
            var entry = iter.next();
            key = entry.getKey();
            var raw = entry.getValue();
            if (raw == null) {
                handled.put(key, null);
                continue;
            }
            handled.put(key, expand(raw));
        }
        return handled;
    }

    @SuppressWarnings("unchecked")
    private <T> T expandBean(T bean) {
        var descOpt = describe(bean.getClass());
        if (descOpt.isEmpty()) {
            return bean;
        }
        var desc = descOpt.get();

        var changed = false;
        var props = HashMap.newHashMap(
                desc.expandableProps().size() + desc.plainProps().size()
        );
        for (var prop : desc.expandableProps()) {
            var raw = prop.callGetter(bean);
            if (raw == null) {
                props.put(prop.name(), null);
                continue;
            }
            var expanded = expand(raw);
            if (raw != expanded) {
                changed = true;
            }
            props.put(prop.name(), expanded);
        }
        if (!changed) {
            return bean;
        }
        for (var prop : desc.plainProps()) {
            props.put(prop.name(),
                    prop.callGetter(bean)
            );
        }
        return (T) SpecMapper.toBean(props, bean.getClass());
    }

    private Optional<Description> describe(Class<?> type) {
        return descriptions.computeIfAbsent(type, this::describe0);
    }

    private Optional<Description> describe0(Class<?> type) {
        var anno = type.getAnnotation(Expandable.class);
        if (anno == null) {
            return Optional.empty();
        }
        if (!phase.isTarget(anno.phase())) {
            return Optional.empty();
        }
        var props = properties(type);

        var expandable = new ArrayList<PropertyDescription>(props.size());
        var plain = new ArrayList<PropertyDescription>(props.size());

        for (var prop : props) {
            if (phase.isTarget(prop.phase())) {
                expandable.add(prop);
            } else {
                plain.add(prop);
            }
        }
        if (expandable.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new Description(
                List.copyOf(expandable),
                List.copyOf(plain)
        ));
    }

    private static List<PropertyDescription> properties(Class<?> type) {
        return TYPE_PROPERTIES.computeIfAbsent(type, t -> Lists.collect(
                introspect(type).findProperties(),
                VarExpander::property
        ));
    }

    private static PropertyDescription property(BeanPropertyDefinition definition) {
        var phase = Optional.of(definition)
                .map(BeanPropertyDefinition::getAccessor)
                .map(a -> a.getAnnotation(Expandable.class))
                .or(() -> Optional.of(definition)
                        .map(BeanPropertyDefinition::getPrimaryType)
                        .map(JavaType::getRawClass)
                        .map(t -> t.getAnnotation(Expandable.class))
                )
                .map(Expandable::phase)
                .orElse(ExpandPhase.NONE);
        return new PropertyDescription(phase, definition);
    }

    private record Description(
            List<PropertyDescription> expandableProps,
            List<PropertyDescription> plainProps
    ) {
    }

    private record PropertyDescription(
            ExpandPhase phase,
            BeanPropertyDefinition definition
    ) {

        public String name() {
            return definition.getName();
        }

        public AnnotatedMethod getter() {
            return definition.getGetter();
        }

        @Nullable
        public Object callGetter(Object bean) {
            try {
                return getter().callOn(bean);
            } catch (Exception e) {
                throw new UncheckedException("Cannot get property: " + getter().getAnnotated(), e);
            }
        }
    }
}
