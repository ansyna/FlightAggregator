package FlightAggregator

import org.apache.spark._
import org.apache.spark.graphx._

object FlightAnalytics {
  def main(args: Array[String]) {
    fligthAnalysis()
  }

  def fligthAnalysis() {
    val conf = new SparkConf().setAppName("Simple Application")
    val sc = new SparkContext(conf)
    val textRDD = sc.textFile("/home/hanna/Downloads/flightDataSimple.csv")
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

    // AirportID is numerical - Mapping airport ID to the 3-letter code
    val airportMap = airports.map { case ((org_id), name) => (org_id -> name) }.collect.toList.toMap

    //airportMap: scala.collection.immutable.Map[Long,String] = Map(13024 -> LMT, 10785 -> BTV, 14574 -> ROA, 14057 -> PDX, 13933 -> ORH, 11898 -> GFK, 14709 -> SCC, 15380 -> TVC,

    // Defining the routes as edges
    val edges = routes.map { case ((org_id, dest_id), distance) => Edge(org_id.toLong, dest_id.toLong, distance) }

    //Defining the Graph
    val graph = Graph(airports, edges, nowhere)
    print(graph.vertices.first())

    // LNumber of airports
    val numairports = graph.numVertices

    graph.vertices.take(2)

    graph.edges.take(2)

    graph.edges.filter { case (Edge(org_id, dest_id, distance)) => distance > 1000 }.take(3)
    // res9: Array[org.apache.spark.graphx.Edge[Int]] = Array(Edge(10140,10397,1269), Edge(10140,10821,1670), Edge(10140,12264,1628))

    // Number of routes
    val numroutes = graph.numEdges

    // The EdgeTriplet class extends the Edge class by adding the srcAttr and dstAttr members which contain the source and destination properties respectively.
    graph.triplets.take(3).foreach(println)

    // Define a reduce operation to compute the highest degree vertex
    def max(a: (VertexId, Int), b: (VertexId, Int)): (VertexId, Int) = {
      if (a._2 > b._2) a else b
    }

    // Compute the max degrees
    val maxInDegree: (VertexId, Int) = graph.inDegrees.reduce(max)
    // maxInDegree: (org.apache.spark.graphx.VertexId, Int) = (10397,152)
    val maxOutDegree: (VertexId, Int) = graph.outDegrees.reduce(max)
    // maxOutDegree: (org.apache.spark.graphx.VertexId, Int) = (10397,153)
    val maxDegrees: (VertexId, Int) = graph.degrees.reduce(max)
    // maxDegrees: (org.apache.spark.graphx.VertexId, Int) = (10397,305)

    // we can compute the in-degree of each vertex (defined in GraphOps) by the following:
    // which airport has the most incoming flights?
    graph.inDegrees.collect.sortWith(_._2 > _._2).map(x => (airportMap(x._1), x._2))
    //res46: Array[(String, Int)] = Array((ATL,152), (ORD,145), (DFW,143), (DEN,132), (IAH,107), (MSP,96), (LAX,82), (EWR,82), (DTW,81), (SLC,80),
    graph.outDegrees.join(airports).sortBy(_._2._1, ascending = false).take(1)
    val maxout = graph.outDegrees.join(airports).sortBy(_._2._1, ascending = false).take(3)
    //res46: Array[(String, Int)] = Array((ATL,152), (ORD,145), (DFW,143), (DEN,132), (IAH,107), (MSP,96), (LAX,82), (EWR,82), (DTW,81), (SLC,80),
    val maxIncoming = graph.inDegrees.collect.sortWith(_._2 > _._2).map(x => (airportMap(x._1), x._2)).take(3)
    maxIncoming.foreach(println)


    maxout.foreach(println)

    val maxOutgoing = graph.outDegrees.collect.sortWith(_._2 > _._2).map(x => (airportMap(x._1), x._2)).take(3)
    maxOutgoing.foreach(println)

    // What are the top 10 flights from airport to airport?
    graph.triplets.sortBy(_.attr, ascending = false).map(triplet =>
      "There were " + triplet.attr.toString + " flights from " + triplet.srcAttr + " to " + triplet.dstAttr + ".").take(10)

    val sourceId: VertexId = 13024
    // 50 + distance / 20
    graph.edges.filter { case (Edge(org_id, dest_id, distance)) => distance > 1000 }.take(3)

    val gg = graph.mapEdges(e => 50.toDouble + e.attr.toDouble / 20)
    val initialGraph = gg.mapVertices((id, _) => if (id == sourceId) 0.0 else Double.PositiveInfinity)
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
