plugins {
	id("java-library")
	id("xyz.jpenilla.run-paper") version "3.0.2"
}

repositories {
	mavenCentral()
	maven("https://repo.papermc.io/repository/maven-public/") {
		content {
			includeGroup("io.papermc.paper")
			includeGroup("io.papermc")
			includeGroup("net.md-5")
			includeGroup("org.spigotmc")
		}
	}
	maven("https://repo.essentialsx.net/releases") {
		content {
			includeGroupByRegex("net\\.essentialsx(\\..*)?")
		}
	}
}

dependencies {
	compileOnly("io.papermc.paper:paper-api:1.20.4-R0.1-SNAPSHOT")
	compileOnly("net.essentialsx:EssentialsX:2.21.0")

	testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

configurations.testImplementation {
	extendsFrom(configurations.compileOnly.get())
}

java {
	toolchain.languageVersion = JavaLanguageVersion.of(17)
}

tasks {
	runServer {
		// Configure the Minecraft version for our task.
		// This is the only required configuration besides applying the plugin.
		// Your plugin's jar (or shadowJar if present) will be used automatically.
		minecraftVersion("1.20.4")
		jvmArgs("-Xms2G", "-Xmx2G")
	}

	processResources {
		val props = mapOf("version" to version)
		filesMatching("paper-plugin.yml") {
			expand(props)
		}
	}

	test {
		useJUnitPlatform()
	}
}
