import java.time.LocalDate
import java.time.format.DateTimeFormatter
import org.apache.spark.sql.{SparkSession}
import java.util.concurrent.TimeUnit
import java.io.File



import org.apache.spark.sql.functions._

object script {
    val DATE_FORMAT = "dd-MM-yyyy"

    println("First SparkContext:")

    def main(args: Array[String]) {

        val outputDir = "../data/csv"
        val delay = if (args.length > 0) args(0).toInt else 20
        val batch = if (args.length > 1) args(1).toInt else 7

        println("Delay value: " + delay)
        println("Batch value: " + batch)

        val spark = SparkSession.builder
          .appName("script")
          .master("local[*]")
          .config("spark.sql.shuffle.partitions", "1")
          .getOrCreate()


        import spark.implicits._

        // Assuming all csv files are in the same directory
        val csvDir = "./data/firstCSV/"


        // Loading all csv files
        val df = spark.read.format("csv").option("header", "true").option("delimiter", ";").load(csvDir)

        val dfWithDate = df.withColumn(
            "JOUR",
            to_date(unix_timestamp($"JOUR", "dd/MM/yyyy").cast("timestamp"))
        )

        val minJour = dfWithDate.agg(min("JOUR")).first()(0)


        // add 'batch' column that represents batch number for each date
        val dfWithBatch = dfWithDate.withColumn(
            "batch",
            (datediff($"JOUR", lit(minJour)) / batch).cast("integer") // casting to integer type
        )

        dfWithBatch.show()

        // get max batch number
        val maxBatch = dfWithBatch.agg(max("batch")).first().getAs[Int](0)

        println(maxBatch)

        println("Max batch: " + maxBatch)

        for (i <- 0 to maxBatch) {
            // select rows for the current batch
            val batchDf = dfWithBatch.filter($"batch" === i)

            batchDf.show()

            // write the batch to a new csv file
            val tempOutputFile = s"$outputDir/temp_batch.csv"
            batchDf.write
              .option("header", false)
              .mode("append")
              .csv("./data/csv")
            // delay
            TimeUnit.SECONDS.sleep(delay)
        }

        spark.stop()
    }


}
