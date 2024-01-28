@file:Suppress("UnstableApiUsage", "RedundantNullableReturnType")

import com.github.breadmoirai.githubreleaseplugin.GithubReleaseTask
import me.modmuss50.mpp.ReleaseType
import net.fabricmc.loom.task.RemapJarTask
import org.ajoberstar.grgit.Grgit
import red.jackf.GenerateChangelogTask

plugins {
    id("maven-publish")
    id("fabric-loom") version "1.5-SNAPSHOT"
    id("com.github.breadmoirai.github-release") version "2.4.1"
    id("org.ajoberstar.grgit") version "5.2.1"
    id("me.modmuss50.mod-publish-plugin") version "0.3.3"
}

val grgit: Grgit? = project.grgit

var canPublish = grgit != null && System.getenv("RELEASE") != null

fun getVersionSuffix(): String {
    return grgit?.branch?.current()?.name ?: "nogit+${properties["minecraft_version"]}"
}

group = properties["maven_group"]!!

if (System.getenv().containsKey("NEW_TAG")) {
    version = System.getenv("NEW_TAG").substring(1)
} else {
    val versionStr = "${properties["mod_version"]}+${properties["minecraft_version"]!!}"
    canPublish = false
    version = if (grgit != null) {
        "$versionStr+dev-${grgit.log()[0].abbreviatedId}"
    } else {
        "$versionStr+dev-nogit"
    }
}

base {
    archivesName.set("${properties["archives_base_name"]}")
}

repositories {
    mavenLocal()

    // Parchment Mappings
    maven {
        name = "ParchmentMC"
        url = uri("https://maven.parchmentmc.org")
        content {
            includeGroup("org.parchmentmc.data")
        }
    }
}

loom {
    splitEnvironmentSourceSets()

    mods {
        create("morechathistory") {
            sourceSet(sourceSets["client"])
        }
    }
}

dependencies {
    // To change the versions see the gradle.properties file
    minecraft("com.mojang:minecraft:${properties["minecraft_version"]}")
    mappings(loom.layered {
        officialMojangMappings()
        parchment("org.parchmentmc.data:parchment-${properties["parchment_version"]}@zip")
    })
    modImplementation("net.fabricmc:fabric-loader:${properties["loader_version"]}")
}

tasks.withType<ProcessResources>().configureEach {
    inputs.property("version", version)

    filesMatching("fabric.mod.json") {
        expand(inputs.properties)
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(17)
}

java {
    withSourcesJar()

    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

/*
tasks.jar {
    from("LICENSE") {
        rename { "${it}_${properties["archivesBaseName"]}"}
    }
}*/

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"]!!)
        }
    }

    repositories {
        // if not in CI we publish to maven local
        if (!System.getenv().containsKey("CI")) repositories.mavenLocal()

        if (canPublish) {
            maven {
                name = "JackFredMaven"
                url = uri("https://maven.jackf.red/releases/")
                content {
                    includeGroupByRegex("red.jackf.*")
                }
                credentials {
                    username = properties["jfmaven.user"]?.toString() ?: System.getenv("JACKFRED_MAVEN_USER")
                    password = properties["jfmaven.key"]?.toString() ?: System.getenv("JACKFRED_MAVEN_PASS")
                }
            }
        }
    }
}

if (canPublish) {
    val lastTag = if (System.getenv("PREVIOUS_TAG") == "NONE") null else System.getenv("PREVIOUS_TAG")
    val newTag = "v$version"

    var generateChangelogTask: TaskProvider<GenerateChangelogTask>? = null

    // Changelog Generation
    if (lastTag != null) {
        val changelogHeader = properties["changelogHeaderAddon"]?.toString()

        generateChangelogTask = tasks.register<GenerateChangelogTask>("generateChangelog") {
            this.lastTag.set(lastTag)
            this.newTag.set(newTag)
            githubUrl.set(properties["github_url"]!!.toString())
            prefixFilters.set(properties["changelog_filter"]!!.toString().split(","))
            changelogHeader?.let { prologue.set(changelogHeader) }
        }
    }

    val changelogTextProvider = if (generateChangelogTask != null) {
        provider {
            generateChangelogTask!!.get().changelogFile.get().asFile.readText()
        }
    } else {
        provider {
            "No Changelog Generated"
        }
    }

    // GitHub Release

    tasks.named<GithubReleaseTask>("githubRelease") {
        generateChangelogTask?.let { dependsOn(it) }

        authorization = System.getenv("GITHUB_TOKEN")?.let { "Bearer $it" }
        owner = properties["github_owner"]!!.toString()
        repo = properties["github_repo"]!!.toString()
        tagName = newTag
        releaseName = "${properties["mod_name"]} $newTag"
        targetCommitish = grgit!!.branch.current().name
        releaseAssets.from(
            tasks["remapJar"].outputs.files,
            tasks["remapSourcesJar"].outputs.files,
        )
        subprojects.forEach {
            releaseAssets.from(
                it.tasks["remapJar"].outputs.files,
                it.tasks["remapSourcesJar"].outputs.files,
            )
        }

        body = changelogTextProvider
    }

    // Mod Platforms
    if (listOf("CURSEFORGE_TOKEN", "MODRINTH_TOKEN").any { System.getenv().containsKey(it) }) {
        publishMods {
            changelog.set(changelogTextProvider)
            type.set(when(properties["release_type"]) {
                "release" -> ReleaseType.STABLE
                "beta" -> ReleaseType.BETA
                else -> ReleaseType.ALPHA
            })
            modLoaders.add("fabric")
            modLoaders.add("quilt")
            file.set(tasks.named<RemapJarTask>("remapJar").get().archiveFile)

            if (System.getenv().containsKey("CURSEFORGE_TOKEN") || dryRun.get()) {
                curseforge {
                    projectId.set("518319")
                    accessToken.set(System.getenv("CURSEFORGE_TOKEN"))
                    properties["game_versions"]!!.toString().split(",").forEach {
                        minecraftVersions.add(it)
                    }
                    displayName.set("${properties["prefix"]!!} ${properties["mod_name"]!!} ${version.get()}")
                }
            }

            if (System.getenv().containsKey("MODRINTH_TOKEN") || dryRun.get()) {
                modrinth {
                    accessToken.set(System.getenv("MODRINTH_TOKEN"))
                    projectId.set("8qkXwOnk")
                    properties["game_versions"]!!.toString().split(",").forEach {
                        minecraftVersions.add(it)
                    }
                    displayName.set("${properties["mod_name"]!!} ${version.get()}")
                }
            }
        }
    }
}