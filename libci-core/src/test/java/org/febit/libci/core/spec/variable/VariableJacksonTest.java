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
package org.febit.libci.core.spec.variable;

import org.febit.libci.core.spec.support.SpecMapper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class VariableJacksonTest {

    @Test
    void generic() {

        assertThat(SpecMapper.toBean(1.0F, GenericVariable.class))
                .isNotNull()
                .returns("1.0", IVariable::value);

        assertThat(SpecMapper.toBean(Map.of(
                "value", "abc",
                "expand", false,
                "description", "desc",
                "options", List.of(
                        "opt1",
                        "opt2"
                )
        ), GenericVariable.class))
                .isNotNull()
                .returns("abc", IVariable::value)
                .returns(false, IVariable::expand)
                .returns("desc", IVariable::description)
                .returns(List.of("opt1", "opt2"), IVariable::options)
        ;
    }

    @Test
    void job() {
        assertThat(SpecMapper.toBean(123, JobVariable.class))
                .isNotNull()
                .returns("123", IVariable::value)
                .returns(true, IVariable::expand)
                .returns(null, IVariable::description)
                .returns(null, IVariable::options)
        ;

        assertThat(SpecMapper.toBean("123", JobVariable.class))
                .isNotNull()
                .returns("123", IVariable::value);

        assertThat(SpecMapper.toBean(true, JobVariable.class))
                .isNotNull()
                .returns("true", IVariable::value);

        assertThat(SpecMapper.toBean(Map.of(
                "value", "abc",
                "expand", false,
                "description", "desc",
                "options", List.of(
                        "opt1",
                        "opt2"
                )
        ), JobVariable.class))
                .isNotNull()
                .returns("abc", IVariable::value)
                .returns(false, IVariable::expand)

                // NOTICE: NOT support description and options, because they are not defined in JobVariable
                .returns(null, IVariable::description)
                .returns(null, IVariable::options)
        ;
    }

    @Test
    void jobRule() {
        assertThat(SpecMapper.toBean(123D, JobRuleVariable.class))
                .isNotNull()
                .returns("123.0", IVariable::value)
                .returns(null, IVariable::expand)
                .returns(null, IVariable::description)
                .returns(null, IVariable::options)
        ;

        assertThat(SpecMapper.toBean("123", JobRuleVariable.class))
                .isNotNull()
                .returns("123", IVariable::value);

        assertThat(SpecMapper.toBean(true, JobRuleVariable.class))
                .isNotNull()
                .returns("true", IVariable::value);

        assertThat(SpecMapper.toBean(Map.of(
                "value", "abc",
                "expand", false,
                "description", "desc",
                "options", List.of(
                        "opt1",
                        "opt2"
                )
        ), JobRuleVariable.class))
                .isNotNull()
                .returns("abc", IVariable::value)
                .returns(null, IVariable::expand)
                .returns(null, IVariable::description)
                .returns(null, IVariable::options)
        ;
    }

    @Test
    void workflowRule() {
        assertThat(SpecMapper.toBean(123L, WorkflowRuleVariable.class))
                .isNotNull()
                .returns("123", IVariable::value)
                .returns(null, IVariable::expand)
                .returns(null, IVariable::description)
                .returns(null, IVariable::options)
        ;

        assertThat(SpecMapper.toBean("123", WorkflowRuleVariable.class))
                .isNotNull()
                .returns("123", IVariable::value);

        assertThat(SpecMapper.toBean(true, WorkflowRuleVariable.class))
                .isNotNull()
                .returns("true", IVariable::value);

        assertThat(SpecMapper.toBean(Map.of(
                "value", "abc",
                "expand", false,
                "description", "desc",
                "options", List.of(
                        "opt1",
                        "opt2"
                )
        ), WorkflowRuleVariable.class))
                .isNotNull()
                .returns("abc", IVariable::value)
                .returns(null, IVariable::expand)
                .returns(null, IVariable::description)
                .returns(null, IVariable::options)
        ;
    }
}
