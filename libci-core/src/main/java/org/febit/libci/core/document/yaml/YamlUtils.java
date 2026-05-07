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
package org.febit.libci.core.document.yaml;

import lombok.experimental.Tolerate;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.febit.libci.core.VarSupplier;
import org.febit.libci.core.exception.ProfileException;
import org.febit.libci.core.resource.Resource;
import org.febit.libci.core.spec.header.HeaderSpec;
import org.febit.libci.core.spec.support.SpecMapper;
import org.febit.libci.core.variable.InputSuppliers;
import org.jspecify.annotations.Nullable;
import org.snakeyaml.engine.v2.api.Dump;
import org.snakeyaml.engine.v2.api.DumpSettings;
import org.snakeyaml.engine.v2.api.LoadSettings;
import org.snakeyaml.engine.v2.common.FlowStyle;
import org.snakeyaml.engine.v2.composer.Composer;
import org.snakeyaml.engine.v2.parser.ParserImpl;
import org.snakeyaml.engine.v2.scanner.StreamReader;

import java.io.Reader;
import java.io.StringReader;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@UtilityClass
public class YamlUtils {

    private static final LoadSettings LOAD_SETTINGS = LoadSettings.builder().build();

    private static final DumpSettings DUMP_SETTINGS = DumpSettings.builder()
            .setDefaultFlowStyle(FlowStyle.BLOCK)
            .setIndent(2)
            .setIndentWithIndicator(true)
            .setIndicatorIndent(2)
            .setWidth(120)
            .build();

    public static String dump(@Nullable Object data) throws ProfileException {
        var dump = new Dump(DUMP_SETTINGS);
        return dump.dumpToString(data);
    }

    public static class Loader {

        @Tolerate
        public Loader source(String text) {
            return source(new StringReader(text));
        }
    }

    @lombok.Builder(
            builderClassName = "Loader",
            builderMethodName = "loader",
            buildMethodName = "load"
    )
    private static LinkedHashMap<String, Object> load0(
            @lombok.NonNull Reader source,
            @Nullable Resource resource,
            @Nullable VarSupplier vars,
            @Nullable LoadSettings settings,
            @lombok.Singular Map<String, Object> inputs
    ) throws ProfileException {
        if (vars == null) {
            vars = name -> null;
        }
        var context = ParserContext.builder()
                .resource(resource)
                .vars(vars)
                .build();
        if (settings == null) {
            settings = LOAD_SETTINGS;
        }

        var composer = new Composer(settings,
                new ParserImpl(settings,
                        new StreamReader(settings, source))
        );
        var constructor = CiSpecConstructor.of(settings, context);

        // If no documents, returns empty content.
        if (!composer.hasNext()) {
            return new LinkedHashMap<>();
        }

        var doc1 = constructor.expectDoc(composer.next());

        // If no more documents, meats without header section.
        if (!composer.hasNext()) {
            return doc1;
        }

        // process header
        var header = SpecMapper.toBean(doc1, HeaderSpec.class);
        var inputSupplier = InputSuppliers.ofMap(header, inputs);
        context.setInputs(inputSupplier);

        var doc2 = constructor.expectDoc(composer.next());

        // Failed if more than two documents.
        if (composer.hasNext()) {
            throw ProfileException.invalidFormat("Expected at most two documents (header and main), but got more.");
        }
        return doc2;
    }

}
