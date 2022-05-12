import Dependencies.Libraries._
import sbt.ThisBuild

Global / onChangedBuildSource := ReloadOnSourceChanges

lazy val scala3 = "3.1.2"
val PrimaryJava = JavaSpec.temurin("8")
val LTSJava = JavaSpec.temurin("17")

inThisBuild(
  List(
    tlBaseVersion := "0.0",
    scalaVersion := scala3,
    fork := true,
    Test / fork := false,
    organization := "dev.hnaderi",
    organizationName := "Hossein Naderi",
    startYear := Some(2021),
    tlSonatypeUseLegacyHost := false,
    tlCiReleaseBranches := Seq(), // No snapshots while not ready!
    tlSitePublishBranch := Some("main"),
    githubWorkflowJavaVersions := Seq(PrimaryJava, LTSJava),
    licenses := Seq(License.Apache2),
    developers := List(
      Developer(
        id = "hnaderi",
        name = "Hossein Naderi",
        email = "mail@hnaderi.dev",
        url = url("https://hnaderi.dev")
      )
    )
  )
)

def module(module: String): Project = {
  val id = s"lepus-$module"
  Project(id, file(s"modules/$module"))
    .settings(
      libraryDependencies ++= (munit)
    )
}

val protocol = module("protocol")

val protocolGen = module("protocol-gen")
  .settings(
    libraryDependencies ++= fs2IO ++ fs2scodec ++ scalaXml,
    Compile / run / baseDirectory := file(".")
  )

val core = module("core")
  .settings(libraryDependencies ++= cats ++ catsEffect ++ fs2)
  .dependsOn(protocol)

val data = module("data")
  .dependsOn(core)

val client = module("client")
  .dependsOn(core)
  .dependsOn(protocol)
  .settings(
    libraryDependencies ++= rabbit ++ scodec ++ fs2IO ++ fs2scodec ++ scalaXml
  )

val std = module("std")
  .dependsOn(core)
  .dependsOn(data)

val dataCirce = module("data-circe")
  .dependsOn(data)
  .settings(libraryDependencies ++= circe)

val docs = project
  .in(file("site"))
  .enablePlugins(TypelevelSitePlugin)
  .dependsOn(core)
  .settings(
    tlSiteRelatedProjects := Seq(
      TypelevelProject.Cats,
      TypelevelProject.CatsEffect,
      TypelevelProject.Fs2
    )
  )

val root = project
  .in(file("."))
  .settings(
    name := "lepus"
  )
  .aggregate(
    protocol,
    protocolGen,
    core,
    client,
    std,
    data,
    dataCirce,
    docs
  )

def addAlias(name: String)(tasks: String*) =
  addCommandAlias(name, tasks.mkString(" ;"))

addAlias("commit")(
  "clean",
  "scalafmtCheckAll",
  "scalafmtSbtCheck",
  "headerCheckAll",
  "githubWorkflowCheck",
  "compile",
  "test"
)
addAlias("precommit")(
  "scalafmtAll",
  "scalafmtSbt",
  "headerCreateAll",
  "githubWorkflowGenerate",
  "compile",
  "test"
)
