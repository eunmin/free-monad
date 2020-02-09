plugins {
    kotlin("jvm") version "1.3.61"
    kotlin("kapt") version "1.3.61"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven(url = "https://dl.bintray.com/arrow-kt/arrow-kt/")
}

apply(plugin = "kotlin-kapt")

val arrow_version = "0.10.4"

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("io.arrow-kt:arrow-fx:$arrow_version")
    implementation("io.arrow-kt:arrow-free:$arrow_version")
    implementation("io.arrow-kt:arrow-syntax:$arrow_version")
    implementation("io.arrow-kt:arrow-mtl:$arrow_version")

    kapt("io.arrow-kt:arrow-meta:$arrow_version")
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
}