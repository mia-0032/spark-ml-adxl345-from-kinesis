import com.amazonaws.services.kinesis.clientlibrary.lib.worker.InitialPositionInStream
import org.apache.log4j.{Logger,Level}
import org.apache.spark.mllib.linalg.{Vector,Vectors}
import org.apache.spark.mllib.regression.LabeledPoint
import org.apache.spark.rdd.RDD
import org.apache.spark.storage.StorageLevel
import org.apache.spark.streaming.{Seconds, Duration, StreamingContext}
import org.apache.spark.streaming.dstream.DStream
import org.apache.spark.streaming.kinesis._
import org.apache.spark.{SparkConf,SparkContext}
import org.apache.spark.mllib.tree.DecisionTree
import org.apache.spark.mllib.tree.model.DecisionTreeModel
import org.json4s._
import org.json4s.native.JsonMethods._

case class Acceleration(X: Double, Y: Double, Z: Double)

object App {
  def main(args: Array[String]): Unit = {
    val conf = new SparkConf().setAppName("SensorValueDecisionTree")
    val ssc = new StreamingContext(conf, Seconds(2))
    val sc = ssc.sparkContext
    Logger.getRootLogger.setLevel(Level.WARN)

    val standingDataPath = args(0)
    val sittingDataPath = args(1)
    val streamName = args(2)

    val standingData = loadTrainingData(standingDataPath, sc, "standing")
    val sittingData = loadTrainingData(sittingDataPath, sc, "sitting")
    val data = standingData.union(sittingData)

    val splits = data.randomSplit(Array(0.8, 0.2))
    val (trainingData, testData) = (splits(0), splits(1))

    val model = createDecisionTreeModel(trainingData)
    testMSE(testData, model)

    val dStream = createDStream(streamName, ssc)
    dStream.foreachRDD { rdd =>
      model.predict(rdd).foreach { p =>
        val s = p match {
          case 1.0 => "standing"
          case 0.0 => "sitting"
        }
        println(s)
      }
    }

    ssc.start()
    ssc.awaitTermination()
  }

  def loadTrainingData(filePath: String, sc: SparkContext, state: String): RDD[LabeledPoint] = {
    sc.textFile(filePath)
      .map { l =>
        implicit val formats = DefaultFormats
        parse(l).extract[Acceleration]
      }.map { a => LabeledPoint(
        state match {
          case "standing" => 1.0
          case "sitting" => 0.0
        },
        Vectors.dense(a.X, a.Y, a.Z)
      )}
  }

  def createDecisionTreeModel(trainingData: RDD[LabeledPoint]): DecisionTreeModel = {
    val categoricalFeaturesInfo = Map[Int, Int]()
    val impurity = "variance"
    val maxDepth = 5
    val maxBins = 32

    val model = DecisionTree.trainRegressor(
      trainingData, categoricalFeaturesInfo, impurity, maxDepth, maxBins
    )

    println("Learned regression tree model:\n" + model.toDebugString)
    model
  }

  def testMSE(testData: RDD[LabeledPoint], model: DecisionTreeModel): Unit = {
    val labelsAndPredictions = testData.map { point =>
      val prediction = model.predict(point.features)
      (point.label, prediction)
    }
    val testMSE = labelsAndPredictions.map{ case(v, p) => math.pow(v - p, 2)}.mean()
    println("Test Mean Squared Error = " + testMSE)
  }

  def createDStream(streamName: String, ssc: StreamingContext): DStream[Vector] = {
    KinesisUtils.createStream(
      ssc,
      "spark-ml-adxl345-from-kinesis",
      streamName,
      "kinesis.ap-northeast-1.amazonaws.com",
      "ap-northeast-1",
      InitialPositionInStream.TRIM_HORIZON,
      Seconds(2),
      StorageLevel.MEMORY_AND_DISK_2
    ).map(s => new String(s))
    .map { l =>
      implicit val formats = DefaultFormats
      println(l)
      parse(l).extract[Acceleration]
    }.map{ a =>
      Vectors.dense(a.X, a.Y, a.Z)
    }
  }
}
