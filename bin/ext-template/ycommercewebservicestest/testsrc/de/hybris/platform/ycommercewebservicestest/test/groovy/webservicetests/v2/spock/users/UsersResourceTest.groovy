/**
 *
 */
package de.hybris.platform.ycommercewebservicestest.test.groovy.webservicetests.v2.spock.users

import static groovyx.net.http.ContentType.JSON
import static groovyx.net.http.ContentType.URLENC
import static groovyx.net.http.ContentType.XML
import static org.apache.http.HttpStatus.SC_BAD_REQUEST
import static org.apache.http.HttpStatus.SC_CREATED
import static org.apache.http.HttpStatus.SC_MOVED_TEMPORARILY

import de.hybris.bootstrap.annotations.ManualTest

import spock.lang.Unroll
import groovyx.net.http.HttpResponseDecorator

/**
 *
 * This class focuses on tests more related to resource like registering users
 *
 */
@ManualTest
@Unroll
class UsersResourceTest extends AbstractUserTest {

	def "Register user with all required data when request: #requestFormat and response: #responseFormat"() {
		given: "authenticated client"
		authorizeClient(restClient)

		when: "user attempts to register"
		def response = restClient.post(
				path: getBasePathWithSite() + '/users',
				body: postBody,
				contentType: responseFormat,
				requestContentType: requestFormat)

		then: "account is created"
		with(response) {
			if (isNotEmpty(data)) println data
			status == SC_CREATED
		}

		where:
		requestFormat | responseFormat | postBody
		URLENC        | JSON           | ['login': System.currentTimeMillis() + '@urlenc2json.pl', 'password': 'haslo', 'firstName': 'Jan', 'lastName': 'Kowalski', 'titleCode': 'mr']
		URLENC        | XML            | ['login': System.currentTimeMillis() + '@urlenc2xml.pl', 'password': 'haslo', 'firstName': 'Jan', 'lastName': 'Kowalski', 'titleCode': 'mr']
		JSON          | JSON           | '{"uid": "' + System.currentTimeMillis() + '@json2json.pl", "password": "haslo", "firstName": "Jan", "lastName": "Kowalski", "titleCode": "mr"}'
		XML           | XML            | "<user><uid>${System.currentTimeMillis()}@xml2xml.pl</uid><password>haslo</password><firstName>Jan</firstName><lastName>Kowalski</lastName><titleCode>mr</titleCode></user>"
	}

	def "Register user using HTTP : #format"() {
		given: "a trusted client"
		authorizeTrustedClient(restClient)
		// YTODO : find a better way to disable automatic redirects
		restClient.setUri(getDefaultHttpUri())

		when: "user attempts to register using HTTP"
		def randomUID = System.currentTimeMillis()
		HttpResponseDecorator response = restClient.post(
				path: getBasePathWithSite() + '/users/',
				body: [
						'login': randomUID + '@test.v2.com',
						'password': CUSTOMER_PASSWORD,
						'firstName': CUSTOMER_FIRST_NAME,
						'lastName': CUSTOMER_LAST_NAME,
						'titleCode': CUSTOMER_TITLE_CODE
				],
				contentType: format,
				requestContentType: URLENC)

		then: "he is not allowed to do so"
		with(response) { status == SC_MOVED_TEMPORARILY }

		where:
		format << [XML, JSON]
	}

	def "Register user with duplicate ID : #format"() {
		given: "a trusted client"
		authorizeTrustedClient(restClient)

		when: "user registers account"
		def randomUID = System.currentTimeMillis()
		with(restClient.post(
				path: getBasePathWithSite() + '/users/',
				body: [
						'login': randomUID + '@test.v2.com',
						'password': CUSTOMER_PASSWORD,
						'firstName': CUSTOMER_FIRST_NAME,
						'lastName': CUSTOMER_LAST_NAME,
						'titleCode': CUSTOMER_TITLE_CODE
				],
				contentType: format,
				requestContentType: URLENC)) {
			if (isNotEmpty(data) && isNotEmpty(data.errors)) println(data)
			status == SC_CREATED
		}

		and: "tries to register account with the same login"
		HttpResponseDecorator response = restClient.post(
				path: getBasePathWithSite() + '/users/',
				body: [
						'login': randomUID + '@test.v2.com',
						'password': CUSTOMER_PASSWORD,
						'firstName': CUSTOMER_FIRST_NAME,
						'lastName': CUSTOMER_LAST_NAME,
						'titleCode': CUSTOMER_TITLE_CODE
				],
				contentType: format,
				requestContentType: URLENC)

		then: "it is not allowed to register same login twice"
		with(response) {
			status == SC_BAD_REQUEST
			isNotEmpty(data.errors)
			data.errors[0].type == 'DuplicateUidError'
		}

		where:
		format << [XML, JSON]
	}

	def "Login a non-existing user : #format"() {
		given: "a trusted client"
		authorizeTrustedClient(restClient)

		when: "a non-existing customer tries to log in"
		HttpResponseDecorator response = restClient.post(
				uri: getOAuth2TokenUri(),
				path: getOAuth2TokenPath(),
				body: [
						'grant_type': 'password',
						'client_id': clientId,
						'client_secret': clientSecret,
						'username': 'nonExistingUser@hybris.com',
						'password': 'password'
				],
				contentType: JSON,
				requestContentType: URLENC)

		then: "he gets an error"
		with(response) {
			status == SC_BAD_REQUEST
			isNotEmpty(data.errors)
			data.errors[0].message == 'Bad credentials'
		}
		where:
		format << [XML, JSON]
	}

	def "Login with wrong password : #format"() {
		given: "a trusted client"
		authorizeTrustedClient(restClient)

		when: "a non-existing customer tries to log in"
		HttpResponseDecorator response = restClient.post(
				uri: getOAuth2TokenUri(),
				path: getOAuth2TokenPath(),
				body: [
						'grant_type': 'password',
						'client_id': clientId,
						'client_secret': clientSecret,
						'username': CUSTOMER_USERNAME,
						'password': 'this_is_wrong'
				],
				contentType: JSON,
				requestContentType: URLENC)

		then: "he gets an error"
		with(response) {
			status == SC_BAD_REQUEST
			isNotEmpty(data.errors)
			data.errors[0].message == 'Bad credentials'
		}
		where:
		format << [XML, JSON]
	}

}