plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
}

sourceSets {
    main {
        kotlin.srcDir("src/hiroGradlePlugin/kotlin")
    }
}

gradlePlugin {
    plugins {
        create("hiroBuildLogic") {
            id = "me.earzuchan.hiro.internal.build-logic"
            implementationClass = "me.earzuchan.hiro.buildlogic.HiroBuildLogicPlugin"
        }
        create("hiro") {
            id = "me.earzuchan.hiro"
            implementationClass = "me.earzuchan.hiro.gradle.HiroGradlePlugin"
        }
    }
}
