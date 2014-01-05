import static com.axeda.sdk.v2.dsl.Bridges.*
import javax.activation.MimetypesFileTypeMap
import com.axeda.services.v2.*
import com.axeda.sdk.v2.exception.*
import com.axeda.drm.sdk.scripto.Request
import groovy.json.JsonSlurper
import com.axeda.drm.sdk.Context
 
 def response
/**
* vgs_GetLastFile.groovy
* -----------------------
*
* Gets the last uploaded file
*
* @params via POST
*  modelNumber (REQUIRED) Str - the model of the asset
*  serialNumber (REQUIRED) Str - new alarm state
*  tag (OPTIONAL) Str - the tag of the file
*
* @note Either the modelnumber/serialnumber or the tag must be provided.
*
* @author sara streeter <sstreeter@axeda.com>
 *
*/
 
def contentType = "application/json"


/*
POST
{
  "modelNumber": "KONTRON_M2MDev",
  "serialNumber": "00B3386EDAC3"
  "tag": "3",
  "excludealarm":["nametoexclude"]
  }
  
  Which returns me
{
  "modelNumber": "KONTRON_M2MDev",
  "serialNumber": "00B3386EDAC3"
  "maxrows": "3",
"alarms":[{"name":"Alarm1","timestamp":"123456"}, {"name":"Alarm2","timestamp":"123455"}]
}
 */
 StringBuilder outputString = new StringBuilder()
 
 try {    
        def body = Request.body

        def knowntypes = [
         [png: 'image/png']
        ,[gif: 'image/gif']
        ,[jpg: 'image/jpeg']
        ,[wav: 'audio/wav']
        ,[mp3: 'audio/mp3']
        ,[ogg: 'video/ogg']
        ,[vorbis: 'audio/vorbis']
		,[mpeg: 'video/mpeg']
		,[mp4: 'video/mp4']
		,[mpeg: 'video/mpeg']
		,[quicktime: 'video/quicktime']
		,[flv: 'video/x-flv']
		,[wmv: 'video/x-ms-wmv']
    ]

    // Uusing the V1 SDK Pattern here. Everything centers around the "Context"
    final def CONTEXT = Context.getSDKContext()
 
    // parse
    def jsonIn = new JsonSlurper().parseText(body)
    def modelNumber = jsonIn.modelNumber
    def serialNumber = jsonIn.serialNumber
	def tag = jsonIn.tag ? jsonIn.tag : "*${serialNumber}*"
    logger.info(tag)
    def findfileresult = fileInfoBridge.find(new FileInfoCriteria(tags: tag, sortAscending: true))
     def latestId = 0
     def latestName = ""

     if (findfileresult.files.size() > 0){
        findfileresult.files.each() { file ->
            if ((file.id as int) > latestId) {
            latestName = file.filename
            latestId = file.id as int
            }
        }
     } else {
        latestId = findfileresult.files.first().id as int
        latestName = findfileresult.files.first().filename
     }

    def fileext = latestName.substring(latestName.indexOf('.') + 1,latestName.size())
    logger.info(fileext)
    def url = "/services/v1/rest/Scripto/execute/vgs_DownloadFile?fileId=${latestId}"
    def type = returnType(knowntypes, fileext)

    outputString.append("{\n  \"url\" :\"${url}\",\"type\" :\"${type}\" \n}")
}
catch (Exception e){
    logger.info(e.localizedMessage)   
}
 
return ["Content-Type": contentType,"Content":outputString]
 
static byte[] getBytes(File file) throws IOException {
	return getBytes(new FileInputStream(file));
  }
 
static byte[] getBytes(InputStream is) throws IOException {
	ByteArrayOutputStream answer = new ByteArrayOutputStream();
	// reading the content of the file within a byte buffer
	byte[] byteBuffer = new byte[8192];
	int nbByteRead /* = 0*/;
	try {
	  while ((nbByteRead = is.read(byteBuffer)) != -1) {
		// appends buffer
		answer.write(byteBuffer, 0, nbByteRead);
	  }
	} finally {
	  is.close()
	}
	return answer.toByteArray();
  }
  
  def returnType(knowntypes, ext){
      return knowntypes.find{ it.containsKey(ext) }?."$ext"
  }
