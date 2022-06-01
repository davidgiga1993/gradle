/*
 * Copyright 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.performance.regression.corefeature

import org.gradle.performance.AbstractCrossVersionPerformanceTest
import org.gradle.performance.annotations.RunFor
import org.gradle.performance.annotations.Scenario

import static org.gradle.performance.annotations.ScenarioType.PER_DAY
import static org.gradle.performance.results.OperatingSystem.LINUX

@RunFor(
    @Scenario(type = PER_DAY, operatingSystems = [LINUX], testProjects = ["withVerboseTestNG", "withVerboseJUnit"])
)
class VerboseTestOutputPerformanceTest extends AbstractCrossVersionPerformanceTest {

    def "cleanTest test with verbose test output"() {
        given:
        runner.tasksToRun = ['cleanTest', 'test']
        runner.args = ['-q']
        runner.targetVersions = ["7.5-20220514015008+0000"]

        when:
        def result = runner.run()

        then:
        result.assertCurrentVersionHasNotRegressed()
    }
}
