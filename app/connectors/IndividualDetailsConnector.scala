
package connectors


import com.google.inject.{Inject, Singleton}
import config.{AppConfig, FrontendAppConfig}
import models.individualDetails.CorrelationId
import play.api.Logging
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class IndividualDetailsConnector @Inject()(
                                            val httpClient: HttpClient,
                                            appConfig:  FrontendAppConfig) extends Logging {

  def getIndividualDetails(nino: String, resolveMerge: String, desHeaders: HeaderCarrier
                          )(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[HttpResponse] = {
    val url = s"${appConfig.individualDetailsServiceUrl}/individuals/details/NINO/${nino.take(8)}/$resolveMerge"
    httpClient.GET[HttpResponse](url)(implicitly, desHeaders, implicitly)
  }
}
