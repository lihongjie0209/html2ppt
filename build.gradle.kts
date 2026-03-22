plugins {
    java
    application
    jacoco
}

group = "com.html2ppt"
version = "1.0.0"

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

val poiVersion = "5.3.0"
val picocliVersion = "4.7.6"

dependencies {
    // HTML parsing
    implementation("org.jsoup:jsoup:1.21.1")

    // CSS parsing (cascade, specificity, <style> blocks)
    implementation("com.helger:ph-css:7.0.4")

    // Flexbox layout engine (pure Java port of Facebook Yoga)
    implementation("org.appliedenergistics.yoga:yoga:1.0.0")

    // PPT read + write — Apache POI
    implementation("org.apache.poi:poi-ooxml:$poiVersion")

    // CLI framework
    implementation("info.picocli:picocli:$picocliVersion")
    annotationProcessor("info.picocli:picocli-codegen:$picocliVersion")

    // Logging
    implementation("org.slf4j:slf4j-simple:2.0.16")

    // Testing
    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.assertj:assertj-core:3.27.3")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

application {
    mainClass = "com.html2ppt.cli.Main"
}

tasks.test {
    useJUnitPlatform()
    finalizedBy(tasks.jacocoTestReport)
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required = true
        html.required = true
    }
}

tasks.jacocoTestCoverageVerification {
    violationRules {
        rule {
            element = "BUNDLE"
            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                minimum = "0.85".toBigDecimal()
            }
            excludes = listOf("com/html2ppt/cli/*", "com/html2ppt/tools/*")
        }
    }
}

tasks.register<JavaExec>("renderSlides") {
    group = "tools"
    description = "Render PPTX slides to PNG images"
    mainClass = "com.html2ppt.tools.SlideRenderer"
    classpath = sourceSets["main"].runtimeClasspath
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.compilerArgs.add("-Aproject=${project.group}/${project.name}")
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "com.html2ppt.cli.Main"
    }
}
