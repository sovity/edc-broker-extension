plugins {
    `java-library`
    id("application")
    id("com.github.johnrengelman.shadow") version "7.0.0"
}

val edcVersion: String by project
val edcGroup: String by project

dependencies {
    implementation(project(":extensions:broker"))
    implementation(project(":extensions:clearinghouse"))

    implementation("${edcGroup}:control-plane-core:${edcVersion}")
    implementation("${edcGroup}:api-observability:${edcVersion}")
    implementation("${edcGroup}:data-management-api:${edcVersion}")
    implementation("${edcGroup}:configuration-filesystem:${edcVersion}")
    implementation("${edcGroup}:http:${edcVersion}")
    implementation("${edcGroup}:control-plane-aggregate-services:${edcVersion}")

    // JDK Logger
    implementation("${edcGroup}:monitor-jdk-logger:${edcVersion}")

    // IDS
    implementation("${edcGroup}:ids:${edcVersion}") {
        // Workaround for https://github.com/eclipse-dataspaceconnector/DataSpaceConnector/issues/1387
        exclude(group = edcGroup, module = "ids-token-validation")
    }

    // To use FileSystem vault e.g. -DuseFsVault="true".Only for non-production usages.
    val useFsVault: Boolean = System.getProperty("useFsVault", "true").toBoolean()
    if (useFsVault) {
        implementation("${edcGroup}:vault-filesystem:${edcVersion}")
    } else {
        implementation("${edcGroup}:azure-vault:${edcVersion}")
    }

    // Authentication for IDS API
    implementation("${edcGroup}:oauth2-core:${edcVersion}")
    implementation("${edcGroup}:jwt-core:${edcVersion}")
}

application {
    mainClass.set("org.eclipse.edc.boot.system.runtime.BaseRuntime")
}

tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    mergeServiceFiles()
    archiveFileName.set("app.jar")
}