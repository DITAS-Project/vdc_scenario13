import java.util.logging.Logger

import com.ditas.ideko.DalMessageProperties.DalMessageProperties
import com.ditas.ideko.QueryInfluxDBRequest.{QueryInfluxDBGrpc, QueryInfluxDBReply, QueryInfluxDBRequest}
import io.grpc._

object IdekoDALInfluxdbServerTest {


  private val LOGGER = Logger.getLogger(getClass.getName)

  def main(args: Array[String]): Unit = {
    val host = "localhost"
    val port = 50052
    val purpose = "maintenance"
    val query = "SELECT time, machine, \"value\" FROM \"{{db}}\".\"{{policy}}\".\"I_CMX_LQLS26_AW8HY7\" ORDER BY time DESC LIMIT 5"
    val machineId = "CMX_LQLS26"


    val channel =
      ManagedChannelBuilder
        .forAddress(host, port)
        .usePlaintext(true)
        .build()

    val blockingStub = QueryInfluxDBGrpc.blockingStub(channel)
    val asyncStub = QueryInfluxDBGrpc.stub(channel)

    val authorization = "Bearer eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJhdXl0YzJUX2MtN1k5SFEtVEI1UUJNc2tQdlhDWXpYU2ZKNnJPaUJLdi1FIn0.eyJqdGkiOiI4NzhiZjcwMS1kOWIwLTRiNGUtYmExYy1lM2E5M2FhYzg3NjgiLCJleHAiOjE1NTcwOTc5NjUsIm5iZiI6MCwiaWF0IjoxNTU3MDYxOTY1LCJpc3MiOiJodHRwczovLzE1My45Mi4zMC41Njo1ODA4MC9hdXRoL3JlYWxtcy8yODgiLCJhdWQiOiJhY2NvdW50Iiwic3ViIjoiZjYxYTgwNDEtYWVhNy00YTlhLTg5ODUtYzllMmJhODhlNmU3IiwidHlwIjoiQmVhcmVyIiwiYXpwIjoidmRjX2NsaWVudCIsImF1dGhfdGltZSI6MCwic2Vzc2lvbl9zdGF0ZSI6ImVmOGM5Nzg1LWE3ZjYtNGFkNC05NTQzLWZlNmQyZDJjNDg5ZiIsImFjciI6IjEiLCJhbGxvd2VkLW9yaWdpbnMiOlsiKiJdLCJyZWFsbV9hY2Nlc3MiOnsicm9sZXMiOlsiZGVtby1yb2xlIiwib2ZmbGluZV9hY2Nlc3MiLCJ1bWFfYXV0aG9yaXphdGlvbiJdfSwicmVzb3VyY2VfYWNjZXNzIjp7ImFjY291bnQiOnsicm9sZXMiOlsibWFuYWdlLWFjY291bnQiLCJtYW5hZ2UtYWNjb3VudC1saW5rcyIsInZpZXctcHJvZmlsZSJdfX0sInNjb3BlIjoicHJvZmlsZSBlbWFpbCIsImVtYWlsX3ZlcmlmaWVkIjpmYWxzZSwicHJlZmVycmVkX3VzZXJuYW1lIjoiZGVtb3VzZXIifQ.FhemkPDzAPeDHYCC23lmey5OIiN9BnJR8l_wxF5Lo01KUzKIor8WddbZDI6ZufnFRl822hvPxirtTLzooA6exq79-_80mNhPRoqc-Eq6ny7xYCMhpV5WkOP6QzwSd1-iiauF85pQ1c2EzKeNO1sWtS9d00nHaauWXA3CMZPEoAuMkhqr-eYIjfA2CmuAEv4S2p6itcO5oHWMKO4iCSKUIX3b3OoVNDytFwfzFRYS9e4Qe7QE2zufGY7-m_iLDWJt_8-J_SUsyz6-Pb7dklJ15vD5QOORhfYJdDb4Yzw1LbnT6pCtt2bHDplY-98MakvGuh0TEnwdeKzY3pe0iqWm6w"
    val dalMessageProperties: DalMessageProperties =
      new DalMessageProperties (purpose = purpose, authorization = authorization)

    val request: QueryInfluxDBRequest = new QueryInfluxDBRequest(query, machineId, Option(dalMessageProperties))

    try {
      val response: QueryInfluxDBReply = blockingStub.query(request)
      print(s"Response: $response")
    } catch {
      case e: Exception => print(e.getMessage)
    }

  }





}