# save-your-national-insurance-number


## About
This is the frontend repository for the 'Save your National Insurance number' service.
It allows authenticated user with PTA enrollment to view, download or print their national insurance number
letter as a PDF, or save the number to a digital wallet card.

The service consists of only 4 pages allowing you to:
- Generate and save your National Insurance number letter in PDF or HTML
- Add your National Insurance number to your Google Wallet and Apple Wallet

## Prerequisites:

### To use Apple Wallet functionality:

You would require the below certs and keys to run all the functionalities of the service:

- **A public certificate from Apple (WWDRCA)**: it can be downloaded from the web page https://www.apple.com/certificateauthority/. If you search for “Worldwide Developer Relations" in that page you will see the certificates with different dates. Any of them will work.

- **A private certificate** created via Apple Developer Account. It is project specific. This certificate should be password protected.

- **A password** for the private certificate.


These then need to be added to be added to env variables, local commands for testing and adding these to env variables could look like this on a macbook:

- export PRIVATE_CERTIFICATE_PASSWORD="add your password here"
- export PRIVATE_CERTIFICATE="add your base64 encoded private certificate"
- export PUBLIC_CERTIFICATE="add your base64 encoded public certificate"


### To use Google Wallet functionality:

You would need a **Google Cloud service account key**, you can potentially create a local one for testing using the steps below, this process can change in future depending on Google's directions and policies or UI changes.


To create a Google Cloud service account key for testing:

1. Log in to a Google Account:
    - Use a Google account (preferably a personal account you have access to).
      It's recommended to use Chrome for the best compatibility.

2. Visit the Google Cloud Console:
    - Go to https://console.cloud.google.com.

3. Create a New Cloud Project (if you don’t have one):
    - Click on the project dropdown at the top of the page.
    - Select "New Project".
    - Fill in the required information (Project name, etc.) and click "Create".
    - If you already have an existing project, select it from the dropdown.

4. Navigate to the IAM & Admin Panel:
    - In the left-hand navigation menu, go to "IAM & Admin".

5. Navigate to the Service Accounts Panel:
    - In the IAM & Admin menu, select "Service Accounts".

6. Create a New Service Account (if needed):
    - Click on the "Create Service Account" button at the top of the page.
    - Fill in the required information (Service account name, ID, description).
    - Click "Create and Continue".
    - Optionally, grant roles to your service account, then click "Continue".
    - Click "Done" to finish creating the service account.

7. Generate a New Key for the Service Account:
    - Find the service account you created in the list.
    - Click on the email of the service account to open its details.
    - Go to the "Keys" tab.
    - Click "Add Key" and select "Create new key".
    - Choose the key type (JSON is recommended) and click "Create".
    - A JSON file containing the key will be downloaded to your computer.


8. Base64 encode the JSON file and use it as env_variable with the key: “GOOGLE_PASS_KEY”, for local testing you can add a export command in your shell init file, for example .zshrc or .bashrc, the export command should look like this :
   export GOOGLE_PASS_KEY=”<base64 encoded json>”


## How to run service locally

- Make sure you have service manager 2 installed and workspace directory configured
- Make sure you are running an instance of MongoDB at the default port.
- Update service manager config: cd $WORKSPACE/service-manager-config && git pull
- Start the services: sm2 --start FMN_ALL
- Service should be now available at http://localhost:14006/save-your-national-insurance-number
- In the browser use the url above. if you are not already logged in, you will be redirected to the http://localhost:9949/auth-login-stub/gg-sign-in?continue=http%3A%2F%2Flocalhost%3A14006%2Fsave-your-national-insurance-number&origin=find-my-nino-add-to-wallet-frontend, which allows you to create a auth session for testing with enrollments and correct confidence levels etc.
- On the auth page enter Confidence Level: 200
- Enter a test National Insurance Number (this should be available to you already as test data to be used)
- Enter these values for Enrollment on the page:
- Enrollment Key: HMRC-PT, Identifier Name: NINO, Identifier Value: test National Insurance Number
- Click the green submit button
- You should be redirected to: http://localhost:14006/save-your-national-insurance-number

## License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").
