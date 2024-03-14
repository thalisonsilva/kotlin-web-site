package tests.buildTypes

import jetbrains.buildServer.configs.kotlin.BuildType
import jetbrains.buildServer.configs.kotlin.buildFeatures.PullRequests
import jetbrains.buildServer.configs.kotlin.buildFeatures.pullRequests
import jetbrains.buildServer.configs.kotlin.buildSteps.script


object E2ETestsViaCompose : BuildType({
  name = "E2E tests via compose"

  vcs {
    root(vcsRoots.KotlinLangOrg)
  }

  steps {
    script {
      scriptContent = "docker-compose -f docker-compose-e2e-statics.yml up -d"
    }

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