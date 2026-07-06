plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
}

gradlePlugin {
    plugins {
        create("hiroBuildLogic") {
            id = "me.earzuchan.hiro.internal.build-logic"
            implementationClass = "me.earzuchan.hiro.buildlogic.HiroBuildLogicPlugin"
        }
    }
}
