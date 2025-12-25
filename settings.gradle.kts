plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}
rootProject.name = "TCPEncryptionTutorial"
include("Server")
include("Client")
include("Common")