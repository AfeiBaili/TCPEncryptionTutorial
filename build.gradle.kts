import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

plugins {
    kotlin("jvm") version "2.2.21" apply false
    id("com.gradleup.shadow") version "9.3.0" apply false
}

ext {
    set("version", version)
}

subprojects {
    apply {
        plugin("org.jetbrains.kotlin.jvm")
        plugin("com.gradleup.shadow")
    }

    group = "online.afeibaili.an"
    version = "1.0"

    repositories {
        mavenCentral()
    }

    dependencies {
        add("testImplementation", kotlin("test"))
        add("implementation", "org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")

        if (project.name != "Common") {
            add("implementation", project(":Common"))
        }
    }

    extensions.configure<KotlinJvmProjectExtension> {
        jvmToolchain(17)
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
    }
}
