/*
 * Copyright 2021 the original author or authors.
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

package org.gradle.smoketests

import org.gradle.testkit.runner.TaskOutcome
import spock.lang.Ignore

@Ignore("until wrapper update because :kotlin-dsl compilation error")
class GradleBuildIntegTestConfigurationCacheSmokeTest extends AbstractGradleBuildConfigurationCacheSmokeTest {
    def "can run Gradle integ tests with configuration cache enabled"() {
        given: "tasks whose configuration can only be loaded in the original daemon"
        def supportedTasks = [
            ":configuration-cache:embeddedIntegTest",
            "--tests=org.gradle.internal.cc.impl.ConfigurationCacheDebugLogIntegrationTest"
        ]

        when:
        configurationCacheRun supportedTasks, 0

        then:
        result.assertConfigurationCacheStateStored()

        when:
        run([":configuration-cache:clean"])

        then:
        configurationCacheRun supportedTasks, 1

        then:
        result.assertConfigurationCacheStateLoaded()
        result.task(":configuration-cache:embeddedIntegTest").outcome == TaskOutcome.FROM_CACHE
        assertTestClassExecutedIn "platforms/core-configuration/configuration-cache", "org.gradle.internal.cc.impl.ConfigurationCacheDebugLogIntegrationTest"
    }
}
