plugins {
    java
}

group = "de.coolepizza"
val baseVersion = "1.2.2"
val isTagRelease = System.getenv("GITHUB_REF_TYPE") == "tag" || project.hasProperty("release")
val buildNumber = System.getenv("GITHUB_RUN_NUMBER") ?: project.findProperty("buildNumber") as String?
version = if (buildNumber != null && !isTagRelease) "$baseVersion-b$buildNumber" else baseVersion

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:26.2.build.123-stable")
    testImplementation("io.papermc.paper:paper-api:26.2.build.123-stable")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<JavaCompile> {
    options.compilerArgs.add("-Xlint:deprecation")
}

tasks.processResources {
    val props = mapOf("version" to project.version.toString())
    inputs.properties(props)
    filteringCharset = "UTF-8"
    filesMatching("plugin.yml") {
        expand(props)
    }
}

tasks.test {
    useJUnitPlatform()
}