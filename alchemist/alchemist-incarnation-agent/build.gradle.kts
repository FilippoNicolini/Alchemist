dependencies {
    api(project(":alchemist-interfaces"))
    api(project(":alchemist-implementationbase"))
    implementation(project(":alchemist-time"))
    implementation("it.unibo.alice.tuprolog:tuprolog:3.3.0")
    testImplementation(project(":alchemist-engine"))
    testImplementation(project(":alchemist-loading"))
}