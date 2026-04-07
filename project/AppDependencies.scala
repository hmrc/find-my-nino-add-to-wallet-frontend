import sbt.*

object AppDependencies {

  private val mongoVersion     = "2.12.0"
  private val playVersion      = "play-30"
  private val scaWrapperVersion = "4.14.0"

  val compile: Seq[ModuleID] = Seq(
    play.sbt.PlayImport.ws,
    "uk.gov.hmrc"                  %% s"play-conditional-form-mapping-$playVersion" % "3.5.0",
    "uk.gov.hmrc.mongo"            %% s"hmrc-mongo-$playVersion"                    % mongoVersion,
    "org.apache.xmlgraphics"        % "fop"                                         % "2.11",
    "org.apache.xmlgraphics"        % "fop-pdf-images"                              % "2.11",
    "org.typelevel"                %% "cats-core"                                   % "2.13.0",
    "uk.gov.hmrc"                  %% s"sca-wrapper-$playVersion"                   % scaWrapperVersion,
    "com.google.auth"               % "google-auth-library-oauth2-http"             % "1.43.0",
    "com.auth0"                     % "java-jwt"                                    % "4.5.1",
    "com.fasterxml.jackson.module" %% "jackson-module-scala"                        % "2.21.2",
    "com.google.api-client"         % "google-api-client"                           % "2.9.0"
  )

  val test: Seq[ModuleID] = Seq(
    "org.scalatestplus"   %% "scalacheck-1-18"                 % "3.2.19.0",
    "uk.gov.hmrc"         %% s"sca-wrapper-test-$playVersion"  % scaWrapperVersion,
    "uk.gov.hmrc.mongo"   %% s"hmrc-mongo-test-$playVersion"   % mongoVersion,
    "com.vladsch.flexmark" % "flexmark-all"                    % "0.64.8"
  ).map(_ % "test")

  def apply(): Seq[ModuleID] = compile ++ test
}
