import play.core.PlayVersion
import play.sbt.PlayImport._
import sbt.Keys.libraryDependencies
import sbt._

object AppDependencies {

  val compile = Seq(
    "uk.gov.hmrc"             %% "bootstrap-frontend-play-28" % "7.3.0",
    "uk.gov.hmrc"             %% "play-frontend-hmrc"         % "3.24.0-play-28"
  )

  val test = Seq(
    "uk.gov.hmrc"             %% "bootstrap-test-play-28"     % "7.3.0"             % "test, it",
    
    "org.jsoup"               %  "jsoup"                      % "1.13.1"            % Test,
  )
}
