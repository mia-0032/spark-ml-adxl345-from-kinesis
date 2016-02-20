# spark-ml-adxl345-from-kinesis
Spark ML using ADXL345 acceleration sensor values from Kinesis

## Setup

```shell
$ brew install scala
$ brew install apache-spark
```

## Build

```shell
$ sbt assembly
```

## Execute

```shell
$ spark-submit --class App --master local[2] \
  target/scala-2.10/spark-ml-adxl345-from-kinesis-assembly-1.0.jar \
  data/standing.json data/sitting.json <your_stream_name>
```
