plugins {
    id("multiloader-platform")
    id("net.fabricmc.fabric-loom")
}

loom {
    runs.named("client") {
        client()

        displayName = "fabric - Client"
        runDirectory = file("../run")
        appendProjectPathToDisplayName = false
        generateRunConfig = true
    }
}

dependencies {
    minecraft("com.mojang:minecraft:${BuildConfig.MINECRAFT_VERSION}")
    implementation("net.fabricmc:fabric-loader:${BuildConfig.FABRIC_LOADER_VERSION}")

    implementation(fabricApi.module("fabric-command-api-v2", BuildConfig.FABRIC_API_VERSION))

    api("net.uku3lig:ukulib-fabric:${BuildConfig.UKULIB_VERSION}")

    implementation("com.github.wagyourtail.luaj:luaj-core:${BuildConfig.LUAJ_REV}")
    implementation("com.github.wagyourtail.luaj:luaj-jse:${BuildConfig.LUAJ_REV}")
    include("com.github.wagyourtail.luaj:luaj-core:${BuildConfig.LUAJ_REV}")
    include("com.github.wagyourtail.luaj:luaj-jse:${BuildConfig.LUAJ_REV}")
}

modrinth {
    loaders.add("quilt")
}