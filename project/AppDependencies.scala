import AppDependencies.playVersion
import sbt._

object AppDependencies {
  import play.core.PlayVersion

  private val playVersion = "play-28"
  private val mongoVersion = "1.1.0"

  val compile = Seq(
    play.sbt.PlayImport.ws,
    "uk.gov.hmrc"             %% "play-conditional-form-mapping"        %  s"1.12.0-$playVersion",
    "uk.gov.hmrc.mongo"       %% "hmrc-mongo-play-28"                   %  mongoVersion,
    "org.apache.xmlgraphics"  % "fop"                                   %  "2.6",
    "org.typelevel"           %% "cats-core"                            %  "2.8.0",
    "uk.gov.hmrc"             %% "sca-wrapper"                          % "1.0.31",
  )

  val test = Seq(
    "org.scalatest"           %% "scalatest"                     % "3.2.10",
    "uk.gov.hmrc"             %% s"bootstrap-test-$playVersion"  % "7.13.0",
    "org.scalatestplus"       %% "scalacheck-1-15"               % "3.2.10.0",
    "org.scalatestplus"       %% "mockito-3-4"                   % "3.2.10.0",
    "org.scalatestplus.play"  %% "scalatestplus-play"            % "5.1.0",
    "org.pegdown"             %  "pegdown"                       % "1.6.0",
    "org.jsoup"               %  "jsoup"                         % "1.14.3",
    "com.typesafe.play"       %% "play-test"                     % PlayVersion.current,
    "org.mockito"             %% "mockito-scala"                 % "1.16.42",
    "org.scalacheck"          %% "scalacheck"                    % "1.15.4",
    "uk.gov.hmrc.mongo"       %% "hmrc-mongo-test-play-28"       % mongoVersion,
    "com.vladsch.flexmark"    %  "flexmark-all"                  % "0.62.2",
    "io.github.wolfendale"    %%  "scalacheck-gen-regexp"        % "1.1.0",
    "com.github.tomakehurst"  % "wiremock-jre8"                  % "2.26.3"
  ).map(_ % "test, it")

  def apply(): Seq[ModuleID] = compile ++ test
}
