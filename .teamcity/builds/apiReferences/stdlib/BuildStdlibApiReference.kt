package builds.apiReferences.stdlib

import jetbrains.buildServer.configs.kotlin.AbsoluteId
import jetbrains.buildServer.configs.kotlin.BuildType

object BuildStdlibApiReference : BuildType({
    name = "Stdlib Api reference"
    artifactRules = "latest-version.zip"

    vcs {
        root(vcsRoots.KotlinLangOrg)
    }

    dependencies {
        dependency(AbsoluteId("Kotlin_KotlinRelease_1920_LibraryReferenceLatestDocs")) {
            artifacts {
                cleanDestination = true
                artifactRules = "latest-version.zip"
            }
        }
    }
})
