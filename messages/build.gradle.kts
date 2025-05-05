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


@Suppress("DEPRECATION")
val generatedDir = file("${buildDir}/generated/src/main/java")
val codecGeneration = configurations.create("codecGeneration")

dependencies {
    "codecGeneration"(libs.sbe)
    implementation(libs.agrona)

    implementation(project(":cluster-protocol"))
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

sourceSets {
    main {
        java.srcDir(generatedDir)
    }
}

tasks {
    task("generateCodecs", JavaExec::class) {
        group = "sbe"
        val sbeFile = "src/main/resources/sbe/fpl/sbe.xsd"
        val adminFile = "src/main/resources/sbe/Admin.xml"
        val marketDataMessageFile = "src/main/resources/sbe/MarketDataMessage.xml"
        val nativeMessageFile = "src/main/resources/sbe/NativeMessage.xml"
        inputs.files(adminFile, marketDataMessageFile, nativeMessageFile)
        outputs.dir(generatedDir)
        classpath = codecGeneration
        mainClass.set("uk.co.real_logic.sbe.SbeTool")
        args = listOf(adminFile, marketDataMessageFile, nativeMessageFile)
        systemProperties["sbe.output.dir"] = generatedDir
        systemProperties["sbe.target.language"] = "Java"
        systemProperties["sbe.validation.xsd"] = sbeFile
        systemProperties["sbe.validation.stop.on.error"] = "true"
        outputs.dir(generatedDir)
    }

    compileJava {
        dependsOn("generateCodecs")
    }
}