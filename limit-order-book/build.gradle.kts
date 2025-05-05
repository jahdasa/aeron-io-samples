/*
 * Copyright 2023 Adaptive Financial Consulting
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

plugins {
    id("java-application-conventions")
    id("me.champeau.jmh") version "0.7.3"
}

dependencies {
    implementation(project(":cluster-protocol"))
    implementation(project(":messages"))

    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.10.0")
//    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    // https://mvnrepository.com/artifact/org.openjdk.jol/jol-core
    implementation("org.openjdk.jol:jol-core:0.17")

    implementation("org.objectlayout:objectlayout:1.0.5-SNAPSHOT")

//    implementation(files("$projectDir/lib/ObjectLayout-1.0.5-SNAPSHOT.jar"))

    // https://mvnrepository.com/artifact/com.carrotsearch/hppc
    implementation("com.carrotsearch:hppc:0.10.0")

    // https://mvnrepository.com/artifact/org.apache.commons/commons-csv
    implementation("org.apache.commons:commons-csv:1.14.0")
}

repositories {
    mavenLocal()
    mavenCentral()
}

configurations.all {
    resolutionStrategy {
        eachDependency {
            if (requested.group == "org.objectlayout" && requested.name == "objectlayout") {
                useTarget("org.objectlayout:ObjectLayout:${requested.version}")
            }
        }
    }
}