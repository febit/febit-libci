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
package org.febit.libci.core.spec;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ArtifactsSpecTest {

    @Nested
    class Access_ {

        @Test
        void values() {
            assertEquals(3, ArtifactsSpec.Access.values().length);
            assertNotNull(ArtifactsSpec.Access.valueOf("ALL"));
            assertNotNull(ArtifactsSpec.Access.valueOf("DEVELOPER"));
            assertNotNull(ArtifactsSpec.Access.valueOf("NONE"));
        }

        @Test
        void valueAndDescription() {
            assertEquals("all", ArtifactsSpec.Access.ALL.getValue());
            assertEquals("developer", ArtifactsSpec.Access.DEVELOPER.getValue());
            assertEquals("none", ArtifactsSpec.Access.NONE.getValue());
            for (var acc : ArtifactsSpec.Access.values()) {
                assertNotNull(acc.getDescription());
                assertFalse(acc.getDescription().isBlank());
            }
        }
    }

    @Nested
    class When_ {

        @Test
        void values() {
            assertEquals(3, ArtifactsSpec.When.values().length);
            assertNotNull(ArtifactsSpec.When.valueOf("ALWAYS"));
            assertNotNull(ArtifactsSpec.When.valueOf("ON_SUCCESS"));
            assertNotNull(ArtifactsSpec.When.valueOf("ON_FAILURE"));
        }

        @Test
        void valueAndDescription() {
            assertEquals("always", ArtifactsSpec.When.ALWAYS.getValue());
            assertEquals("on_success", ArtifactsSpec.When.ON_SUCCESS.getValue());
            assertEquals("on_failure", ArtifactsSpec.When.ON_FAILURE.getValue());
            for (var w : ArtifactsSpec.When.values()) {
                assertNotNull(w.getDescription());
                assertFalse(w.getDescription().isBlank());
            }
        }

        @Test
        void isMethods() {
            assertTrue(ArtifactsSpec.When.ALWAYS.isAlways());
            assertFalse(ArtifactsSpec.When.ALWAYS.isOnSuccess());
            assertFalse(ArtifactsSpec.When.ALWAYS.isOnFailure());

            assertFalse(ArtifactsSpec.When.ON_SUCCESS.isAlways());
            assertTrue(ArtifactsSpec.When.ON_SUCCESS.isOnSuccess());
            assertFalse(ArtifactsSpec.When.ON_SUCCESS.isOnFailure());

            assertFalse(ArtifactsSpec.When.ON_FAILURE.isAlways());
            assertFalse(ArtifactsSpec.When.ON_FAILURE.isOnSuccess());
            assertTrue(ArtifactsSpec.When.ON_FAILURE.isOnFailure());
        }
    }

    @Nested
    class ReportKind_ {

        @Test
        void values() {
            assertEquals(19, ArtifactsSpec.ReportKind.values().length);
        }

        @Test
        void valueAndDescription() {
            for (var kind : ArtifactsSpec.ReportKind.values()) {
                assertNotNull(kind.value(), kind.name());
                assertNotNull(kind.description(), kind.name());
                assertFalse(kind.description().isBlank(), kind.name());
            }
        }

        @Test
        void normalizerNonNull() {
            for (var kind : ArtifactsSpec.ReportKind.values()) {
                assertNotNull(kind.normalizer(), kind.name() + " should have a normalizer");
            }
        }

        @Test
        void stringNormalizer() {
            assertEquals("hello", ArtifactsSpec.ReportKind.ACCESSIBILITY.normalizer().apply("hello"));
            assertNull(ArtifactsSpec.ReportKind.ACCESSIBILITY.normalizer().apply(null));
        }

        @Test
        void stringListNormalizer() {
            var result = ArtifactsSpec.ReportKind.JUNIT.normalizer().apply("path/to/report.xml");
            assertInstanceOf(List.class, result);
            assertEquals(List.of("path/to/report.xml"), result);

            var nullResult = ArtifactsSpec.ReportKind.JUNIT.normalizer().apply(null);
            assertInstanceOf(List.class, nullResult);
            assertTrue(((List<?>) nullResult).isEmpty());
        }

        @Test
        void stringListWithMultipleValues() {
            var result = ArtifactsSpec.ReportKind.CODE_QUALITY.normalizer()
                    .apply(List.of("a.json", "b.json"));
            assertEquals(List.of("a.json", "b.json"), result);
        }

        @Test
        void coverageReportNormalizer() {
            var input = Map.of("coverage_format", "cobertura", "path", "coverage.xml");
            var result = ArtifactsSpec.ReportKind.COVERAGE_REPORT.normalizer().apply(input);
            assertInstanceOf(ArtifactsSpec.CoverageReport.class, result);
            var report = (ArtifactsSpec.CoverageReport) result;
            assertEquals(ArtifactsSpec.CoverageReport.Format.COBERTURA, report.coverageFormat());
            assertEquals("coverage.xml", report.path());

            assertNull(ArtifactsSpec.ReportKind.COVERAGE_REPORT.normalizer().apply(null));
        }

        @Test
        void unknownNormalizer() {
            var obj = new String[]{"something"};
            var result = ArtifactsSpec.ReportKind.API_FUZZING.normalizer().apply(obj);
            assertSame(obj, result);

            assertNull(ArtifactsSpec.ReportKind.API_FUZZING.normalizer().apply(null));
        }
    }

    @Nested
    class CoverageReport_ {

        @Test
        void formatEnum() {
            assertEquals(2, ArtifactsSpec.CoverageReport.Format.values().length);
            assertEquals("cobertura", ArtifactsSpec.CoverageReport.Format.COBERTURA.getValue());
            assertEquals("jacoco", ArtifactsSpec.CoverageReport.Format.JACOCO.getValue());
        }

        @Test
        void builder() {
            var report = ArtifactsSpec.CoverageReport.builder()
                    .coverageFormat(ArtifactsSpec.CoverageReport.Format.JACOCO)
                    .path("build/reports/jacoco/test/jacocoTestReport.xml")
                    .build();
            assertEquals(ArtifactsSpec.CoverageReport.Format.JACOCO, report.coverageFormat());
            assertEquals("build/reports/jacoco/test/jacocoTestReport.xml", report.path());
        }

        @Test
        void implementsIReport() {
            var report = ArtifactsSpec.CoverageReport.builder()
                    .coverageFormat(ArtifactsSpec.CoverageReport.Format.COBERTURA)
                    .path("target/coverage.xml")
                    .build();
            assertInstanceOf(ArtifactsSpec.IReport.class, report);
            assertInstanceOf(ISpec.class, report);
        }
    }

    @Nested
    class ArtifactsSpec_ {

        @Test
        void defaults() {
            var spec = ArtifactsSpec.builder().build();
            assertEquals("artifacts", spec.name());
            assertEquals(true, spec.isPublic());
            assertEquals(false, spec.untracked());
            assertEquals(ArtifactsSpec.When.ON_SUCCESS, spec.when());
            assertEquals(ArtifactsSpec.Access.ALL, spec.access());
            assertTrue(spec.paths().isEmpty());
            assertTrue(spec.exclude().isEmpty());
            assertTrue(spec.reports().isEmpty());
            assertNull(spec.expireIn());
            assertNull(spec.exposeAs());
        }

        @Test
        void customValues() {
            var spec = ArtifactsSpec.builder()
                    .name("custom-name")
                    .isPublic(false)
                    .untracked(true)
                    .when(ArtifactsSpec.When.ALWAYS)
                    .access(ArtifactsSpec.Access.DEVELOPER)
                    .paths(List.of("dist/*.jar"))
                    .exclude(List.of("dist/*-sources.jar"))
                    .exposeAs("My Artifacts")
                    .build();

            assertEquals("custom-name", spec.name());
            assertEquals(false, spec.isPublic());
            assertEquals(true, spec.untracked());
            assertEquals(ArtifactsSpec.When.ALWAYS, spec.when());
            assertEquals(ArtifactsSpec.Access.DEVELOPER, spec.access());
            assertEquals(List.of("dist/*.jar"), spec.paths());
            assertEquals(List.of("dist/*-sources.jar"), spec.exclude());
            assertEquals("My Artifacts", spec.exposeAs());
        }

        @Test
        void withReports() {
            var reports = Collections.singletonMap(
                    ArtifactsSpec.ReportKind.JUNIT,
                    (Serializable) "target/surefire-reports/*.xml"
            );
            var spec = ArtifactsSpec.builder().reports(reports).build();

            assertEquals(1, spec.reports().size());
            var junitReport = spec.reports().get(ArtifactsSpec.ReportKind.JUNIT);
            assertNotNull(junitReport);
            assertInstanceOf(List.class, junitReport);
        }

        @Test
        void filtersNullReportKey() {
            var map = new java.util.LinkedHashMap<ArtifactsSpec.ReportKind, Serializable>();
            map.put(ArtifactsSpec.ReportKind.DOTENV, "build/artifacts.env");
            map.put(null, "should-be-filtered");

            var spec = ArtifactsSpec.builder().reports(map).build();
            assertEquals(1, spec.reports().size());
            assertNotNull(spec.reports().get(ArtifactsSpec.ReportKind.DOTENV));
            assertNull(spec.reports().get(null));
        }

        @Test
        void implementsISpec() {
            var spec = ArtifactsSpec.builder().build();
            assertInstanceOf(ISpec.class, spec);
        }
    }

    @Nested
    class Defaults_ {

        @Test
        void constants() {
            assertEquals("artifacts", ArtifactsSpec.Defaults.NAME);
            assertEquals(true, ArtifactsSpec.Defaults.IS_PUBLIC);
            assertEquals(false, ArtifactsSpec.Defaults.UNTRACKED);
            assertEquals(ArtifactsSpec.When.ON_SUCCESS, ArtifactsSpec.Defaults.WHEN);
            assertEquals(ArtifactsSpec.Access.ALL, ArtifactsSpec.Defaults.ACCESS);
            assertTrue(ArtifactsSpec.Defaults.PATHS.isEmpty());
            assertTrue(ArtifactsSpec.Defaults.EXCLUDE.isEmpty());
            assertTrue(ArtifactsSpec.Defaults.REPORTS.isEmpty());
        }
    }
}
