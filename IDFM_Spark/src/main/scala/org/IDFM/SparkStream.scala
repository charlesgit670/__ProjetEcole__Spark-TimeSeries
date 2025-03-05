package org.IDFM
import org.apache.spark.sql.functions.current_timestamp
import org.apache.spark.sql.functions._
import org.apache.spark.sql.streaming.Trigger
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.types.{DateType, IntegerType, StringType, StructField, StructType, LongType, TimestampType}
import org.apache.spark.sql.functions.approx_count_distinct
import org.apache.spark.sql.expressions.Window

object SparkStream {

  def main(args: Array[String]): Unit = {

    val spark: SparkSession = SparkSession.builder()
      .master("local[3]")
      .appName("SparkStream")
      .config("spark.sql.shuffle.partitions", "1")
      .config("spark.sql.adaptive.enabled", false)
      .getOrCreate()


    spark.sparkContext.setLogLevel("ERROR")

    val schema = StructType(
      List(
        StructField("JOUR", StringType, true),
        StructField("CODE_STIF_TRNS", IntegerType, true),
        StructField("CODE_STIF_RES", IntegerType, true),
        StructField("CODE_STIF_ARRET", IntegerType, true),
        StructField("LIBELLE_ARRET", StringType, true),
        StructField("ID_REFA_LDA", IntegerType, true),
        StructField("CATEGORIE_TITRE", StringType, true),
        StructField("NB_VALD", LongType, true),
        StructField("batch", IntegerType, true)
      )
    )

    val df = spark.readStream
      .schema(schema)
      .option("header", false)
      .option("sep", ",")
      .csv("./data/csv")
    df.printSchema()

    val dfTime = df.withColumn("date_timestamp", unix_timestamp(col("JOUR"), "yyyy-MM-dd").cast(TimestampType))

    println("Streaming DataFrame : " + df.isStreaming)

    val groupByLibelleDf = dfTime
      .withWatermark("date_timestamp", "1 minute")
      .groupBy(window(col("date_timestamp"), "1 minute", "1 minute"), col("LIBELLE_ARRET"))
      .agg(sum("NB_VALD") as "totalValid")
      .select(
        to_date(col("window.start")).alias("JOUR"),
        col("LIBELLE_ARRET"),
        col("totalValid")
      )

    val groupByCategTitreDf = dfTime
      .withWatermark("date_timestamp", "1 minute")
      .groupBy(window(col("date_timestamp"), "1 minute", "1 minute"), col("CATEGORIE_TITRE"))
      .agg(sum("NB_VALD") as "totalValid")
      .select(
        to_date(col("window.start")).alias("JOUR"),
        col("CATEGORIE_TITRE"),
        col("totalValid")
      )

    val groupByJourDf = dfTime
      .withWatermark("date_timestamp", "1 minute")
      .groupBy(window(col("date_timestamp"), "1 minute", "1 minute"))
      .agg(sum("NB_VALD") as "totalValid")
      .select(
        to_date(col("window.start"), "yyyy-MM-dd").alias("JOUR"),
        col("totalValid")
      )

    groupByLibelleDf.writeStream
      .format("csv")
      .outputMode("append")
      .option("path", "./data/output")
      .option("checkpointLocation", "./tmp")
      .option("header", true)
      .start()

    groupByCategTitreDf.writeStream
      .format("csv")
      .outputMode("append")
      .option("path", "./data/output2")
      .option("checkpointLocation", "./tmp2")
      .option("header", true)
      .start()

    groupByJourDf.writeStream
      .format("csv")
      .outputMode("append")
      .option("path", "./data/output3")
      .option("checkpointLocation", "./tmp3")
      .option("header", true)
      .start()

    spark.streams.awaitAnyTermination()
  }
}