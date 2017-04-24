package FlightAggregator

import PathFind.ShortPathLibrary
import org.apache.spark._
import org.apache.spark.graphx._
import org.apache.log4j.{Level, Logger}

  object ShortestPathFinder {
    def main(args: Array[String]) {
      shortestPath()
    }

    def shortestPath() {
      val conf = new SparkConf().setAppName("Simple Application")
      val sc = new SparkContext(conf)
      val rootLogger = Logger.getRootLogger()
      rootLogger.setLevel(Level.ERROR)
      val textRDD = sc.textFile("resources/flightData.csv")
      print(textRDD.first())
      val flightsRDD = textRDD.map(parseFlight).cache()
      print(flightsRDD.first())

      val airports = flightsRDD.map(flight => (flight.org_id, flight.origin)).distinct
      print(airports.first())
      // Defining a default vertex called nowhere
      val nowhere = "nowhere"

      val routes = flightsRDD.map(flight => ((flight.org_id, flight.dest_id), flight.dist)).distinct
      print(routes.first())
      routes.cache

      // Defining the routes as edges
      val edges = routes.map { case ((org_id, dest_id), distance) => Edge(org_id.toLong, dest_id.toLong, distance) }

      //Defining the Graph
      val graph = Graph(airports, edges)
      graph.vertices.foreach(println)
      val v1 = 12478
      val v2 = 12173
      val result = ShortPathLibrary.run(graph, Seq(v2))
      //result.vertices.foreach(println)

      // val result = ShortestPaths.run(graph, Seq(v2))
      //result.vertices.foreach(println)

      val shortestPath = result               // result is a graph
        .vertices                             // we get the vertices RDD
        .filter({case(vId, _) => vId == v1})  // we filter to get only the shortest path from v1
        .first                                // there's only one value
        ._2                                   // the result is a tuple (v1, Map)
        .get(v2)                              // we get its shortest path to v2 as an Option object

      shortestPath.foreach(println) // will be 2 for this case as (12478,Map(12173 -> 2))
    }

    def parseFlight(str: String): Flight = {
      val line = str.split(",")
      val line4 = if (line(4).isEmpty) 0 else line(4).toInt
      val line5 = if (line(5).isEmpty) 0 else line(5).toLong
      val line7 = if (line(7).isEmpty) 0 else line(7).toLong
      val line9 = if (line(9).isEmpty) 0 else line(9).toDouble
      val line10 = if (line(10).isEmpty) 0 else line(10).toDouble
      val line11 = if (line(11).isEmpty) 0 else line(11).toDouble
      val line12 = if (line(12).isEmpty) 0 else line(12).toDouble
      val line13 = if (line(13).isEmpty) 0 else line(13).toDouble
      val line14 = if (line(14).isEmpty) 0 else line(14).toDouble
      val line15 = if (line(15).isEmpty) 0 else line(15).toDouble
      val line16 = if (line(16).isEmpty) 0 else line(16).toInt

      Flight(line(0), line(1), line(2), line(3), line4, line5,
        line(6), line7, line(8), line9, line10,
        line11 , line12, line13, line14, line15, line16)
    }
  }

