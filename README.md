
# find-my-nino-add-to-wallet-frontend

### To run the service locally follow these steps:

**1.** Make sure you have MONGO db running on default port

**2.** Make sure you have service-manager installed on your local setup

**3.** Run the command below to start these services:
Sm2 --start AFFINITY_GROUP AUTH AUTH_LOGIN_STUB AUTHENTICATOR AUTH_DES_STUB AUTH_LOGIN_API BAS_PROXY BAS_STUBS CITIZEN_DETAILS DELEGATION IDENTITY_VERIFICATION USER_DETAILS PAY_AS_YOU_EARN_STUB

**4.** Please ensure backend service is running locally
Note you can git clone the repo and 'sbt run', or run it through IntelliJ or service manager

**5.** Please ensure frontend service is running locally
Note you can git clone the repo and 'sbt run', or run it through IntelliJ or service manager

**6.** On your browser Visit http://localhost:9949/auth-login-stub/gg-sign-in

**7.** In the Redirect URL textbox enter "http://localhost:9900/find-my-nino-add-to-wallet-frontend/store-my-nino"

**8.** Confidence Level: set to 200

**9.** In the NINO textbox enter "AA000003B"

**10.** In the Enrolments Section enter:
    a. In the Enrolment Key enter "HMRC-PT"
    b. In the Identifier Name enter "NINO"
    c. In the Identifier Value enter "AA000003B"

**11.** Click the green 'Submit' Button

###### you should now see the store-my-nino 


### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").
