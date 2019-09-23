package org.renaissance.apache.spark

import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.nio.file.Paths

import org.apache.commons.io.FileUtils
import org.apache.commons.io.IOUtils
import org.apache.spark.SparkContext
import org.apache.spark.ml.Pipeline
import org.apache.spark.ml.PipelineModel
import org.apache.spark.ml.PipelineStage
import org.apache.spark.ml.classification.DecisionTreeClassificationModel
import org.apache.spark.ml.classification.DecisionTreeClassifier
import org.apache.spark.ml.feature.StringIndexer
import org.apache.spark.ml.feature.VectorIndexer
import org.apache.spark.sql.DataFrame
import org.apache.spark.sql.SQLContext
import org.renaissance.Benchmark._
import org.renaissance.Config
import org.renaissance.License
import org.renaissance.RenaissanceBenchmark

@Name("dec-tree")
@Group("apache-spark")
@Summary("Runs the Random Forest algorithm from Spark MLlib.")
@Licenses(Array(License.APACHE2))
@Repetitions(40)
class DecTree extends RenaissanceBenchmark with SparkUtil {

  // TODO: Consolidate benchmark parameters across the suite.
  //  See: https://github.com/renaissance-benchmarks/renaissance/issues/27

  var numCopies = org.renaissance.BenchmarkInputs.getNumCopies

  val THREAD_COUNT = org.renaissance.BenchmarkInputs.getNumThreads

  // TODO: Unify handling of scratch directories throughout the suite.
  //  See: https://github.com/renaissance-benchmarks/renaissance/issues/13

  val decisionTreePath = Paths.get("target", "dec-tree")

  val outputPath = decisionTreePath.resolve("output")

  val inputFile = "/sample_libsvm_data.txt"

  val bigInputFile = decisionTreePath.resolve("bigfile.txt")

  var tempDirPath: Path = null

  var sc: SparkContext = null

  var training: DataFrame = null

  var pipeline: Pipeline = null

  var pipelineModel: PipelineModel = null

  var iteration: Int = 0

  def prepareAndLoadInput(decisionTreePath: Path, inputFile: String): DataFrame = {
    FileUtils.deleteDirectory(decisionTreePath.toFile)

    val text =
      IOUtils.toString(this.getClass.getResourceAsStream(inputFile), StandardCharsets.UTF_8)
    for (i <- 0 until numCopies) {
      FileUtils.write(bigInputFile.toFile, text, StandardCharsets.UTF_8, true)
    }

    val sqlContext = new SQLContext(sc)
    return sqlContext.read.format("libsvm").load(bigInputFile.toString)
  }

  def constructPipeline(): Pipeline = {
    val labelIndexer = new StringIndexer()
      .setInputCol("label")
      .setOutputCol("indexedLabel")
    val featureIndexer = new VectorIndexer()
      .setInputCol("features")
      .setOutputCol("indexedFeatures")
      .setMaxCategories(10)
    val dtc = new DecisionTreeClassifier()
      .setFeaturesCol("indexedFeatures")
      .setLabelCol("indexedLabel")
      .setMaxDepth(5)
      .setMaxBins(32)
      .setMinInstancesPerNode(1)
      .setMinInfoGain(0.0)
      .setCacheNodeIds(false)
      .setCheckpointInterval(10)
    return new Pipeline().setStages(
      Array[PipelineStage](
        labelIndexer,
        featureIndexer,
        dtc
      )
    )
  }

  override def setUpBeforeAll(c: Config): Unit = {
    tempDirPath = RenaissanceBenchmark.generateTempDir("dec_tree")
    sc = setUpSparkContext(tempDirPath, THREAD_COUNT)
    if (c.functionalTest) {
      numCopies = 5
    }
    training = prepareAndLoadInput(decisionTreePath, inputFile)
    pipeline = constructPipeline()
  }

  override def runIteration(c: Config): Unit = {
    pipelineModel = pipeline.fit(training)
    val treeModel =
      pipelineModel.stages.last.asInstanceOf[DecisionTreeClassificationModel]
    val thisIterOutputPath = outputPath.resolve(iteration.toString)
    FileUtils.write(
      thisIterOutputPath.toFile,
      treeModel.toDebugString,
      StandardCharsets.UTF_8,
      true
    )
    iteration += 1

  }

  override def tearDownAfterAll(c: Config): Unit = {
    tearDownSparkContext(sc)
    RenaissanceBenchmark.deleteTempDir(tempDirPath)
  }
}
