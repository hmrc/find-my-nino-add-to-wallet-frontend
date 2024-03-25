
package services

import connectors.IndividualDetailsConnector
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
class IndividualDetailsService @Inject()(
                                          individualDetailsConnector: IndividualDetailsConnector
                                        )(implicit val ec:ExecutionContext,
                                          headerCarrier: HeaderCarrier) {
  def getIndividualDetails(nino:String, resolveMerge:String): Future[HttpResponse] = {
    individualDetailsConnector.getIndividualDetails(nino, resolveMerge)
  }
}
