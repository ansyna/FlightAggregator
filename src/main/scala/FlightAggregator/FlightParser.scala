package FlightAggregator

import org.apache.spark._
import org.apache.spark.graphx._
import org.apache.log4j.{Level, Logger}

case class Flight(dofM: String, dofW: String, carrier: String, tailnum: String, flnum: Int, org_id: Long, origin: String, dest_id: Long, dest: String, crsdeptime: Double, deptime: Double, depdelaymins: Double, crsarrtime: Double, arrtime: Double, arrdelay: Double, crselapsedtime: Double, dist: Int)

object FlightParser {
  def main(args: Array[String]) {
    findShortDistBetweenFlights("DFW","JDK")
  }

  def findShortDistBetweenFlights(v1StrArg: String, v2StrArg: String) {
    val conf = new SparkConf().setAppName("Simple Application")
    val sc = new SparkContext(conf)
    val rootLogger = Logger.getRootLogger()
    rootLogger.setLevel(Level.ERROR)
    val textRDD = sc.textFile("/home/hanna/Downloads/flightDataSimpl.csv")

    val flightsRDD = textRDD.map(parseFlight).cache()


    val airports = flightsRDD.map(flight => (flight.org_id, flight.origin)).distinct

    // Defining a default vertex called nowhere
    val nowhere = "nowhere"

    val routes = flightsRDD.map(flight => ((flight.org_id, flight.dest_id), flight.dist)).distinct

    routes.cache

    // AirportID is numerical - Mapping airport ID to the 3-letter code
    val airportMap = airports.map { case ((org_id), name) => (org_id -> name) }.collect.toList.toMap
    //airportMap: scala.collection.immutable.Map[Long,String] = Map(13024 -> LMT, 10785 -> BTV, 14574 -> ROA, 14057 -> PDX, 13933 -> ORH, 11898 -> GFK, 14709 -> SCC, 15380 -> TVC,

    // Defining the routes as edges
    val edges = routes.map { case ((org_id, dest_id), distance) => Edge(org_id.toLong, dest_id.toLong, distance) }

    //Defining the Graph
    val graph = Graph(airports, edges)

    val v1: Long = 12478
    val v2: Long = 11298
    val v1Str = airportMap(v1)
    val v2Str = airportMap(v2)
    println(s"Flight from $v2Str (id $v2) to $v1Str (id $v1)")
    val result = dijkstraFlights(graph, v2)
      .vertices
      .filter({case(vId, _) => vId == v1})
      .map(x => x._2).map(x => x._2).map(x => x(1))
      .collect
    printList(result(0), airportMap)
  }


  def dijkstraFlights[VD](g:Graph[VD,Int], origin:VertexId) = {
    var g2 = g.mapVertices(
      (vid,vd) => (false, if (vid == origin) 0 else Double.MaxValue,
        List[VertexId]()))
    for (i <- 1L to g.vertices.count-1) {
      val currentVertexId =
        g2.vertices.filter(!_._2._1)
          .fold((0L,(false,Double.MaxValue,List[VertexId]())))((a,b) =>
            if (a._2._2 < b._2._2) a else b)
          ._1

      val newDistances = g2.aggregateMessages[(Double,List[VertexId])](
        ctx => if (ctx.srcId == currentVertexId)
          ctx.sendToDst((ctx.srcAttr._2 + ctx.attr,
            ctx.srcAttr._3 :+ ctx.srcId)),
        (a,b) => if (a._1 < b._1) a else b)
      g2 = g2.outerJoinVertices(newDistances)((vid, vd, newSum) => {
        val newSumVal =
          newSum.getOrElse((Double.MaxValue,List[VertexId]()))
        (vd._1 || vid == currentVertexId,
          math.min(vd._2, newSumVal._1),
          if (vd._2 < newSumVal._1) vd._3 else newSumVal._2)})
    }
    g.outerJoinVertices(g2.vertices)((vid, vd, dist) =>
      (vd, dist.getOrElse((false,Double.MaxValue,List[VertexId]()))
        .productIterator.toList.tail))
  }

  def printItem(a: Any, map: Map[Long,String] ) {
    a match {
      case l: Long =>
        println("Airport " + map(l) + " (id " + l + ")")
    }
  }

  def printList(a: Any, map: Map[Long,String] ) {
    a match {
      case l: List[_] =>
        println("Flight path:")
        l.foreach(printItem(_,map))
    }
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