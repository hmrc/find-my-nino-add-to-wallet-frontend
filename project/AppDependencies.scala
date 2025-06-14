import sbt._

object AppDependencies {

  private val mongoVersion     = "2.6.0"
  private val playVersion      = "play-30"
  private val bootstrapVersion = "9.12.0"

  val compile: Seq[ModuleID] = Seq(
    play.sbt.PlayImport.ws,
    "uk.gov.hmrc"                  %% s"play-conditional-form-mapping-$playVersion" % "3.3.0",
    "uk.gov.hmrc.mongo"            %% s"hmrc-mongo-$playVersion"                    % mongoVersion,
    "org.apache.xmlgraphics"        % "fop"                                         % "2.11",
    "org.typelevel"                %% "cats-core"                                   % "2.13.0",
    "uk.gov.hmrc"                  %% s"sca-wrapper-$playVersion"                   % "2.10.0",
    "com.google.auth"               % "google-auth-library-oauth2-http"             % "1.33.1",
    "com.auth0"                     % "java-jwt"                                    % "4.5.0",
    "com.fasterxml.jackson.module" %% "jackson-module-scala"                        % "2.18.3",
    "com.google.api-client"         % "google-api-client"                           % "2.7.2"
  )

  val test: Seq[ModuleID] = Seq(
    "org.scalatestplus"   %% "mockito-5-10"                  % "3.2.18.0",
    "org.scalatestplus"   %% "scalacheck-1-17"               % "3.2.18.0",
    "uk.gov.hmrc"         %% s"bootstrap-test-$playVersion"  % bootstrapVersion,
    "uk.gov.hmrc.mongo"   %% s"hmrc-mongo-test-$playVersion" % mongoVersion,
    "com.vladsch.flexmark" % "flexmark-all"                  % "0.64.8"
  ).map(_ % "test")

  def apply(): Seq[ModuleID] = compile ++ test
}
