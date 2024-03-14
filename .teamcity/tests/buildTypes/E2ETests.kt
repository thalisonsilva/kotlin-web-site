package tests.buildTypes

import jetbrains.buildServer.configs.kotlin.BuildStep
import jetbrains.buildServer.configs.kotlin.BuildType
import jetbrains.buildServer.configs.kotlin.buildFeatures.PullRequests
import jetbrains.buildServer.configs.kotlin.buildFeatures.pullRequests
import jetbrains.buildServer.configs.kotlin.buildSteps.DockerComposeStep
import jetbrains.buildServer.configs.kotlin.buildSteps.dockerCommand
import jetbrains.buildServer.configs.kotlin.buildSteps.dockerCompose
import jetbrains.buildServer.configs.kotlin.buildSteps.script


object E2ETests : BuildType({
  name = "E2E tests"

  vcs {
    root(vcsRoots.KotlinLangOrg)
  }

  steps {

      dockerCompose {
          file = "docker-compose-e2e-statics.yml"
      }

//      dockerCommand {
//          name = "Run tests"
//            commandType = other() {
//                subCommand = "exec playwright yarn run test:visual:ci"
//            }
//      }

    script {
      scriptContent = "docker-compose exec playwright yarn run test:visual:ci"
    }

    script {
      scriptContent = "docker-compose down"
    }
  }

  requirements {
    exists("docker.server.version")
    contains("docker.server.osType", "linux")
  }

  features {
    pullRequests {
      vcsRootExtId = "${vcsRoots.KotlinLangOrg.id}"
      provider = github {
        authType = token {
          token = "%github.oauth%"
        }
        filterAuthorRole = PullRequests.GitHubRoleFilter.MEMBER_OR_COLLABORATOR
      }
    }
  }
})