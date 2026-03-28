description = "Api functions for valiobungee"

java {
    withJavadocJar()
    withSourcesJar()
}

dependencies {
    testImplementation(libs.testing.juipter)
}

tasks.test {
    useJUnitPlatform()
}

