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
    testImplementation(libs.bundles.testing)

    // https://mvnrepository.com/artifact/com.carrotsearch/hppc
    implementation("com.carrotsearch:hppc:0.10.0")
}

repositories {
    mavenLocal()
    mavenCentral()
}

tasks.test {
    jvmArgs("--add-opens=java.base/sun.nio.ch=ALL-UNNAMED")
}
