plugins {
    id("java")
    id("org.jetbrains.intellij.platform") version "2.16.0"
    // id("org.jetbrains.intellij") version "1.17.4"
    id("com.diffplug.spotless") version "8.5.1"
    id("io.gatling.gradle") version "3.15.1"
}

group = "org.oci.devops.plugin"
version = "0.0.1-SNAPSHOT"

val customTaskGroup = "dasarathi"
val jspecifyVersion = "1.0.0"
val javaxActivationVersion = "1.1.1"
val jakartaActivationVersion = "2.1.4"
val ociSdkVersion = "2.82.0"
val joseJwtVersion = "10.9"
val slf4jVersion = "2.0.18"
val logbackVersion = "1.5.32"

val mockitoVersion = "5.23.0"
val junit5Version = "5.14.3"
val junit4Version = "4.13.2"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()
    intellijPlatform { defaultRepositories() }
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            // IntelliJ IDEA 2025.2 (Community Edition) Build #IC-252.23892.409
            sinceBuild = "252.23892"
        }
        changeNotes = "Initial version"
    }
}

tasks {
    publishPlugin {
        // gradle.properties 'marketplace.publish.token'
        // ENV VAR 'MARKETPLACE_PUBLISH_TOKEN'
        token.set(System.getenv("MARKETPLACE_PUBLISH_TOKEN"))
    }
}

dependencies {
    intellijPlatform {
        intellijIdeaCommunity("2025.2")
        bundledPlugin("com.intellij.java")
        bundledPlugin("Git4Idea")
        bundledPlugin("com.intellij.modules.json")
        bundledPlugin("org.jetbrains.plugins.yaml")
        bundledPlugin("org.intellij.plugins.markdown")
    }

    // Core & annotations
    implementation("org.jspecify:jspecify:$jspecifyVersion")

    // OCI SDKs
    implementation("com.oracle.oci.sdk:oci-java-sdk-core:$ociSdkVersion")
    implementation("com.oracle.oci.sdk:oci-java-sdk-identity:$ociSdkVersion")
    implementation("com.oracle.oci.sdk:oci-java-sdk-devops:$ociSdkVersion")
    implementation("com.oracle.oci.sdk:oci-java-sdk-common:$ociSdkVersion")
    // Misc
    implementation("javax.activation:activation:$javaxActivationVersion")
    implementation("jakarta.activation:jakarta.activation-api:$jakartaActivationVersion")
    implementation("com.nimbusds:nimbus-jose-jwt:$joseJwtVersion")

    // Logging
    implementation("org.slf4j:slf4j-api:$slf4jVersion")
    runtimeOnly("ch.qos.logback:logback-classic:$logbackVersion")

    // Tests
    testImplementation(platform("org.junit:junit-bom:$junit5Version"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testRuntimeOnly("junit:junit:$junit4Version")
    testImplementation("org.mockito:mockito-core:$mockitoVersion")
    testImplementation("org.mockito:mockito-junit-jupiter:$mockitoVersion")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

spotless {
    java {
        target("**/src/**/*.java")
        googleJavaFormat("1.34.1")
        removeUnusedImports()
        trimTrailingWhitespace()
    }
    format("xml") {
        target("**/src/**/*.xml", "**/src/**/*.xsd", "**/src/**/*.wsdl")
        trimTrailingWhitespace()
        endWithNewline()
    }
    format("properties") {
        target("**/src/**/*.properties")
        trimTrailingWhitespace()
        endWithNewline()
    }
    format("misc") {
        target("**/*.md", "**/*.json", ".gitignore")
        targetExclude("build/**", "plugin/build/**", ".gradle/**", ".gradle-user-home/**")
        trimTrailingWhitespace()
        endWithNewline()
    }
    kotlinGradle {
        target("*.gradle.kts")
        target("**/*.gradle.kts")
        ktlint()
        trimTrailingWhitespace()
        endWithNewline()
    }
    groovyGradle {
        target("*.gradle", "gradle/*.gradle")
        greclipse()
        trimTrailingWhitespace()
    }

    tasks.register("devAction") {
        group = customTaskGroup
        description = "Clean => Spotless @ Project."
        dependsOn("clean", "spotlessApply")
    }
}
