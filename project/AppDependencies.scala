import play.sbt.PlayImport.*
import sbt.*

object AppDependencies {

  private val bootstrapVersion = "9.5.0"
  private val hmrcMongoVersion = "2.3.0"

  val compile = Seq(
    "uk.gov.hmrc"       %% "bootstrap-backend-play-30"         % bootstrapVersion,
    "uk.gov.hmrc.mongo" %% "hmrc-mongo-work-item-repo-play-30" % hmrcMongoVersion,
    "org.typelevel"     %% "cats-core"                         % "2.3.0"
  )

  val test = Seq(
    "uk.gov.hmrc"       %% "bootstrap-test-play-30"  % bootstrapVersion % Test,
    "uk.gov.hmrc.mongo" %% "hmrc-mongo-test-play-30" % hmrcMongoVersion % Test,
    "org.mockito"       %% "mockito-scala"           % "1.17.31"        % Test
  )

  val it: Seq[Nothing] = Seq.empty
}
