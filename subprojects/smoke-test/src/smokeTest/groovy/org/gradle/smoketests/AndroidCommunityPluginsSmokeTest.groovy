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

import org.gradle.internal.reflect.validation.ValidationMessageChecker

import static org.gradle.internal.reflect.validation.Severity.ERROR

class AndroidCommunityPluginsSmokeTest extends AbstractPluginValidatingSmokeTest implements ValidationMessageChecker {

    private static final String ANDROID_PLUGIN_VERSION_FOR_TESTS = TestedVersions.androidGradle.latestStartsWith("4.2")

    private static final String DAGGER_HILT_ANDROID_PLUGIN_ID = 'dagger.hilt.android.plugin'
    private static final String TRIPLET_PLAY_PLUGIN_ID = 'com.github.triplet.play'
    private static final String FLADLE_PLUGIN_ID = 'com.osacky.fladle'
    private static final String SAFEARGS_PLUGIN_ID = 'androidx.navigation.safeargs'
    private static final String GOOGLE_SERVICES_PLUGIN_ID = 'com.google.gms.google-services'
    private static final String CRASHLYTICS_PLUGIN_ID = 'com.google.firebase.crashlytics'
    private static final String FIREBASE_PERF_PLUGIN_ID = 'com.google.firebase.firebase-perf'
    private static final String BUGSNAG_PLUGIN_ID = 'com.bugsnag.android.gradle'
    private static final String PROGUARD_PLUGIN_ID = 'io.sentry.android.gradle'

    @Override
    Map<String, Versions> getPluginsToValidate() {
        [
            (GOOGLE_SERVICES_PLUGIN_ID): Versions.of('4.3.5'),
            (CRASHLYTICS_PLUGIN_ID): Versions.of('2.5.1'),
            (FIREBASE_PERF_PLUGIN_ID): Versions.of('1.3.5'),
            (BUGSNAG_PLUGIN_ID): Versions.of('5.7.4'),
            (FLADLE_PLUGIN_ID): Versions.of('0.6.7'),
            (TRIPLET_PLAY_PLUGIN_ID): Versions.of('3.3.0-agp4.2'),
            (SAFEARGS_PLUGIN_ID): Versions.of('2.3.3'),
            (DAGGER_HILT_ANDROID_PLUGIN_ID): Versions.of('2.33-beta'),
            (PROGUARD_PLUGIN_ID): Versions.of('1.7.36'),
        ]
    }

    @Override
    void configureValidation(String testedPluginId, String version) {
        AGP_VERSIONS.assumeCurrentJavaVersionIsSupportedBy(ANDROID_PLUGIN_VERSION_FOR_TESTS)
        configureAndroidProject(testedPluginId)

        List<String> failingAndroidPlugins = failingAndroidPlugins(testedPluginId)

        validatePlugins {
            onPlugins(failingAndroidPlugins) {
                failsWith([
                    (missingAnnotationMessage { type('TaskManager.ConfigAttrTask').property('consumable').missingInputOrOutput().includeLink() }): ERROR,
                    (missingAnnotationMessage { type('TaskManager.ConfigAttrTask').property('resolvable').missingInputOrOutput().includeLink() }): ERROR,
                ])
            }
            switch (testedPluginId) {
                case FLADLE_PLUGIN_ID:
                    passing {
                        it !in (failingAndroidPlugins + FLADLE_PLUGIN_ID)
                    }
                    onPlugins([FLADLE_PLUGIN_ID]) {
                        failsWith([
                            (missingAnnotationMessage { type('YamlConfigWriterTask').property('description').missingInputOrOutput().includeLink() }): ERROR,
                            (missingAnnotationMessage { type('YamlConfigWriterTask').property('group').missingInputOrOutput().includeLink() }): ERROR,

                        ])
                    }
                    break
                case PROGUARD_PLUGIN_ID:
                    passing {
                        it !in (failingAndroidPlugins + PROGUARD_PLUGIN_ID)
                    }
                    onPlugins([PROGUARD_PLUGIN_ID]) {
                        failsWith([
                            (missingAnnotationMessage { type('SentryProguardConfigTask').property('applicationVariant').missingInputOrOutput().includeLink() }): ERROR,
                        ])
                    }
                    break
                default:
                    passing {
                        it !in failingAndroidPlugins
                    }
                    break
            }
        }
    }

