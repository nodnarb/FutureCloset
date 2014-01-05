import groovyx.net.http.ContentType
import groovyx.net.http.HTTPBuilder
import org.apache.commons.lang.exception.ExceptionUtils

import static groovyx.net.http.Method.POST

/**
 * Created with IntelliJ IDEA.
 * User: kholbrook
 * Date: 12/26/13
 * Time: 4:18 PM
 * To change this template use File | Settings | File Templates.
 */

/*
    Expected result from external oauth call :
    {
        "access_token":"UDQgj6dKGFrrOGHrO09yWhIS3Gv6koyR",
        "token_type":"bearer",
        "expires_in":157680000,
        "refresh_token":"OEomlL8RQpi8QlxPI0xppWaCDVzgO9yL"
    }

    Expected response from this service :
    {
        "access_token":"UDQgj6dKGFrrOGHrO09yWhIS3Gv6koyR",
        "refresh_token":"OEomlL8RQpi8QlxPI0xppWaCDVzgO9yL",
        "error":"usually empty"
    }
*/

def responseString = new StringBuilder()
def scope = "MMS,SMS,SPEECH,STTC,TTS"

try {

/*
https://api.att.com/oauth/token?
grant_type=client_credentials&client_id=&client_secret=&scope=SMS

 */
    def http = new HTTPBuilder("https://api.att.com/oauth/")

//    http.client.params.setBooleanParameter 'http.protocol.handle-authentication', false
    http.request(POST) { req ->
        uri.path = 'token'
        requestContentType = ContentType.URLENC
        body = ["client_id": "tnthv9fiai556mnmf02a5snkrepsfcjp", "client_secret": "ij1azdlde8tkbo0yi4t9kn8c94x2b1yu", "scope": "${scope}", "grant_type": "client_credentials"]
        headers.Accept = 'application/json'

        response.success = { resp, json ->
            assert resp.statusLine.statusCode == 200
            responseString.append "{\n\t\"access_token\": \"${json?.access_token}\",\n"
            responseString.append "\t\"refresh_token\": \"${json?.refresh_token}\",\n"
            responseString.append "\t\"error\": \"\"\n}"
        }

        response.failure = { resp ->
            responseString.append "{\n\t\"access_token\": \"\",\n"
            responseString.append "\t\"refresh_token\": \"\",\n"
            responseString.append "\t\"error\": \"HTTP Status $resp.status,$resp.statusLine\"\n}"
        }
    }
} catch (Exception e) {
    def stack = ExceptionUtils.getFullStackTrace(e)
    responseString.append "{\n\t\"access_token\": \"\",\n"
    responseString.append "\t\"refresh_token\": \"\",\n"
    responseString.append "\t\"error\": \"FAILED: Exception occurred: ${e.message}, ${stack}\"\n}"
}

return ["Content-Type":"text/plain","Content":responseString.toString()]