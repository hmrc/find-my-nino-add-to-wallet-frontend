import sbt.*

object AppDependencies {

  private val mongoVersion = "1.1.0"
  private val playVersion = "play-28"
  private val bootstrapVersion = "8.1.0"

  val compile = Seq(
    play.sbt.PlayImport.ws,
    "uk.gov.hmrc"                   %% "play-conditional-form-mapping"         %  s"1.12.0-$playVersion",
    "uk.gov.hmrc.mongo"             %% s"hmrc-mongo-$playVersion"              %  mongoVersion,
    "org.apache.xmlgraphics"        % "fop"                                    %  "2.6",
    "org.typelevel"                 %% "cats-core"                             %  "2.8.0",
    "uk.gov.hmrc"                   %% s"sca-wrapper-$playVersion"             % "1.3.0",
    "com.google.auth"               % "google-auth-library-oauth2-http"        % "1.16.0",
    "com.auth0"                     % "java-jwt"                               % "4.4.0",
    "com.fasterxml.jackson.module"  %% "jackson-module-scala"                  % "2.14.2",
    "com.google.api-client"         % "google-api-client"                      %   "2.2.0"
  )

  val test = Seq(
    "org.scalatestplus" %% "mockito-4-6" % "3.2.15.0",
    "org.mockito" %% "mockito-scala" % "1.16.42",
    "org.scalatestplus" %% "scalacheck-1-17" % "3.2.15.0",
    "uk.gov.hmrc" %% s"bootstrap-test-$playVersion" % bootstrapVersion,
    "uk.gov.hmrc.mongo" %% s"hmrc-mongo-test-$playVersion" % mongoVersion,
    "com.vladsch.flexmark" % "flexmark-all" % "0.62.2"
  ).map(_ % "test, it")

  def apply(): Seq[ModuleID] = compile ++ test
}
