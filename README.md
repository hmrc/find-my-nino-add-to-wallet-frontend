
# save-your-national-insurance-number


## About
This is the frontend repository for the 'Save your National Insurance number' service.
It allows authenticated user with PTA enrollment to view, download or print their national insurance number 
letter as a PDF, or save the number to a digital wallet card.

The service consists of only 4 pages allowing you to:
- Generate and save your National Insurance number letter in PDF or HTML
- Add your National Insurance number to your Google Wallet and Apple Wallet

## How to run locally
- Make sure you have sm2 (service manager 2 installed and workspace directory configured)
- Update service manager config: cd $WORKSPACE/service-manager-config && git pull
- Start the services: sm2 --start FMN_ALL
- Service should be now available at http://localhost:14006/save-your-national-insurance-number
- In the browser use the url above. if you are not already logged in, you will be redirected to the http://localhost:9949/auth-login-stub/gg-sign-in?continue=http%3A%2F%2Flocalhost%3A14006%2Fsave-your-national-insurance-number&origin=find-my-nino-add-to-wallet-frontend, which allows you to create a auth session for testing with enrollments and correct confidence levels etc.
- On the auth page enter Confidence Level: 200
- Enter AA000003B as test Nino
- Enter these values for Enrollment on the page:
- Enrollment Key: HMRC-PT, Identifier Name: NINO, Identifier Value: AA000003B
- Click the green submit button
- You should be redirected to: http://localhost:14006/save-your-national-insurance-number


## License
This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").
