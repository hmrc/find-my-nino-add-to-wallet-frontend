import play.sbt.routes.RoutesKeys
import sbt.Def
import scoverage.ScoverageKeys
import uk.gov.hmrc.versioning.SbtGitVersioning.autoImport.majorVersion
import uk.gov.hmrc.DefaultBuildSettings

lazy val appName: String = "find-my-nino-add-to-wallet-frontend"

ThisBuild / majorVersion := 1
ThisBuild / scalaVersion := "3.3.5"


lazy val scoverageSettings = {
  import scoverage.ScoverageKeys
  Seq(
    // Semicolon-separated list of regexs matching classes to exclude
    ScoverageKeys.coverageExcludedPackages := "<empty>;Reverse.*;models/.data/..*;view.*;models.*;pages.*;.*queries;.*(AuthService|BuildInfo|Routes).*;util.*;.*\\$anon.*",
    ScoverageKeys.coverageExcludedFiles := ";.*services.AuditService;.*util.BaseResourceStreamResolver;.*util.FopURIResolver;.*util.StylesheetResourceStreamResolver;.*util.XmlFoToPDF;",
    ScoverageKeys.coverageMinimumStmtTotal := 90,
    ScoverageKeys.coverageFailOnMinimum := true,
    ScoverageKeys.coverageHighlighting := true,
  )
}

addCommandAlias("report", ";clean; coverage; test; it/test; coverageReport")

lazy val root = (project in file("."))
  .enablePlugins(PlayScala, SbtDistributablesPlugin)
  .disablePlugins(JUnitXmlReportPlugin) //Required to prevent https://github.com/scalatest/scalatest/issues/1427
  .settings(inConfig(Test)(testSettings): _*)
  .settings(ThisBuild / useSuperShell := false)
  .settings(
      name := appName,
      RoutesKeys.routesImport ++= Seq(
          "uk.gov.hmrc.play.bootstrap.binders._",
          "controllers.bindable._",
          "models._"
      ),
      TwirlKeys.templateImports ++= Seq(
          "play.twirl.api.HtmlFormat",
          "play.twirl.api.HtmlFormat._",
          "uk.gov.hmrc.govukfrontend.views.html.components._",
          "uk.gov.hmrc.hmrcfrontend.views.html.components._",
          "uk.gov.hmrc.hmrcfrontend.views.html.helpers._",
          "uk.gov.hmrc.hmrcfrontend.views.config._",
          "views.ViewUtils._",
          "models.Mode",
          "controllers.routes._",
          "viewmodels.govuk.all._",
          "uk.gov.hmrc.play.bootstrap.binders._",
      ),
      PlayKeys.playDefaultPort := 14006,
    scalacOptions ++= Seq(
      "-unchecked",
      "-feature",
      "-language:noAutoTupling",
      "-Werror",
      "-Wconf:msg=unused import&src=.*views/.*:s",
      "-Wconf:msg=unused import&src=<empty>:s",
      "-Wconf:msg=unused&src=.*RoutesPrefix\\.scala:s",
      "-Wconf:msg=unused&src=.*Routes\\.scala:s",
      "-Wconf:msg=unused&src=.*ReverseRoutes\\.scala:s",
      "-Wconf:msg=unused&src=.*JavaScriptReverseRoutes\\.scala:s",
      "-Wconf:msg=other-match-analysis:s",
      "-Wconf:msg=Flag.*repeatedly:s",
      "-Wconf:src=routes/.*:s"
    ),
      libraryDependencies ++= AppDependencies.compile ++ AppDependencies.test,
      retrieveManaged := true,
      Global / excludeLintKeys += update / evictionWarningOptions,
      update / evictionWarningOptions :=
        EvictionWarningOptions.default.withWarnScalaVersionEviction(false),
      resolvers ++= Seq(Resolver.jcenterRepo),
      // concatenate js
      Concat.groups := Seq(
          "javascripts/application.js" ->
            group(Seq(
                "javascripts/app.js"
            ))
      ),
      pipelineStages := Seq(digest),
  ).settings(scoverageSettings: _ *)

lazy val testSettings: Seq[Def.Setting[_]] = Seq(
    fork := true,
    unmanagedSourceDirectories += baseDirectory.value / "test-utils"
)

lazy val it = project
  .enablePlugins(PlayScala)
  .dependsOn(root % "test->test") // the "test->test" allows reusing test code and test dependencies
  .settings(
    libraryDependencies ++= AppDependencies.test,
    DefaultBuildSettings.itSettings()
  )
