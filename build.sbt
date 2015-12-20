name := "spark-ml-adxl345-from-kinesis"

version := "1.0"

scalaVersion := "2.10.6"

libraryDependencies ++= Seq(
  "org.apache.spark" %% "spark-core"                  % "1.4.1" % "provided",
  "org.apache.spark" %% "spark-sql"                   % "1.4.1" % "provided",
  "org.apache.spark" %% "spark-mllib"                 % "1.4.1" % "provided",
  "org.apache.spark" %% "spark-streaming"             % "1.4.1" % "provided",
  "org.apache.spark" %% "spark-streaming-kinesis-asl" % "1.4.1",
  "org.json4s"       %% "json4s-native"               % "3.3.0"
)

assemblyMergeStrategy in assembly := {
  case PathList("javax", "servlet", xs @ _*)         => MergeStrategy.first
  case PathList(ps @ _*) if ps.last endsWith ".properties" => MergeStrategy.first
  case PathList(ps @ _*) if ps.last endsWith ".xml" => MergeStrategy.first
  case PathList(ps @ _*) if ps.last endsWith ".types" => MergeStrategy.first
  case PathList(ps @ _*) if ps.last endsWith ".class" => MergeStrategy.first
  case "application.conf"                            => MergeStrategy.concat
  case "unwanted.txt"                                => MergeStrategy.discard
  case x =>
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    oldStrategy(x)
}