    private static List<String> failingAndroidPlugins(String testedPluginId) {
        String androidApplicationPluginId = testedPluginId in [GOOGLE_SERVICES_PLUGIN_ID, DAGGER_HILT_ANDROID_PLUGIN_ID] ? 'android' : 'com.android.application'
        ['com.android.internal.version-check', 'com.android.internal.application', androidApplicationPluginId]
    }

    private void configureAndroidProject(String testedPluginId) {
        settingsFile << """
            pluginManagement {
                repositories {
                    gradlePluginPortal()
                    google()
                }
                resolutionStrategy.eachPlugin {
                    if (pluginRequest.id.id.startsWith('com.android')) {
                        useModule("com.android.tools.build:gradle:\${pluginRequest.version}")
                    }
                    if (pluginRequest.id.id == '${GOOGLE_SERVICES_PLUGIN_ID}') {
                        useModule("com.google.gms:google-services:\${pluginRequest.version}")
                    }
                    if (pluginRequest.id.id == '${CRASHLYTICS_PLUGIN_ID}') {
                        useModule("com.google.firebase:firebase-crashlytics-gradle:\${pluginRequest.version}")
                    }
                    if (pluginRequest.id.id == '${FIREBASE_PERF_PLUGIN_ID}') {
                        useModule("com.google.firebase:perf-plugin:\${pluginRequest.version}")
                    }
                    if (pluginRequest.id.id == '${SAFEARGS_PLUGIN_ID}') {
                        useModule("androidx.navigation:navigation-safe-args-gradle-plugin:\${pluginRequest.version}")
                    }
                    if (pluginRequest.id.id == '${DAGGER_HILT_ANDROID_PLUGIN_ID}') {
                        useModule("com.google.dagger:hilt-android-gradle-plugin:\${pluginRequest.version}")
                    }
                    if (pluginRequest.id.id == '${PROGUARD_PLUGIN_ID}') {
                        useModule("io.sentry:sentry-android-gradle-plugin:\${pluginRequest.version}")
                    }
                }
            }
        """
        buildFile << """
                android {
                    compileSdkVersion 24
                    buildToolsVersion '${TestedVersions.androidTools}'
                    defaultConfig {
                        versionCode 1
                    }
                }
        """
        file("gradle.properties") << """
            android.useAndroidX=true
            android.enableJetifier=true
        """.stripIndent()
        file("src/main/AndroidManifest.xml") << """<?xml version="1.0" encoding="utf-8"?>
            <manifest xmlns:android="http://schemas.android.com/apk/res/android"
                package="org.gradle.android.example.app">

                <application android:label="@string/app_name" >
                    <activity
                        android:name=".AppActivity"
                        android:label="@string/app_name" >
                        <intent-filter>
                            <action android:name="android.intent.action.MAIN" />
                            <category android:name="android.intent.category.LAUNCHER" />
                        </intent-filter>
                    </activity>
                    <activity
                        android:name="org.gradle.android.example.app.AppActivity">
                    </activity>
                </application>

            </manifest>""".stripIndent()

        switch (testedPluginId) {
            case DAGGER_HILT_ANDROID_PLUGIN_ID:
                buildFile << """
                    dependencies {
                        implementation "com.google.dagger:hilt-android:2.33-beta"
                        implementation "com.google.dagger:hilt-compiler:2.33-beta"
                    }
                """
                break
            case TRIPLET_PLAY_PLUGIN_ID:
                buildFile << """
                    play {
                        serviceAccountCredentials.set(file("your-key.json"))
                        updatePriority.set(2)
                    }
                """
                break
        }
    }

    @Override
    Map<String, String> getExtraPluginsRequiredForValidation(String testedPluginId, String version) {
        if (testedPluginId == DAGGER_HILT_ANDROID_PLUGIN_ID) {
            return [
                'com.android.application': ANDROID_PLUGIN_VERSION_FOR_TESTS,
                'org.jetbrains.kotlin.android': TestedVersions.kotlin.latest()
            ]
        }
        return ['com.android.application': ANDROID_PLUGIN_VERSION_FOR_TESTS]
    }
}
