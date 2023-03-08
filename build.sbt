import play.sbt.routes.RoutesKeys
import sbt.Def
import scoverage.ScoverageKeys
import uk.gov.hmrc.versioning.SbtGitVersioning.autoImport.majorVersion

lazy val appName: String = "find-my-nino-add-to-wallet-frontend"

lazy val scoverageSettings = {
  import scoverage.ScoverageKeys
  Seq(
    // Semicolon-separated list of regexs matching classes to exclude
    ScoverageKeys.coverageExcludedPackages := "<empty>;Reverse.*;models/.data/..*;view.*;models.*;.*(AuthService|BuildInfo|Routes).*",
    ScoverageKeys.coverageMinimumStmtTotal := 75,
    ScoverageKeys.coverageFailOnMinimum := true,
    ScoverageKeys.coverageHighlighting := true,
  )
}

addCommandAlias("report", ";clean; coverage; test; it:test; coverageReport")

lazy val root = (project in file("."))
  .enablePlugins(PlayScala, SbtAutoBuildPlugin, SbtDistributablesPlugin)
  .disablePlugins(JUnitXmlReportPlugin) //Required to prevent https://github.com/scalatest/scalatest/issues/1427
  .settings(SbtDistributablesPlugin.publishingSettings: _*)
  .settings(inConfig(Test)(testSettings): _*)
  .configs(IntegrationTest)
  .settings(inConfig(IntegrationTest)(itSettings): _*)
  .settings(majorVersion := 0)
  .settings(useSuperShell in ThisBuild := false)
  .settings(
      scalaVersion := "2.13.8",
      name := appName,
      RoutesKeys.routesImport ++= Seq(
          "models._",
          "uk.gov.hmrc.play.bootstrap.binders.RedirectUrl"
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
          "viewmodels.govuk.all._"
      ),
      PlayKeys.playDefaultPort := 14006,
      scalacOptions ++= Seq(
          "-feature",
          "-language:postfixOps",
          "-rootdir",
          baseDirectory.value.getCanonicalPath,
          "-Wconf:cat=deprecation:ws,cat=feature:ws,cat=optimizer:ws,src=target/.*:s"
      ),
      libraryDependencies ++= AppDependencies(),
      retrieveManaged := true,
      evictionWarningOptions in update :=
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
  ).settings(scoverageSettings: _*)

lazy val testSettings: Seq[Def.Setting[_]] = Seq(
    fork := true,
    unmanagedSourceDirectories += baseDirectory.value / "test-utils"
)

lazy val itSettings = Defaults.itSettings ++ Seq(
    unmanagedSourceDirectories := Seq(
        baseDirectory.value / "it",
        baseDirectory.value / "test-utils"
    ),
    unmanagedResourceDirectories := Seq(
        baseDirectory.value / "it" / "resources"
    ),
    parallelExecution := false,
    fork := true
)
