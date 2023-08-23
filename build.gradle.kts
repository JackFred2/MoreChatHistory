@file:Suppress("UnstableApiUsage")

import com.github.breadmoirai.githubreleaseplugin.GithubReleaseTask
import me.modmuss50.mpp.ReleaseType
import net.fabricmc.loom.task.RemapJarTask
import red.jackf.GenerateChangelogTask
import java.net.URI

plugins {
    id("maven-publish")
    id("fabric-loom") version "1.3-SNAPSHOT"
    id("com.github.breadmoirai.github-release") version "2.4.1"
    id("org.ajoberstar.grgit") version "5.0.+"
    id("me.modmuss50.mod-publish-plugin") version "0.3.3"
}

group = properties["maven_group"]!!
        version = properties["mod_version"] ?: "dev"

val modReleaseType = properties["type"]?.toString() ?: "release"

base {
    archivesName.set("${properties["archives_base_name"]}")
}

repositories {
    // Parchment Mappings
    maven {
        name = "ParchmentMC"
        url = URI("https://maven.parchmentmc.org")
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

    runConfigs {
        configureEach {
            val path = buildscript.sourceFile?.parentFile?.resolve("log4j2.xml")
            path?.let { property("log4j2.configurationFile", path.path) }
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

tasks.jar {
    from("LICENSE") {
        rename { "${it}_${properties["archivesBaseName"]}"}
    }
}

val lastTagVal = properties["lastTag"]?.toString()
val newTagVal = properties["newTag"]?.toString()
if (lastTagVal != null && newTagVal != null) {
    val generateChangelogTask = tasks.register<GenerateChangelogTask>("generateChangelog") {
        lastTag.set(lastTagVal)
        newTag.set(newTagVal)
        githubUrl.set(properties["github_url"]!!.toString())
        prefixFilters.set(properties["changelog_filter"]!!.toString().split(","))
    }

    if (System.getenv().containsKey("GITHUB_TOKEN")) {
        tasks.named<GithubReleaseTask>("githubRelease") {
            dependsOn(generateChangelogTask)

            authorization.set(System.getenv("GITHUB_TOKEN")?.let { "Bearer $it" })
            owner.set(properties["github_owner"]!!.toString())
            repo.set(properties["github_repo"]!!.toString())
            tagName.set(newTagVal)
            releaseName.set("${properties["mod_name"]} $newTagVal")
            targetCommitish.set(grgit.branch.current().name)
            releaseAssets.from(
                    tasks["remapJar"].outputs.files,
                    tasks["remapSourcesJar"].outputs.files,
            )

            body.set(provider {
                return@provider generateChangelogTask.get().changelogFile.get().asFile.readText()
            })
        }
    }

    tasks.named<DefaultTask>("publishMods") {
        dependsOn(generateChangelogTask)
    }

    if (listOf("CURSEFORGE_TOKEN", "MODRINTH_TOKEN").any { System.getenv().containsKey(it) }) {
        publishMods {
            changelog.set(provider {
                return@provider generateChangelogTask.get().changelogFile.get().asFile.readText()
            })
            type.set(ReleaseType.STABLE)
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

// configure the maven publication
publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"]!!)
        }
    }
}