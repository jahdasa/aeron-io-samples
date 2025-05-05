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
    implementation(libs.agrona)
    implementation(libs.aeron)
    implementation(libs.slf4j)
    implementation(libs.logback)
    implementation(project(":cluster-protocol"))
    implementation(project(":limit-order-book"))
    implementation(project(":messages"))
    implementation(project(":socket"))
    testImplementation(libs.bundles.testing)

    // https://mvnrepository.com/artifact/com.carrotsearch/hppc
    implementation("com.carrotsearch:hppc:0.10.0")

    // https://mvnrepository.com/artifact/joda-time/joda-time
    implementation("joda-time:joda-time:2.14.0")

    // https://mvnrepository.com/artifact/org.apache.commons/commons-csv
    implementation("org.apache.commons:commons-csv:1.14.0")

    testImplementation("org.apache.commons:commons-lang3:3.17.0")
    testImplementation("com.itextpdf:itextpdf:5.5.13.4")

    jmh("commons-io:commons-io:2.19.0")
}

tasks {
    task("runSingleNodeCluster", JavaExec::class) {
        group = "run"
        classpath = sourceSets.main.get().runtimeClasspath
        mainClass.set("io.aeron.samples.ClusterApp")
        jvmArgs("--add-opens=java.base/sun.nio.ch=ALL-UNNAMED")
    }


    task ("uberJar", Jar::class) {
        group = "uber"
        manifest {
            attributes["Main-Class"]="io.aeron.samples.ClusterApp"
            attributes["Add-Opens"]="java.base/sun.nio.ch"
        }
        archiveClassifier.set("uber")
        from(sourceSets.main.get().output)
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        dependsOn(configurations.runtimeClasspath)
        from({
            configurations.runtimeClasspath.get().filter { it.name.endsWith("jar") }.map { zipTree(it) }
        })
    }

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
